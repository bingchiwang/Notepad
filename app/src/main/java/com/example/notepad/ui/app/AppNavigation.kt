package com.example.notepad.ui.app

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.notepad.data.sync.GoogleDriveAuthHelper
import com.example.notepad.data.sync.GoogleDriveAuthStartResult
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
import com.example.notepad.widget.WidgetDeepLink
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
    googleDriveAuthHelper: GoogleDriveAuthHelper,
    modifier: Modifier = Modifier,
    pendingWidgetDeepLink: WidgetDeepLink? = null,
    onConsumeWidgetDeepLink: () -> Unit = {}
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
        setOf(NotesRoute, TaskRoute, CalendarRoute, ScheduleRoute)
    }

    val navigationState = rememberNavigationState(
        startRoute = NotesRoute,
        topLevelRoutes = topLevelRoutes
    )
    val navigator = remember { Navigator(navigationState) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    var activePomodoroTask by remember { mutableStateOf<String?>("自由專注") }
    var openAddToDoFromWidget by remember { mutableStateOf(false) }
    var initialTaskTrackFromWidget by remember { mutableStateOf<Int?>(null) }
    var calendarDateFromWidget by remember { mutableStateOf<Long?>(null) }
    var openCalendarQuickAddFromWidget by remember { mutableStateOf(false) }
    var scheduleTabOnLaunch by remember { mutableStateOf<Int?>(null) }

    val currentTopRoute = navigationState.topLevelRoute
    val currentLeafRoute = navigationState.backStacks[currentTopRoute]?.lastOrNull() ?: currentTopRoute
    val isDetailScreen = currentLeafRoute is NoteEditorRoute || currentLeafRoute is TaskDetailRoute

    // 處理來自桌面小工具（快速記事／待辦事項／今日行程）的深度連結導航
    LaunchedEffect(pendingWidgetDeepLink) {
        val deepLink = pendingWidgetDeepLink ?: return@LaunchedEffect
        when (deepLink) {
            is WidgetDeepLink.CreateNote -> {
                navigationState.topLevelRoute = NotesRoute
                navigationState.backStacks[NotesRoute]?.let { notesStack ->
                    while (notesStack.size > 1) {
                        notesStack.removeLastOrNull()
                    }
                    notesStack.add(
                        NoteEditorRoute(
                            noteId = null,
                            initialCategoryId = deepLink.categoryId
                        )
                    )
                }
            }
            is WidgetDeepLink.EditNote -> {
                navigationState.topLevelRoute = NotesRoute
                navigationState.backStacks[NotesRoute]?.let { notesStack ->
                    while (notesStack.size > 1) {
                        notesStack.removeLastOrNull()
                    }
                    notesStack.add(NoteEditorRoute(noteId = deepLink.noteId))
                }
            }
            is WidgetDeepLink.OpenToDoTab -> {
                navigationState.topLevelRoute = TaskRoute
                navigationState.backStacks[TaskRoute]?.let { taskStack ->
                    while (taskStack.size > 1) {
                        taskStack.removeLastOrNull()
                    }
                }
                initialTaskTrackFromWidget = 0
                openAddToDoFromWidget = deepLink.openAddDialog
            }
            is WidgetDeepLink.OpenToDoDetail -> {
                navigationState.topLevelRoute = TaskRoute
                navigationState.backStacks[TaskRoute]?.let { taskStack ->
                    while (taskStack.size > 1) {
                        taskStack.removeLastOrNull()
                    }
                    taskStack.add(TaskDetailRoute(deepLink.taskId))
                }
            }
            is WidgetDeepLink.OpenScheduleTab -> {
                navigationState.topLevelRoute = ScheduleRoute
                navigationState.backStacks[ScheduleRoute]?.let { scheduleStack ->
                    while (scheduleStack.size > 1) {
                        scheduleStack.removeLastOrNull()
                    }
                }
                scheduleTabOnLaunch = 0
            }
            is WidgetDeepLink.OpenRemindersTrack -> {
                navigationState.topLevelRoute = TaskRoute
                navigationState.backStacks[TaskRoute]?.let { taskStack ->
                    while (taskStack.size > 1) {
                        taskStack.removeLastOrNull()
                    }
                }
                initialTaskTrackFromWidget = 1
            }
            is WidgetDeepLink.OpenCalendarDay -> {
                navigationState.topLevelRoute = CalendarRoute
                navigationState.backStacks[CalendarRoute]?.let { calendarStack ->
                    while (calendarStack.size > 1) {
                        calendarStack.removeLastOrNull()
                    }
                }
                calendarDateFromWidget = deepLink.dateMillis
                openCalendarQuickAddFromWidget = deepLink.openQuickAdd
            }
        }
        onConsumeWidgetDeepLink()
    }

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
                onNavigateBack = { navigator.goBack() },
                initialCategoryId = key.initialCategoryId,
                initialCreatedAtMillis = key.initialCreatedAtMillis
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
                    scheduleTabOnLaunch = 1
                    navigator.navigate(ScheduleRoute)
                },
                notificationsEnabled = notificationsEnabled,
                openAddToDoOnLaunch = openAddToDoFromWidget,
                onConsumeOpenAddToDo = { openAddToDoFromWidget = false },
                initialTrackOnLaunch = initialTaskTrackFromWidget,
                onConsumeInitialTrack = { initialTaskTrackFromWidget = null }
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
                personalization = personalization,
                noteRepository = noteRepository,
                onNoteClick = { noteId ->
                    navigator.navigate(NoteEditorRoute(noteId = noteId))
                },
                onToDoClick = { taskId ->
                    navigator.navigate(TaskDetailRoute(taskId = taskId))
                },
                onQuickCreateNoteForDate = { targetDateMillis ->
                    navigator.navigate(
                        NoteEditorRoute(
                            noteId = null,
                            initialCreatedAtMillis = targetDateMillis
                        )
                    )
                },
                initialSelectedDateMillis = calendarDateFromWidget,
                onConsumeInitialDate = { calendarDateFromWidget = null },
                openQuickAddOnLaunch = openCalendarQuickAddFromWidget,
                onConsumeOpenQuickAdd = { openCalendarQuickAddFromWidget = false }
            )
        }

        entry<ScheduleRoute> {
            ScheduleScreen(
                taskRepository = taskRepository,
                initialTab = scheduleTabOnLaunch,
                onConsumeInitialTab = { scheduleTabOnLaunch = null },
                initialPomodoroTaskTitle = activePomodoroTask
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
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "時間規畫表") },
                    label = { Text("時間規畫表", maxLines = 1) }
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
                        ScheduleRoute -> "⏰ 時間規畫表"
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
                                    !syncUiState.syncSettings.syncEnabled ||
                                        !syncUiState.isOnline ||
                                        !syncUiState.isCloudConfigured -> Icons.Default.CloudOff
                                    syncUiState.pendingCount > 0 -> Icons.Default.CloudUpload
                                    else -> Icons.Default.CloudDone
                                }
                                val syncTint = when {
                                    syncUiState.isSyncing -> MaterialTheme.colorScheme.primary
                                    !syncUiState.isOnline || !syncUiState.isCloudConfigured -> MaterialTheme.colorScheme.error
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
        var isAuthorizingGoogle by remember { mutableStateOf(false) }
        var googleAuthError by remember { mutableStateOf<String?>(null) }

        val googleAuthLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                coroutineScope.launch {
                    isAuthorizingGoogle = true
                    googleAuthError = null
                    try {
                        val profile = googleDriveAuthHelper.completeAuthorizationFromResult(
                            activityContext = context,
                            resultData = result.data
                        )
                        syncPreferences.updateGoogleDriveAccount(
                            email = profile.email,
                            displayName = profile.displayName
                        )
                        showFeedback(context, "已連結 Google 帳號：${profile.email}")
                        val syncResult = cloudSyncManager.syncNow(isManual = true)
                        showFeedback(context, syncResult.message)
                    } catch (e: Exception) {
                        val msg = e.localizedMessage ?: "Google 帳號授權解析失敗"
                        googleAuthError = msg
                        showFeedback(context, msg)
                    } finally {
                        isAuthorizingGoogle = false
                    }
                }
            } else {
                isAuthorizingGoogle = false
                googleAuthError = "已取消連結 Google 帳號"
                showFeedback(context, "已取消連結 Google 帳號")
            }
        }

        CloudSyncDialog(
            uiState = syncUiState,
            isAuthorizingGoogle = isAuthorizingGoogle,
            googleAuthError = googleAuthError,
            onAuthorizeGoogleDrive = {
                coroutineScope.launch {
                    isAuthorizingGoogle = true
                    googleAuthError = null
                    try {
                        when (val startResult = googleDriveAuthHelper.beginAuthorization(context)) {
                            is GoogleDriveAuthStartResult.Authorized -> {
                                val profile = startResult.profile
                                syncPreferences.updateGoogleDriveAccount(
                                    email = profile.email,
                                    displayName = profile.displayName
                                )
                                showFeedback(context, "已連結 Google 帳號：${profile.email}")
                                isAuthorizingGoogle = false
                                val syncResult = cloudSyncManager.syncNow(isManual = true)
                                showFeedback(context, syncResult.message)
                            }

                            is GoogleDriveAuthStartResult.NeedsUserResolution -> {
                                googleAuthLauncher.launch(
                                    IntentSenderRequest.Builder(startResult.intentSender).build()
                                )
                            }
                        }
                    } catch (e: Exception) {
                        val msg = e.localizedMessage ?: "啟動 Google 帳號授權失敗"
                        googleAuthError = msg
                        showFeedback(context, msg)
                        isAuthorizingGoogle = false
                    }
                }
            },
            onDisconnectGoogleDrive = {
                coroutineScope.launch {
                    try {
                        syncPreferences.clearGoogleDriveAccount()
                        googleAuthError = null
                        showFeedback(context, "已中斷 Google 雲端硬碟連結")
                    } catch (e: Exception) {
                        showFeedback(context, "中斷連結失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onSaveConfig = { enabled ->
                coroutineScope.launch {
                    try {
                        val current = syncPreferences.getSyncSettingsOnce()
                        syncPreferences.updateConfiguration(
                            cloudProvider = com.example.notepad.data.preferences.CloudBackendProvider.GOOGLE_DRIVE,
                            syncEnabled = enabled,
                            syncAccountId = current.syncAccountId
                        )
                        showFeedback(context, "已儲存 Google 雲端同步設定")
                        if (enabled) {
                            cloudSyncManager.requestSyncOnDataChanged()
                        }
                    } catch (e: Exception) {
                        showFeedback(context, "雲端設定儲存失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onSyncNow = { enabled ->
                coroutineScope.launch {
                    try {
                        val current = syncPreferences.getSyncSettingsOnce()
                        syncPreferences.updateConfiguration(
                            cloudProvider = com.example.notepad.data.preferences.CloudBackendProvider.GOOGLE_DRIVE,
                            syncEnabled = enabled,
                            syncAccountId = current.syncAccountId
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
