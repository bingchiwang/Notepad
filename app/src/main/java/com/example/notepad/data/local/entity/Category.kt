package com.example.notepad.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 記事分類資料實體（規範 4.3）
 * - 每個分類具有唯一識別碼 (id) 與跨裝置同步識別碼 (syncId)
 * - 分類名稱 (name) 透過資料庫唯一索引避免重複
 */
@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["syncId"], unique = true)
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: Long = 0xFF2196F3L,
    val createdAt: Long = System.currentTimeMillis(),
    val syncId: String = UUID.randomUUID().toString(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)
