package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class ToDo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val dueDate: Long? = null, // epoch millis for normalized date (start of day)
    val tag: String = "日常",
    val color: Long = 0xFF2196F3L,
    val createdAt: Long = System.currentTimeMillis()
)
