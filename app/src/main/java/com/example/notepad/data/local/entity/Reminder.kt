package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String,
    val triggerAt: Long,
    val isFired: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
