package com.example.notepad.widget

import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.ToDo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * 月曆桌面小工具月份網格計算、事件排序／截斷（＋N 項）與深度連結契約單元測試
 */
class CalendarWidgetLogicTest {

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `月份切換支援跨年自動進退位`() {
        assertEquals(2025 to 12, CalendarWidgetLogic.shiftMonth(2026, 1, -1))
        assertEquals(2027 to 1, CalendarWidgetLogic.shiftMonth(2026, 12, 1))
        assertEquals(2028 to 2, CalendarWidgetLogic.shiftMonth(2026, 12, 14))
        assertEquals(2025 to 10, CalendarWidgetLogic.shiftMonth(2026, 1, -3))
    }

    @Test
    fun `完整月份網格固定為42格且週日對齊並正確標示今天日期`() {
        // 2026 年 12 月 1 日為星期二，因此第 0 格應為 11 月 29 日（星期日）
        val today = localMillis(2026, 12, 15, 14, 30)
        val state = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 12,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = today
        )

        assertEquals("2026年 12月", state.headerTitle)
        assertEquals(CalendarWidgetLogic.GRID_CELL_COUNT, state.cells.size)
        assertEquals(localMillis(2026, 12, 15, 0, 0), state.quickAddDefaultDateMillis)

        // 第一格為上個月（11/29），isCurrentMonth 應為 false
        assertEquals(29, state.cells.first().dayOfMonth)
        assertFalse(state.cells.first().isCurrentMonth)

        // 本月最後幾格為隔月（1/9）
        assertFalse(state.cells.last().isCurrentMonth)

        // 當月天數應恰為 31 格，且僅有 12/15 標示 isToday = true
        val currentMonthCells = state.cells.filter { it.isCurrentMonth }
        assertEquals(31, currentMonthCells.size)
        val todayCells = state.cells.filter { it.isToday }
        assertEquals(1, todayCells.size)
        assertEquals(15, todayCells.first().dayOfMonth)
    }

    @Test
    fun `切換至非本月時快速新增預設日期為該月一號`() {
        val today = localMillis(2026, 11, 15, 9, 0)
        val state = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 12,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = today
        )

        assertEquals(localMillis(2026, 12, 1, 0, 0), state.quickAddDefaultDateMillis)
        assertTrue(state.cells.none { it.isToday })
    }

    @Test
    fun `記事與待辦依照日期歸類且忽略無截止日待辦並處理空白標題`() {
        val today = localMillis(2026, 11, 15)
        val notes = listOf(
            Note(
                id = 10,
                title = "   ",
                content = "空白標題測試內文",
                createdAt = localMillis(2026, 11, 15, 8, 20),
                updatedAt = localMillis(2026, 11, 15, 9, 0)
            )
        )
        val todos = listOf(
            ToDo(
                id = 20,
                title = "完成簡報",
                isCompleted = false,
                dueDate = localMillis(2026, 11, 15, 0, 0),
                priority = Priority.HIGH
            ),
            ToDo(
                id = 21,
                title = "沒有截止日不顯示在月曆格子",
                isCompleted = false,
                dueDate = null,
                priority = Priority.MEDIUM
            )
        )

        val state = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 11,
            notes = notes,
            todos = todos,
            todayMillis = today
        )

        val day15 = state.cells.first { it.isCurrentMonth && it.dayOfMonth == 15 }
        assertEquals(2, day15.visibleEvents.size)
        assertEquals(0, day15.overflowCount)
        assertNull(day15.overflowLabel)

        val firstEvent = day15.visibleEvents[0]
        val secondEvent = day15.visibleEvents[1]
        assertEquals(CalendarEventType.TODO, firstEvent.type)
        assertEquals("完成簡報", firstEvent.title)
        assertEquals(CalendarEventType.NOTE, secondEvent.type)
        assertEquals("（無標題）", secondEvent.title)
    }

    @Test
    fun `單日超過兩筆事項時最多顯示兩筆並顯示剩餘筆數＋3項`() {
        val day10Morning = localMillis(2026, 11, 10, 9, 0)
        val day10Midnight = localMillis(2026, 11, 10, 0, 0)

        // 共 5 筆同日事項：2 筆未完成待辦 + 2 筆記事 + 1 筆已完成待辦
        val todos = listOf(
            ToDo(id = 1, title = "高優待辦", isCompleted = false, dueDate = day10Midnight, priority = Priority.HIGH, createdAt = 100L),
            ToDo(id = 2, title = "中優待辦", isCompleted = false, dueDate = day10Midnight, priority = Priority.MEDIUM, createdAt = 200L),
            ToDo(id = 3, title = "已完成待辦", isCompleted = true, dueDate = day10Midnight, priority = Priority.LOW, createdAt = 300L)
        )
        val notes = listOf(
            Note(id = 11, title = "會議紀錄", content = "討論摘要", createdAt = day10Morning, updatedAt = 400L),
            Note(id = 12, title = "靈感草稿", content = "架構想法", createdAt = day10Morning, updatedAt = 500L)
        )

        val state = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 11,
            notes = notes,
            todos = todos,
            todayMillis = localMillis(2026, 11, 1)
        )

        val day10 = state.cells.first { it.isCurrentMonth && it.dayOfMonth == 10 }
        assertEquals(2, day10.visibleEvents.size)
        assertEquals("高優待辦", day10.visibleEvents[0].title)
        assertEquals("中優待辦", day10.visibleEvents[1].title)
        assertEquals(3, day10.overflowCount)
        assertEquals("＋3 項", day10.overflowLabel)
    }

    @Test
    fun `月曆小工具深度連結解析點擊日期與快速新增參數正確無誤`() {
        val targetMillis = localMillis(2026, 11, 20)

        val openDay = QuickNoteWidgetContract.parseWidgetDeepLink(
            action = QuickNoteWidgetContract.ACTION_OPEN_CALENDAR_DAY,
            calendarDateMillis = targetMillis,
            calendarQuickAdd = false
        )
        assertEquals(
            WidgetDeepLink.OpenCalendarDay(dateMillis = targetMillis, openQuickAdd = false),
            openDay
        )

        val quickAdd = QuickNoteWidgetContract.parseWidgetDeepLink(
            action = QuickNoteWidgetContract.ACTION_OPEN_CALENDAR_DAY,
            calendarDateMillis = targetMillis,
            calendarQuickAdd = true
        )
        assertEquals(
            WidgetDeepLink.OpenCalendarDay(dateMillis = targetMillis, openQuickAdd = true),
            quickAdd
        )

        val invalidDate = QuickNoteWidgetContract.parseWidgetDeepLink(
            action = QuickNoteWidgetContract.ACTION_OPEN_CALENDAR_DAY,
            calendarDateMillis = QuickNoteWidgetContract.INVALID_DATE_MILLIS,
            calendarQuickAdd = true
        )
        assertNull(invalidDate)
    }
}
