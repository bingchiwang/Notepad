package com.example.notepad.data.repository

import com.example.notepad.data.local.dao.ReminderDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.ToDo
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val toDoDao: ToDoDao,
    private val reminderDao: ReminderDao
) {
    // ToDo Operations
    val allToDos: Flow<List<ToDo>> = toDoDao.getAllToDos()

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
}
