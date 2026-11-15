package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 番茄鐘專注紀錄資料實體
 * - 紀錄每次完成之專注時長、關聯目標名稱、日期毫秒數與跨裝置雲端同步狀態
 */
@Entity(
    tableName = "pomodoro_records",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class PomodoroRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 25,
    val taskTitle: String = "專注時段",
    val date: Long, // normalized start-of-day timestamp in milliseconds
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
