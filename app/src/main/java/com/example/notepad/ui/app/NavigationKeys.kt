package com.example.notepad.ui.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 應用程式全局導覽路由鍵值定義（採用 Navigation 3 型別安全路由，遵循操作規範 6）
 */
@Serializable
data object NotesRoute : NavKey

@Serializable
data class NoteEditorRoute(
    val noteId: Int? = null,
    val initialCategoryId: Int? = null
) : NavKey

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
