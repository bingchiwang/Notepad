package com.example.notepad.widget

import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * 今日行程桌面小工具核心邏輯、完成進度統計、提醒篩選排序與深度連結單元測試
 */
class TodayScheduleWidgetLogicTest {

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `僅顯示今天到期的未完成待辦並排除昨天明天及已完成項目`() {
        val now = localMillis(2026, 11, 15, 10, 0)
        val todos = listOf(
            ToDo(id = 1, title = "昨天逾期", isCompleted = false, dueDate = localMillis(2026, 11, 14), priority = Priority.HIGH),
            ToDo(id = 2, title = "今天低優先", isCompleted = false, dueDate = localMillis(2026, 11, 15, 18, 0), priority = Priority.LOW),
            ToDo(id = 3, title = "今天已完成", isCompleted = true, dueDate = localMillis(2026, 11, 15), priority = Priority.HIGH),
            ToDo(id = 4, title = "今天高優先", isCompleted = false, dueDate = localMillis(2026, 11, 15, 9, 0), priority = Priority.HIGH),
            ToDo(id = 5, title = "明天到期", isCompleted = false, dueDate = localMillis(2026, 11, 16), priority = Priority.HIGH),
            ToDo(id = 6, title = "無截止日", isCompleted = false, dueDate = null, priority = Priority.HIGH)
        )

        val resolved = TodayScheduleWidgetLogic.resolveTodaySchedule(
            todos = todos,
            timeBlocks = emptyList(),
            reminders = emptyList(),
            nowMillis = now
        )

        assertEquals(2, resolved.dueTodayToDosCount)
        val dueRows = resolved.rows.filterIsInstance<TodayScheduleRow.DueTodayToDoRow>()
        assertEquals(listOf(4, 2), dueRows.map { it.item.id })
    }

    @Test
    fun `應彙整今日時間規劃與今天到期任務正確計算完成進度`() {
        val todayMidnight = localMillis(2026, 11, 15)
        val now = localMillis(2026, 11, 15, 14, 0)
        val todos = listOf(
            ToDo(id = 1, title = "今日已完成任務", isCompleted = true, dueDate = todayMidnight),
            ToDo(id = 2, title = "今日未完成任務", isCompleted = false, dueDate = todayMidnight),
            ToDo(id = 3, title = "明日任務不計入今日進度", isCompleted = true, dueDate = localMillis(2026, 11, 16))
        )
        val blocks = listOf(
            TimeBlock(id = 10, date = todayMidnight, startTime = "09:00", endTime = "10:00", title = "晨間會議", isCompleted = true),
            TimeBlock(id = 11, date = todayMidnight, startTime = "15:00", endTime = "16:00", title = "程式碼審查", isCompleted = false),
            TimeBlock(id = 12, date = localMillis(2026, 11, 14), startTime = "09:00", endTime = "10:00", title = "昨日行程", isCompleted = false)
        )

        val resolved = TodayScheduleWidgetLogic.resolveTodaySchedule(
            todos = todos,
            timeBlocks = blocks,
            reminders = emptyList(),
            nowMillis = now
        )

        // 今日共 4 項（2 TimeBlock + 2 Today ToDo），其中 2 項已完成 => 50%
        assertEquals(2, resolved.progress.completedCount)
        assertEquals(4, resolved.progress.totalCount)
        assertEquals(50, resolved.progress.percent)
        assertEquals("2 / 4 · 50%", resolved.progress.summaryText)
    }

    @Test
    fun `今日無排程與到期項目時進度應為零且全完成應顯示完成提示`() {
        val now = localMillis(2026, 11, 15, 20, 0)
        val emptyState = TodayScheduleWidgetLogic.resolveTodaySchedule(
            todos = emptyList(),
            timeBlocks = emptyList(),
            reminders = emptyList(),
            nowMillis = now
        )
        assertEquals(0, emptyState.progress.totalCount)
        assertEquals(0, emptyState.progress.percent)
        assertEquals("尚無今日項目 · 0%", emptyState.progress.summaryText)

        val allDoneState = TodayScheduleWidgetLogic.resolveTodaySchedule(
            todos = listOf(
                ToDo(id = 1, title = "已完成", isCompleted = true, dueDate = localMillis(2026, 11, 15))
            ),
            timeBlocks = emptyList(),
            reminders = emptyList(),
            nowMillis = now
        )
        assertEquals(100, allDoneState.progress.percent)
        val emptyNotices = allDoneState.rows.filterIsInstance<TodayScheduleRow.EmptyNoticeRow>()
        assertTrue(emptyNotices.any { it.message.contains("全數完成") })
    }

    @Test
    fun `應過濾已觸發與過期提醒並依時間升冪格式化顯示即將到來的提醒`() {
        val now = localMillis(2026, 11, 15, 20, 0)
        val reminders = listOf(
            Reminder(id = 1, title = "已過期", triggerAt = localMillis(2026, 11, 15, 19, 0), isFired = false),
            Reminder(id = 2, title = "已觸發", triggerAt = localMillis(2026, 11, 15, 21, 0), isFired = true),
            Reminder(id = 3, title = "三天後體檢", message = "空腹抽血", triggerAt = localMillis(2026, 11, 18, 8, 30), isFired = false),
            Reminder(id = 4, title = "睡前吃藥", message = "第一行\n第二行", triggerAt = localMillis(2026, 11, 15, 22, 15), isFired = false),
            Reminder(id = 5, title = "  ", message = "", triggerAt = localMillis(2026, 11, 16, 9, 5), isFired = false)
        )

        val resolved = TodayScheduleWidgetLogic.resolveTodaySchedule(
            todos = emptyList(),
            timeBlocks = emptyList(),
            reminders = reminders,
            nowMillis = now,
            timeFormatter = { ts ->
                val cal = Calendar.getInstance().apply { timeInMillis = ts }
                "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            },
            dateFormatter = { ts ->
                val cal = Calendar.getInstance().apply { timeInMillis = ts }
                "%02d/%02d".format(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            }
        )

        assertEquals(3, resolved.upcomingRemindersCount)
        val reminderRows = resolved.rows.filterIsInstance<TodayScheduleRow.UpcomingReminderRow>().map { it.reminder }
        assertEquals(listOf(4, 5, 3), reminderRows.map { it.id })

        assertTrue(reminderRows[0].isToday)
        assertEquals("今天 22:15", reminderRows[0].timeText)
        assertEquals("第一行 第二行", reminderRows[0].messagePreview)

        assertFalse(reminderRows[1].isToday)
        assertEquals("明天 09:05", reminderRows[1].timeText)
        assertEquals("（未命名提醒）", reminderRows[1].title)

        assertFalse(reminderRows[2].isToday)
        assertEquals("11/18 08:30", reminderRows[2].timeText)
    }

    @Test
    fun `今日行程小工具深度連結應正確解析每日排程與提醒事項導航`() {
        assertEquals(
            WidgetDeepLink.OpenScheduleTab,
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_OPEN_SCHEDULE_TAB
            )
        )
        assertEquals(
            WidgetDeepLink.OpenRemindersTrack,
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_OPEN_REMINDERS_TRACK
            )
        )
    }
}
