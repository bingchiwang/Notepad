package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 提醒通知資料實體（規範 4.4）
 * - 與待辦事項的截止日期 (dueDate) 分開管理
 * - 可透過 linkedToDoId 關聯對應任務，於任務完成時自動取消尚未觸發的通知
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val triggerAt: Long,
    val tag: String = "提醒",
    val color: Long = 0xFFFF9800L,
    val isFired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val linkedToDoId: Int? = null
)
