package com.example.notepad.reminder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ReminderScheduler 排程規則單元測試（規範 4.4）
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
}
