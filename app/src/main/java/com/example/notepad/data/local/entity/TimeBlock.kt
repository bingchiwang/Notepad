package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 每日時間規劃區塊資料實體
 * - 每筆行程具有唯一識別碼 (id) 與跨裝置同步識別碼 (syncId)
 * - 支援開始/結束時段、完成狀態、分類標籤與色彩標記
 */
@Entity(
    tableName = "time_blocks",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class TimeBlock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // normalized start-of-day timestamp in milliseconds
    val startTime: String, // e.g. "09:00"
    val endTime: String, // e.g. "10:30"
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val tag: String = "工作",
    val color: Long = 0xFF4CAF50L,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
