package com.example.notepad.ui.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.ui.screens.CalendarScreen
import com.example.notepad.ui.screens.TaskDetailScreen
import com.example.notepad.ui.screens.TaskScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation(
    taskRepository: TaskRepository,
    calendarFacade: LunarCalendarFacade
) {
    val navigationState = rememberNavigationState(
        startRoute = TaskRoute,
        topLevelRoutes = setOf(TaskRoute, CalendarRoute)
    )
    val navigator = remember { Navigator(navigationState) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    val entryProvider = entryProvider<NavKey> {
        entry<TaskRoute>(
            metadata = ListDetailSceneStrategy.listPane(
                detailPlaceholder = { Text("Select a task to view details") }
            )
        ) {
            TaskScreen(
                taskRepository = taskRepository,
                onTaskClick = { taskId ->
                    navigator.navigate(TaskDetailRoute(taskId))
                }
            )
        }
        
        entry<TaskDetailRoute>(
            metadata = ListDetailSceneStrategy.detailPane()
        ) { key ->
            TaskDetailScreen(
                taskId = key.taskId,
                taskRepository = taskRepository
            )
        }

        entry<CalendarRoute> {
            CalendarScreen(calendarFacade)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = navigationState.topLevelRoute == TaskRoute,
                onClick = { navigator.navigate(TaskRoute) },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Tasks") },
                label = { Text("Tasks") }
            )
            item(
                selected = navigationState.topLevelRoute == CalendarRoute,
                onClick = { navigator.navigate(CalendarRoute) },
                icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                label = { Text("Calendar") }
            )
        }
    ) {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() },
            sceneStrategies = listOf(listDetailStrategy)
        )
    }
}
