package com.example.notepad.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.noteDraftDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "note_draft_preferences"
)

/**
 * 記事未保存草稿資料結構（規範 4.1：避免切換頁面或重開 App 遺失未儲存內容）
 */
data class NoteDraft(
    val hasActiveDraft: Boolean = false,
    val editingNoteId: Int? = null,
    val title: String = "",
    val content: String = "",
    val categoryId: Int? = null,
    val color: Long = 0xFF2196F3L,
    val isPinned: Boolean = false
)

/**
 * DataStore 記事草稿持久化管理（規範 4.1）
 */
class NoteDraftPreferences(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.noteDraftDataStore)

    companion object {
        private val KEY_HAS_DRAFT = booleanPreferencesKey("has_active_draft")
        private val KEY_EDITING_NOTE_ID = intPreferencesKey("editing_note_id")
        private val KEY_TITLE = stringPreferencesKey("draft_title")
        private val KEY_CONTENT = stringPreferencesKey("draft_content")
        private val KEY_CATEGORY_ID = intPreferencesKey("draft_category_id")
        private val KEY_COLOR = longPreferencesKey("draft_color")
        private val KEY_IS_PINNED = booleanPreferencesKey("draft_is_pinned")
    }

    /**
     * 響應式取得目前暫存中的記事草稿狀態
     */
    val draftFlow: Flow<NoteDraft> = dataStore.data.map { preferences ->
        val hasDraft = preferences[KEY_HAS_DRAFT] ?: false
        if (!hasDraft) {
            NoteDraft(hasActiveDraft = false)
        } else {
            val rawNoteId = preferences[KEY_EDITING_NOTE_ID]
            val rawCategoryId = preferences[KEY_CATEGORY_ID]
            NoteDraft(
                hasActiveDraft = true,
                editingNoteId = if (rawNoteId != null && rawNoteId > 0) rawNoteId else null,
                title = preferences[KEY_TITLE] ?: "",
                content = preferences[KEY_CONTENT] ?: "",
                categoryId = if (rawCategoryId != null && rawCategoryId > 0) rawCategoryId else null,
                color = preferences[KEY_COLOR] ?: 0xFF2196F3L,
                isPinned = preferences[KEY_IS_PINNED] ?: false
            )
        }
    }

    /**
     * 單次取得目前草稿狀態
     */
    suspend fun getDraftOnce(): NoteDraft = draftFlow.first()

    /**
     * 儲存未完成之記事草稿內容
     */
    suspend fun saveDraft(draft: NoteDraft) {
        dataStore.edit { preferences ->
            preferences[KEY_HAS_DRAFT] = true
            if (draft.editingNoteId != null && draft.editingNoteId > 0) {
                preferences[KEY_EDITING_NOTE_ID] = draft.editingNoteId
            } else {
                preferences.remove(KEY_EDITING_NOTE_ID)
            }
            preferences[KEY_TITLE] = draft.title
            preferences[KEY_CONTENT] = draft.content
            if (draft.categoryId != null && draft.categoryId > 0) {
                preferences[KEY_CATEGORY_ID] = draft.categoryId
            } else {
                preferences.remove(KEY_CATEGORY_ID)
            }
            preferences[KEY_COLOR] = draft.color
            preferences[KEY_IS_PINNED] = draft.isPinned
        }
    }

    /**
     * 儲存成功或手動捨棄後清除草稿狀態
     */
    suspend fun clearDraft() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
