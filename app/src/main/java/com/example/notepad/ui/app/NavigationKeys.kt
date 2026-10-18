package com.example.notepad.ui.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object TaskRoute : NavKey

@Serializable
data class TaskDetailRoute(val taskId: Int) : NavKey

@Serializable
data object CalendarRoute : NavKey
