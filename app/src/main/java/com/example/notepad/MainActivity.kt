package com.example.notepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.local.AppDatabase
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.ui.app.AppNavigation
import com.example.notepad.ui.theme.NotepadTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(this)
        val taskRepository = TaskRepository(database.toDoDao(), database.reminderDao())
        val calendarFacade = LunarCalendarFacade()

        setContent {
            NotepadTheme {
                AppNavigation(
                    taskRepository = taskRepository,
                    calendarFacade = calendarFacade
                )
            }
        }
    }
}
