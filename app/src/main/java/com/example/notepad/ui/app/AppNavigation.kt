package com.example.notepad.ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.preferences.NoteDraftPreferences
import com.example.notepad.data.preferences.NotificationPreferences
import com.example.notepad.data.preferences.PersonalizationSettings
import com.example.notepad.data.preferences.SyncPreferences
import com.example.notepad.data.preferences.ThemeMode
import com.example.notepad.data.preferences.ThemePreferences
import com.example.notepad.data.repository.NoteRepository
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.data.sync.CloudSyncManager
import com.example.notepad.reminder.ReminderScheduler
import com.example.notepad.ui.components.AppSettingsDialog
import com.example.notepad.ui.components.CloudSyncDialog
import com.example.notepad.ui.components.showFeedback
import com.example.notepad.ui.screens.CalendarScreen
import com.example.notepad.ui.screens.NoteEditorScreen
import com.example.notepad.ui.screens.NotesScreen
import com.example.notepad.ui.screens.PomodoroScreen
import com.example.notepad.ui.screens.ScheduleScreen
import com.example.notepad.ui.screens.TaskDetailScreen
import com.example.notepad.ui.screens.TaskScreen
import kotlinx.coroutines.launch

/**
 * 應用程式全局導航架構（遵循操作規範 1、6 & 系統欄安全防護）
 * - 使用 statusBarsPadding、navigationBarsPadding 與 displayCutoutPadding 避免介面與按鈕被手機導覽列、狀態欄或挖孔遮擋
 * - 在所有主要頁面保留一致的底部導覽方式與頂部主題／通知設定入口
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AppNavigation(
    taskRepository: TaskRepository,
    noteRepository: NoteRepository,
    calendarFacade: LunarCalendarFacade,
    themePreferences: ThemePreferences,
    notificationPreferences: NotificationPreferences,
    noteDraftPreferences: NoteDraftPreferences,
    syncPreferences: SyncPreferences,
    cloudSyncManager: CloudSyncManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val personalization by themePreferences.personalizationFlow.collectAsState(
        initial = PersonalizationSettings()
    )
    val currentThemeMode = personalization.themeMode
    val notificationsEnabled by notificationPreferences.notificationsEnabledFlow.collectAsState(initial = true)
    val syncUiState by cloudSyncManager.uiState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }

    fun updatePersonalization(feedbackText: String, action: suspend () -> Unit) {
        coroutineScope.launch {
            try {
                action()
                syncPreferences.markSettingsPending()
                cloudSyncManager.requestSyncOnDataChanged()
                showFeedback(context, feedbackText)
            } catch (e: Exception) {
                showFeedback(context, "個人化設定更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
            }
        }
    }

    val topLevelRoutes = remember {
        setOf(NotesRoute, TaskRoute, CalendarRoute, ScheduleRoute, PomodoroRoute)
    }

    val navigationState = rememberNavigationState(
        startRoute = NotesRoute,
        topLevelRoutes = topLevelRoutes
    )
    val navigator = remember { Navigator(navigationState) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    var activePomodoroTask by remember { mutableStateOf<String?>("自由專注") }

    val currentTopRoute = navigationState.topLevelRoute
    val currentLeafRoute = navigationState.backStacks[currentTopRoute]?.lastOrNull() ?: currentTopRoute
    val isDetailScreen = currentLeafRoute is NoteEditorRoute || currentLeafRoute is TaskDetailRoute

    val entryProvider = entryProvider<NavKey> {
        entry<NotesRoute>(
            metadata = ListDetailSceneStrategy.listPane(
                detailPlaceholder = { Text("請選取一篇記事以瀏覽或編輯") }
            )
        ) {
            NotesScreen(
                noteRepository = noteRepository,
                noteDraftPreferences = noteDraftPreferences,
                onAddNoteClick = {
                    navigator.navigate(NoteEditorRoute(noteId = null))
                },
                onNoteClick = { noteId ->
                    navigator.navigate(NoteEditorRoute(noteId = noteId))
                }
            )
        }

        entry<NoteEditorRoute>(
            metadata = ListDetailSceneStrategy.detailPane()
        ) { key ->
            NoteEditorScreen(
                noteId = key.noteId,
                noteRepository = noteRepository,
                noteDraftPreferences = noteDraftPreferences,
                onNavigateBack = { navigator.goBack() }
            )
        }

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
                },
                notificationsEnabled = notificationsEnabled
            )
        }

        entry<TaskDetailRoute>(
            metadata = ListDetailSceneStrategy.detailPane()
        ) { key ->
            TaskDetailScreen(
                taskId = key.taskId,
                taskRepository = taskRepository,
                notificationsEnabled = notificationsEnabled,
                onNavigateBack = { navigator.goBack() }
            )
        }

        entry<CalendarRoute> {
            CalendarScreen(
                calendarFacade = calendarFacade,
                taskRepository = taskRepository,
                notificationsEnabled = notificationsEnabled,
                personalization = personalization
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

    // 整體容器安全避開頂部狀態欄、相機挖孔與手機底部系統導覽列，避免任何按鈕或內容遭遮擋
    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
    ) {
        NavigationSuiteScaffold(
            modifier = Modifier.fillMaxSize(),
            navigationSuiteItems = {
                item(
                    selected = currentTopRoute == NotesRoute,
                    onClick = { navigator.navigate(NotesRoute) },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = "記事") },
                    label = { Text("記事") }
                )
                item(
                    selected = currentTopRoute == TaskRoute,
                    onClick = { navigator.navigate(TaskRoute) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "待辦") },
                    label = { Text("待辦") }
                )
                item(
                    selected = currentTopRoute == CalendarRoute,
                    onClick = { navigator.navigate(CalendarRoute) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "月曆") },
                    label = { Text("月曆") }
                )
                item(
                    selected = currentTopRoute == ScheduleRoute,
                    onClick = { navigator.navigate(ScheduleRoute) },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "排程") },
                    label = { Text("排程") }
                )
                item(
                    selected = currentTopRoute == PomodoroRoute,
                    onClick = { navigator.navigate(PomodoroRoute) },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "番茄鐘") },
                    label = { Text("番茄鐘") }
                )
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 所有頂層主畫面顯示一致的輕量頂部列，便於 1 鍵切換主題與通知總開關
                if (!isDetailScreen) {
                    val headerTitle = when (currentTopRoute) {
                        NotesRoute -> "📝 記事與分類搜尋"
                        TaskRoute -> "📋 雙軌待辦與提醒"
                        CalendarRoute -> "📅 農曆萬年曆"
                        ScheduleRoute -> "⏰ 每日時間規劃"
                        PomodoroRoute -> "🍅 專注番茄鐘"
                        else -> "記事本"
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 雲端同步狀態指示與管理入口
                                val syncIcon = when {
                                    syncUiState.isSyncing -> Icons.Default.CloudSync
                                    !syncUiState.syncSettings.syncEnabled || !syncUiState.isOnline -> Icons.Default.CloudOff
                                    syncUiState.pendingCount > 0 -> Icons.Default.CloudUpload
                                    else -> Icons.Default.CloudDone
                                }
                                val syncTint = when {
                                    syncUiState.isSyncing -> MaterialTheme.colorScheme.primary
                                    !syncUiState.isOnline || !syncUiState.isFirebaseConfigured -> MaterialTheme.colorScheme.error
                                    syncUiState.pendingCount > 0 -> MaterialTheme.colorScheme.tertiary
                                    syncUiState.syncSettings.syncEnabled -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                IconButton(
                                    onClick = { showCloudSyncDialog = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (syncUiState.pendingCount > 0) {
                                                Badge {
                                                    val label = if (syncUiState.pendingCount > 99) {
                                                        "99+"
                                                    } else {
                                                        syncUiState.pendingCount.toString()
                                                    }
                                                    Text(label)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = syncIcon,
                                            contentDescription = "雲端同步狀態",
                                            tint = syncTint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // 通知總開關快速指示與設定入口
                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (notificationsEnabled) {
                                            Icons.Default.Notifications
                                        } else {
                                            Icons.Default.NotificationsOff
                                        },
                                        contentDescription = "通知狀態設定",
                                        tint = if (notificationsEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // 主題模式快速切換入口（淺色 / 深色 / 跟隨系統）
                                val themeIcon = when (currentThemeMode) {
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                }
                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = themeIcon,
                                        contentDescription = "外觀主題切換",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "設定",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }

                NavDisplay(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    entries = navigationState.toEntries(entryProvider),
                    onBack = { navigator.goBack() },
                    sceneStrategies = listOf(listDetailStrategy)
                )
            }
        }
    }

    if (showSettingsDialog) {
        AppSettingsDialog(
            personalization = personalization,
            notificationsEnabled = notificationsEnabled,
            syncUiState = syncUiState,
            onThemeModeSelected = { mode ->
                updatePersonalization("已切換為${mode.label}") {
                    themePreferences.setThemeMode(mode)
                }
            },
            onButtonColorSelected = { colorLong ->
                val feedback = if (colorLong == null) "已恢復預設按鍵顏色" else "已套用自訂按鍵顏色（文字對比已同步優化）"
                updatePersonalization(feedback) {
                    themePreferences.setButtonColor(colorLong)
                }
            },
            onCalendarStyleSelected = { style ->
                updatePersonalization("已切換月曆樣式為「${style.label}」") {
                    themePreferences.setCalendarStyle(style)
                }
            },
            onCalendarAccentColorSelected = { colorLong ->
                val feedback = if (colorLong == null) "月曆選色已恢復依樣式預設" else "已更新月曆選取色（文字對比已同步保護）"
                updatePersonalization(feedback) {
                    themePreferences.setCalendarAccentColor(colorLong)
                }
            },
            onFontScaleSelected = { fontScale ->
                updatePersonalization("已調整字體大小為「${fontScale.label}」") {
                    themePreferences.setFontScale(fontScale)
                }
            },
            onResetPersonalization = {
                updatePersonalization("已重設外觀、月曆樣式與字體大小為預設值") {
                    themePreferences.resetPersonalizationDefaults()
                }
            },
            onNotificationsToggled = { enabled ->
                coroutineScope.launch {
                    try {
                        ReminderScheduler.toggleNotificationsMasterSwitch(
                            context = context,
                            taskRepository = taskRepository,
                            notificationPreferences = notificationPreferences,
                            enabled = enabled
                        )
                        syncPreferences.markSettingsPending()
                        cloudSyncManager.requestSyncOnDataChanged()
                        showFeedback(
                            context,
                            if (enabled) "已開啟提醒通知並恢復未來排程" else "已關閉提醒通知功能"
                        )
                    } catch (e: Exception) {
                        showFeedback(context, "通知設定更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onSyncNow = {
                coroutineScope.launch {
                    try {
                        val result = cloudSyncManager.syncNow(isManual = true)
                        showFeedback(context, result.message)
                    } catch (e: Exception) {
                        showFeedback(context, "雲端同步失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onOpenCloudSyncSettings = {
                showSettingsDialog = false
                showCloudSyncDialog = true
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            uiState = syncUiState,
            onSaveConfig = { enabled, accountId, projectId, appId, apiKey ->
                coroutineScope.launch {
                    try {
                        syncPreferences.updateConfiguration(
                            syncEnabled = enabled,
                            syncAccountId = accountId,
                            firebaseProjectId = projectId,
                            firebaseAppId = appId,
                            firebaseApiKey = apiKey
                        )
                        showFeedback(context, "已儲存雲端同步設定")
                        if (enabled) {
                            cloudSyncManager.requestSyncOnDataChanged()
                        }
                    } catch (e: Exception) {
                        showFeedback(context, "雲端設定儲存失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onSyncNow = { enabled, accountId, projectId, appId, apiKey ->
                coroutineScope.launch {
                    try {
                        syncPreferences.updateConfiguration(
                            syncEnabled = enabled,
                            syncAccountId = accountId,
                            firebaseProjectId = projectId,
                            firebaseAppId = appId,
                            firebaseApiKey = apiKey
                        )
                        val result = cloudSyncManager.syncNow(isManual = true)
                        showFeedback(context, result.message)
                    } catch (e: Exception) {
                        showFeedback(context, "雲端同步失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onDismiss = { showCloudSyncDialog = false }
        )
    }
}
