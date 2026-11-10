package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
