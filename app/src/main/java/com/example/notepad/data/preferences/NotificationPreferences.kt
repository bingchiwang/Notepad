package com.example.notepad.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

/**
 * DataStore 通知總開關設定持久化管理（規範 4.4）
 *
 * 關閉通知功能後，系統不再安排或觸發新提醒通知。
 */
class NotificationPreferences(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.notificationDataStore)

    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    /**
     * 響應式取得通知總開關狀態（預設開啟）
     */
    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    /**
     * 單次讀取通知總開關狀態
     */
    suspend fun isNotificationsEnabled(): Boolean {
        return notificationsEnabledFlow.first()
    }

    /**
     * 更新並儲存通知總開關狀態
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
