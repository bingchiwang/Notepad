package com.example.notepad.widget

import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 今日行程小工具完成進度統計資料模型
 */
data class TodayProgressStats(
    val completedCount: Int,
    val totalCount: Int,
    val percent: Int,
    val summaryText: String
)

/**
 * 即將到來的提醒展示項目
 */
data class WidgetReminderItem(
    val id: Int,
    val title: String,
    val messagePreview: String,
    val timeText: String,
    val accentColor: Long,
    val isToday: Boolean
)

/**
 * 今日行程小工具多型態清單列模型
 */
sealed interface TodayScheduleRow {
    val stableId: Long

    data class SectionHeader(
        override val stableId: Long,
        val title: String,
        val countText: String
    ) : TodayScheduleRow

    data class DueTodayToDoRow(
        val item: WidgetToDoItem
    ) : TodayScheduleRow {
        override val stableId: Long get() = item.id.toLong()
    }

    data class UpcomingReminderRow(
        val reminder: WidgetReminderItem
    ) : TodayScheduleRow {
        override val stableId: Long get() = 100_000_000L + reminder.id.toLong()
    }

    data class EmptyNoticeRow(
        override val stableId: Long,
        val message: String
    ) : TodayScheduleRow
}

/**
 * 經統計與分類排序後的今日行程小工具完整狀態
 */
data class ResolvedTodayScheduleState(
    val headerDateText: String,
    val progress: TodayProgressStats,
    val dueTodayToDosCount: Int,
    val upcomingRemindersCount: Int,
    val rows: List<TodayScheduleRow>
)

/**
 * 今日行程桌面小工具核心業務邏輯（純 Kotlin 實作，支援完整單元測試）
 *
 * 職責：
 * 1. 計算今日完成進度（彙整今日排程時間區塊與今日到期之待辦事項）
 * 2. 篩選今天到期的未完成待辦（重用 [ToDoWidgetLogic] 排序與格式化邏輯）
 * 3. 篩選尚未觸發且晚於目前時間的即將到來提醒（依觸發時間升冪排序）
 */
object TodayScheduleWidgetLogic {

    const val DEFAULT_MAX_DUE_TODOS = 8
    const val DEFAULT_MAX_REMINDERS = 5

    private const val ID_HEADER_DUE_TODAY = -10L
    private const val ID_HEADER_REMINDERS = -11L
    private const val ID_EMPTY_DUE_TODAY = -20L
    private const val ID_EMPTY_REMINDERS = -21L

    fun resolveTodaySchedule(
        todos: List<ToDo>,
        timeBlocks: List<TimeBlock>,
        reminders: List<Reminder>,
        nowMillis: Long = System.currentTimeMillis(),
        maxDueTodayTodos: Int = DEFAULT_MAX_DUE_TODOS,
        maxUpcomingReminders: Int = DEFAULT_MAX_REMINDERS,
        dateNormalizer: (Long) -> Long = ToDoWidgetLogic::normalizeToLocalMidnight,
        headerDateFormatter: (Long) -> String = ::formatTodayHeaderDate,
        timeFormatter: (Long) -> String = ::formatReminderTime,
        dateFormatter: (Long) -> String = ToDoWidgetLogic::formatDueDate
    ): ResolvedTodayScheduleState {
        val normalizedToday = dateNormalizer(nowMillis)
        val normalizedTomorrow = ToDoWidgetLogic.addDaysToMidnight(normalizedToday, 1)

        // 1. 彙整今日時間規劃區塊與今天到期的待辦任務以計算今日完成進度
        val todayBlocks = timeBlocks.filter { dateNormalizer(it.date) == normalizedToday }
        val allDueTodayTodos = todos.filter { todo ->
            todo.dueDate != null && dateNormalizer(todo.dueDate) == normalizedToday
        }

        val totalTodayCount = todayBlocks.size + allDueTodayTodos.size
        val completedTodayCount = todayBlocks.count { it.isCompleted } + allDueTodayTodos.count { it.isCompleted }
        val progressPercent = if (totalTodayCount > 0) {
            ((completedTodayCount * 100f) / totalTodayCount).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val progressSummary = if (totalTodayCount > 0) {
            "$completedTodayCount / $totalTodayCount · $progressPercent%"
        } else {
            "尚無今日項目 · 0%"
        }

        // 2. 重用 ToDoWidgetLogic 處理今天到期的未完成待辦排序與模型轉換
        val activeDueToday = ToDoWidgetLogic.resolveActiveTasks(
            todos = allDueTodayTodos,
            todayMillis = nowMillis,
            maxItems = maxDueTodayTodos,
            dateNormalizer = dateNormalizer,
            dateFormatter = dateFormatter
        )

        // 3. 篩選尚未觸發且未過期的即將到來提醒
        val upcomingRemindersAll = reminders
            .filter { !it.isFired && it.triggerAt > nowMillis }
            .sortedWith(compareBy<Reminder> { it.triggerAt }.thenBy { it.id })

        val upcomingItems = upcomingRemindersAll
            .take(maxUpcomingReminders.coerceAtLeast(1))
            .map { reminder ->
                val normalizedTriggerDay = dateNormalizer(reminder.triggerAt)
                val timePart = timeFormatter(reminder.triggerAt)
                val (displayTime, isToday) = when (normalizedTriggerDay) {
                    normalizedToday -> "今天 $timePart" to true
                    normalizedTomorrow -> "明天 $timePart" to false
                    else -> "${dateFormatter(reminder.triggerAt)} $timePart" to false
                }
                WidgetReminderItem(
                    id = reminder.id,
                    title = reminder.title.trim().ifEmpty { "（未命名提醒）" },
                    messagePreview = formatReminderMessage(reminder.message),
                    timeText = displayTime,
                    accentColor = reminder.color,
                    isToday = isToday
                )
            }

        // 4. 組合分區清單列
        val rows = buildList {
            add(
                TodayScheduleRow.SectionHeader(
                    stableId = ID_HEADER_DUE_TODAY,
                    title = "今天到期的待辦",
                    countText = "${activeDueToday.activeCount} 項"
                )
            )
            if (activeDueToday.items.isNotEmpty()) {
                activeDueToday.items.forEach { add(TodayScheduleRow.DueTodayToDoRow(it)) }
            } else if (allDueTodayTodos.isNotEmpty()) {
                add(TodayScheduleRow.EmptyNoticeRow(ID_EMPTY_DUE_TODAY, "今日到期任務已全數完成 ✓"))
            } else {
                add(TodayScheduleRow.EmptyNoticeRow(ID_EMPTY_DUE_TODAY, "今天沒有到期的待辦任務"))
            }

            add(
                TodayScheduleRow.SectionHeader(
                    stableId = ID_HEADER_REMINDERS,
                    title = "即將到來的提醒",
                    countText = "${upcomingRemindersAll.size} 項"
                )
            )
            if (upcomingItems.isNotEmpty()) {
                upcomingItems.forEach { add(TodayScheduleRow.UpcomingReminderRow(it)) }
            } else {
                add(TodayScheduleRow.EmptyNoticeRow(ID_EMPTY_REMINDERS, "目前沒有即將到來的提醒"))
            }
        }

        return ResolvedTodayScheduleState(
            headerDateText = headerDateFormatter(nowMillis),
            progress = TodayProgressStats(
                completedCount = completedTodayCount,
                totalCount = totalTodayCount,
                percent = progressPercent,
                summaryText = progressSummary
            ),
            dueTodayToDosCount = activeDueToday.activeCount,
            upcomingRemindersCount = upcomingRemindersAll.size,
            rows = rows
        )
    }

    /**
     * 將提醒訊息摺疊為單行摘要文字
     */
    fun formatReminderMessage(message: String): String {
        val normalized = message
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (normalized.length > 60) {
            normalized.take(60).trimEnd() + "…"
        } else {
            normalized
        }
    }

    /**
     * 格式化小工具頂部今日日期與星期標籤
     */
    fun formatTodayHeaderDate(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val weekday = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "週日"
            Calendar.MONDAY -> "週一"
            Calendar.TUESDAY -> "週二"
            Calendar.WEDNESDAY -> "週三"
            Calendar.THURSDAY -> "週四"
            Calendar.FRIDAY -> "週五"
            Calendar.SATURDAY -> "週六"
            else -> ""
        }
        return "$month/$day $weekday"
    }

    /**
     * 格式化提醒時間為 HH:mm
     */
    fun formatReminderTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
