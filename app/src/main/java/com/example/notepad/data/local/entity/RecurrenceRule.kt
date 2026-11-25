package com.example.notepad.data.local.entity

import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

/**
 * 例行重複任務週期類型
 */
enum class RecurrenceType(val code: Int, val label: String) {
    NONE(0, "不重複"),
    DAILY(1, "每天"),
    WEEKLY(2, "每週"),
    MONTHLY(3, "每月");

    companion object {
        fun fromCode(code: Int): RecurrenceType {
            return entries.firstOrNull { it.code == code } ?: NONE
        }
    }
}

/**
 * 自動化重複待辦事項（Recurring Tasks）核心演算法與顯示文字封裝
 */
object RecurringTaskLogic {

    /** 星期選項對照（對應 [Calendar.SUNDAY] .. [Calendar.SATURDAY]） */
    val WEEKDAY_OPTIONS: List<Pair<Int, String>> = listOf(
        Calendar.MONDAY to "一",
        Calendar.TUESDAY to "二",
        Calendar.WEDNESDAY to "三",
        Calendar.THURSDAY to "四",
        Calendar.FRIDAY to "五",
        Calendar.SATURDAY to "六",
        Calendar.SUNDAY to "日"
    )

    /**
     * 取得簡短星期中文（日／一／二／三／四／五／六）
     */
    fun dayOfWeekShortName(calendarDayOfWeek: Int): String {
        return when (calendarDayOfWeek) {
            Calendar.SUNDAY -> "日"
            Calendar.MONDAY -> "一"
            Calendar.TUESDAY -> "二"
            Calendar.WEDNESDAY -> "三"
            Calendar.THURSDAY -> "四"
            Calendar.FRIDAY -> "五"
            Calendar.SATURDAY -> "六"
            else -> ""
        }
    }

    /**
     * 產生卡片與詳情頁顯示之重複規則標籤（例如「🔁 每週一」、「🔁 每月15日」）
     */
    fun formatRuleLabel(
        recurrenceType: RecurrenceType,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null
    ): String {
        return when (recurrenceType) {
            RecurrenceType.NONE -> ""
            RecurrenceType.DAILY -> "🔁 每天"
            RecurrenceType.WEEKLY -> {
                val weekText = dayOfWeek?.let(::dayOfWeekShortName).orEmpty()
                if (weekText.isNotEmpty()) "🔁 每週$weekText" else "🔁 每週"
            }

            RecurrenceType.MONTHLY -> {
                val monthText = dayOfMonth?.let { "${it.coerceIn(1, 31)}日" }.orEmpty()
                if (monthText.isNotEmpty()) "🔁 每月$monthText" else "🔁 每月"
            }
        }
    }

    /**
     * 將時間戳正規化至指定時區當日 00:00:00.000
     */
    fun normalizeStartOfDay(
        millis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * 依所選重複規則推算新增／切換規則時建議的首期到期日（從當日起算最近符合日）
     */
    fun resolveInitialDueDate(
        recurrenceType: RecurrenceType,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        referenceMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        if (recurrenceType == RecurrenceType.NONE) return null
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = normalizeStartOfDay(referenceMillis, timeZone)
        }
        when (recurrenceType) {
            RecurrenceType.NONE -> return null
            RecurrenceType.DAILY -> return cal.timeInMillis
            RecurrenceType.WEEKLY -> {
                val targetWeekday = dayOfWeek?.takeIf { it in Calendar.SUNDAY..Calendar.SATURDAY }
                    ?: cal.get(Calendar.DAY_OF_WEEK)
                repeat(7) {
                    if (cal.get(Calendar.DAY_OF_WEEK) == targetWeekday) {
                        return cal.timeInMillis
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return cal.timeInMillis
            }

            RecurrenceType.MONTHLY -> {
                val targetDay = (dayOfMonth ?: cal.get(Calendar.DAY_OF_MONTH)).coerceIn(1, 31)
                val todayDay = cal.get(Calendar.DAY_OF_MONTH)
                val currentMonthTarget = minOf(targetDay, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                if (currentMonthTarget >= todayDay) {
                    cal.set(Calendar.DAY_OF_MONTH, currentMonthTarget)
                } else {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.add(Calendar.MONTH, 1)
                    val nextMax = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, minOf(targetDay, nextMax))
                }
                return cal.timeInMillis
            }
        }
    }

    /**
     * 計算完成當期任務後，下一期應自動產生的到期日（正規化至當日 00:00:00.000）
     */
    fun calculateNextDueDate(
        currentDueDate: Long?,
        recurrenceType: RecurrenceType,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        completedAtMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long? {
        if (recurrenceType == RecurrenceType.NONE) return null

        val anchorMillis = normalizeStartOfDay(currentDueDate ?: completedAtMillis, timeZone)
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = anchorMillis
        }

        return when (recurrenceType) {
            RecurrenceType.NONE -> null
            RecurrenceType.DAILY -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }

            RecurrenceType.WEEKLY -> {
                val targetWeekday = dayOfWeek?.takeIf { it in Calendar.SUNDAY..Calendar.SATURDAY }
                    ?: cal.get(Calendar.DAY_OF_WEEK)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                repeat(7) {
                    if (cal.get(Calendar.DAY_OF_WEEK) == targetWeekday) {
                        return cal.timeInMillis
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }

            RecurrenceType.MONTHLY -> {
                val targetDay = (dayOfMonth ?: cal.get(Calendar.DAY_OF_MONTH)).coerceIn(1, 31)
                if (currentDueDate == null) {
                    val anchorDay = cal.get(Calendar.DAY_OF_MONTH)
                    val thisMonthTarget = minOf(targetDay, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    if (thisMonthTarget > anchorDay) {
                        cal.set(Calendar.DAY_OF_MONTH, thisMonthTarget)
                        return cal.timeInMillis
                    }
                }
                // 移至下個月 1 號並依該月最大天數截斷（例如 1/31 -> 2/28，且保留 targetDay=31 下期回到 3/31）
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, 1)
                val maxDayInNextMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, minOf(targetDay, maxDayInNextMonth))
                cal.timeInMillis
            }
        }
    }

    /**
     * 依據已勾選完成的重複待辦，建立下一期待辦事項實例；
     * 若不具備重複規則或先前已產生過下一期，則回傳 `null` 防止重複衍生。
     */
    fun createNextOccurrence(
        completedToDo: ToDo,
        completedAtMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): ToDo? {
        if (completedToDo.recurrenceType == RecurrenceType.NONE || completedToDo.nextOccurrenceGenerated) {
            return null
        }

        val anchorCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = normalizeStartOfDay(completedToDo.dueDate ?: completedAtMillis, timeZone)
        }
        val effectiveDayOfWeek = if (completedToDo.recurrenceType == RecurrenceType.WEEKLY) {
            completedToDo.recurrenceDayOfWeek?.takeIf { it in Calendar.SUNDAY..Calendar.SATURDAY }
                ?: anchorCal.get(Calendar.DAY_OF_WEEK)
        } else {
            null
        }
        val effectiveDayOfMonth = if (completedToDo.recurrenceType == RecurrenceType.MONTHLY) {
            completedToDo.recurrenceDayOfMonth?.coerceIn(1, 31)
                ?: anchorCal.get(Calendar.DAY_OF_MONTH)
        } else {
            null
        }

        val nextDueDate = calculateNextDueDate(
            currentDueDate = completedToDo.dueDate,
            recurrenceType = completedToDo.recurrenceType,
            dayOfWeek = effectiveDayOfWeek,
            dayOfMonth = effectiveDayOfMonth,
            completedAtMillis = completedAtMillis,
            timeZone = timeZone
        )

        return ToDo(
            id = 0,
            title = completedToDo.title,
            description = completedToDo.description,
            isCompleted = false,
            dueDate = nextDueDate,
            tag = completedToDo.tag,
            color = completedToDo.color,
            createdAt = completedAtMillis,
            priority = completedToDo.priority,
            recurrenceType = completedToDo.recurrenceType,
            recurrenceDayOfWeek = effectiveDayOfWeek,
            recurrenceDayOfMonth = effectiveDayOfMonth,
            nextOccurrenceGenerated = false,
            syncId = UUID.randomUUID().toString(),
            updatedAt = completedAtMillis,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
    }
}
