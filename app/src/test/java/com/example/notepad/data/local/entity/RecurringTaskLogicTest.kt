package com.example.notepad.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * [RecurringTaskLogic] 單元測試：
 * 驗證每天／每週（指定星期）／每月（含月底 31 日保留不偏移）自動計算下期到期日、
 * 規則文字格式化與防止重複生成機制。
 */
class RecurringTaskLogicTest {

    private val testTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Taipei")

    private fun dateMillis(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance(testTimeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun formatRuleLabel_formatsDailyWeeklyMonthlyAndNoneCleanly() {
        assertEquals("", RecurringTaskLogic.formatRuleLabel(RecurrenceType.NONE))
        assertEquals("🔁 每天", RecurringTaskLogic.formatRuleLabel(RecurrenceType.DAILY))
        assertEquals(
            "🔁 每週一",
            RecurringTaskLogic.formatRuleLabel(RecurrenceType.WEEKLY, dayOfWeek = Calendar.MONDAY)
        )
        assertEquals(
            "🔁 每月15日",
            RecurringTaskLogic.formatRuleLabel(RecurrenceType.MONTHLY, dayOfMonth = 15)
        )
    }

    @Test
    fun calculateNextDueDate_daily_advancesByOneCalendarDay() {
        val currentDue = dateMillis(2026, 11, 30)
        val nextDue = RecurringTaskLogic.calculateNextDueDate(
            currentDueDate = currentDue,
            recurrenceType = RecurrenceType.DAILY,
            timeZone = testTimeZone
        )
        assertEquals(dateMillis(2026, 12, 1), nextDue)
    }

    @Test
    fun calculateNextDueDate_weekly_matchesEveryMondayMeeting() {
        // 2026-11-30 為週一 -> 下一個週一應為 2026-12-07
        val currentDue = dateMillis(2026, 11, 30)
        val nextDue = RecurringTaskLogic.calculateNextDueDate(
            currentDueDate = currentDue,
            recurrenceType = RecurrenceType.WEEKLY,
            dayOfWeek = Calendar.MONDAY,
            timeZone = testTimeZone
        )
        assertEquals(dateMillis(2026, 12, 7), nextDue)
    }

    @Test
    fun calculateNextDueDate_monthly_clampsShortMonthAndRestoresOriginalDay() {
        // 1 月 31 日 -> 2 月截斷為 2 月 28 日；因保留 dayOfMonth=31，再下一期應回到 3 月 31 日
        val jan31 = dateMillis(2026, 1, 31)
        val feb28 = RecurringTaskLogic.calculateNextDueDate(
            currentDueDate = jan31,
            recurrenceType = RecurrenceType.MONTHLY,
            dayOfMonth = 31,
            timeZone = testTimeZone
        )
        assertEquals(dateMillis(2026, 2, 28), feb28)

        val mar31 = RecurringTaskLogic.calculateNextDueDate(
            currentDueDate = feb28,
            recurrenceType = RecurrenceType.MONTHLY,
            dayOfMonth = 31,
            timeZone = testTimeZone
        )
        assertEquals(dateMillis(2026, 3, 31), mar31)
    }

    @Test
    fun createNextOccurrence_createsUncompletedNextTaskAndPreventsDuplicateGeneration() {
        val mondayDue = dateMillis(2026, 11, 23)
        val currentTask = ToDo(
            id = 12,
            title = "每週一專案同步會議",
            description = "核對週進度",
            isCompleted = false,
            dueDate = mondayDue,
            tag = "工作",
            color = 0xFF1976D2L,
            priority = Priority.HIGH,
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceDayOfWeek = Calendar.MONDAY,
            nextOccurrenceGenerated = false
        )

        val nextTask = RecurringTaskLogic.createNextOccurrence(
            completedToDo = currentTask,
            completedAtMillis = dateMillis(2026, 11, 23),
            timeZone = testTimeZone
        )
        assertNotNull(nextTask)
        assertEquals(0, nextTask!!.id)
        assertFalse(nextTask.isCompleted)
        assertFalse(nextTask.nextOccurrenceGenerated)
        assertEquals(dateMillis(2026, 11, 30), nextTask.dueDate)
        assertEquals(RecurrenceType.WEEKLY, nextTask.recurrenceType)
        assertEquals(Calendar.MONDAY, nextTask.recurrenceDayOfWeek)
        assertEquals(Priority.HIGH, nextTask.priority)
        assertTrue(nextTask.syncId != currentTask.syncId)

        // 若已完成且曾生成下期（nextOccurrenceGenerated = true），不得再次生成重複項目
        val alreadyGenerated = currentTask.copy(nextOccurrenceGenerated = true)
        assertNull(
            RecurringTaskLogic.createNextOccurrence(
                completedToDo = alreadyGenerated,
                completedAtMillis = dateMillis(2026, 11, 23),
                timeZone = testTimeZone
            )
        )
    }
}
