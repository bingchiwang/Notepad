package com.example.notepad.data.repository

import com.example.notepad.data.local.dao.CategoryDao
import com.example.notepad.data.local.dao.NoteDao
import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

/**
 * 記事與分類資料倉儲（遵循規範 4.1 記事管理 & 4.3 分類搜尋）
 *
 * 統一封裝記事與分類之 CRUD、修改時間更新、分類名稱防重檢核、
 * 刪除分類連動處理及全文搜尋排序邏輯。
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao
) {
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
     * 新增記事（規範 4.1：標題與內容允許為空白）
     */
    suspend fun insertNote(note: Note): Long {
        return noteDao.insertNote(note)
    }

    /**
     * 更新記事（規範 4.1：修改記事後必須自動更新修改時間 updatedAt）
     */
    suspend fun updateNote(
        note: Note,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        noteDao.updateNote(note.copy(updatedAt = updatedAt))
    }

    /**
     * 切換記事釘選狀態並同步更新修改時間
     */
    suspend fun toggleNotePinned(
        note: Note,
        updatedAt: Long = System.currentTimeMillis()
    ) {
        noteDao.updateNote(
            note.copy(
                isPinned = !note.isPinned,
                updatedAt = updatedAt
            )
        )
    }

    /**
     * 刪除記事（規範 4.1：同步更新資料狀態）
     */
    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    // === 分類查詢與管理 ===

    /** 所有分類串流 */
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoryById(id: Int): Flow<Category?> {
        return categoryDao.getCategoryById(id)
    }

    /**
     * 新增分類（規範 4.3：分類名稱不得空白且不得重複）
     * @throws IllegalArgumentException 當名稱為空白或與既有分類重複時拋出明確錯誤訊息
     */
    suspend fun insertCategory(
        name: String,
        color: Long = 0xFF2196F3L
    ): Long {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "分類名稱不得為空白" }

        val existing = categoryDao.getCategoryByName(trimmedName)
        require(existing == null) { "分類名稱「$trimmedName」已存在，請使用其他名稱" }

        return categoryDao.insertCategory(
            Category(
                name = trimmedName,
                color = color
            )
        )
    }

    /**
     * 更新分類名稱與色彩（規範 4.3：檢查名稱不可與其他分類重複）
     */
    suspend fun updateCategory(category: Category) {
        val trimmedName = category.name.trim()
        require(trimmedName.isNotEmpty()) { "分類名稱不得為空白" }

        val existing = categoryDao.getCategoryByName(trimmedName)
        require(existing == null || existing.id == category.id) {
            "分類名稱「$trimmedName」已存在，請使用其他名稱"
        }

        categoryDao.updateCategory(category.copy(name = trimmedName))
    }

    /**
     * 刪除分類並處理該分類下的記事（規範 4.3）：
     * - 若指定 [targetCategoryId]，將該分類下的記事移轉至目標分類
     * - 若未指定，將該分類下的記事改為未分類（預設分類，categoryId = null）
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
        categoryDao.deleteCategory(category)
    }
}
