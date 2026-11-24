package com.example.notepad.data.sync

import com.example.notepad.data.local.entity.SyncEntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [GoogleDriveSyncPayloadSerializer] 單元測試：
 * 驗證各類同步實體（含 Long 時間戳、色彩值、null 欄位、布林與刪除墓碑）之序列化與還原正確性
 */
class GoogleDriveSyncPayloadSerializerTest {

    @Test
    fun serializeAndDeserializeSnapshot_preservesAllEntityTypesAndNumericPrecision() {
        val noteDoc = CloudSyncDocument(
            entityType = SyncEntityType.NOTE,
            syncId = "note-uuid-1",
            updatedAt = 1732400001234L,
            isDeleted = false,
            payload = mapOf(
                "title" to "Google 雲端測試記事",
                "content" to "保留換行\n與特殊字元",
                "color" to 0xFFE3F2FDL,
                "isPinned" to true,
                "createdAt" to 1732400000000L,
                "categorySyncId" to null
            )
        )
        val todoDoc = CloudSyncDocument(
            entityType = SyncEntityType.TODO,
            syncId = "todo-uuid-2",
            updatedAt = 1732400002500L,
            isDeleted = false,
            payload = mapOf(
                "title" to "整理週報",
                "isCompleted" to false,
                "priorityLevel" to 3,
                "dueDate" to 1732499999999L
            )
        )
        val tombstoneDoc = CloudSyncDocument(
            entityType = SyncEntityType.REMINDER,
            syncId = "rem-uuid-3",
            updatedAt = 1732400003999L,
            isDeleted = true,
            payload = emptyMap()
        )
        val settingsDoc = CloudSyncDocument(
            entityType = SyncEntityType.SETTINGS,
            syncId = "app_personalization_settings",
            updatedAt = 1732400005000L,
            isDeleted = false,
            payload = mapOf(
                "themeMode" to "DARK",
                "fontScale" to 115,
                "notificationsEnabled" to true
            )
        )

        val json = GoogleDriveSyncPayloadSerializer.serializeSnapshot(
            documents = listOf(noteDoc, todoDoc, tombstoneDoc, settingsDoc),
            snapshotUpdatedAt = 1732400009000L
        )
        val restored = GoogleDriveSyncPayloadSerializer.deserializeSnapshot(json)

        assertEquals(4, restored.size)

        val restoredNote = restored[0]
        assertEquals(SyncEntityType.NOTE, restoredNote.entityType)
        assertEquals("note-uuid-1", restoredNote.syncId)
        assertEquals(1732400001234L, restoredNote.updatedAt)
        assertFalse(restoredNote.isDeleted)
        assertEquals("Google 雲端測試記事", restoredNote.payload["title"])
        assertEquals("保留換行\n與特殊字元", restoredNote.payload["content"])
        assertEquals(0xFFE3F2FDL, restoredNote.payload["color"])
        assertEquals(true, restoredNote.payload["isPinned"])
        assertEquals(1732400000000L, restoredNote.payload["createdAt"])
        assertNull(restoredNote.payload["categorySyncId"])

        val restoredTodo = restored[1]
        assertEquals(SyncEntityType.TODO, restoredTodo.entityType)
        assertEquals(3L, (restoredTodo.payload["priorityLevel"] as Number).toLong())
        assertEquals(1732499999999L, restoredTodo.payload["dueDate"])

        val restoredTombstone = restored[2]
        assertEquals(SyncEntityType.REMINDER, restoredTombstone.entityType)
        assertTrue(restoredTombstone.isDeleted)
        assertEquals(1732400003999L, restoredTombstone.updatedAt)

        val restoredSettings = restored[3]
        assertEquals(SyncEntityType.SETTINGS, restoredSettings.entityType)
        assertEquals("DARK", restoredSettings.payload["themeMode"])
        assertEquals(true, restoredSettings.payload["notificationsEnabled"])
    }

    @Test
    fun deserializeSnapshot_handlesBlankOrInvalidJsonSafely() {
        assertTrue(GoogleDriveSyncPayloadSerializer.deserializeSnapshot("").isEmpty())
        assertTrue(GoogleDriveSyncPayloadSerializer.deserializeSnapshot("   ").isEmpty())
        assertTrue(GoogleDriveSyncPayloadSerializer.deserializeSnapshot("{}").isEmpty())
    }
}
