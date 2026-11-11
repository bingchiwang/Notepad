package com.example.notepad

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
import com.example.notepad.reminder.ReminderScheduler
import com.example.notepad.ui.app.AppNavigation
import com.example.notepad.ui.theme.NotepadTheme

class MainActivity : ComponentActivity() {

    private var networkMonitor: AndroidNetworkMonitor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        val remoteDataSource = FirebaseFirestoreDataSource(this)
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
            }
        )

        // 本地優先寫入後，即時通知雲端同步協調器（有網同步，離線保留 PENDING_UPLOAD）
        taskRepository.onLocalDataChanged = { cloudSyncManager.requestSyncOnDataChanged() }
        noteRepository.onLocalDataChanged = { cloudSyncManager.requestSyncOnDataChanged() }
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
                    AppNavigation(
                        taskRepository = taskRepository,
                        noteRepository = noteRepository,
                        calendarFacade = calendarFacade,
                        themePreferences = themePreferences,
                        notificationPreferences = notificationPreferences,
                        noteDraftPreferences = noteDraftPreferences,
                        syncPreferences = syncPreferences,
                        cloudSyncManager = cloudSyncManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        networkMonitor?.unregister()
        networkMonitor = null
        super.onDestroy()
    }
}
