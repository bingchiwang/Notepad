package com.example.notepad.ui.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.ui.screens.CalendarScreen
import com.example.notepad.ui.screens.PomodoroScreen
import com.example.notepad.ui.screens.ScheduleScreen
import com.example.notepad.ui.screens.TaskDetailScreen
import com.example.notepad.ui.screens.TaskScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation(
    taskRepository: TaskRepository,
    calendarFacade: LunarCalendarFacade
) {
    val topLevelRoutes = remember {
        setOf(TaskRoute, CalendarRoute, ScheduleRoute, PomodoroRoute)
    }

    val navigationState = rememberNavigationState(
        startRoute = TaskRoute,
        topLevelRoutes = topLevelRoutes
    )
    val navigator = remember { Navigator(navigationState) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    var activePomodoroTask by remember { mutableStateOf<String?>("自由專注") }

    val entryProvider = entryProvider<NavKey> {
        entry<TaskRoute>(
            metadata = ListDetailSceneStrategy.listPane(
                detailPlaceholder = { Text("請選取一項待做事項以查看詳細內容") }
            )
        ) {
            TaskScreen(
                taskRepository = taskRepository,
                calendarFacade = calendarFacade,
                onTaskClick = { taskId ->
                    navigator.navigate(TaskDetailRoute(taskId))
                },
                onStartPomodoro = { taskName ->
                    activePomodoroTask = taskName
                    navigator.navigate(PomodoroRoute)
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
            CalendarScreen(
                calendarFacade = calendarFacade,
                taskRepository = taskRepository
            )
        }

        entry<ScheduleRoute> {
            ScheduleScreen(
                taskRepository = taskRepository,
                onStartPomodoroForTask = { taskName ->
                    activePomodoroTask = taskName
                    navigator.navigate(PomodoroRoute)
                }
            )
        }

        entry<PomodoroRoute> {
            PomodoroScreen(
                taskRepository = taskRepository,
                initialTaskTitle = activePomodoroTask
            )
        }
    }

    NavigationSuiteScaffold(
        modifier = Modifier.fillMaxSize(),
        navigationSuiteItems = {
            item(
                selected = navigationState.topLevelRoute == TaskRoute,
                onClick = { navigator.navigate(TaskRoute) },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "待辦") },
                label = { Text("待辦清單") }
            )
            item(
                selected = navigationState.topLevelRoute == CalendarRoute,
                onClick = { navigator.navigate(CalendarRoute) },
                icon = { Icon(Icons.Default.DateRange, contentDescription = "月曆") },
                label = { Text("農曆月曆") }
            )
            item(
                selected = navigationState.topLevelRoute == ScheduleRoute,
                onClick = { navigator.navigate(ScheduleRoute) },
                icon = { Icon(Icons.Default.Schedule, contentDescription = "排程") },
                label = { Text("每日排程") }
            )
            item(
                selected = navigationState.topLevelRoute == PomodoroRoute,
                onClick = { navigator.navigate(PomodoroRoute) },
                icon = { Icon(Icons.Default.Timer, contentDescription = "番茄鐘") },
                label = { Text("番茄鐘") }
            )
        }
    ) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() },
            sceneStrategies = listOf(listDetailStrategy)
        )
    }
}
