package com.example.notepad.reminder

import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.ToDo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ReminderScheduler 排程規則與開機／更新後重新排程篩選邏輯單元測試（規範 3.4 / 4.4）
 */
class ReminderSchedulerLogicTest {

    @Test
    fun `rejects past or current timestamps even when notifications are enabled`() {
        val now = 1_000_000L
        assertFalse(ReminderScheduler.isReminderScheduleAllowed(triggerAt = 999_999L, notificationsEnabled = true, currentTimeMillis = now))
        assertFalse(ReminderScheduler.isReminderScheduleAllowed(triggerAt = 1_000_000L, notificationsEnabled = true, currentTimeMillis = now))
    }

    @Test
    fun `rejects future timestamps when notifications master switch is disabled`() {
        val now = 1_000_000L
        assertFalse(ReminderScheduler.isReminderScheduleAllowed(triggerAt = 2_000_000L, notificationsEnabled = false, currentTimeMillis = now))
    }

    @Test
    fun `allows future timestamps when notifications master switch is enabled`() {
        val now = 1_000_000L
        assertTrue(ReminderScheduler.isReminderScheduleAllowed(triggerAt = 1_000_001L, notificationsEnabled = true, currentTimeMillis = now))
    }

    @Test
    fun `reschedule filter keeps only unfired future reminders not linked to completed todos`() {
        val now = 5_000_000L
        val todos = listOf(
            ToDo(id = 10, title = "已完成任務", isCompleted = true),
            ToDo(id = 11, title = "未完成任務", isCompleted = false)
        )
        val reminders = listOf(
            Reminder(id = 1, title = "已過期", triggerAt = 4_999_999L, isFired = false),
            Reminder(id = 2, title = "已觸發", triggerAt = 6_000_000L, isFired = true),
            Reminder(id = 3, title = "關聯已完成任務", triggerAt = 6_000_000L, isFired = false, linkedToDoId = 10),
            Reminder(id = 4, title = "關聯未完成任務", triggerAt = 6_500_000L, isFired = false, linkedToDoId = 11),
            Reminder(id = 5, title = "獨立未來提醒", triggerAt = 7_000_000L, isFired = false, linkedToDoId = null)
        )

        val toReschedule = ReminderScheduler.filterRemindersToReschedule(
            reminders = reminders,
            todos = todos,
            notificationsEnabled = true,
            currentTimeMillis = now
        )
        assertEquals(listOf(4, 5), toReschedule.map { it.id })

        // 全域通知關閉時不得重新排程任何提醒
        val whenDisabled = ReminderScheduler.filterRemindersToReschedule(
            reminders = reminders,
            todos = todos,
            notificationsEnabled = false,
            currentTimeMillis = now
        )
        assertTrue(whenDisabled.isEmpty())
    }

    @Test
    fun `boot receiver accepts boot completed package replaced and quickboot actions only`() {
        assertTrue(BootCompletedReceiver.isSupportedBootAction("android.intent.action.BOOT_COMPLETED"))
        assertTrue(BootCompletedReceiver.isSupportedBootAction("android.intent.action.MY_PACKAGE_REPLACED"))
        assertTrue(BootCompletedReceiver.isSupportedBootAction("android.intent.action.QUICKBOOT_POWERON"))
        assertTrue(BootCompletedReceiver.isSupportedBootAction("com.htc.intent.action.QUICKBOOT_POWERON"))

        assertFalse(BootCompletedReceiver.isSupportedBootAction(null))
        assertFalse(BootCompletedReceiver.isSupportedBootAction("com.example.notepad.UNKNOWN_ACTION"))
    }
}
