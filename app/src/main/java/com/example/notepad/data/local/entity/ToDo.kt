package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 待辦事項資料實體（規範 4.2）
 * - 每項任務具有唯一識別碼 id 與跨裝置同步識別碼 syncId
 * - 支援高/中/低優先級 (priority)
 * - 截止日期 (dueDate) 與提醒排程 (Reminder) 分開管理
 */
@Entity(
    tableName = "todos",
    indices = [Index(value = ["syncId"], unique = true)]
)
data class ToDo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null, // epoch millis for normalized date (start of day)
    val tag: String = "日常",
    val color: Long = 0xFF2196F3L,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Priority = Priority.MEDIUM,
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
