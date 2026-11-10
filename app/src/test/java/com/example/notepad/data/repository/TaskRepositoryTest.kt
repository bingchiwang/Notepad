package com.example.notepad.data.repository

import com.example.notepad.data.local.dao.PomodoroDao
import com.example.notepad.data.local.dao.ReminderDao
import com.example.notepad.data.local.dao.TimeBlockDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.PomodoroRecord
import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * TaskRepository 單元測試（驗證規範 4.2 待辦優先級、非空白檢核、狀態分離與提醒連動）
 */
class TaskRepositoryTest {

    private lateinit var repository: TaskRepository

    @Before
    fun setup() {
        repository = TaskRepository(
            toDoDao = FakeToDoDao(),
            reminderDao = FakeReminderDao(),
            timeBlockDao = FakeTimeBlockDao(),
            pomodoroDao = FakePomodoroDao()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject blank task title on insertToDo`() = runTest {
        repository.insertToDo(ToDo(title = "   "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `reject blank task title on updateToDo`() = runTest {
        repository.insertToDo(ToDo(title = "有效名稱"))
        val saved = repository.allToDos.first().first()
        repository.updateToDo(saved.copy(title = ""))
    }

    @Test
    fun `tasks sort by priority and completed tasks do not appear in activeToDos`() = runTest {
        repository.insertToDo(ToDo(title = "低優先", priority = Priority.LOW))
        repository.insertToDo(ToDo(title = "高優先", priority = Priority.HIGH))
        repository.insertToDo(ToDo(title = "中優先", priority = Priority.MEDIUM))

        val activeBefore = repository.activeToDos.first()
        assertEquals(listOf("高優先", "中優先", "低優先"), activeBefore.map { it.title })

        val highTask = activeBefore.first()
        repository.updateToDo(highTask.copy(isCompleted = true))

        val activeAfter = repository.activeToDos.first()
        val completedAfter = repository.completedToDos.first()
        assertEquals(listOf("中優先", "低優先"), activeAfter.map { it.title })
        assertEquals(listOf("高優先"), completedAfter.map { it.title })
    }

    @Test
    fun `getPendingRemindersByToDoId returns only unfired reminders for task`() = runTest {
        repository.insertReminder(
            Reminder(title = "待觸發提醒", triggerAt = 10_000L, linkedToDoId = 42, isFired = false)
        )
        repository.insertReminder(
            Reminder(title = "已觸發提醒", triggerAt = 5_000L, linkedToDoId = 42, isFired = true)
        )
        repository.insertReminder(
            Reminder(title = "其他任務提醒", triggerAt = 10_000L, linkedToDoId = 99, isFired = false)
        )

        val pending = repository.getPendingRemindersByToDoId(42)
        assertEquals(1, pending.size)
        assertEquals("待觸發提醒", pending.first().title)
        assertTrue(pending.none { it.isFired })
    }
}

private class FakeToDoDao : ToDoDao {
    private val flow = MutableStateFlow<List<ToDo>>(emptyList())
    private var nextId = 1

    private fun sortToDos(list: List<ToDo>): List<ToDo> = list.sortedWith(
        compareBy<ToDo> { it.isCompleted }
            .thenByDescending { it.priority.level }
            .thenBy { it.dueDate ?: Long.MAX_VALUE }
            .thenByDescending { it.createdAt }
    )

    override fun getAllToDos(): Flow<List<ToDo>> = flow.map(::sortToDos)

    override fun getActiveToDos(): Flow<List<ToDo>> = flow.map { list ->
        sortToDos(list.filter { !it.isCompleted })
    }

    override fun getCompletedToDos(): Flow<List<ToDo>> = flow.map { list ->
        list.filter { it.isCompleted }.sortedByDescending { it.createdAt }
    }

    override fun getToDosByPriority(priority: Priority): Flow<List<ToDo>> = flow.map { list ->
        sortToDos(list.filter { it.priority == priority })
    }

    override fun getToDosByDate(date: Long): Flow<List<ToDo>> = flow.map { list ->
        sortToDos(list.filter { it.dueDate == date })
    }

    override fun getToDosByDateRange(startDate: Long, endDate: Long): Flow<List<ToDo>> = flow.map { list ->
        sortToDos(list.filter { it.dueDate != null && it.dueDate in startDate..endDate })
    }

    override fun getToDoById(id: Int): Flow<ToDo?> = flow.map { it.find { item -> item.id == id } }

    override suspend fun insertToDo(toDo: ToDo) {
        val id = if (toDo.id == 0) nextId++ else toDo.id
        flow.value = flow.value.filterNot { it.id == id } + toDo.copy(id = id)
    }

    override suspend fun updateToDo(toDo: ToDo) {
        flow.value = flow.value.map { if (it.id == toDo.id) toDo else it }
    }

    override suspend fun deleteToDo(toDo: ToDo) {
        flow.value = flow.value.filterNot { it.id == toDo.id }
    }
}

private class FakeReminderDao : ReminderDao {
    private val flow = MutableStateFlow<List<Reminder>>(emptyList())
    private var nextId = 1

    override fun getAllReminders(): Flow<List<Reminder>> = flow

    override fun getRemindersByDate(startMillis: Long, endMillis: Long): Flow<List<Reminder>> =
        flow.map { it.filter { r -> r.triggerAt in startMillis..endMillis } }

    override fun getReminderById(id: Int): Flow<Reminder?> =
        flow.map { it.find { r -> r.id == id } }

    override suspend fun getPendingRemindersByToDoId(toDoId: Int): List<Reminder> =
        flow.value.filter { it.linkedToDoId == toDoId && !it.isFired }

    override suspend fun insertReminder(reminder: Reminder): Long {
        val id = if (reminder.id == 0) nextId++ else reminder.id
        flow.value = flow.value.filterNot { it.id == id } + reminder.copy(id = id)
        return id.toLong()
    }

    override suspend fun updateReminder(reminder: Reminder) {
        flow.value = flow.value.map { if (it.id == reminder.id) reminder else it }
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        flow.value = flow.value.filterNot { it.id == reminder.id }
    }

    override suspend fun markAsFired(id: Int) {
        flow.value = flow.value.map { if (it.id == id) it.copy(isFired = true) else it }
    }
}

private class FakeTimeBlockDao : TimeBlockDao {
    private val flow = MutableStateFlow<List<TimeBlock>>(emptyList())
    override fun getAllTimeBlocks(): Flow<List<TimeBlock>> = flow
    override fun getTimeBlocksByDate(date: Long): Flow<List<TimeBlock>> = flow
    override suspend fun insertTimeBlock(timeBlock: TimeBlock) {}
    override suspend fun updateTimeBlock(timeBlock: TimeBlock) {}
    override suspend fun deleteTimeBlock(timeBlock: TimeBlock) {}
}

private class FakePomodoroDao : PomodoroDao {
    override fun getTodayCount(date: Long): Flow<Int> = MutableStateFlow(0)
    override fun getRecordsByDate(date: Long): Flow<List<PomodoroRecord>> = MutableStateFlow(emptyList())
    override suspend fun insertRecord(record: PomodoroRecord) {}
}
