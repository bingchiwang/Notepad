package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 離線刪除墓碑紀錄實體（資料同步規則 3 & 6）
 *
 * 當使用者於離線狀態刪除資料時，先即時清除主要資料表以避免重複索引衝突或干擾查詢，
 * 同時將待刪除之同步識別碼保存於此表，待網路恢復後通知 Firebase 完成刪除並進行時間戳比對。
 */
@Entity(
    tableName = "sync_tombstones",
    indices = [Index(value = ["entityType", "syncId"], unique = true)]
)
data class SyncTombstone(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: SyncEntityType,
    val syncId: String,
    val deletedAt: Long = System.currentTimeMillis()
)
