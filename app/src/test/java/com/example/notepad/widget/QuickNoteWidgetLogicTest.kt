package com.example.notepad.widget

import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 快速記事桌面小工具邏輯、篩選序列化與深度連結單元測試
 */
class QuickNoteWidgetLogicTest {

    private val workCategory = Category(id = 1, name = "工作", color = 0xFF1E88E5L)
    private val lifeCategory = Category(id = 2, name = "生活", color = 0xFF43A047L)

    @Test
    fun `分類篩選條件編碼與解碼應支援全部分類與異常退回`() {
        val allEncoded = QuickNoteWidgetLogic.encodeFilter(WidgetCategoryFilter.All)
        val uncategorizedEncoded = QuickNoteWidgetLogic.encodeFilter(WidgetCategoryFilter.Uncategorized)
        val categoryEncoded = QuickNoteWidgetLogic.encodeFilter(WidgetCategoryFilter.CategoryFilter(7))

        assertEquals(WidgetCategoryFilter.All, QuickNoteWidgetLogic.decodeFilter(allEncoded))
        assertEquals(WidgetCategoryFilter.Uncategorized, QuickNoteWidgetLogic.decodeFilter(uncategorizedEncoded))
        assertEquals(WidgetCategoryFilter.CategoryFilter(7), QuickNoteWidgetLogic.decodeFilter(categoryEncoded))

        assertEquals(WidgetCategoryFilter.All, QuickNoteWidgetLogic.decodeFilter(null))
        assertEquals(WidgetCategoryFilter.All, QuickNoteWidgetLogic.decodeFilter(""))
        assertEquals(WidgetCategoryFilter.All, QuickNoteWidgetLogic.decodeFilter("UNKNOWN"))
        assertEquals(WidgetCategoryFilter.All, QuickNoteWidgetLogic.decodeFilter("CAT:abc"))
        assertEquals(WidgetCategoryFilter.All, QuickNoteWidgetLogic.decodeFilter("CAT:0"))
        assertEquals(WidgetCategoryFilter.All, QuickNoteWidgetLogic.decodeFilter("CAT:-5"))
    }

    @Test
    fun `全部記事模式應將釘選置頂並依最新修改時間排序且遵守上限筆數`() {
        val notes = listOf(
            Note(id = 1, title = "較舊普通記事", content = "內容1", isPinned = false, updatedAt = 1_000L),
            Note(id = 2, title = "最新普通記事", content = "內容2", isPinned = false, updatedAt = 5_000L),
            Note(id = 3, title = "較舊釘選記事", content = "內容3", isPinned = true, updatedAt = 2_000L),
            Note(id = 4, title = "最新釘選記事", content = "內容4", isPinned = true, updatedAt = 4_000L)
        )

        val resolved = QuickNoteWidgetLogic.resolveWidgetState(
            notes = notes,
            categories = listOf(workCategory, lifeCategory),
            requestedFilter = WidgetCategoryFilter.All,
            maxItems = 3,
            timeFormatter = { ts -> "T:$ts" }
        )

        assertEquals("全部記事", resolved.filterLabel)
        assertEquals(WidgetCategoryFilter.All, resolved.effectiveFilter)
        assertNull(resolved.prefillCategoryId)
        assertFalse(resolved.isCategoryMissingFallback)
        assertEquals(listOf(4, 3, 2), resolved.notes.map { it.id })
        assertEquals("T:4000", resolved.notes.first().formattedTime)
    }

    @Test
    fun `指定分類篩選應只顯示該分類記事並預帶新增分類識別碼`() {
        val notes = listOf(
            Note(id = 10, title = "工作週報", content = "完成測試", categoryId = 1, updatedAt = 3_000L),
            Note(id = 11, title = "買菜清單", content = "蘋果牛奶", categoryId = 2, updatedAt = 4_000L),
            Note(id = 12, title = "臨時想法", content = "未分類備忘", categoryId = null, updatedAt = 5_000L)
        )

        val resolved = QuickNoteWidgetLogic.resolveWidgetState(
            notes = notes,
            categories = listOf(workCategory, lifeCategory),
            requestedFilter = WidgetCategoryFilter.CategoryFilter(1),
            timeFormatter = { "05/12 09:00" }
        )

        assertEquals("工作", resolved.filterLabel)
        assertEquals(WidgetCategoryFilter.CategoryFilter(1), resolved.effectiveFilter)
        assertEquals(1, resolved.prefillCategoryId)
        assertFalse(resolved.isCategoryMissingFallback)
        assertEquals(1, resolved.notes.size)
        assertEquals(10, resolved.notes.first().id)
        assertEquals("工作", resolved.notes.first().categoryName)
    }

    @Test
    fun `未分類篩選應僅包含未指定分類的記事`() {
        val notes = listOf(
            Note(id = 10, title = "工作週報", content = "完成測試", categoryId = 1, updatedAt = 3_000L),
            Note(id = 12, title = "臨時想法", content = "未分類備忘", categoryId = null, updatedAt = 5_000L)
        )

        val resolved = QuickNoteWidgetLogic.resolveWidgetState(
            notes = notes,
            categories = listOf(workCategory),
            requestedFilter = WidgetCategoryFilter.Uncategorized
        )

        assertEquals("未分類", resolved.filterLabel)
        assertEquals(WidgetCategoryFilter.Uncategorized, resolved.effectiveFilter)
        assertNull(resolved.prefillCategoryId)
        assertFalse(resolved.isCategoryMissingFallback)
        assertEquals(listOf(12), resolved.notes.map { it.id })
        assertNull(resolved.notes.first().categoryName)
    }

    @Test
    fun `原指定分類已被刪除時應安全退回顯示全部記事`() {
        val notes = listOf(
            Note(id = 1, title = "記事甲", content = "內容甲", categoryId = 1, updatedAt = 2_000L),
            Note(id = 2, title = "記事乙", content = "內容乙", categoryId = null, updatedAt = 3_000L)
        )

        val resolved = QuickNoteWidgetLogic.resolveWidgetState(
            notes = notes,
            categories = listOf(workCategory),
            requestedFilter = WidgetCategoryFilter.CategoryFilter(999)
        )

        assertTrue(resolved.isCategoryMissingFallback)
        assertEquals(WidgetCategoryFilter.All, resolved.effectiveFilter)
        assertEquals("全部記事", resolved.filterLabel)
        assertNull(resolved.prefillCategoryId)
        assertEquals(listOf(2, 1), resolved.notes.map { it.id })
    }

    @Test
    fun `空白標題與多行超長內文應轉換為清晰的預覽文字`() {
        val longContent = "第一行\n\n   第二行   " + "長文字內容".repeat(30)
        val notes = listOf(
            Note(id = 21, title = "   ", content = "", updatedAt = 1_000L),
            Note(id = 22, title = "含超長內文", content = longContent, updatedAt = 2_000L)
        )

        val resolved = QuickNoteWidgetLogic.resolveWidgetState(
            notes = notes,
            categories = emptyList(),
            requestedFilter = WidgetCategoryFilter.All
        )

        val longItem = resolved.notes[0]
        val blankItem = resolved.notes[1]

        assertEquals("（無標題記事）", blankItem.title)
        assertEquals("（尚無內文）", blankItem.contentPreview)

        assertFalse(longItem.contentPreview.contains('\n'))
        assertTrue(longItem.contentPreview.startsWith("第一行 第二行 "))
        assertTrue(longItem.contentPreview.endsWith("…"))
    }

    @Test
    fun `深度連結解析應正確區分新增記事與編輯記事動作並忽略無效參數`() {
        assertEquals(
            WidgetDeepLink.CreateNote(categoryId = null),
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_CREATE_NOTE
            )
        )
        assertEquals(
            WidgetDeepLink.CreateNote(categoryId = 5),
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_CREATE_NOTE,
                categoryId = 5
            )
        )
        assertEquals(
            WidgetDeepLink.CreateNote(categoryId = null),
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_CREATE_NOTE,
                categoryId = 0
            )
        )
        assertEquals(
            WidgetDeepLink.EditNote(noteId = 42),
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_EDIT_NOTE,
                noteId = 42
            )
        )
        assertNull(
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_EDIT_NOTE,
                noteId = QuickNoteWidgetContract.INVALID_ID
            )
        )
        assertNull(
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_EDIT_NOTE,
                noteId = 0
            )
        )
        assertNull(QuickNoteWidgetContract.parseWidgetDeepLink(action = null))
        assertNull(QuickNoteWidgetContract.parseWidgetDeepLink(action = "UNKNOWN"))
    }
}
