package com.example.notepad.data.repository

import com.example.notepad.data.local.dao.PomodoroDao
import com.example.notepad.data.local.dao.ReminderDao
import com.example.notepad.data.local.dao.TimeBlockDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.PomodoroRecord
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val toDoDao: ToDoDao,
    private val reminderDao: ReminderDao,
    private val timeBlockDao: TimeBlockDao,
    private val pomodoroDao: PomodoroDao
) {
    // ToDo Operations
    val allToDos: Flow<List<ToDo>> = toDoDao.getAllToDos()

    fun getToDosByDate(date: Long): Flow<List<ToDo>> {
        return toDoDao.getToDosByDate(date)
    }

    fun getToDosByDateRange(startDate: Long, endDate: Long): Flow<List<ToDo>> {
        return toDoDao.getToDosByDateRange(startDate, endDate)
    }

    fun getToDoById(id: Int): Flow<ToDo?> {
        return toDoDao.getToDoById(id)
    }

    suspend fun insertToDo(toDo: ToDo) {
        toDoDao.insertToDo(toDo)
    }

    suspend fun updateToDo(toDo: ToDo) {
        toDoDao.updateToDo(toDo)
    }

    suspend fun deleteToDo(toDo: ToDo) {
        toDoDao.deleteToDo(toDo)
    }

    // Reminder Operations
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()

    suspend fun insertReminder(reminder: Reminder) {
        reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
    }

    // TimeBlock Operations (Daily Schedule)
    val allTimeBlocks: Flow<List<TimeBlock>> = timeBlockDao.getAllTimeBlocks()

    fun getTimeBlocksByDate(date: Long): Flow<List<TimeBlock>> {
        return timeBlockDao.getTimeBlocksByDate(date)
    }

    suspend fun insertTimeBlock(timeBlock: TimeBlock) {
        timeBlockDao.insertTimeBlock(timeBlock)
    }

    suspend fun updateTimeBlock(timeBlock: TimeBlock) {
        timeBlockDao.updateTimeBlock(timeBlock)
    }

    suspend fun deleteTimeBlock(timeBlock: TimeBlock) {
        timeBlockDao.deleteTimeBlock(timeBlock)
    }

    // Pomodoro Operations
    fun getTodayPomodoroCount(date: Long): Flow<Int> {
        return pomodoroDao.getTodayCount(date)
    }

    fun getPomodoroRecordsByDate(date: Long): Flow<List<PomodoroRecord>> {
        return pomodoroDao.getRecordsByDate(date)
    }

    suspend fun insertPomodoroRecord(record: PomodoroRecord) {
        pomodoroDao.insertRecord(record)
    }
}
