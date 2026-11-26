package com.example.notepad.data.sync

import com.example.notepad.data.local.entity.SyncEntityType
import com.example.notepad.data.preferences.CloudBackendProvider
import com.example.notepad.data.preferences.SyncSettings
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [GoogleDriveRemoteDataSource] 單元測試：
 * 驗證 Google Drive appDataFolder 檔案建立、增量更新合併既有 NO_OP 資料、檔案命名區隔與授權失敗提示。
 */
class GoogleDriveRemoteDataSourceTest {

    private class FakeDriveServerInterceptor : Interceptor {
        var existingFileId: String? = null
        var storedFileName: String? = null
        var storedSnapshotJson: String? = null
        var forceStatusCode: Int? = null
        val recordedRequests = mutableListOf<String>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val path = request.url.encodedPath
            val method = request.method
            recordedRequests.add("$method $path")

            val forcedCode = forceStatusCode
            if (forcedCode != null) {
                return buildResponse(
                    chain = chain,
                    code = forcedCode,
                    body = """{"error":{"message":"Simulated HTTP $forcedCode"}}"""
                )
            }

            assertEquals("Bearer test-drive-token", request.header("Authorization"))

            return when {
                method == "GET" && path == "/drive/v3/files" -> {
                    val body = if (existingFileId != null) {
                        """{"files":[{"id":"$existingFileId","name":"$storedFileName"}]}"""
                    } else {
                        """{"files":[]}"""
                    }
                    buildResponse(chain, 200, body)
                }

                method == "GET" && path == "/drive/v3/files/$existingFileId" -> {
                    buildResponse(chain, 200, storedSnapshotJson ?: "{}")
                }

                method == "POST" && path == "/upload/drive/v3/files" -> {
                    val bodyText = readBodyText(request.body)
                    existingFileId = "drive-file-100"
                    storedSnapshotJson = extractMultipartJsonPayload(bodyText)
                    buildResponse(chain, 200, """{"id":"drive-file-100"}""")
                }

                method == "PATCH" && path == "/upload/drive/v3/files/$existingFileId" -> {
                    storedSnapshotJson = readBodyText(request.body)
                    buildResponse(chain, 200, """{"id":"$existingFileId"}""")
                }

                else -> buildResponse(chain, 404, """{"error":{"message":"Unexpected endpoint"}}""")
            }
        }

        private fun readBodyText(body: okhttp3.RequestBody?): String {
            if (body == null) return ""
            val buffer = Buffer()
            body.writeTo(buffer)
            return buffer.readUtf8()
        }

        private fun extractMultipartJsonPayload(multipartBody: String): String {
            val marker = "Content-Type: application/json; charset=UTF-8\r\n\r\n"
            val firstIdx = multipartBody.indexOf(marker)
            val secondIdx = multipartBody.indexOf(marker, startIndex = firstIdx + marker.length)
            require(firstIdx >= 0 && secondIdx > firstIdx) { "Multipart metadata/payload parts missing" }

            val metadataPart = multipartBody.substring(firstIdx + marker.length, multipartBody.indexOf("\r\n--", firstIdx))
            // 驗證 parents 包含 appDataFolder 且寫入正確檔名
            assertTrue(metadataPart.contains("\"appDataFolder\""))
            val nameRegex = Regex("\"name\":\"([^\"]+)\"")
            storedFileName = nameRegex.find(metadataPart)?.groupValues?.getOrNull(1)

            val payloadStart = secondIdx + marker.length
            val payloadEnd = multipartBody.indexOf("\r\n--", payloadStart)
            return multipartBody.substring(payloadStart, payloadEnd)
        }

        private fun buildResponse(chain: Interceptor.Chain, code: Int, body: String): Response {
            return Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("OK")
                .body(body.toResponseBody("application/json; charset=UTF-8".toMediaType()))
                .build()
        }
    }

    @Test
    fun fetchAndUpsert_createsSnapshotAndPreservesUnchangedDocumentsAcrossDeltaUpsert() = runTest {
        val fakeServer = FakeDriveServerInterceptor()
        val client = OkHttpClient.Builder().addInterceptor(fakeServer).build()
        val dataSource = GoogleDriveRemoteDataSource(
            tokenProvider = { "test-drive-token" },
            httpClient = client,
            baseUrl = "https://mock.drive.local"
        )
        val settings = SyncSettings(
            cloudProvider = CloudBackendProvider.GOOGLE_DRIVE,
            googleAccountEmail = "user@example.com",
            googleDriveSpaceKey = ""
        )

        assertTrue(dataSource.isConfigured(settings))
        assertTrue(dataSource.fetchAllDocuments("acct", settings).isEmpty())

        // 首次上傳兩筆文件（記事 + 待辦）
        val note1 = CloudSyncDocument(
            entityType = SyncEntityType.NOTE,
            syncId = "n-1",
            updatedAt = 1000L,
            payload = mapOf("title" to "記事一")
        )
        val todo1 = CloudSyncDocument(
            entityType = SyncEntityType.TODO,
            syncId = "t-1",
            updatedAt = 1000L,
            payload = mapOf("title" to "待辦一", "isCompleted" to false)
        )
        dataSource.upsertDocuments("acct", settings, listOf(note1, todo1))
        assertEquals("lunar_notepad_sync_v1.json", fakeServer.storedFileName)

        // 第二次僅增量上傳 todo1 更新與新增 note2（note1 應維持保留，不得遺失）
        val todo1Updated = todo1.copy(
            updatedAt = 2000L,
            payload = mapOf("title" to "待辦一（完成）", "isCompleted" to true)
        )
        val note2 = CloudSyncDocument(
            entityType = SyncEntityType.NOTE,
            syncId = "n-2",
            updatedAt = 2500L,
            payload = mapOf("title" to "記事二")
        )
        dataSource.upsertDocuments("acct", settings, listOf(todo1Updated, note2))

        val remoteDocs = dataSource.fetchAllDocuments("acct", settings)
        assertEquals(3, remoteDocs.size)
        val byId = remoteDocs.associateBy { it.documentId }
        assertEquals("記事一", byId["NOTE_n-1"]?.payload?.get("title"))
        assertEquals(true, byId["TODO_t-1"]?.payload?.get("isCompleted"))
        assertEquals("記事二", byId["NOTE_n-2"]?.payload?.get("title"))
        assertTrue(fakeServer.recordedRequests.any { it.startsWith("POST /upload/drive/v3/files") })
        assertTrue(fakeServer.recordedRequests.any { it.startsWith("PATCH /upload/drive/v3/files/drive-file-100") })
    }

    @Test
    fun unauthorizedResponse_throwsActionableTraditionalChineseMessage() = runTest {
        val fakeServer = FakeDriveServerInterceptor().apply {
            forceStatusCode = 401
        }
        val client = OkHttpClient.Builder().addInterceptor(fakeServer).build()
        val dataSource = GoogleDriveRemoteDataSource(
            tokenProvider = { "test-drive-token" },
            httpClient = client,
            baseUrl = "https://mock.drive.local"
        )
        val settings = SyncSettings(googleAccountEmail = "user@example.com")

        val ex = runCatching { dataSource.fetchAllDocuments("acct", settings) }.exceptionOrNull()
        assertTrue(ex is IllegalStateException)
        assertTrue(ex?.message?.contains("401") == true)
        assertTrue(ex?.message?.contains("重新連結 Google 帳號") == true)
    }
}
