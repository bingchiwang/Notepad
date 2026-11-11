package com.example.notepad.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * ThemePreferences 與 ThemeMode 單元測試（規範 4.5）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemePreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `default theme mode is SYSTEM`() = runTest {
        val dataStoreFile = File(tempFolder.root, "test_default.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { dataStoreFile }
        )
        val preferences = ThemePreferences(dataStore)

        assertEquals(ThemeMode.SYSTEM, preferences.themeModeFlow.first())
    }

    @Test
    fun `setThemeMode persists DARK and LIGHT`() = runTest {
        val dataStoreFile = File(tempFolder.root, "test_persist.preferences_pb")
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val storeScope1 = CoroutineScope(Job() + testDispatcher)
        val dataStore1 = PreferenceDataStoreFactory.create(
            scope = storeScope1,
            produceFile = { dataStoreFile }
        )
        val preferences1 = ThemePreferences(dataStore1)

        preferences1.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, preferences1.themeModeFlow.first())

        preferences1.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, preferences1.themeModeFlow.first())
        storeScope1.cancel()

        // 建立新實例讀取同一持久化檔案，驗證重開 App 後設定仍有效
        val storeScope2 = CoroutineScope(Job() + testDispatcher)
        val dataStore2 = PreferenceDataStoreFactory.create(
            scope = storeScope2,
            produceFile = { dataStoreFile }
        )
        val preferences2 = ThemePreferences(dataStore2)
        assertEquals(ThemeMode.LIGHT, preferences2.themeModeFlow.first())
        storeScope2.cancel()
    }

    @Test
    fun `shouldUseDarkTheme respects LIGHT DARK and SYSTEM`() {
        assertFalse(ThemeMode.LIGHT.shouldUseDarkTheme(isSystemInDarkTheme = false))
        assertFalse(ThemeMode.LIGHT.shouldUseDarkTheme(isSystemInDarkTheme = true))

        assertTrue(ThemeMode.DARK.shouldUseDarkTheme(isSystemInDarkTheme = false))
        assertTrue(ThemeMode.DARK.shouldUseDarkTheme(isSystemInDarkTheme = true))

        assertFalse(ThemeMode.SYSTEM.shouldUseDarkTheme(isSystemInDarkTheme = false))
        assertTrue(ThemeMode.SYSTEM.shouldUseDarkTheme(isSystemInDarkTheme = true))
    }

    @Test
    fun `ThemeMode fromString handles invalid and case-insensitive values`() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromString("dark"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromString(" LIGHT "))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString("unknown"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromString(null))
    }

    @Test
    fun `personalization settings persist button color calendar style and font scale and reset cleanly`() = runTest {
        val file = File(tempFolder.root, "personalization_persist.preferences_pb")
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val storeScope1 = CoroutineScope(Job() + testDispatcher)
        val prefs1 = ThemePreferences(
            PreferenceDataStoreFactory.create(scope = storeScope1, produceFile = { file })
        )

        val initial = prefs1.personalizationFlow.first()
        assertEquals(ThemeMode.SYSTEM, initial.themeMode)
        assertNull(initial.buttonColorLong)
        assertEquals(CalendarStyle.CLASSIC, initial.calendarStyle)
        assertNull(initial.calendarAccentColorLong)
        assertEquals(AppFontScale.MEDIUM, initial.fontScale)

        prefs1.setThemeMode(ThemeMode.DARK)
        prefs1.setButtonColor(0xFFFFEB3BL)
        prefs1.setCalendarStyle(CalendarStyle.FOREST_ZEN)
        prefs1.setCalendarAccentColor(0xFFF44336L)
        prefs1.setFontScale(AppFontScale.EXTRA_LARGE)
        storeScope1.cancel()

        val storeScope2 = CoroutineScope(Job() + testDispatcher)
        val prefs2 = ThemePreferences(
            PreferenceDataStoreFactory.create(scope = storeScope2, produceFile = { file })
        )
        val restored = prefs2.personalizationFlow.first()
        assertEquals(ThemeMode.DARK, restored.themeMode)
        assertEquals(0xFFFFEB3BL, restored.buttonColorLong)
        assertEquals(CalendarStyle.FOREST_ZEN, restored.calendarStyle)
        assertEquals(0xFFF44336L, restored.calendarAccentColorLong)
        assertEquals(AppFontScale.EXTRA_LARGE, restored.fontScale)

        prefs2.resetPersonalizationDefaults()
        val afterReset = prefs2.personalizationFlow.first()
        assertEquals(ThemeMode.DARK, afterReset.themeMode)
        assertNull(afterReset.buttonColorLong)
        assertEquals(CalendarStyle.CLASSIC, afterReset.calendarStyle)
        assertNull(afterReset.calendarAccentColorLong)
        assertEquals(AppFontScale.MEDIUM, afterReset.fontScale)
        storeScope2.cancel()
    }

    @Test
    fun `CalendarStyle and AppFontScale fromString handle invalid values cleanly`() {
        assertEquals(CalendarStyle.INK_WASH, CalendarStyle.fromString("ink_wash"))
        assertEquals(CalendarStyle.CLASSIC, CalendarStyle.fromString("non_existent"))
        assertEquals(CalendarStyle.CLASSIC, CalendarStyle.fromString(null))

        assertEquals(AppFontScale.LARGE, AppFontScale.fromString(" large "))
        assertEquals(AppFontScale.MEDIUM, AppFontScale.fromString("invalid"))
        assertEquals(AppFontScale.MEDIUM, AppFontScale.fromString(null))
    }
}
