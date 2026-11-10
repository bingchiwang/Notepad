package com.example.notepad.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 應用程式主題模式列舉（規範 4.5：支援淺色模式、深色模式、跟隨系統模式）
 */
enum class ThemeMode(val label: String) {
    SYSTEM("跟隨系統"),
    LIGHT("淺色模式"),
    DARK("深色模式");

    /**
     * 判斷當前主題設定是否應顯示深色介面
     */
    fun shouldUseDarkTheme(isSystemInDarkTheme: Boolean): Boolean {
        return when (this) {
            LIGHT -> false
            DARK -> true
            SYSTEM -> isSystemInDarkTheme
        }
    }

    companion object {
        /**
         * 依據儲存字串還原主題模式，若格式無效則預設跟隨系統
         */
        fun fromString(value: String?): ThemeMode {
            if (value.isNullOrBlank()) return SYSTEM
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) } ?: SYSTEM
        }
    }
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

/**
 * DataStore 主題設定持久化管理（規範 4.5）
 *
 * 確保使用者切換主題後跨所有頁面保持一致，並於重新開啟 App 後維持設定。
 */
class ThemePreferences(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.themeDataStore)

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /**
     * 響應式取得當前主題模式串流（預設跟隨系統）
     */
    val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromString(preferences[KEY_THEME_MODE])
    }

    /**
     * 儲存主題模式至 DataStore 持久化檔案
     */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = themeMode.name
        }
    }
}
