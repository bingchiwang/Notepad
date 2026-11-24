package com.example.notepad

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.local.AppDatabase
import com.example.notepad.data.preferences.NoteDraftPreferences
import com.example.notepad.data.preferences.NotificationPreferences
import com.example.notepad.data.preferences.PersonalizationSettings
import com.example.notepad.data.preferences.SyncPreferences
import com.example.notepad.data.preferences.ThemePreferences
import com.example.notepad.data.repository.NoteRepository
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.data.sync.AndroidNetworkMonitor
import com.example.notepad.data.sync.CloudSyncManager
import com.example.notepad.data.sync.FirebaseFirestoreDataSource
import com.example.notepad.data.sync.GoogleDriveAuthHelper
import com.example.notepad.data.sync.GoogleDriveRemoteDataSource
import com.example.notepad.data.sync.RoutingCloudRemoteDataSource
import com.example.notepad.reminder.ReminderScheduler
import com.example.notepad.ui.app.AppNavigation
import com.example.notepad.ui.theme.NotepadTheme
import com.example.notepad.widget.CalendarWidgetUpdater
import com.example.notepad.widget.QuickNoteWidgetContract
import com.example.notepad.widget.QuickNoteWidgetUpdater
import com.example.notepad.widget.ToDoWidgetUpdater
import com.example.notepad.widget.TodayScheduleWidgetUpdater
import com.example.notepad.widget.WidgetDeepLink
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private var networkMonitor: AndroidNetworkMonitor? = null
    private val widgetDeepLinkFlow = MutableStateFlow<WidgetDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        QuickNoteWidgetContract.parseWidgetDeepLink(intent)?.let { deepLink ->
            widgetDeepLinkFlow.value = deepLink
        }

        val database = AppDatabase.getDatabase(this)
        val tombstoneDao = database.syncTombstoneDao()
        val taskRepository = TaskRepository(
            toDoDao = database.toDoDao(),
            reminderDao = database.reminderDao(),
            timeBlockDao = database.timeBlockDao(),
            pomodoroDao = database.pomodoroDao(),
            syncTombstoneDao = tombstoneDao
        )
        val noteRepository = NoteRepository(
            noteDao = database.noteDao(),
            categoryDao = database.categoryDao(),
            syncTombstoneDao = tombstoneDao
        )
        val themePreferences = ThemePreferences(this)
        val notificationPreferences = NotificationPreferences(this)
        val noteDraftPreferences = NoteDraftPreferences(this)
        val syncPreferences = SyncPreferences(this)
        val monitor = AndroidNetworkMonitor(this).also { networkMonitor = it }
        val googleDriveAuthHelper = GoogleDriveAuthHelper(this)
        val googleDriveDataSource = GoogleDriveRemoteDataSource(googleDriveAuthHelper)
        val firebaseDataSource = FirebaseFirestoreDataSource(this)
        val remoteDataSource = RoutingCloudRemoteDataSource(
            googleDriveDataSource = googleDriveDataSource,
            firebaseDataSource = firebaseDataSource
        )
        val cloudSyncManager = CloudSyncManager(
            noteDao = database.noteDao(),
            categoryDao = database.categoryDao(),
            toDoDao = database.toDoDao(),
            reminderDao = database.reminderDao(),
            timeBlockDao = database.timeBlockDao(),
            pomodoroDao = database.pomodoroDao(),
            tombstoneDao = tombstoneDao,
            syncPreferences = syncPreferences,
            themePreferences = themePreferences,
            notificationPreferences = notificationPreferences,
            networkObserver = monitor,
            remoteDataSource = remoteDataSource,
            onReminderSyncedFromCloud = { reminder, isDeleted ->
                if (isDeleted || reminder.isFired) {
                    ReminderScheduler.cancelReminder(this@MainActivity, reminder.id)
                } else {
                    val enabled = notificationPreferences.isNotificationsEnabled()
                    ReminderScheduler.scheduleReminder(this@MainActivity, reminder, enabled)
                }
            },
            onRemoteDataSynced = {
                QuickNoteWidgetUpdater.notifyDataChanged(this@MainActivity)
                ToDoWidgetUpdater.notifyDataChanged(this@MainActivity)
                TodayScheduleWidgetUpdater.notifyDataChanged(this@MainActivity)
                CalendarWidgetUpdater.notifyDataChanged(this@MainActivity)
            }
        )

        // 本地優先寫入後，即時重新整理桌面小工具並通知雲端同步協調器（有網同步，離線保留 PENDING_UPLOAD）
        taskRepository.onLocalDataChanged = {
            ToDoWidgetUpdater.notifyDataChanged(this)
            TodayScheduleWidgetUpdater.notifyDataChanged(this)
            CalendarWidgetUpdater.notifyDataChanged(this)
            cloudSyncManager.requestSyncOnDataChanged()
        }
        noteRepository.onLocalDataChanged = {
            QuickNoteWidgetUpdater.notifyDataChanged(this)
            CalendarWidgetUpdater.notifyDataChanged(this)
            cloudSyncManager.requestSyncOnDataChanged()
        }
        cloudSyncManager.startObserving(lifecycleScope)

        val calendarFacade = LunarCalendarFacade()

        setContent {
            val personalization by themePreferences.personalizationFlow.collectAsState(
                initial = PersonalizationSettings()
            )
            val useDarkTheme = personalization.themeMode.shouldUseDarkTheme(isSystemInDarkTheme())

            // 隨主題切換即時更新系統狀態欄與導覽列圖示對比色彩，確保深/淺色模式下皆清晰不反光
            LaunchedEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { useDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { useDarkTheme }
                )
            }

            NotepadTheme(
                darkTheme = useDarkTheme,
                personalization = personalization
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val pendingWidgetDeepLink by widgetDeepLinkFlow.collectAsState()
                    AppNavigation(
                        taskRepository = taskRepository,
                        noteRepository = noteRepository,
                        calendarFacade = calendarFacade,
                        themePreferences = themePreferences,
                        notificationPreferences = notificationPreferences,
                        noteDraftPreferences = noteDraftPreferences,
                        syncPreferences = syncPreferences,
                        cloudSyncManager = cloudSyncManager,
                        googleDriveAuthHelper = googleDriveAuthHelper,
                        pendingWidgetDeepLink = pendingWidgetDeepLink,
                        onConsumeWidgetDeepLink = { widgetDeepLinkFlow.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        QuickNoteWidgetContract.parseWidgetDeepLink(intent)?.let { deepLink ->
            widgetDeepLinkFlow.value = deepLink
        }
    }

    override fun onDestroy() {
        networkMonitor?.unregister()
        networkMonitor = null
        super.onDestroy()
    }
}
