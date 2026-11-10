package com.example.notepad.data.local.entity

/**
 * 待辦事項優先級列舉（規範 4.2：高、中、低）
 *
 * @property level 數值權重（數值越大代表優先級越高，用於資料庫排序）
 * @property label 繁體中文顯示標籤
 * @property colorLong 預設對應警示色彩色碼
 */
enum class Priority(
    val level: Int,
    val label: String,
    val colorLong: Long
) {
    HIGH(level = 3, label = "高", colorLong = 0xFFE53935L),
    MEDIUM(level = 2, label = "中", colorLong = 0xFFFF9800L),
    LOW(level = 1, label = "低", colorLong = 0xFF4CAF50L);

    companion object {
        /**
         * 依據數值權重轉換為優先級，預設回傳中優先級
         */
        fun fromLevel(level: Int): Priority {
            return entries.find { it.level == level } ?: MEDIUM
        }

        /**
         * 依據名稱或標籤轉換為優先級，預設回傳中優先級
         */
        fun fromString(value: String?): Priority {
            if (value.isNullOrBlank()) return MEDIUM
            return entries.find {
                it.name.equals(value.trim(), ignoreCase = true) ||
                    it.label == value.trim()
            } ?: MEDIUM
        }
    }
}
