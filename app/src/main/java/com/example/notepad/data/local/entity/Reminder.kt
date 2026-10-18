package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val triggerAt: Long,
    val tag: String = "提醒",
    val color: Long = 0xFFFF9800L,
    val isFired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
