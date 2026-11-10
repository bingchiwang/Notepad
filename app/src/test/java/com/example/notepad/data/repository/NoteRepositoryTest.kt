package com.example.notepad.data.repository

import com.example.notepad.data.local.dao.CategoryDao
import com.example.notepad.data.local.dao.NoteDao
import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NoteRepository 單元測試（驗證規範 4.1 記事管理 & 4.3 分類搜尋）
 */
class NoteRepositoryTest {

    private lateinit var fakeNoteDao: FakeNoteDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var repository: NoteRepository

    @Before
    fun setup() {
        fakeNoteDao = FakeNoteDao()
        fakeCategoryDao = FakeCategoryDao()
        repository = NoteRepository(fakeNoteDao, fakeCategoryDao)
    }

    @Test
    fun `allow blank title and blank content when inserting note`() = runTest {
        val id = repository.insertNote(Note(title = "", content = ""))
        val notes = repository.allNotes.first()

        assertEquals(1L, id)
        assertEquals(1, notes.size)
        assertEquals("", notes.first().title)
        assertEquals("", notes.first().content)
    }

    @Test
    fun `updateNote updates modified timestamp`() = runTest {
        val id = repository.insertNote(
            Note(title = "舊標題", content = "內容", createdAt = 1000L, updatedAt = 1000L)
        ).toInt()
        val inserted = repository.getNoteById(id).first()!!

        repository.updateNote(inserted.copy(title = "新標題"), updatedAt = 5000L)
        val updated = repository.getNoteById(id).first()!!

        assertEquals("新標題", updated.title)
        assertEquals(5000L, updated.updatedAt)
        assertEquals(1000L, updated.createdAt)
    }

    @Test
    fun `pinned notes appear before unpinned notes`() = runTest {
        repository.insertNote(Note(title = "普通最新", isPinned = false, updatedAt = 3000L))
        repository.insertNote(Note(title = "釘選較舊", isPinned = true, updatedAt = 1000L))
        repository.insertNote(Note(title = "釘選最新", isPinned = true, updatedAt = 2000L))

        val notes = repository.allNotes.first()
        assertEquals(listOf("釘選最新", "釘選較舊", "普通最新"), notes.map { it.title })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject duplicate category name ignoring case and whitespace`() = runTest {
        repository.insertCategory("Work")
        repository.insertCategory("  work  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject blank category name`() = runTest {
        repository.insertCategory("   ")
    }

    @Test
    fun `deleteCategory unassigns notes under that category`() = runTest {
        val workId = repository.insertCategory("工作").toInt()
        repository.insertNote(Note(title = "會議記錄", categoryId = workId))
        val category = repository.getCategoryById(workId).first()!!

        repository.deleteCategory(category, updatedAt = 9000L)

        val remainingCategories = repository.allCategories.first()
        val notes = repository.allNotes.first()
        assertTrue(remainingCategories.isEmpty())
        assertEquals(1, notes.size)
        assertNull(notes.first().categoryId)
        assertEquals(9000L, notes.first().updatedAt)
    }

    @Test
    fun `deleteCategory can reassign notes to target category`() = runTest {
        val workId = repository.insertCategory("工作").toInt()
        val archiveId = repository.insertCategory("封存").toInt()
        repository.insertNote(Note(title = "專案資料", categoryId = workId))
        val workCategory = repository.getCategoryById(workId).first()!!

        repository.deleteCategory(workCategory, targetCategoryId = archiveId, updatedAt = 8000L)

        val notes = repository.allNotes.first()
        assertEquals(archiveId, notes.first().categoryId)
    }

    @Test
    fun `searchNotes matches title and content and sorts by updatedAt descending`() = runTest {
        repository.insertNote(Note(title = "購物清單", content = "牛奶", updatedAt = 1000L))
        repository.insertNote(Note(title = "讀書筆記", content = "Kotlin 購物車練習", updatedAt = 3000L))
        repository.insertNote(Note(title = "運動紀錄", content = "跑步 5 公里", updatedAt = 4000L))

        val results = repository.searchNotes("購物").first()
        assertEquals(listOf("讀書筆記", "購物清單"), results.map { it.title })
    }

    @Test
    fun `searchNotes returns empty list when no match exists`() = runTest {
        repository.insertNote(Note(title = "記事一", content = "測試內容"))
        val results = repository.searchNotes("不存在的關鍵字").first()
        assertTrue(results.isEmpty())
    }
}

private class FakeNoteDao : NoteDao {
    private val notesFlow = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1

    override fun getAllNotes(): Flow<List<Note>> = notesFlow.map { list ->
        list.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
    }

    override fun getNotesByCategory(categoryId: Int): Flow<List<Note>> = notesFlow.map { list ->
        list.filter { it.categoryId == categoryId }
            .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
    }

    override fun getUncategorizedNotes(): Flow<List<Note>> = notesFlow.map { list ->
        list.filter { it.categoryId == null }
            .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
    }

    override fun searchNotes(query: String): Flow<List<Note>> = notesFlow.map { list ->
        list.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
        }.sortedByDescending { it.updatedAt }
    }

    override fun searchNotesByCategory(query: String, categoryId: Int): Flow<List<Note>> = notesFlow.map { list ->
        list.filter {
            it.categoryId == categoryId &&
                (it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true))
        }.sortedByDescending { it.updatedAt }
    }

    override fun getNoteById(id: Int): Flow<Note?> = notesFlow.map { list ->
        list.find { it.id == id }
    }

    override suspend fun getNoteByIdOnce(id: Int): Note? = notesFlow.value.find { it.id == id }

    override suspend fun insertNote(note: Note): Long {
        val assignedId = if (note.id == 0) nextId++ else note.id
        val saved = note.copy(id = assignedId)
        notesFlow.value = notesFlow.value.filterNot { it.id == assignedId } + saved
        return assignedId.toLong()
    }

    override suspend fun updateNote(note: Note) {
        notesFlow.value = notesFlow.value.map { if (it.id == note.id) note else it }
    }

    override suspend fun deleteNote(note: Note) {
        notesFlow.value = notesFlow.value.filterNot { it.id == note.id }
    }

    override suspend fun clearCategoryFromNotes(categoryId: Int, updatedAt: Long) {
        notesFlow.value = notesFlow.value.map {
            if (it.categoryId == categoryId) it.copy(categoryId = null, updatedAt = updatedAt) else it
        }
    }

    override suspend fun reassignNotesCategory(oldCategoryId: Int, newCategoryId: Int?, updatedAt: Long) {
        notesFlow.value = notesFlow.value.map {
            if (it.categoryId == oldCategoryId) it.copy(categoryId = newCategoryId, updatedAt = updatedAt) else it
        }
    }
}

private class FakeCategoryDao : CategoryDao {
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private var nextId = 1

    override fun getAllCategories(): Flow<List<Category>> = categoriesFlow.map { list ->
        list.sortedBy { it.createdAt }
    }

    override fun getCategoryById(id: Int): Flow<Category?> = categoriesFlow.map { list ->
        list.find { it.id == id }
    }

    override suspend fun getCategoryByIdOnce(id: Int): Category? = categoriesFlow.value.find { it.id == id }

    override suspend fun getCategoryByName(name: String): Category? {
        val normalized = name.trim()
        return categoriesFlow.value.find { it.name.trim().equals(normalized, ignoreCase = true) }
    }

    override suspend fun insertCategory(category: Category): Long {
        if (getCategoryByName(category.name) != null) {
            throw IllegalStateException("Duplicate category")
        }
        val assignedId = if (category.id == 0) nextId++ else category.id
        val saved = category.copy(id = assignedId)
        categoriesFlow.value = categoriesFlow.value + saved
        return assignedId.toLong()
    }

    override suspend fun updateCategory(category: Category) {
        categoriesFlow.value = categoriesFlow.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun deleteCategory(category: Category) {
        categoriesFlow.value = categoriesFlow.value.filterNot { it.id == category.id }
    }

    override suspend fun deleteCategoryById(id: Int) {
        categoriesFlow.value = categoriesFlow.value.filterNot { it.id == id }
    }
}
