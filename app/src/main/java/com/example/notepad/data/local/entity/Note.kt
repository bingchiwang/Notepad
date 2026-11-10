package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 記事資料實體（規範 4.1 & 4.3）
 * - 每篇記事皆具唯一識別碼 (id) 與跨裝置雲端同步識別碼 (syncId)
 * - 標題 (title) 與內文 (content) 均允許為空白
 * - 支援釘選置頂 (isPinned) 與最後修改時間追蹤 (updatedAt)
 * - 刪除所屬分類時自動解除關聯 (SET_NULL)，確保記事不會隨分類一併遺失
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["syncId"], unique = true)
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val isPinned: Boolean = false,
    val categoryId: Int? = null,
    val color: Long = 0xFF2196F3L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
