package com.example.notepad.widget

import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.ToDo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 桌面待辦小工具之截止日期緊急程度分級
 */
enum class DueDateUrgency {
    NONE,
    OVERDUE,
    TODAY,
    TOMORROW,
    UPCOMING
}

/**
 * 桌面待辦小工具單筆任務展示模型
 */
data class WidgetToDoItem(
    val id: Int,
    val title: String,
    val priority: Priority,
    val priorityLabel: String,
    val dueDateText: String,
    val dueDateUrgency: DueDateUrgency,
    val tagText: String
)

/**
 * 經未完成過濾、優先級與截止日排序後的待辦小工具完整狀態
 */
data class ResolvedToDoWidgetState(
    val activeCount: Int,
    val items: List<WidgetToDoItem>
)

/**
 * 待辦事項桌面小工具核心業務邏輯與格式化工具（純 Kotlin 實作，支援完整單元測試）
 */
object ToDoWidgetLogic {

    const val DEFAULT_MAX_ITEMS = 15

    /**
     * 過濾僅保留未完成任務（遵循規範 3.2：已完成任務不得繼續顯示為未完成），
     * 並依「高＞中＞低優先級、截止日期由近到遠（未設定者排後）、建立時間最新優先」排序。
     */
    fun resolveActiveTasks(
        todos: List<ToDo>,
        todayMillis: Long = System.currentTimeMillis(),
        maxItems: Int = DEFAULT_MAX_ITEMS,
        dateNormalizer: (Long) -> Long = ::normalizeToLocalMidnight,
        dateFormatter: (Long) -> String = ::formatDueDate
    ): ResolvedToDoWidgetState {
        val normalizedToday = dateNormalizer(todayMillis)
        val normalizedTomorrow = addDaysToMidnight(normalizedToday, 1)

        val activeTodos = todos
            .filter { !it.isCompleted }
            .sortedWith(
                compareByDescending<ToDo> { it.priority.level }
                    .thenBy { it.dueDate?.let(dateNormalizer) ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
                    .thenByDescending { it.id }
            )

        val items = activeTodos
            .take(maxItems.coerceAtLeast(1))
            .map { todo ->
                val (dueText, urgency) = resolveDueDateInfo(
                    dueDateMillis = todo.dueDate,
                    todayMidnightMillis = normalizedToday,
                    tomorrowMidnightMillis = normalizedTomorrow,
                    dateNormalizer = dateNormalizer,
                    dateFormatter = dateFormatter
                )
                val cleanTag = todo.tag.trim()
                WidgetToDoItem(
                    id = todo.id,
                    title = todo.title.trim().ifEmpty { "（無名稱任務）" },
                    priority = todo.priority,
                    priorityLabel = "${todo.priority.label}優先",
                    dueDateText = dueText,
                    dueDateUrgency = urgency,
                    tagText = if (cleanTag.isNotEmpty()) "#$cleanTag" else ""
                )
            }

        return ResolvedToDoWidgetState(
            activeCount = activeTodos.size,
            items = items
        )
    }

    /**
     * 計算任務截止日期之顯示文字與緊急狀態分類
     */
    fun resolveDueDateInfo(
        dueDateMillis: Long?,
        todayMidnightMillis: Long,
        tomorrowMidnightMillis: Long = addDaysToMidnight(todayMidnightMillis, 1),
        dateNormalizer: (Long) -> Long = ::normalizeToLocalMidnight,
        dateFormatter: (Long) -> String = ::formatDueDate
    ): Pair<String, DueDateUrgency> {
        if (dueDateMillis == null) {
            return "未設截止日" to DueDateUrgency.NONE
        }
        val normalizedDue = dateNormalizer(dueDateMillis)
        val formatted = dateFormatter(dueDateMillis)
        return when {
            normalizedDue < todayMidnightMillis -> "已逾期 · $formatted" to DueDateUrgency.OVERDUE
            normalizedDue == todayMidnightMillis -> "今天到期 · $formatted" to DueDateUrgency.TODAY
            normalizedDue == tomorrowMidnightMillis -> "明天到期 · $formatted" to DueDateUrgency.TOMORROW
            else -> "截止 $formatted" to DueDateUrgency.UPCOMING
        }
    }

    /**
     * 將時間戳正規化為本地時區當日 00:00:00.000
     */
    fun normalizeToLocalMidnight(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * 於午夜時間戳加上指定天數（避開夏令時間加減誤差）
     */
    fun addDaysToMidnight(midnightMillis: Long, days: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = midnightMillis
            add(Calendar.DAY_OF_YEAR, days)
        }
        return calendar.timeInMillis
    }

    /**
     * 格式化截止日期為 MM/dd
     */
    fun formatDueDate(timestamp: Long): String {
        return SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
    }
}
