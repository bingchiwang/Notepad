package com.example.notepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
        val taskRepository = TaskRepository(
            toDoDao = database.toDoDao(),
            reminderDao = database.reminderDao(),
            timeBlockDao = database.timeBlockDao(),
            pomodoroDao = database.pomodoroDao()
        )
        val calendarFacade = LunarCalendarFacade()

        setContent {
            NotepadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        taskRepository = taskRepository,
                        calendarFacade = calendarFacade
                    )
                }
            }
        }
    }
}
