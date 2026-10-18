package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pomodoro_records")
data class PomodoroRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 25,
    val taskTitle: String = "專注時段",
    val date: Long // normalized start-of-day timestamp in milliseconds
)
