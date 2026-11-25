package com.example.notepad.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [NoteChecklistLogic] 單元測試：
 * 驗證 Markdown 核取方塊（`- [ ]` / `- [x]`）解析、勾選切換、游標／選區插入、
 * 換行自動延續與選取文字萃取轉待辦標題邏輯。
 */
class NoteChecklistLogicTest {

    @Test
    fun parseChecklistItems_extractsAllValidMarkdownChecklistLines() {
        val content = """
            # 今日筆記
            - [ ] 整理會議紀要
            普通段落說明
              * [x] 寄出週報
            - [X] 備份檔案
        """.trimIndent()

        val items = NoteChecklistLogic.parseChecklistItems(content)
        assertEquals(3, items.size)

        assertEquals(1, items[0].lineIndex)
        assertFalse(items[0].isChecked)
        assertEquals("整理會議紀要", items[0].text)

        assertEquals(3, items[1].lineIndex)
        assertTrue(items[1].isChecked)
        assertEquals("寄出週報", items[1].text)

        assertEquals(4, items[2].lineIndex)
        assertTrue(items[2].isChecked)
        assertEquals("備份檔案", items[2].text)
    }

    @Test
    fun toggleChecklistLine_flipsTargetLineWithoutAffectingNeighbors() {
        val initial = "前置文字\n- [ ] 第一項\n  * [x] 第二項\n收尾文字"
        val afterCheck = NoteChecklistLogic.toggleChecklistLine(initial, lineIndex = 1)
        assertEquals("前置文字\n- [x] 第一項\n  * [x] 第二項\n收尾文字", afterCheck)

        val afterUncheck = NoteChecklistLogic.toggleChecklistLine(afterCheck, lineIndex = 2)
        assertEquals("前置文字\n- [x] 第一項\n  * [ ] 第二項\n收尾文字", afterUncheck)

        // 無效行號應安全忽略
        assertEquals(afterUncheck, NoteChecklistLogic.toggleChecklistLine(afterUncheck, lineIndex = 99))
    }

    @Test
    fun insertChecklistItem_handlesBlankLinePlainLineChecklistLineAndMultiLineSelection() {
        // 1. 空白內容
        val (blankResult, blankCursor) = NoteChecklistLogic.insertChecklistItem("", 0, 0)
        assertEquals("- [ ] ", blankResult)
        assertEquals(6, blankCursor)

        // 2. 純文字行 -> 轉換成 `- [ ] `
        val (plainResult, plainCursor) = NoteChecklistLogic.insertChecklistItem("繳交水電費", 2, 2)
        assertEquals("- [ ] 繳交水電費", plainResult)
        assertEquals("- [ ] 繳交水電費".length, plainCursor)

        // 3. 已在核取清單行 -> 於行尾新增下一行 `- [ ] `
        val (nextLineResult, nextCursor) = NoteChecklistLogic.insertChecklistItem("- [ ] 買牛奶", 4, 4)
        assertEquals("- [ ] 買牛奶\n- [ ] ", nextLineResult)
        assertEquals(nextLineResult.length, nextCursor)

        // 4. 跨多行選取 -> 批次轉換後再點一次可批次取消
        val multi = "項目甲\n項目乙"
        val (converted, _) = NoteChecklistLogic.insertChecklistItem(multi, 0, multi.length)
        assertEquals("- [ ] 項目甲\n- [ ] 項目乙", converted)
        val (reverted, _) = NoteChecklistLogic.insertChecklistItem(converted, 0, converted.length)
        assertEquals("項目甲\n項目乙", reverted)
    }

    @Test
    fun handleEnterAutoContinue_continuesNonEmptyAndExitsEmptyChecklistLine() {
        // 模擬在 "- [ ] 採買水果" 尾端按下 Enter
        val oldNonEmpty = "- [ ] 採買水果"
        val newNonEmpty = "- [ ] 採買水果\n"
        val continued = NoteChecklistLogic.handleEnterAutoContinue(
            oldContent = oldNonEmpty,
            newContent = newNonEmpty,
            newCursor = newNonEmpty.length
        )
        assertEquals("- [ ] 採買水果\n- [ ] " to "- [ ] 採買水果\n- [ ] ".length, continued)

        // 模擬在空項目 "- [ ] " 再按一次 Enter 結束清單
        val oldEmpty = "- [ ] 第一項\n- [ ] "
        val newEmpty = "- [ ] 第一項\n- [ ] \n"
        val exited = NoteChecklistLogic.handleEnterAutoContinue(
            oldContent = oldEmpty,
            newContent = newEmpty,
            newCursor = newEmpty.length
        )
        assertEquals("- [ ] 第一項\n" to "- [ ] 第一項\n".length, exited)

        // 一般文字換行不應介入
        assertNull(NoteChecklistLogic.handleEnterAutoContinue("一般筆記", "一般筆記\n", 5))
    }

    @Test
    fun extractActionableTitle_stripsMarkdownPrefixesFromSelectionOrCursorLine() {
        val text = "- [ ] 準備季度報告\n- [x] 完成投影片\n### 與設計討論介面"
        // 選取單一核取行
        val startIdx = text.indexOf("- [ ] 準備季度報告")
        val endIdx = startIdx + "- [ ] 準備季度報告".length
        assertEquals(
            "準備季度報告",
            NoteChecklistLogic.extractActionableTitle(text, startIdx, endIdx)
        )

        // 游標位於標題行（無選區）
        val headingCursor = text.indexOf("與設計")
        assertEquals(
            "與設計討論介面",
            NoteChecklistLogic.extractActionableTitle(text, headingCursor, headingCursor)
        )
    }
}
