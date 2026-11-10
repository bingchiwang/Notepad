package com.example.notepad.ui.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object NotesRoute : NavKey

@Serializable
data class NoteEditorRoute(val noteId: Int? = null) : NavKey

@Serializable
data object TaskRoute : NavKey

@Serializable
data class TaskDetailRoute(val taskId: Int) : NavKey

@Serializable
data object CalendarRoute : NavKey

@Serializable
data object ScheduleRoute : NavKey

@Serializable
data object PomodoroRoute : NavKey
