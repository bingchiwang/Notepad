package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_blocks")
data class TimeBlock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long, // normalized start-of-day timestamp in milliseconds
    val startTime: String, // e.g. "09:00"
    val endTime: String, // e.g. "10:30"
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val tag: String = "工作",
    val color: Long = 0xFF4CAF50L
)
