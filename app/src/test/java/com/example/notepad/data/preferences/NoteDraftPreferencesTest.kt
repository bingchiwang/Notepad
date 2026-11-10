package com.example.notepad.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
 * NoteDraftPreferences 單元測試（規範 4.1 切換頁面不遺失尚未保存內容）
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NoteDraftPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `saveDraft persists fields including nullable IDs and clearDraft resets state`() = runTest {
        val file = File(tempFolder.root, "note_draft_test.preferences_pb")
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope1 = CoroutineScope(Job() + dispatcher)
        val prefs1 = NoteDraftPreferences(
            PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file })
        )

        val initial = prefs1.getDraftOnce()
        assertFalse(initial.hasActiveDraft)

        val draft = NoteDraft(
            hasActiveDraft = true,
            editingNoteId = 15,
            title = "草稿標題",
            content = "還沒儲存的筆記內容",
            categoryId = 3,
            color = 0xFF4CAF50L,
            isPinned = true
        )
        prefs1.saveDraft(draft)
        scope1.cancel()

        val scope2 = CoroutineScope(Job() + dispatcher)
        val prefs2 = NoteDraftPreferences(
            PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file })
        )
        val restored = prefs2.getDraftOnce()
        assertTrue(restored.hasActiveDraft)
        assertEquals(15, restored.editingNoteId)
        assertEquals("草稿標題", restored.title)
        assertEquals("還沒儲存的筆記內容", restored.content)
        assertEquals(3, restored.categoryId)
        assertEquals(0xFF4CAF50L, restored.color)
        assertTrue(restored.isPinned)

        prefs2.saveDraft(draft.copy(editingNoteId = null, categoryId = null))
        val newNoteDraft = prefs2.getDraftOnce()
        assertNull(newNoteDraft.editingNoteId)
        assertNull(newNoteDraft.categoryId)

        prefs2.clearDraft()
        assertFalse(prefs2.getDraftOnce().hasActiveDraft)
        scope2.cancel()
    }
}
