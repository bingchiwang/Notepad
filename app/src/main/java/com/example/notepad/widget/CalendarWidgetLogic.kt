package com.example.notepad.widget

import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.local.entity.ToDo
import java.util.Calendar

/**
 * 月曆桌面小工具單筆行事曆事項類型（用於套用不同底色區分記事與待辦）
 */
enum class CalendarEventType {
    NOTE,
    TODO
}

/**
 * 月曆格子內直接顯示的單筆事項摘要
 */
data class CalendarDayEvent(
    val id: Int,
    val type: CalendarEventType,
    val title: String,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false
)

/**
 * 月曆桌面小工具單一日期格完整狀態（6 週 x 7 天 共 42 格）
 */
data class CalendarDayCellState(
    val dayStartMillis: Long,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val visibleEvents: List<CalendarDayEvent>,
    val overflowCount: Int,
    val overflowLabel: String?
)

/**
 * 經月份對齊、多筆截斷與色彩語意整理後的月曆小工具狀態
 */
data class ResolvedCalendarWidgetState(
    val year: Int,
    val month: Int,
    val headerTitle: String,
    val quickAddDefaultDateMillis: Long,
    val cells: List<CalendarDayCellState>
)

/**
 * 月曆桌面小工具核心業務邏輯與格網排程演算法（純 Kotlin 實作，支援完整單元測試）
 */
object CalendarWidgetLogic {

    /** 每個日期格最多直接顯示的文字事項筆數 */
    const val MAX_VISIBLE_EVENTS = 2

    /** 完整月曆固定顯示 6 週 x 7 天 */
    const val GRID_CELL_COUNT = 42

    /**
     * 取得指定時間戳記所屬之本地年、月（1-12）
     */
    fun currentYearMonth(todayMillis: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = todayMillis
        }
        return cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
    }

    /**
     * 跨月安全位移計算（支援負數／跨年自動進退位，例如 2026 年 1 月 -1 -> 2025 年 12 月）
     */
    fun shiftMonth(year: Int, month: Int, deltaMonths: Int): Pair<Int, Int> {
        val zeroBasedTotalMonths = year * 12 + (month - 1) + deltaMonths
        val targetYear = Math.floorDiv(zeroBasedTotalMonths, 12)
        val targetMonth = Math.floorMod(zeroBasedTotalMonths, 12) + 1
        return targetYear to targetMonth
    }

    /**
     * 建立完整 42 格月曆視圖資料：
     * - 每個日期將同一天的記事（依 `createdAt` 歸類）與待辦（依 `dueDate` 歸類）彙整排序。
     * - 每個日期格最多直接顯示 [MAX_VISIBLE_EVENTS] 筆事項；超過 2 筆時計算剩餘筆數並產生「＋N 項」標籤。
     */
    fun resolveMonthGrid(
        year: Int,
        month: Int,
        notes: List<Note>,
        todos: List<ToDo>,
        todayMillis: Long = System.currentTimeMillis(),
        dateNormalizer: (Long) -> Long = ToDoWidgetLogic::normalizeToLocalMidnight
    ): ResolvedCalendarWidgetState {
        val normalizedToday = dateNormalizer(todayMillis)

        val eventsByDay = buildEventsByDayMap(
            notes = notes,
            todos = todos,
            dateNormalizer = dateNormalizer
        )

        val cursor = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val firstDayOfMonthMillis = dateNormalizer(cursor.timeInMillis)
        val leadingDaysCount = cursor.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        cursor.add(Calendar.DAY_OF_MONTH, -leadingDaysCount)

        var todayContainedInMonth = false
        val cells = ArrayList<CalendarDayCellState>(GRID_CELL_COUNT)

        repeat(GRID_CELL_COUNT) {
            val cellYear = cursor.get(Calendar.YEAR)
            val cellMonth = cursor.get(Calendar.MONTH) + 1
            val cellDay = cursor.get(Calendar.DAY_OF_MONTH)
            val cellMidnight = dateNormalizer(cursor.timeInMillis)
            val isCurrentMonth = cellYear == year && cellMonth == month
            val isToday = cellMidnight == normalizedToday
            if (isToday && isCurrentMonth) {
                todayContainedInMonth = true
            }

            val dayEvents = eventsByDay[cellMidnight].orEmpty()
            val visibleEvents = dayEvents.take(MAX_VISIBLE_EVENTS)
            val overflowCount = (dayEvents.size - MAX_VISIBLE_EVENTS).coerceAtLeast(0)
            val overflowLabel = if (overflowCount > 0) "＋${overflowCount} 項" else null

            cells.add(
                CalendarDayCellState(
                    dayStartMillis = cellMidnight,
                    dayOfMonth = cellDay,
                    isCurrentMonth = isCurrentMonth,
                    isToday = isToday,
                    visibleEvents = visibleEvents,
                    overflowCount = overflowCount,
                    overflowLabel = overflowLabel
                )
            )

            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }

        val quickAddDefaultDateMillis = if (todayContainedInMonth) {
            normalizedToday
        } else {
            firstDayOfMonthMillis
        }

        return ResolvedCalendarWidgetState(
            year = year,
            month = month,
            headerTitle = "${year}年 ${month}月",
            quickAddDefaultDateMillis = quickAddDefaultDateMillis,
            cells = cells
        )
    }

    private fun buildEventsByDayMap(
        notes: List<Note>,
        todos: List<ToDo>,
        dateNormalizer: (Long) -> Long
    ): Map<Long, List<CalendarDayEvent>> {
        val groupedTodos = todos
            .filter { it.dueDate != null }
            .groupBy { dateNormalizer(it.dueDate!!) }

        val groupedNotes = notes
            .groupBy { dateNormalizer(it.createdAt) }

        val allDays = (groupedTodos.keys + groupedNotes.keys).distinct()
        val result = HashMap<Long, List<CalendarDayEvent>>(allDays.size)

        allDays.forEach { dayMillis ->
            val dayTodos = groupedTodos[dayMillis].orEmpty()
            val activeToDoEvents = dayTodos
                .filter { !it.isCompleted }
                .sortedWith(
                    compareByDescending<ToDo> { it.priority.level }
                        .thenByDescending { it.createdAt }
                        .thenByDescending { it.id }
                )
                .map { todo ->
                    CalendarDayEvent(
                        id = todo.id,
                        type = CalendarEventType.TODO,
                        title = todo.title.ifBlank { "未命名待辦" },
                        isCompleted = false
                    )
                }

            val noteEvents = groupedNotes[dayMillis].orEmpty()
                .sortedWith(
                    compareByDescending<Note> { it.isPinned }
                        .thenByDescending { it.updatedAt }
                        .thenByDescending { it.id }
                )
                .map { note ->
                    CalendarDayEvent(
                        id = note.id,
                        type = CalendarEventType.NOTE,
                        title = note.title.ifBlank { "（無標題）" },
                        isPinned = note.isPinned
                    )
                }

            val completedToDoEvents = dayTodos
                .filter { it.isCompleted }
                .sortedByDescending { it.createdAt }
                .map { todo ->
                    CalendarDayEvent(
                        id = todo.id,
                        type = CalendarEventType.TODO,
                        title = "✓ ${todo.title.ifBlank { "未命名待辦" }}",
                        isCompleted = true
                    )
                }

            result[dayMillis] = activeToDoEvents + noteEvents + completedToDoEvents
        }

        return result
    }
}
