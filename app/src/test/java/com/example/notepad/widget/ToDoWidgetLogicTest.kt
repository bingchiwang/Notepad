package com.example.notepad.widget

import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.ToDo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

/**
 * 待辦事項桌面小工具核心邏輯、截止日期緊迫度分類、優先級排序與深度連結單元測試
 */
class ToDoWidgetLogicTest {

    private fun localMillis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `僅顯示尚未完成的任務並過濾已完成項目`() {
        val today = localMillis(2026, 11, 15, 10, 0)
        val todos = listOf(
            ToDo(id = 1, title = "已完成高優先", isCompleted = true, priority = Priority.HIGH, dueDate = localMillis(2026, 11, 14)),
            ToDo(id = 2, title = "未完成低優先", isCompleted = false, priority = Priority.LOW),
            ToDo(id = 3, title = "已完成一般", isCompleted = true, priority = Priority.MEDIUM),
            ToDo(id = 4, title = "未完成中優先", isCompleted = false, priority = Priority.MEDIUM)
        )

        val resolved = ToDoWidgetLogic.resolveActiveTasks(todos = todos, todayMillis = today)

        assertEquals(2, resolved.activeCount)
        assertEquals(listOf(4, 2), resolved.items.map { it.id })
    }

    @Test
    fun `應依高中低優先級與截止日由近到遠排序且保留總未完成筆數`() {
        val today = localMillis(2026, 11, 15, 9, 30)
        val todos = listOf(
            ToDo(id = 1, title = "低優先明天", priority = Priority.LOW, dueDate = localMillis(2026, 11, 16)),
            ToDo(id = 2, title = "高優先無期限", priority = Priority.HIGH, dueDate = null, createdAt = 100L),
            ToDo(id = 3, title = "高優先三天後", priority = Priority.HIGH, dueDate = localMillis(2026, 11, 18)),
            ToDo(id = 4, title = "高優先今天", priority = Priority.HIGH, dueDate = localMillis(2026, 11, 15)),
            ToDo(id = 5, title = "中優先今天", priority = Priority.MEDIUM, dueDate = localMillis(2026, 11, 15))
        )

        val resolved = ToDoWidgetLogic.resolveActiveTasks(
            todos = todos,
            todayMillis = today,
            maxItems = 3
        )

        assertEquals(5, resolved.activeCount)
        assertEquals(listOf(4, 3, 2), resolved.items.map { it.id })
    }

    @Test
    fun `應正確標示高中低優先級徽章與標籤格式`() {
        val today = localMillis(2026, 11, 15, 8, 0)
        val todos = listOf(
            ToDo(id = 1, title = "高任務", priority = Priority.HIGH, tag = "工作", createdAt = 1L),
            ToDo(id = 2, title = "中任務", priority = Priority.MEDIUM, tag = "  ", createdAt = 1L),
            ToDo(id = 3, title = "低任務", priority = Priority.LOW, tag = "日常", createdAt = 1L)
        )

        val resolved = ToDoWidgetLogic.resolveActiveTasks(todos = todos, todayMillis = today)

        assertEquals(Priority.HIGH, resolved.items[0].priority)
        assertEquals("高優先", resolved.items[0].priorityLabel)
        assertEquals("#工作", resolved.items[0].tagText)

        assertEquals(Priority.MEDIUM, resolved.items[1].priority)
        assertEquals("中優先", resolved.items[1].priorityLabel)
        assertEquals("", resolved.items[1].tagText)

        assertEquals(Priority.LOW, resolved.items[2].priority)
        assertEquals("低優先", resolved.items[2].priorityLabel)
        assertEquals("#日常", resolved.items[2].tagText)
    }

    @Test
    fun `應正確判別逾期今天明天未來與未設截止日狀態`() {
        val todayMorning = localMillis(2026, 11, 15, 8, 30)
        val todos = listOf(
            ToDo(id = 1, title = "昨天逾期", priority = Priority.HIGH, dueDate = localMillis(2026, 11, 14, 23, 59)),
            ToDo(id = 2, title = "今天稍晚", priority = Priority.MEDIUM, dueDate = localMillis(2026, 11, 15, 22, 0)),
            ToDo(id = 3, title = "明天到期", priority = Priority.MEDIUM, dueDate = localMillis(2026, 11, 16, 0, 0), createdAt = 10L),
            ToDo(id = 4, title = "下週任務", priority = Priority.LOW, dueDate = localMillis(2026, 11, 20, 12, 0)),
            ToDo(id = 5, title = "無截止日", priority = Priority.LOW, dueDate = null, createdAt = 1L)
        )

        val resolved = ToDoWidgetLogic.resolveActiveTasks(
            todos = todos,
            todayMillis = todayMorning,
            dateFormatter = { ts ->
                val cal = Calendar.getInstance().apply { timeInMillis = ts }
                "%02d/%02d".format(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            }
        )

        val byId = resolved.items.associateBy { it.id }

        assertEquals(DueDateUrgency.OVERDUE, byId.getValue(1).dueDateUrgency)
        assertEquals("已逾期 · 11/14", byId.getValue(1).dueDateText)

        assertEquals(DueDateUrgency.TODAY, byId.getValue(2).dueDateUrgency)
        assertEquals("今天到期 · 11/15", byId.getValue(2).dueDateText)

        assertEquals(DueDateUrgency.TOMORROW, byId.getValue(3).dueDateUrgency)
        assertEquals("明天到期 · 11/16", byId.getValue(3).dueDateText)

        assertEquals(DueDateUrgency.UPCOMING, byId.getValue(4).dueDateUrgency)
        assertEquals("截止 11/20", byId.getValue(4).dueDateText)

        assertEquals(DueDateUrgency.NONE, byId.getValue(5).dueDateUrgency)
        assertEquals("未設截止日", byId.getValue(5).dueDateText)
    }

    @Test
    fun `待辦小工具深度連結應正確解析新增與任務詳情動作`() {
        assertEquals(
            WidgetDeepLink.OpenToDoTab(openAddDialog = true),
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_OPEN_TODO_TAB,
                openAddToDo = true
            )
        )
        assertEquals(
            WidgetDeepLink.OpenToDoTab(openAddDialog = false),
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_OPEN_TODO_TAB,
                openAddToDo = false
            )
        )
        assertEquals(
            WidgetDeepLink.OpenToDoDetail(taskId = 15),
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_OPEN_TODO_DETAIL,
                todoId = 15
            )
        )
        assertNull(
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_OPEN_TODO_DETAIL,
                todoId = 0
            )
        )
        assertNull(
            QuickNoteWidgetContract.parseWidgetDeepLink(
                action = QuickNoteWidgetContract.ACTION_OPEN_TODO_DETAIL,
                todoId = QuickNoteWidgetContract.INVALID_ID
            )
        )
    }
}
