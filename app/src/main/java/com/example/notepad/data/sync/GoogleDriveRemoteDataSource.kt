package com.example.notepad.data.sync

import com.example.notepad.data.preferences.SyncSettings
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Google 雲端硬碟（Google Drive REST API v3 `appDataFolder`）遠端同步資料來源
 *
 * 將本 App 的所有記事、分類、待辦、提醒、時間規劃表、番茄鐘與個人化設定
 * 封裝為專屬隱藏空間（`appDataFolder`）內的 JSON 快照檔案，不佔用使用者可見雲端硬碟資料夾，
 * 且會在增量上傳（`upsertDocuments`）時自動合併既有的雲端文件，確保未變動之紀錄完整保留。
 */
class GoogleDriveRemoteDataSource(
    private val tokenProvider: suspend (SyncSettings) -> String,
    httpClient: OkHttpClient? = null,
    private val baseUrl: String = "https://www.googleapis.com"
) : CloudRemoteDataSource {

    // 延遲初始化 OkHttpClient，降低冷啟動記憶體佔用並避免未同步時佔用網路資源
    private val httpClient: OkHttpClient by lazy {
        httpClient ?: OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    constructor(authHelper: GoogleDriveAuthHelper) : this(
        tokenProvider = { settings -> authHelper.resolveFreshAccessToken(settings) }
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=UTF-8".toMediaType()
    }

    override fun isConfigured(settings: SyncSettings): Boolean {
        return settings.hasGoogleDriveConfig()
    }

    override suspend fun fetchAllDocuments(
        accountId: String,
        settings: SyncSettings
    ): List<CloudSyncDocument> = withContext(Dispatchers.IO) {
        val token = tokenProvider(settings).trim()
        require(token.isNotBlank()) { "尚未取得 Google 雲端硬碟授權權杖" }

        val fileName = settings.resolveGoogleDriveFileName()
        val fileId = findSyncFileId(fileName = fileName, accessToken = token) ?: return@withContext emptyList()
        downloadFileDocuments(fileId = fileId, accessToken = token)
    }

    override suspend fun upsertDocuments(
        accountId: String,
        settings: SyncSettings,
        documents: List<CloudSyncDocument>
    ) = withContext(Dispatchers.IO) {
        if (documents.isEmpty()) return@withContext
        val token = tokenProvider(settings).trim()
        require(token.isNotBlank()) { "尚未取得 Google 雲端硬碟授權權杖" }

        val fileName = settings.resolveGoogleDriveFileName()
        val existingFileId = findSyncFileId(fileName = fileName, accessToken = token)

        // 先讀取雲端現有文件並依 documentId 覆蓋增量異動，避免只上傳 delta 時覆蓋掉既有 NO_OP 資料
        val mergedById = linkedMapOf<String, CloudSyncDocument>()
        if (existingFileId != null) {
            downloadFileDocuments(fileId = existingFileId, accessToken = token).forEach { existingDoc ->
                mergedById[existingDoc.documentId] = existingDoc
            }
        }
        documents.forEach { incomingDoc ->
            mergedById[incomingDoc.documentId] = incomingDoc
        }

        val payloadJson = GoogleDriveSyncPayloadSerializer.serializeSnapshot(mergedById.values.toList())
        if (existingFileId == null) {
            createSnapshotFile(fileName = fileName, snapshotJson = payloadJson, accessToken = token)
        } else {
            updateSnapshotFile(fileId = existingFileId, snapshotJson = payloadJson, accessToken = token)
        }
    }

    private fun findSyncFileId(fileName: String, accessToken: String): String? {
        val escapedName = fileName.replace("'", "\\'")
        val url = "$baseUrl/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("fields", "files(id,name,modifiedTime)")
            .addQueryParameter("pageSize", "10")
            .addQueryParameter("q", "name = '$escapedName' and trashed = false")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            ensureSuccessfulResponse(response.code, body, "查詢 Google 雲端硬碟同步檔案")
            return parseFirstFileId(body)
        }
    }

    private fun downloadFileDocuments(fileId: String, accessToken: String): List<CloudSyncDocument> {
        val url = "$baseUrl/drive/v3/files/$fileId".toHttpUrl().newBuilder()
            .addQueryParameter("alt", "media")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            ensureSuccessfulResponse(response.code, body, "下載 Google 雲端硬碟同步資料")
            return GoogleDriveSyncPayloadSerializer.deserializeSnapshot(body)
        }
    }

    private fun createSnapshotFile(fileName: String, snapshotJson: String, accessToken: String) {
        val boundary = "lunar_notepad_sync_${UUID.randomUUID()}"
        val metadataJson = buildFileMetadataJson(fileName)
        val multipartBodyString = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(snapshotJson).append("\r\n")
            append("--").append(boundary).append("--\r\n")
        }

        val url = "$baseUrl/upload/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", "id")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(multipartBodyString.toRequestBody("multipart/related; boundary=$boundary".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            ensureSuccessfulResponse(response.code, body, "建立 Google 雲端硬碟同步檔案")
        }
    }

    private fun updateSnapshotFile(fileId: String, snapshotJson: String, accessToken: String) {
        val url = "$baseUrl/upload/drive/v3/files/$fileId".toHttpUrl().newBuilder()
            .addQueryParameter("uploadType", "media")
            .build()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .patch(snapshotJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body.string()
            ensureSuccessfulResponse(response.code, body, "更新 Google 雲端硬碟同步檔案")
        }
    }

    private fun buildFileMetadataJson(fileName: String): String {
        val buffer = Buffer()
        JsonWriter.of(buffer).use { writer ->
            writer.beginObject()
            writer.name("name").value(fileName)
            writer.name("mimeType").value("application/json")
            writer.name("parents")
            writer.beginArray()
            writer.value("appDataFolder")
            writer.endArray()
            writer.endObject()
        }
        return buffer.readUtf8()
    }

    private fun parseFirstFileId(json: String): String? {
        if (json.isBlank()) return null
        val buffer = Buffer().writeUtf8(json)
        JsonReader.of(buffer).use { reader ->
            if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) return null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "files" -> {
                        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                            reader.beginArray()
                            var foundId: String? = null
                            while (reader.hasNext()) {
                                val id = readIdFromFileObject(reader)
                                if (foundId == null && !id.isNullOrBlank()) {
                                    foundId = id
                                }
                            }
                            reader.endArray()
                            return foundId
                        } else {
                            reader.skipValue()
                        }
                    }

                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        return null
    }

    private fun readIdFromFileObject(reader: JsonReader): String? {
        if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) {
            reader.skipValue()
            return null
        }
        var id: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = if (reader.peek() == JsonReader.Token.NULL) {
                    reader.nextNull<Any>()
                    null
                } else {
                    reader.nextString()
                }

                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return id
    }

    private fun ensureSuccessfulResponse(code: Int, body: String, actionLabel: String) {
        if (code in 200..299) return
        when (code) {
            401 -> throw IllegalStateException("Google 雲端硬碟授權已失效（401），請重新連結 Google 帳號")
            403 -> throw IllegalStateException("Google 雲端硬碟存取被拒（403），請確認已在 Google Cloud Console 啟用 Drive API 與授權範圍")
            else -> {
                val detail = extractErrorMessage(body).ifBlank { body.take(180) }
                throw IllegalStateException("${actionLabel}失敗（HTTP $code）：$detail")
            }
        }
    }

    private fun extractErrorMessage(json: String): String {
        if (json.isBlank()) return ""
        return runCatching {
            val buffer = Buffer().writeUtf8(json)
            var message = ""
            JsonReader.of(buffer).use { reader ->
                if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) return@runCatching ""
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "error" -> {
                            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "message" -> message = reader.nextString()
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            } else {
                                reader.skipValue()
                            }
                        }

                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
            }
            message
        }.getOrDefault("")
    }
}
