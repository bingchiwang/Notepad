package com.example.notepad.data.local

import androidx.room.TypeConverter
import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.SyncEntityType
import com.example.notepad.data.local.entity.SyncStatus

/**
 * Room 資料庫型別轉換器，負責列舉與基本型別之間的雙向轉換
 */
class DatabaseConverters {

    /**
     * 將待辦優先級轉為資料庫整數欄位（高=3, 中=2, 低=1），支援 SQL 排序
     */
    @TypeConverter
    fun fromPriority(priority: Priority): Int {
        return priority.level
    }

    /**
     * 將資料庫整數值還原為待辦優先級列舉
     */
    @TypeConverter
    fun toPriority(level: Int): Priority {
        return Priority.fromLevel(level)
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): Int {
        return status.code
    }

    @TypeConverter
    fun toSyncStatus(code: Int): SyncStatus {
        return SyncStatus.fromCode(code)
    }

    @TypeConverter
    fun fromSyncEntityType(type: SyncEntityType): String {
        return type.value
    }

    @TypeConverter
    fun toSyncEntityType(value: String): SyncEntityType {
        return SyncEntityType.fromValue(value) ?: SyncEntityType.NOTE
    }
}
