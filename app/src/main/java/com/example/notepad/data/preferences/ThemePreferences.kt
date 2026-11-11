package com.example.notepad.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
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

/**
 * 個人化月曆外觀樣式
 */
enum class CalendarStyle(val label: String, val description: String) {
    CLASSIC("經典簡約", "標準圓角卡片與主題清爽配色"),
    INK_WASH("水墨東方", "宣紙暖底搭配硃砂選日圓章，典雅柔和"),
    FOREST_ZEN("青翠森境", "森綠襯底與膠囊日格，自然護眼"),
    OCEAN_NIGHT("湛藍晴空", "海洋晴藍標題搭配正圓選框，明確醒目"),
    SUNSET_WARM("暖陽暮靄", "暖杏暮光卡片與格線框邊，溫暖突出"),
    HIGH_CONTRAST("極簡高對比", "高反差黑白墨線設計，極致清晰可辨");

    companion object {
        fun fromString(value: String?): CalendarStyle {
            if (value.isNullOrBlank()) return CLASSIC
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) } ?: CLASSIC
        }
    }
}

/**
 * 全局字體大小倍率設定
 */
enum class AppFontScale(val label: String, val scaleFactor: Float) {
    SMALL("精巧小 (85%)", 0.85f),
    MEDIUM("標準中 (100%)", 1.0f),
    LARGE("舒適大 (115%)", 1.15f),
    EXTRA_LARGE("特大清晰 (130%)", 1.30f);

    companion object {
        fun fromString(value: String?): AppFontScale {
            if (value.isNullOrBlank()) return MEDIUM
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) } ?: MEDIUM
        }
    }
}

/**
 * 個人化色彩預設選項（涵蓋高亮、高對比與深色色系，測試並展現文字自動反色防護）
 */
data class PersonalizationColorPreset(val name: String, val colorLong: Long)

val PERSONALIZATION_COLOR_PRESETS = listOf(
    PersonalizationColorPreset("經典藍", 0xFF2196F3L),
    PersonalizationColorPreset("深靛藍", 0xFF1A237EL),
    PersonalizationColorPreset("蒂芬妮", 0xFF009688L),
    PersonalizationColorPreset("森林綠", 0xFF2E7D32L),
    PersonalizationColorPreset("琥珀橙", 0xFFFF9800L),
    PersonalizationColorPreset("珊瑚紅", 0xFFF44336L),
    PersonalizationColorPreset("典雅紫", 0xFF9C27B0L),
    PersonalizationColorPreset("明亮黃", 0xFFFFEB3BL),
    PersonalizationColorPreset("櫻花粉", 0xFFF8BBD0L),
    PersonalizationColorPreset("冰雪藍", 0xFF81D4FAL),
    PersonalizationColorPreset("極簡白", 0xFFFFFFFFL),
    PersonalizationColorPreset("曜石黑", 0xFF212121L)
)

/**
 * 完整個人化外觀與偏好資料模型
 */
data class PersonalizationSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val buttonColorLong: Long? = null,
    val calendarStyle: CalendarStyle = CalendarStyle.CLASSIC,
    val calendarAccentColorLong: Long? = null,
    val fontScale: AppFontScale = AppFontScale.MEDIUM
)

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

/**
 * DataStore 主題與個人化設定持久化管理（規範 4.5 & 個人化設定管理）
 *
 * 確保使用者調整主題模式、按鍵顏色、月曆樣式與字體大小後跨所有頁面保持一致，
 * 並於重新開啟 App 後維持設定。
 */
class ThemePreferences(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.themeDataStore)

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_BUTTON_COLOR = longPreferencesKey("button_color_long")
        private val KEY_CALENDAR_STYLE = stringPreferencesKey("calendar_style")
        private val KEY_CALENDAR_ACCENT_COLOR = longPreferencesKey("calendar_accent_color_long")
        private val KEY_FONT_SCALE = stringPreferencesKey("font_scale")
    }

    /**
     * 響應式取得當前主題模式串流（預設跟隨系統）
     */
    val themeModeFlow: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromString(preferences[KEY_THEME_MODE])
    }

    /**
     * 響應式取得完整個人化外觀設定串流
     */
    val personalizationFlow: Flow<PersonalizationSettings> = dataStore.data.map { preferences ->
        PersonalizationSettings(
            themeMode = ThemeMode.fromString(preferences[KEY_THEME_MODE]),
            buttonColorLong = preferences[KEY_BUTTON_COLOR],
            calendarStyle = CalendarStyle.fromString(preferences[KEY_CALENDAR_STYLE]),
            calendarAccentColorLong = preferences[KEY_CALENDAR_ACCENT_COLOR],
            fontScale = AppFontScale.fromString(preferences[KEY_FONT_SCALE])
        )
    }

    /**
     * 儲存主題模式至 DataStore 持久化檔案
     */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = themeMode.name
        }
    }

    /**
     * 儲存自訂按鍵顏色（傳入 null 代表恢復主題預設）
     */
    suspend fun setButtonColor(colorLong: Long?) {
        dataStore.edit { preferences ->
            if (colorLong != null) {
                preferences[KEY_BUTTON_COLOR] = colorLong
            } else {
                preferences.remove(KEY_BUTTON_COLOR)
            }
        }
    }

    /**
     * 儲存自訂月曆外觀樣式
     */
    suspend fun setCalendarStyle(style: CalendarStyle) {
        dataStore.edit { preferences ->
            preferences[KEY_CALENDAR_STYLE] = style.name
        }
    }

    /**
     * 儲存自訂月曆強調與選日顏色（傳入 null 代表跟隨月曆樣式預設配色）
     */
    suspend fun setCalendarAccentColor(colorLong: Long?) {
        dataStore.edit { preferences ->
            if (colorLong != null) {
                preferences[KEY_CALENDAR_ACCENT_COLOR] = colorLong
            } else {
                preferences.remove(KEY_CALENDAR_ACCENT_COLOR)
            }
        }
    }

    /**
     * 儲存全域字體大小倍率
     */
    suspend fun setFontScale(fontScale: AppFontScale) {
        dataStore.edit { preferences ->
            preferences[KEY_FONT_SCALE] = fontScale.name
        }
    }

    /**
     * 批次寫入完整個人化設定（主要供雲端同步下載套用）
     */
    suspend fun applyPersonalization(settings: PersonalizationSettings) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = settings.themeMode.name
            if (settings.buttonColorLong != null) {
                preferences[KEY_BUTTON_COLOR] = settings.buttonColorLong
            } else {
                preferences.remove(KEY_BUTTON_COLOR)
            }
            preferences[KEY_CALENDAR_STYLE] = settings.calendarStyle.name
            if (settings.calendarAccentColorLong != null) {
                preferences[KEY_CALENDAR_ACCENT_COLOR] = settings.calendarAccentColorLong
            } else {
                preferences.remove(KEY_CALENDAR_ACCENT_COLOR)
            }
            preferences[KEY_FONT_SCALE] = settings.fontScale.name
        }
    }

    /**
     * 重設自訂按鍵顏色、月曆樣式、月曆配色與字體大小為系統預設值
     */
    suspend fun resetPersonalizationDefaults() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_BUTTON_COLOR)
            preferences[KEY_CALENDAR_STYLE] = CalendarStyle.CLASSIC.name
            preferences.remove(KEY_CALENDAR_ACCENT_COLOR)
            preferences[KEY_FONT_SCALE] = AppFontScale.MEDIUM.name
        }
    }
}
