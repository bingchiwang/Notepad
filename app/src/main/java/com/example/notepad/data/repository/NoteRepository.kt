package com.example.notepad.data.repository

import com.example.notepad.data.local.dao.CategoryDao
import com.example.notepad.data.local.dao.NoteDao
import com.example.notepad.data.local.dao.SyncTombstoneDao
import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.local.entity.SyncEntityType
import com.example.notepad.data.local.entity.SyncStatus
import com.example.notepad.data.local.entity.SyncTombstone
import kotlinx.coroutines.flow.Flow

/**
 * 記事與分類資料倉儲（遵循規範 4.1、4.3 及資料同步規則）
 *
 * - 優先寫入本機 Room 資料庫，並自動標記為 PENDING_UPLOAD 等待／觸發雲端同步
 * - 刪除記事或分類時記錄 SyncTombstone，確保離線刪除後連網可正確同步至雲端
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val syncTombstoneDao: SyncTombstoneDao? = null
) {
    /** 本機資料異動通知鉤子，供雲端同步協調器即時觸發同步 */
    var onLocalDataChanged: (() -> Unit)? = null

    // === 記事查詢與管理 ===

    /** 所有記事串流（釘選優先，其次依最新修改時間降冪排列） */
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesByCategory(categoryId: Int): Flow<List<Note>> {
        return noteDao.getNotesByCategory(categoryId)
    }

    fun getUncategorizedNotes(): Flow<List<Note>> {
        return noteDao.getUncategorizedNotes()
    }

    fun getNoteById(id: Int): Flow<Note?> {
        return noteDao.getNoteById(id)
    }

    /**
     * 搜尋記事（規範 4.3）：
     * - 關鍵字空白時回傳一般列表（釘選優先）
     * - 有關鍵字時比對標題與內文，並依最新修改時間 (updatedAt) 降冪排序
     */
    fun searchNotes(query: String, categoryId: Int? = null): Flow<List<Note>> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            return if (categoryId != null) {
                noteDao.getNotesByCategory(categoryId)
            } else {
                noteDao.getAllNotes()
            }
        }
        return if (categoryId != null) {
            noteDao.searchNotesByCategory(trimmedQuery, categoryId)
        } else {
            noteDao.searchNotes(trimmedQuery)
        }
    }

    /**
     * 新增記事（優先寫入本機並標記為待同步）
     */
    suspend fun insertNote(note: Note): Long {
        syncTombstoneDao?.deleteTombstone(SyncEntityType.NOTE, note.syncId)
        val insertedId = noteDao.insertNote(
            note.copy(syncStatus = SyncStatus.PENDING_UPLOAD)
        )
        onLocalDataChanged?.invoke()
        return insertedId
    }

    /**
     * 更新記事（自動更新 updatedAt 並標記為待同步）
     */
    suspend fun updateNote(
        note: Note,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        noteDao.updateNote(
            note.copy(
                updatedAt = updatedAt,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }

    /**
     * 切換記事釘選狀態並同步更新修改時間與待同步狀態
     */
    suspend fun toggleNotePinned(
        note: Note,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        noteDao.updateNote(
            note.copy(
                isPinned = !note.isPinned,
                updatedAt = updatedAt,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }

    /**
     * 刪除記事（同步記錄刪除墓碑供雲端同步刪除）
     */
    suspend fun deleteNote(
        note: Note,
        deletedAt: Long = System.currentTimeMillis()
    ) {
        syncTombstoneDao?.upsertTombstone(
            SyncTombstone(
                entityType = SyncEntityType.NOTE,
                syncId = note.syncId,
                deletedAt = deletedAt
            )
        )
        noteDao.deleteNote(note)
        onLocalDataChanged?.invoke()
    }

    // === 分類查詢與管理 ===

    /** 所有分類串流 */
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoryById(id: Int): Flow<Category?> {
        return categoryDao.getCategoryById(id)
    }

    /**
     * 新增分類（分類名稱不得空白且不得重複，並標記為待同步）
     */
    suspend fun insertCategory(
        name: String,
        color: Long = 0xFF2196F3L
    ): Long {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "分類名稱不得為空白" }

        val existing = categoryDao.getCategoryByName(trimmedName)
        require(existing == null) { "分類名稱「$trimmedName」已存在，請使用其他名稱" }

        val now = System.currentTimeMillis()
        val insertedId = categoryDao.insertCategory(
            Category(
                name = trimmedName,
                color = color,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
        return insertedId
    }

    /**
     * 更新分類名稱與色彩（檢查名稱不可重複並標記待同步）
     */
    suspend fun updateCategory(
        category: Category,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        val trimmedName = category.name.trim()
        require(trimmedName.isNotEmpty()) { "分類名稱不得為空白" }

        val existing = categoryDao.getCategoryByName(trimmedName)
        require(existing == null || existing.id == category.id) {
            "分類名稱「$trimmedName」已存在，請使用其他名稱"
        }

        categoryDao.updateCategory(
            category.copy(
                name = trimmedName,
                updatedAt = updatedAt,
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        onLocalDataChanged?.invoke()
    }

    /**
     * 刪除分類並處理該分類下的記事，同時寫入分類刪除墓碑供雲端同步
     */
    suspend fun deleteCategory(
        category: Category,
        targetCategoryId: Int? = null,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        if (targetCategoryId != null) {
            require(targetCategoryId != category.id) { "目標分類不能與即將刪除的分類相同" }
            noteDao.reassignNotesCategory(category.id, targetCategoryId, updatedAt)
        } else {
            noteDao.clearCategoryFromNotes(category.id, updatedAt)
        }
        syncTombstoneDao?.upsertTombstone(
            SyncTombstone(
                entityType = SyncEntityType.CATEGORY,
                syncId = category.syncId,
                deletedAt = updatedAt
            )
        )
        categoryDao.deleteCategory(category)
        onLocalDataChanged?.invoke()
    }
}
