package com.example.notepad.data.local.entity

/**
 * 本機資料與雲端同步狀態列舉（資料同步規則 3 & 5）
 */
enum class SyncStatus(val code: Int, val label: String) {
    SYNCED(code = 0, label = "已同步"),
    PENDING_UPLOAD(code = 1, label = "待同步");

    companion object {
        fun fromCode(code: Int): SyncStatus {
            return entries.find { it.code == code } ?: PENDING_UPLOAD
        }
    }
}

/**
 * 支援雲端同步的資料實體類型識別常數
 */
enum class SyncEntityType(val value: String) {
    CATEGORY("CATEGORY"),
    NOTE("NOTE"),
    TODO("TODO"),
    REMINDER("REMINDER"),
    TIME_BLOCK("TIME_BLOCK"),
    POMODORO("POMODORO"),
    SETTINGS("SETTINGS");

    companion object {
        fun fromValue(value: String?): SyncEntityType? {
            return entries.find { it.value == value }
        }
    }
}
