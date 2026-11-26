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
    fun `完整月份網格依當月實際週數動態對齊且正確標示週日與今天日期`() {
        // 2026 年 12 月 1 日為星期二，共跨 5 週（35 格），第 0 格應為 11 月 29 日（星期日）
        val today = localMillis(2026, 12, 15, 14, 30)
        val state = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 12,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = today
        )

        assertEquals("2026年 12月", state.headerTitle)
        assertEquals(5, state.weekCount)
        assertEquals(35, state.cells.size)
        assertEquals(localMillis(2026, 12, 15, 0, 0), state.quickAddDefaultDateMillis)

        // 第一格為上個月（11/29 星期日），isCurrentMonth 為 false 且 isSunday 為 true
        assertEquals(29, state.cells.first().dayOfMonth)
        assertFalse(state.cells.first().isCurrentMonth)
        assertTrue(state.cells.first().isSunday)
        assertFalse(state.cells[1].isSunday)

        // 本月最後幾格為隔月（1/2）
        assertFalse(state.cells.last().isCurrentMonth)

        // 當月天數應恰為 31 格，且僅有 12/15 標示 isToday = true
        val currentMonthCells = state.cells.filter { it.isCurrentMonth }
        assertEquals(31, currentMonthCells.size)
        val todayCells = state.cells.filter { it.isToday }
        assertEquals(1, todayCells.size)
        assertEquals(15, todayCells.first().dayOfMonth)
    }

    @Test
    fun `動態週數支援4週5週與6週月份自動增減列數`() {
        // 2026 年 2 月：2/1 為週日且共 28 天 -> 恰為 4 週（28 格）
        val feb2026 = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 2,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = localMillis(2026, 2, 1)
        )
        assertEquals(4, feb2026.weekCount)
        assertEquals(28, feb2026.cells.size)

        // 2026 年 11 月：11/1 為週日且共 30 天 -> 5 週（35 格）
        val nov2026 = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 11,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = localMillis(2026, 11, 1)
        )
        assertEquals(5, nov2026.weekCount)
        assertEquals(35, nov2026.cells.size)

        // 2026 年 5 月：5/1 為週五且共 31 天 -> 6 週（42 格）
        val may2026 = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 5,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = localMillis(2026, 5, 1)
        )
        assertEquals(6, may2026.weekCount)
        assertEquals(42, may2026.cells.size)
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
    fun `可切換顯示或隱藏農曆日期並正確帶出節慶與初一標籤`() {
        val withoutLunar = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 2,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = localMillis(2026, 2, 17),
            showLunarDate = false
        )
        assertFalse(withoutLunar.showLunarDate)
        assertTrue(withoutLunar.cells.all { it.lunarLabel == null && !it.isLunarHighlight })

        // 2026 年 2 月 17 日為農曆正月初一（春節）
        val withLunar = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 2,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = localMillis(2026, 2, 17),
            showLunarDate = true
        )
        assertTrue(withLunar.showLunarDate)
        val springFestivalCell = withLunar.cells.first { it.isCurrentMonth && it.dayOfMonth == 17 }
        assertEquals("春節", springFestivalCell.lunarLabel)
        assertTrue(springFestivalCell.isLunarHighlight)

        // 2026 年 2 月 18 日 23:52 (UTC+8) 為「雨水」節氣（醒目標示），2 月 19 日為一般農曆「初三」
        val feb18Cell = withLunar.cells.first { it.isCurrentMonth && it.dayOfMonth == 18 }
        assertEquals("雨水", feb18Cell.lunarLabel)
        assertTrue(feb18Cell.isLunarHighlight)

        val feb19Cell = withLunar.cells.first { it.isCurrentMonth && it.dayOfMonth == 19 }
        assertEquals("初三", feb19Cell.lunarLabel)
        assertFalse(feb19Cell.isLunarHighlight)
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

    @Test
    fun `小工具月曆日期字體大小支援四段縮放解析與網格狀態傳遞`() {
        assertEquals(CalendarWidgetDateFontSize.MEDIUM, CalendarWidgetDateFontSize.fromString(null))
        assertEquals(CalendarWidgetDateFontSize.MEDIUM, CalendarWidgetDateFontSize.fromString("  "))
        assertEquals(CalendarWidgetDateFontSize.MEDIUM, CalendarWidgetDateFontSize.fromString("UNKNOWN"))
        assertEquals(CalendarWidgetDateFontSize.SMALL, CalendarWidgetDateFontSize.fromString("small"))
        assertEquals(CalendarWidgetDateFontSize.LARGE, CalendarWidgetDateFontSize.fromString("LARGE"))
        assertEquals(CalendarWidgetDateFontSize.EXTRA_LARGE, CalendarWidgetDateFontSize.fromString(" extra_large "))

        assertTrue(CalendarWidgetDateFontSize.SMALL.dayNumberSp < CalendarWidgetDateFontSize.MEDIUM.dayNumberSp)
        assertTrue(CalendarWidgetDateFontSize.MEDIUM.dayNumberSp < CalendarWidgetDateFontSize.LARGE.dayNumberSp)
        assertTrue(CalendarWidgetDateFontSize.LARGE.dayNumberSp < CalendarWidgetDateFontSize.EXTRA_LARGE.dayNumberSp)
        assertTrue(CalendarWidgetDateFontSize.SMALL.lunarSp < CalendarWidgetDateFontSize.EXTRA_LARGE.lunarSp)

        val defaultState = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 11,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = localMillis(2026, 11, 26)
        )
        assertEquals(CalendarWidgetDateFontSize.MEDIUM, defaultState.dateFontSize)

        val extraLargeState = CalendarWidgetLogic.resolveMonthGrid(
            year = 2026,
            month = 11,
            notes = emptyList(),
            todos = emptyList(),
            todayMillis = localMillis(2026, 11, 26),
            dateFontSize = CalendarWidgetDateFontSize.EXTRA_LARGE
        )
        assertEquals(CalendarWidgetDateFontSize.EXTRA_LARGE, extraLargeState.dateFontSize)
        assertEquals(16f, extraLargeState.dateFontSize.dayNumberSp, 0.01f)
    }
}
