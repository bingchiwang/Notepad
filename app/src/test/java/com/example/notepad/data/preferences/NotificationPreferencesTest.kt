package com.example.notepad.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * NotificationPreferences 單元測試（規範 4.4 通知總開關持久化）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `default notificationsEnabled is true and updates persist`() = runTest {
        val file = File(tempFolder.root, "notification_test.preferences_pb")
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope1 = CoroutineScope(Job() + dispatcher)
        val prefs1 = NotificationPreferences(
            PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file })
        )

        assertTrue(prefs1.isNotificationsEnabled())
        prefs1.setNotificationsEnabled(false)
        assertFalse(prefs1.isNotificationsEnabled())
        scope1.cancel()

        val scope2 = CoroutineScope(Job() + dispatcher)
        val prefs2 = NotificationPreferences(
            PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file })
        )
        assertFalse(prefs2.isNotificationsEnabled())
        prefs2.setNotificationsEnabled(true)
        assertTrue(prefs2.isNotificationsEnabled())
        scope2.cancel()
    }
}
