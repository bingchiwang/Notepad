package com.example.notepad.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cloud_sync_preferences"
)

/**
 * Firebase 雲端同步設定與最後同步狀態模型（資料同步規則 3、5）
 */
data class SyncSettings(
    val syncEnabled: Boolean = true,
    val syncAccountId: String = "",
    val firebaseProjectId: String = "",
    val firebaseAppId: String = "",
    val firebaseApiKey: String = "",
    val lastSyncAt: Long? = null,
    val lastSyncMessage: String = "尚未同步",
    val settingsUpdatedAt: Long = 0L,
    val settingsSyncPending: Boolean = false
) {
    /**
     * 檢查是否已於介面設定手動 Firebase 專案連線參數
     */
    fun hasManualFirebaseConfig(): Boolean {
        return firebaseProjectId.isNotBlank() &&
            firebaseAppId.isNotBlank() &&
            firebaseApiKey.isNotBlank()
    }
}

/**
 * 雲端同步設定與最後同步時間 DataStore 持久化管理
 */
class SyncPreferences(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.syncDataStore)

    companion object {
        private val KEY_SYNC_ENABLED = booleanPreferencesKey("sync_enabled")
        private val KEY_SYNC_ACCOUNT_ID = stringPreferencesKey("sync_account_id")
        private val KEY_FIREBASE_PROJECT_ID = stringPreferencesKey("firebase_project_id")
        private val KEY_FIREBASE_APP_ID = stringPreferencesKey("firebase_app_id")
        private val KEY_FIREBASE_API_KEY = stringPreferencesKey("firebase_api_key")
        private val KEY_LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        private val KEY_LAST_SYNC_MESSAGE = stringPreferencesKey("last_sync_message")
        private val KEY_SETTINGS_UPDATED_AT = longPreferencesKey("settings_updated_at")
        private val KEY_SETTINGS_SYNC_PENDING = booleanPreferencesKey("settings_sync_pending")
    }

    val syncSettingsFlow: Flow<SyncSettings> = dataStore.data.map { preferences ->
        val rawLastSyncAt = preferences[KEY_LAST_SYNC_AT] ?: 0L
        SyncSettings(
            syncEnabled = preferences[KEY_SYNC_ENABLED] ?: true,
            syncAccountId = preferences[KEY_SYNC_ACCOUNT_ID] ?: "",
            firebaseProjectId = preferences[KEY_FIREBASE_PROJECT_ID] ?: "",
            firebaseAppId = preferences[KEY_FIREBASE_APP_ID] ?: "",
            firebaseApiKey = preferences[KEY_FIREBASE_API_KEY] ?: "",
            lastSyncAt = if (rawLastSyncAt > 0L) rawLastSyncAt else null,
            lastSyncMessage = preferences[KEY_LAST_SYNC_MESSAGE] ?: "尚未同步",
            settingsUpdatedAt = preferences[KEY_SETTINGS_UPDATED_AT] ?: 0L,
            settingsSyncPending = preferences[KEY_SETTINGS_SYNC_PENDING] ?: false
        )
    }

    suspend fun getSyncSettingsOnce(): SyncSettings = syncSettingsFlow.first()

    /**
     * 若尚未分配同步空間代碼，自動產生一組穩定且可分享給其他裝置的預設代碼
     */
    suspend fun ensureSyncAccountId(): String {
        val current = getSyncSettingsOnce().syncAccountId
        if (current.isNotBlank()) return current
        val generated = "notepad-" + UUID.randomUUID().toString().take(8)
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_ACCOUNT_ID] = generated
        }
        return generated
    }

    suspend fun updateConfiguration(
        syncEnabled: Boolean,
        syncAccountId: String,
        firebaseProjectId: String,
        firebaseAppId: String,
        firebaseApiKey: String
    ) {
        val normalizedAccount = syncAccountId.trim().ifBlank {
            "notepad-" + UUID.randomUUID().toString().take(8)
        }
        dataStore.edit { prefs ->
            prefs[KEY_SYNC_ENABLED] = syncEnabled
            prefs[KEY_SYNC_ACCOUNT_ID] = normalizedAccount
            prefs[KEY_FIREBASE_PROJECT_ID] = firebaseProjectId.trim()
            prefs[KEY_FIREBASE_APP_ID] = firebaseAppId.trim()
            prefs[KEY_FIREBASE_API_KEY] = firebaseApiKey.trim()
        }
    }

    /**
     * 標記使用者設定（主題／通知開關）於本機有異動，等待同步上傳
     */
    suspend fun markSettingsPending(updatedAt: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            prefs[KEY_SETTINGS_UPDATED_AT] = updatedAt
            prefs[KEY_SETTINGS_SYNC_PENDING] = true
        }
    }

    /**
     * 套用雲端設定時同步紀錄更新時間並清除待同步旗標
     */
    suspend fun markSettingsSynced(updatedAt: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_SETTINGS_UPDATED_AT] = updatedAt
            prefs[KEY_SETTINGS_SYNC_PENDING] = false
        }
    }

    /**
     * 同步成功後更新最後同步時間與狀態訊息（資料同步規則 5）
     */
    suspend fun recordSyncSuccess(syncedAt: Long, message: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_AT] = syncedAt
            prefs[KEY_LAST_SYNC_MESSAGE] = message
            prefs[KEY_SETTINGS_SYNC_PENDING] = false
        }
    }

    /**
     * 紀錄同步暫緩或失敗狀態（不覆寫既有的 lastSyncAt 成功時間）
     */
    suspend fun recordSyncStatusMessage(message: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SYNC_MESSAGE] = message
        }
    }
}
