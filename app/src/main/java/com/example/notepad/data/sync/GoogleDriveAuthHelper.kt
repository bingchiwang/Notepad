package com.example.notepad.data.sync

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentSender
import com.example.notepad.data.preferences.SyncSettings
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.squareup.moshi.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 透過 Google Drive `about` 端點取得之已登入帳號基本資料與權杖
 */
data class GoogleAccountProfile(
    val email: String,
    val displayName: String,
    val accessToken: String
)

/**
 * 互動式 Google 雲端硬碟授權起始結果
 */
sealed class GoogleDriveAuthStartResult {
    /** 已直接取得有效授權權杖與帳號資訊 */
    data class Authorized(val profile: GoogleAccountProfile) : GoogleDriveAuthStartResult()

    /** 需要跳出 Google 官方授權同意畫面 */
    data class NeedsUserResolution(val intentSender: IntentSender) : GoogleDriveAuthStartResult()
}

/**
 * Google 雲端硬碟（`drive.appdata`）OAuth 2.0 授權管理員
 *
 * 使用 Google Play Services `Identity.getAuthorizationClient` 申請 App 專屬隱藏空間權限，
 * 不需申請危險的通訊錄帳號讀取權限，即可安全取得存取權杖並查詢使用者信箱。
 */
class GoogleDriveAuthHelper(
    context: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val appContext = context.applicationContext

    companion object {
        const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }

    /**
     * 從 UI 啟動 Google 雲端硬碟授權流程（若已同意過會直接回傳權杖，否則回傳 Consent IntentSender）
     */
    suspend fun beginAuthorization(activityContext: Context): GoogleDriveAuthStartResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
            .build()

        val authClient = activityContext.findActivity()?.let {
            Identity.getAuthorizationClient(it)
        } ?: Identity.getAuthorizationClient(activityContext)

        val result = try {
            awaitTask(authClient.authorize(request))
        } catch (ex: Exception) {
            throw IllegalStateException(mapAuthorizationError(ex), ex)
        }

        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent
                ?: throw IllegalStateException("Google 授權流程未回傳同意畫面，請稍後再試")
            return GoogleDriveAuthStartResult.NeedsUserResolution(pendingIntent.intentSender)
        }

        val token = result.accessToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("未能取得 Google 雲端硬碟存取權杖，請重新嘗試連結")
        val profile = fetchUserProfileWithToken(token)
        return GoogleDriveAuthStartResult.Authorized(profile)
    }

    /**
     * 解析使用者完成 Google Consent 畫面後回傳的 [Intent]
     */
    suspend fun completeAuthorizationFromResult(
        activityContext: Context,
        resultData: Intent?
    ): GoogleAccountProfile {
        val authClient = activityContext.findActivity()?.let {
            Identity.getAuthorizationClient(it)
        } ?: Identity.getAuthorizationClient(activityContext)

        val result = try {
            authClient.getAuthorizationResultFromIntent(resultData)
        } catch (ex: Exception) {
            throw IllegalStateException(mapAuthorizationError(ex), ex)
        }

        val token = result.accessToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("未取得 Google 雲端硬碟授權，請確認已允許儲存應用程式資料權限")
        return fetchUserProfileWithToken(token)
    }

    /**
     * 供背景或手動同步解析即時可用之 Google OAuth 2.0 Access Token；
     * 優先靜默續簽 GMS 權杖，若使用者填有手動測試權杖則於無法靜默授權時作為備援。
     */
    suspend fun resolveFreshAccessToken(settings: SyncSettings): String {
        val manualToken = settings.googleDriveAccessToken.trim()
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
            .build()

        val gmsAttempt = runCatching {
            awaitTask(Identity.getAuthorizationClient(appContext).authorize(request))
        }

        val authResult = gmsAttempt.getOrNull()
        if (authResult != null && !authResult.hasResolution()) {
            val freshToken = authResult.accessToken?.takeIf { it.isNotBlank() }
            if (freshToken != null) return freshToken
        }

        if (manualToken.isNotBlank()) {
            return manualToken
        }

        val failure = gmsAttempt.exceptionOrNull()
        if (failure != null) {
            throw IllegalStateException(mapAuthorizationError(failure), failure)
        }
        throw IllegalStateException("Google 帳號授權需要重新確認，請開啟「雲端同步設定」並重新連結 Google 帳號")
    }

    /**
     * 透過 Google Drive `about` API 查詢目前授權帳號的電子郵件與顯示名稱
     */
    suspend fun fetchUserProfileWithToken(accessToken: String): GoogleAccountProfile =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/about?fields=user(emailAddress,displayName)")
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    if (response.code == 401) {
                        throw IllegalStateException("Google 存取權杖無效或已過期（401），請重新取得授權")
                    }
                    if (response.code == 403) {
                        throw IllegalStateException("無權存取 Google Drive 個人資訊（403），請確認已啟用 Google Drive API")
                    }
                    throw IllegalStateException("讀取 Google 帳號資訊失敗（HTTP ${response.code}）")
                }

                val parsed = parseAboutUser(body)
                GoogleAccountProfile(
                    email = parsed.first.ifBlank { "已授權 Google 帳號" },
                    displayName = parsed.second,
                    accessToken = accessToken
                )
            }
        }

    private fun parseAboutUser(json: String): Pair<String, String> {
        if (json.isBlank()) return "" to ""
        var email = ""
        var displayName = ""
        val buffer = Buffer().writeUtf8(json)
        JsonReader.of(buffer).use { reader ->
            if (reader.peek() != JsonReader.Token.BEGIN_OBJECT) return "" to ""
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "user" -> {
                        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "emailAddress" -> email = if (reader.peek() == JsonReader.Token.NULL) {
                                        reader.nextNull<Any>()
                                        ""
                                    } else {
                                        reader.nextString()
                                    }

                                    "displayName" -> displayName = if (reader.peek() == JsonReader.Token.NULL) {
                                        reader.nextNull<Any>()
                                        ""
                                    } else {
                                        reader.nextString()
                                    }

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
        return email to displayName
    }

    private fun mapAuthorizationError(error: Throwable): String {
        val apiEx = error as? ApiException
        return when (apiEx?.statusCode) {
            CommonStatusCodes.DEVELOPER_ERROR ->
                "Google OAuth 設定尚未完成（DEVELOPER_ERROR 10）：請於 Google Cloud Console 建立 Android OAuth 2.0 用戶端 ID（套件 com.example.notepad + SHA-1）並啟用 Google Drive API。"

            CommonStatusCodes.SIGN_IN_REQUIRED ->
                "此裝置尚未登入 Google 帳號，請先於系統設定登入後再試。"

            CommonStatusCodes.NETWORK_ERROR ->
                "Google 授權連線失敗，請檢查網路狀態後重試。"

            CommonStatusCodes.CANCELED ->
                "已取消連結 Google 帳號。"

            CommonStatusCodes.API_NOT_CONNECTED ->
                "裝置缺少 Google Play 服務或版本過舊，無法執行帳號授權。"

            else -> "Google 雲端硬碟授權失敗：${error.message ?: "未知錯誤"}"
        }
    }

    private tailrec fun Context.findActivity(): Activity? {
        return when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }
    }

    private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result)
            }.addOnFailureListener { ex ->
                if (cont.isActive) cont.resumeWithException(ex)
            }.addOnCanceledListener {
                if (cont.isActive) cont.cancel()
            }
        }
}
