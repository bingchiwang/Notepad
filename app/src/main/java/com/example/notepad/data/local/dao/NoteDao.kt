package com.example.notepad.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.notepad.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * 記事資料存取介面（規範 4.1 & 4.3）
 */
@Dao
interface NoteDao {
    /**
     * 取得所有記事：釘選記事置頂，其餘依最新修改時間降冪排列
     */
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    /**
     * 依分類取得記事：釘選記事置頂，其餘依最新修改時間降冪排列
     */
    @Query("SELECT * FROM notes WHERE categoryId = :categoryId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByCategory(categoryId: Int): Flow<List<Note>>

    /**
     * 取得未分類（預設分類）記事
     */
    @Query("SELECT * FROM notes WHERE categoryId IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getUncategorizedNotes(): Flow<List<Note>>

    /**
     * 搜尋記事（規範 4.3）：同時比對標題及內文，並依最新修改時間排序
     */
    @Query(
        "SELECT * FROM notes " +
            "WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' " +
            "ORDER BY updatedAt DESC"
    )
    fun searchNotes(query: String): Flow<List<Note>>

    /**
     * 在指定分類內搜尋記事（規範 4.3）：同時比對標題及內文，並依最新修改時間排序
     */
    @Query(
        "SELECT * FROM notes " +
            "WHERE categoryId = :categoryId " +
            "AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') " +
            "ORDER BY updatedAt DESC"
    )
    fun searchNotesByCategory(query: String, categoryId: Int): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Int): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteByIdOnce(id: Int): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    /**
     * 刪除分類時將該分類底下的記事改為未分類，防止資料遺失
     */
    @Query("UPDATE notes SET categoryId = NULL, updatedAt = :updatedAt, syncStatus = 1 WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromNotes(categoryId: Int, updatedAt: Long)

    /**
     * 刪除分類時將該分類底下的記事批次轉移至目標分類
     */
    @Query("UPDATE notes SET categoryId = :newCategoryId, updatedAt = :updatedAt, syncStatus = 1 WHERE categoryId = :oldCategoryId")
    suspend fun reassignNotesCategory(oldCategoryId: Int, newCategoryId: Int?, updatedAt: Long)

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesOnce(): List<Note>

    @Query("SELECT * FROM notes WHERE syncId = :syncId LIMIT 1")
    suspend fun getNoteBySyncIdOnce(syncId: String): Note?

    @Query("SELECT COUNT(*) FROM notes WHERE syncStatus = 1")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE notes SET syncStatus = 0 WHERE syncId = :syncId")
    suspend fun markAsSynced(syncId: String)

    @Query("DELETE FROM notes WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
