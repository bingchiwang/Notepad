package com.example.notepad.data.sync

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.notepad.data.local.dao.CategoryDao
import com.example.notepad.data.local.dao.NoteDao
import com.example.notepad.data.local.dao.PomodoroDao
import com.example.notepad.data.local.dao.ReminderDao
import com.example.notepad.data.local.dao.SyncTombstoneDao
import com.example.notepad.data.local.dao.TimeBlockDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.local.entity.PomodoroRecord
import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.SyncEntityType
import com.example.notepad.data.local.entity.SyncStatus
import com.example.notepad.data.local.entity.SyncTombstone
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo
import com.example.notepad.data.preferences.AppFontScale
import com.example.notepad.data.preferences.CalendarStyle
import com.example.notepad.data.preferences.NotificationPreferences
import com.example.notepad.data.preferences.SyncPreferences
import com.example.notepad.data.preferences.SyncSettings
import com.example.notepad.data.preferences.ThemeMode
import com.example.notepad.data.preferences.ThemePreferences
import com.example.notepad.data.repository.NoteRepository
import com.example.notepad.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 雲端同步協調器單元測試（驗證資料同步規則 1 ~ 6）
 *
 * 使用 Fake 記憶體式 Room DAO 與 FakeCloudRemoteDataSource 模擬多裝置 Firebase Firestore 行為，
 * 涵蓋：本地優先寫入、全部實體同步、離線變更與墓碑保留、連網自動上傳、lastSyncAt 紀錄及 updatedAt 衝突解決。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CloudSyncManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `rule 1 2 and 5 local-first writes sync all entities to remote and update lastSyncAt`() = runTest {
        val remote = FakeCloudRemoteDataSource()
        val device = createDevice("deviceA", remote, initialOnline = true)
        try {
            val catId = device.noteRepository.insertCategory("工作").toInt()
            device.noteRepository.insertNote(
                Note(
                    title = "季度計畫",
                    content = "整理 Q4 重點工作",
                    isPinned = true,
                    categoryId = catId,
                    syncId = "note-1"
                )
            )
            device.taskRepository.insertToDo(
                ToDo(
                    id = 1,
                    title = "提交報表",
                    priority = Priority.HIGH,
                    syncId = "todo-1"
                )
            )
            device.taskRepository.insertReminder(
                Reminder(
                    title = "開會提醒",
                    message = "10:00 同步會議",
                    triggerAt = 9_999_999L,
                    linkedToDoId = 1,
                    syncId = "rem-1"
                )
            )
            device.taskRepository.insertTimeBlock(
                TimeBlock(
                    date = 20261110L,
                    startTime = "09:00",
                    endTime = "10:30",
                    title = "深度撰寫",
                    syncId = "tb-1"
                )
            )
            device.taskRepository.insertPomodoroRecord(
                PomodoroRecord(
                    timestamp = 1000L,
                    durationMinutes = 25,
                    taskTitle = "提交報表",
                    date = 20261110L,
                    syncId = "pomo-1"
                )
            )
            device.themePreferences.setThemeMode(ThemeMode.DARK)
            device.notificationPreferences.setNotificationsEnabled(false)
            device.syncPreferences.markSettingsPending(updatedAt = 5000L)

            val result = device.syncManager.syncNow(isManual = true)

            assertTrue(result.succeeded)
            assertNotNull(result.syncedAt)
            assertEquals(7, result.uploadedCount)

            val accountId = device.syncPreferences.getSyncSettingsOnce().syncAccountId
            val remoteDocs = remote.getAccountDocs(accountId)
            assertEquals(7, remoteDocs.size)
            assertEquals(SyncStatus.SYNCED, device.noteDao.getNoteBySyncIdOnce("note-1")?.syncStatus)
            assertEquals(SyncStatus.SYNCED, device.toDoDao.getToDoBySyncIdOnce("todo-1")?.syncStatus)
            assertEquals(
                result.syncedAt,
                device.syncPreferences.getSyncSettingsOnce().lastSyncAt
            )
            assertFalse(device.syncPreferences.getSyncSettingsOnce().settingsSyncPending)
        } finally {
            device.close()
        }
    }

    @Test
    fun `rule 3 and 4 offline edits and tombstones are preserved then auto-uploaded upon network recovery`() = runTest {
        val remote = FakeCloudRemoteDataSource()
        val device = createDevice("offlineDevice", remote, initialOnline = true)
        val syncedCallbacks = mutableListOf<Pair<String, Boolean>>()
        val managerWithCallbacks = device.createManager { reminder, isDeleted ->
            syncedCallbacks += reminder.syncId to isDeleted
        }

        try {
            // 先於線上建立並同步一筆舊記事
            device.noteRepository.insertNote(
                Note(title = "舊記事", content = "待刪除", syncId = "note-old", updatedAt = 1000L)
            )
            assertTrue(managerWithCallbacks.syncNow().succeeded)
            val firstSyncAt = device.syncPreferences.getSyncSettingsOnce().lastSyncAt
            assertNotNull(firstSyncAt)

            // 切換為離線狀態並啟動觀察者
            device.networkObserver.setOnline(false)
            managerWithCallbacks.startObserving(backgroundScope)
            runCurrent()

            // 離線執行新增記事與刪除舊記事
            val existingNote = device.noteDao.getNoteBySyncIdOnce("note-old")!!
            device.noteRepository.deleteNote(existingNote)
            device.noteRepository.insertNote(
                Note(title = "離線草稿", content = "離線建立內容", syncId = "note-offline", updatedAt = 2000L)
            )

            // 離線期間執行同步應保留待同步狀態與墓碑，且不覆寫先前 lastSyncAt
            val offlineResult = managerWithCallbacks.syncNow(isManual = false)
            assertFalse(offlineResult.succeeded)
            assertEquals(1, device.tombstoneDao.getAllTombstonesOnce().size)
            assertEquals(
                SyncStatus.PENDING_UPLOAD,
                device.noteDao.getNoteBySyncIdOnce("note-offline")?.syncStatus
            )
            assertEquals(firstSyncAt, device.syncPreferences.getSyncSettingsOnce().lastSyncAt)

            // 恢復網路連線 -> 觸發自動上傳
            device.networkObserver.setOnline(true)
            advanceTimeBy(400L)
            runCurrent()

            val accountId = device.syncPreferences.getSyncSettingsOnce().syncAccountId
            val remoteDocs = remote.getAccountDocs(accountId).associateBy { it.documentId }
            assertEquals(true, remoteDocs["NOTE_note-old"]?.isDeleted)
            assertEquals(false, remoteDocs["NOTE_note-offline"]?.isDeleted)
            assertEquals("離線草稿", remoteDocs["NOTE_note-offline"]?.payload?.get("title"))
            assertEquals(0, device.tombstoneDao.getAllTombstonesOnce().size)
            assertEquals(
                SyncStatus.SYNCED,
                device.noteDao.getNoteBySyncIdOnce("note-offline")?.syncStatus
            )
            assertNotNull(device.syncPreferences.getSyncSettingsOnce().lastSyncAt)
            assertTrue(syncedCallbacks.isEmpty())
        } finally {
            device.close()
        }
    }

    @Test
    fun `rule 5 sync failure preserves existing lastSyncAt`() = runTest {
        val remote = FakeCloudRemoteDataSource()
        val device = createDevice("errorDevice", remote, initialOnline = true)
        try {
            device.noteRepository.insertNote(Note(title = "A", content = "B", syncId = "n1"))
            val successResult = device.syncManager.syncNow()
            assertTrue(successResult.succeeded)
            val savedTime = device.syncPreferences.getSyncSettingsOnce().lastSyncAt
            assertNotNull(savedTime)

            remote.shouldThrowOnNextSync = true
            device.noteRepository.insertNote(Note(title = "C", content = "D", syncId = "n2"))
            val failedResult = device.syncManager.syncNow()

            assertFalse(failedResult.succeeded)
            assertEquals(savedTime, device.syncPreferences.getSyncSettingsOnce().lastSyncAt)
            assertEquals(
                SyncStatus.PENDING_UPLOAD,
                device.noteDao.getNoteBySyncIdOnce("n2")?.syncStatus
            )
        } finally {
            device.close()
        }
    }

    @Test
    fun `rule 6 multi-device conflict resolves by latest updatedAt across entities and settings`() = runTest {
        val remote = FakeCloudRemoteDataSource()
        val deviceA = createDevice("devA", remote, initialOnline = true, sharedAccountId = "team-space")
        val deviceB = createDevice("devB", remote, initialOnline = true, sharedAccountId = "team-space")
        val reminderHooksOnB = mutableListOf<Pair<String, Boolean>>()
        val managerB = deviceB.createManager { reminder, isDeleted ->
            reminderHooksOnB += reminder.syncId to isDeleted
        }

        try {
            // Device A 建立完整資料集並上傳
            val catIdA = deviceA.noteRepository.insertCategory("學習").toInt()
            val syncedCat = deviceA.categoryDao.getCategoryByIdOnce(catIdA)!!
            deviceA.noteRepository.insertNote(
                Note(
                    title = "舊筆記標題",
                    content = "裝置 A 初始內容",
                    categoryId = catIdA,
                    updatedAt = 1000L,
                    syncId = "shared-note"
                )
            )
            deviceA.noteRepository.insertNote(
                Note(
                    title = "裝置 A 較新版本",
                    content = "A 更新較晚",
                    updatedAt = 9000L,
                    syncId = "note-a-wins"
                )
            )
            deviceA.taskRepository.insertToDo(
                ToDo(id = 10, title = "複習單字", updatedAt = 1000L, syncId = "shared-todo")
            )
            deviceA.taskRepository.insertReminder(
                Reminder(
                    title = "晚間複習",
                    message = "背 20 個單字",
                    triggerAt = 8_888_888L,
                    linkedToDoId = 10,
                    updatedAt = 2000L,
                    syncId = "shared-rem"
                )
            )
            deviceA.themePreferences.setThemeMode(ThemeMode.LIGHT)
            deviceA.notificationPreferences.setNotificationsEnabled(true)
            deviceA.syncPreferences.markSettingsPending(updatedAt = 1500L)
            assertTrue(deviceA.syncManager.syncNow().succeeded)

            // Device B 離線時也做出更晚或更早的本地編輯，製造雙向衝突
            deviceB.noteDao.insertNote(
                Note(
                    title = "裝置 B 較新勝出",
                    content = "B 於 5000ms 更新",
                    updatedAt = 5000L,
                    syncId = "shared-note",
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
            )
            deviceB.noteDao.insertNote(
                Note(
                    title = "裝置 B 較舊版本",
                    content = "B 於 3000ms 更新",
                    updatedAt = 3000L,
                    syncId = "note-a-wins",
                    syncStatus = SyncStatus.PENDING_UPLOAD
                )
            )
            deviceB.themePreferences.setThemeMode(ThemeMode.DARK)
            deviceB.themePreferences.setButtonColor(0xFF009688L)
            deviceB.themePreferences.setCalendarStyle(CalendarStyle.OCEAN_NIGHT)
            deviceB.themePreferences.setCalendarAccentColor(0xFFFFEB3BL)
            deviceB.themePreferences.setFontScale(AppFontScale.LARGE)
            deviceB.notificationPreferences.setNotificationsEnabled(false)
            deviceB.syncPreferences.markSettingsPending(updatedAt = 6000L)

            val syncResultB = managerB.syncNow()
            assertTrue(syncResultB.succeeded)
            assertTrue(syncResultB.conflictResolvedCount >= 2)

            // 1. 關聯類別與筆記關聯鍵正確重建
            val downloadedCatB = deviceB.categoryDao.getCategoryBySyncIdOnce(syncedCat.syncId)
            assertNotNull(downloadedCatB)
            // 2. shared-note 由 B 較新勝出 (5000 > 1000)
            assertEquals("裝置 B 較新勝出", deviceB.noteDao.getNoteBySyncIdOnce("shared-note")?.title)
            // 3. note-a-wins 由 A 較新勝出 (9000 > 3000)
            assertEquals("裝置 A 較新版本", deviceB.noteDao.getNoteBySyncIdOnce("note-a-wins")?.title)
            // 4. 提醒重連至 B 上新的 ToDo ID 並觸發通知排程回呼
            val downloadedToDoB = deviceB.toDoDao.getToDoBySyncIdOnce("shared-todo")
            val downloadedRemB = deviceB.reminderDao.getReminderBySyncIdOnce("shared-rem")
            assertNotNull(downloadedToDoB)
            assertNotNull(downloadedRemB)
            assertEquals(downloadedToDoB?.id, downloadedRemB?.linkedToDoId)
            assertTrue(reminderHooksOnB.any { it.first == "shared-rem" && !it.second })

            // 5. Device A 再度同步，應收到 B 勝出的 shared-note 以及 B 較新的主題、個人化外觀與通知設定
            val syncResultA = deviceA.syncManager.syncNow()
            assertTrue(syncResultA.succeeded)
            assertEquals("裝置 B 較新勝出", deviceA.noteDao.getNoteBySyncIdOnce("shared-note")?.title)
            val syncedPersonalizationA = deviceA.themePreferences.personalizationFlow.first()
            assertEquals(ThemeMode.DARK, syncedPersonalizationA.themeMode)
            assertEquals(0xFF009688L, syncedPersonalizationA.buttonColorLong)
            assertEquals(CalendarStyle.OCEAN_NIGHT, syncedPersonalizationA.calendarStyle)
            assertEquals(0xFFFFEB3BL, syncedPersonalizationA.calendarAccentColorLong)
            assertEquals(AppFontScale.LARGE, syncedPersonalizationA.fontScale)
            assertFalse(deviceA.notificationPreferences.isNotificationsEnabled())
        } finally {
            deviceA.close()
            deviceB.close()
        }
    }

    private suspend fun TestScope.createDevice(
        name: String,
        remote: FakeCloudRemoteDataSource,
        initialOnline: Boolean,
        sharedAccountId: String = "acc-$name"
    ): TestDeviceHarness {
        val dir = tempFolder.newFolder(name)
        val storeScope = CoroutineScope(Job() + UnconfinedTestDispatcher(testScheduler))
        val syncPrefs = SyncPreferences(
            PreferenceDataStoreFactory.create(scope = storeScope) { File(dir, "sync.preferences_pb") }
        )
        val themePrefs = ThemePreferences(
            PreferenceDataStoreFactory.create(scope = storeScope) { File(dir, "theme.preferences_pb") }
        )
        val notifPrefs = NotificationPreferences(
            PreferenceDataStoreFactory.create(scope = storeScope) { File(dir, "notif.preferences_pb") }
        )
        syncPrefs.updateConfiguration(
            syncEnabled = true,
            syncAccountId = sharedAccountId,
            firebaseProjectId = "test-project",
            firebaseAppId = "1:123:android:test",
            firebaseApiKey = "AIzaTest"
        )

        val noteDao = InMemoryNoteDao()
        val categoryDao = InMemoryCategoryDao()
        val toDoDao = InMemoryToDoDao()
        val reminderDao = InMemoryReminderDao()
        val timeBlockDao = InMemoryTimeBlockDao()
        val pomodoroDao = InMemoryPomodoroDao()
        val tombstoneDao = InMemoryTombstoneDao()
        val networkObserver = FakeNetworkObserver(initialOnline)

        val noteRepo = NoteRepository(noteDao, categoryDao, tombstoneDao)
        val taskRepo = TaskRepository(toDoDao, reminderDao, timeBlockDao, pomodoroDao, tombstoneDao)

        return TestDeviceHarness(
            storeScope = storeScope,
            noteDao = noteDao,
            categoryDao = categoryDao,
            toDoDao = toDoDao,
            reminderDao = reminderDao,
            timeBlockDao = timeBlockDao,
            pomodoroDao = pomodoroDao,
            tombstoneDao = tombstoneDao,
            syncPreferences = syncPrefs,
            themePreferences = themePrefs,
            notificationPreferences = notifPrefs,
            networkObserver = networkObserver,
            remoteDataSource = remote,
            noteRepository = noteRepo,
            taskRepository = taskRepo
        )
    }

    private class TestDeviceHarness(
        private val storeScope: CoroutineScope,
        val noteDao: InMemoryNoteDao,
        val categoryDao: InMemoryCategoryDao,
        val toDoDao: InMemoryToDoDao,
        val reminderDao: InMemoryReminderDao,
        val timeBlockDao: InMemoryTimeBlockDao,
        val pomodoroDao: InMemoryPomodoroDao,
        val tombstoneDao: InMemoryTombstoneDao,
        val syncPreferences: SyncPreferences,
        val themePreferences: ThemePreferences,
        val notificationPreferences: NotificationPreferences,
        val networkObserver: FakeNetworkObserver,
        val remoteDataSource: FakeCloudRemoteDataSource,
        val noteRepository: NoteRepository,
        val taskRepository: TaskRepository
    ) {
        val syncManager: CloudSyncManager = createManager()

        fun createManager(
            onReminderSyncedFromCloud: (suspend (Reminder, Boolean) -> Unit)? = null
        ): CloudSyncManager {
            return CloudSyncManager(
                noteDao = noteDao,
                categoryDao = categoryDao,
                toDoDao = toDoDao,
                reminderDao = reminderDao,
                timeBlockDao = timeBlockDao,
                pomodoroDao = pomodoroDao,
                tombstoneDao = tombstoneDao,
                syncPreferences = syncPreferences,
                themePreferences = themePreferences,
                notificationPreferences = notificationPreferences,
                networkObserver = networkObserver,
                remoteDataSource = remoteDataSource,
                onReminderSyncedFromCloud = onReminderSyncedFromCloud
            )
        }

        fun close() {
            storeScope.cancel()
        }
    }
}

private class FakeNetworkObserver(initialOnline: Boolean) : NetworkConnectivityObserver {
    private val _flow = MutableStateFlow(initialOnline)
    override val isOnlineFlow: StateFlow<Boolean> = _flow.asStateFlow()

    override fun isCurrentlyOnline(): Boolean = _flow.value

    fun setOnline(online: Boolean) {
        _flow.value = online
    }
}

private class FakeCloudRemoteDataSource : CloudRemoteDataSource {
    private val storage = ConcurrentHashMap<String, MutableMap<String, CloudSyncDocument>>()
    var shouldThrowOnNextSync: Boolean = false

    override fun isConfigured(settings: SyncSettings): Boolean =
        settings.hasManualFirebaseConfig()

    override suspend fun fetchAllDocuments(
        accountId: String,
        settings: SyncSettings
    ): List<CloudSyncDocument> {
        if (shouldThrowOnNextSync) {
            shouldThrowOnNextSync = false
            throw IllegalStateException("模擬遠端讀取逾時")
        }
        return storage[accountId]?.values?.toList() ?: emptyList()
    }

    override suspend fun upsertDocuments(
        accountId: String,
        settings: SyncSettings,
        documents: List<CloudSyncDocument>
    ) {
        if (shouldThrowOnNextSync) {
            shouldThrowOnNextSync = false
            throw IllegalStateException("模擬遠端寫入失敗")
        }
        val bucket = storage.getOrPut(accountId) { ConcurrentHashMap() }
        documents.forEach { doc ->
            bucket[doc.documentId] = doc
        }
    }

    fun getAccountDocs(accountId: String): List<CloudSyncDocument> =
        storage[accountId]?.values?.toList() ?: emptyList()
}

private class InMemoryNoteDao : NoteDao {
    private val flow = MutableStateFlow<List<Note>>(emptyList())
    private var nextId = 1

    override fun getAllNotes(): Flow<List<Note>> = flow
    override fun getNotesByCategory(categoryId: Int): Flow<List<Note>> =
        flow.map { it.filter { n -> n.categoryId == categoryId } }
    override fun getUncategorizedNotes(): Flow<List<Note>> =
        flow.map { it.filter { n -> n.categoryId == null } }
    override fun searchNotes(query: String): Flow<List<Note>> = flow
    override fun searchNotesByCategory(query: String, categoryId: Int): Flow<List<Note>> = flow
    override fun getNoteById(id: Int): Flow<Note?> = flow.map { it.find { n -> n.id == id } }
    override suspend fun getNoteByIdOnce(id: Int): Note? = flow.value.find { it.id == id }

    override suspend fun insertNote(note: Note): Long {
        val assigned = if (note.id == 0) nextId++ else note.id
        if (assigned >= nextId) nextId = assigned + 1
        val saved = note.copy(id = assigned)
        flow.value = flow.value.filterNot { it.id == assigned } + saved
        return assigned.toLong()
    }

    override suspend fun updateNote(note: Note) {
        flow.value = flow.value.map { if (it.id == note.id) note else it }
    }

    override suspend fun deleteNote(note: Note) {
        flow.value = flow.value.filterNot { it.id == note.id }
    }

    override suspend fun clearCategoryFromNotes(categoryId: Int, updatedAt: Long) {
        flow.value = flow.value.map {
            if (it.categoryId == categoryId) {
                it.copy(categoryId = null, updatedAt = updatedAt, syncStatus = SyncStatus.PENDING_UPLOAD)
            } else {
                it
            }
        }
    }

    override suspend fun reassignNotesCategory(oldCategoryId: Int, newCategoryId: Int?, updatedAt: Long) {
        flow.value = flow.value.map {
            if (it.categoryId == oldCategoryId) {
                it.copy(categoryId = newCategoryId, updatedAt = updatedAt, syncStatus = SyncStatus.PENDING_UPLOAD)
            } else {
                it
            }
        }
    }

    override suspend fun getAllNotesOnce(): List<Note> = flow.value
    override suspend fun getNoteBySyncIdOnce(syncId: String): Note? =
        flow.value.find { it.syncId == syncId }
    override fun observePendingCount(): Flow<Int> =
        flow.map { list -> list.count { it.syncStatus == SyncStatus.PENDING_UPLOAD } }
    override suspend fun markAsSynced(syncId: String) {
        flow.value = flow.value.map {
            if (it.syncId == syncId) it.copy(syncStatus = SyncStatus.SYNCED) else it
        }
    }
    override suspend fun deleteBySyncId(syncId: String) {
        flow.value = flow.value.filterNot { it.syncId == syncId }
    }
}

private class InMemoryCategoryDao : CategoryDao {
    private val flow = MutableStateFlow<List<Category>>(emptyList())
    private var nextId = 1

    override fun getAllCategories(): Flow<List<Category>> = flow
    override fun getCategoryById(id: Int): Flow<Category?> = flow.map { it.find { c -> c.id == id } }
    override suspend fun getCategoryByIdOnce(id: Int): Category? = flow.value.find { it.id == id }
    override suspend fun getCategoryByName(name: String): Category? =
        flow.value.find { it.name.trim().equals(name.trim(), ignoreCase = true) }

    override suspend fun insertCategory(category: Category): Long {
        val assigned = if (category.id == 0) nextId++ else category.id
        if (assigned >= nextId) nextId = assigned + 1
        val saved = category.copy(id = assigned)
        flow.value = flow.value.filterNot { it.id == assigned } + saved
        return assigned.toLong()
    }

    override suspend fun updateCategory(category: Category) {
        flow.value = flow.value.map { if (it.id == category.id) category else it }
    }

    override suspend fun deleteCategory(category: Category) {
        flow.value = flow.value.filterNot { it.id == category.id }
    }

    override suspend fun deleteCategoryById(id: Int) {
        flow.value = flow.value.filterNot { it.id == id }
    }

    override suspend fun getAllCategoriesOnce(): List<Category> = flow.value
    override suspend fun getCategoryBySyncIdOnce(syncId: String): Category? =
        flow.value.find { it.syncId == syncId }
    override fun observePendingCount(): Flow<Int> =
        flow.map { list -> list.count { it.syncStatus == SyncStatus.PENDING_UPLOAD } }
    override suspend fun markAsSynced(syncId: String) {
        flow.value = flow.value.map {
            if (it.syncId == syncId) it.copy(syncStatus = SyncStatus.SYNCED) else it
        }
    }
    override suspend fun deleteBySyncId(syncId: String) {
        flow.value = flow.value.filterNot { it.syncId == syncId }
    }
}

private class InMemoryToDoDao : ToDoDao {
    private val flow = MutableStateFlow<List<ToDo>>(emptyList())
    private var nextId = 1

    override fun getAllToDos(): Flow<List<ToDo>> = flow
    override fun getActiveToDos(): Flow<List<ToDo>> = flow.map { it.filter { t -> !t.isCompleted } }
    override fun getCompletedToDos(): Flow<List<ToDo>> = flow.map { it.filter { t -> t.isCompleted } }
    override fun getToDosByPriority(priority: Priority): Flow<List<ToDo>> =
        flow.map { it.filter { t -> t.priority == priority } }
    override fun getToDosByDate(date: Long): Flow<List<ToDo>> =
        flow.map { it.filter { t -> t.dueDate == date } }
    override fun getToDosByDateRange(startDate: Long, endDate: Long): Flow<List<ToDo>> = flow
    override fun getToDoById(id: Int): Flow<ToDo?> = flow.map { it.find { t -> t.id == id } }

    override suspend fun insertToDo(toDo: ToDo) {
        val assigned = if (toDo.id == 0) nextId++ else toDo.id
        if (assigned >= nextId) nextId = assigned + 1
        flow.value = flow.value.filterNot { it.id == assigned } + toDo.copy(id = assigned)
    }

    override suspend fun updateToDo(toDo: ToDo) {
        flow.value = flow.value.map { if (it.id == toDo.id) toDo else it }
    }

    override suspend fun deleteToDo(toDo: ToDo) {
        flow.value = flow.value.filterNot { it.id == toDo.id }
    }

    override suspend fun getAllToDosOnce(): List<ToDo> = flow.value
    override suspend fun getToDoBySyncIdOnce(syncId: String): ToDo? =
        flow.value.find { it.syncId == syncId }
    override fun observePendingCount(): Flow<Int> =
        flow.map { list -> list.count { it.syncStatus == SyncStatus.PENDING_UPLOAD } }
    override suspend fun markAsSynced(syncId: String) {
        flow.value = flow.value.map {
            if (it.syncId == syncId) it.copy(syncStatus = SyncStatus.SYNCED) else it
        }
    }
    override suspend fun deleteBySyncId(syncId: String) {
        flow.value = flow.value.filterNot { it.syncId == syncId }
    }
}

private class InMemoryReminderDao : ReminderDao {
    private val flow = MutableStateFlow<List<Reminder>>(emptyList())
    private var nextId = 1

    override fun getAllReminders(): Flow<List<Reminder>> = flow
    override fun getRemindersByDate(startMillis: Long, endMillis: Long): Flow<List<Reminder>> =
        flow.map { it.filter { r -> r.triggerAt in startMillis..endMillis } }
    override fun getReminderById(id: Int): Flow<Reminder?> = flow.map { it.find { r -> r.id == id } }
    override suspend fun getPendingRemindersByToDoId(toDoId: Int): List<Reminder> =
        flow.value.filter { it.linkedToDoId == toDoId && !it.isFired }

    override suspend fun insertReminder(reminder: Reminder): Long {
        val assigned = if (reminder.id == 0) nextId++ else reminder.id
        if (assigned >= nextId) nextId = assigned + 1
        flow.value = flow.value.filterNot { it.id == assigned } + reminder.copy(id = assigned)
        return assigned.toLong()
    }

    override suspend fun updateReminder(reminder: Reminder) {
        flow.value = flow.value.map { if (it.id == reminder.id) reminder else it }
    }

    override suspend fun deleteReminder(reminder: Reminder) {
        flow.value = flow.value.filterNot { it.id == reminder.id }
    }

    override suspend fun markAsFired(id: Int, updatedAt: Long) {
        flow.value = flow.value.map {
            if (it.id == id) it.copy(isFired = true, updatedAt = updatedAt, syncStatus = SyncStatus.PENDING_UPLOAD) else it
        }
    }

    override suspend fun getAllRemindersOnce(): List<Reminder> = flow.value
    override suspend fun getReminderBySyncIdOnce(syncId: String): Reminder? =
        flow.value.find { it.syncId == syncId }
    override fun observePendingCount(): Flow<Int> =
        flow.map { list -> list.count { it.syncStatus == SyncStatus.PENDING_UPLOAD } }
    override suspend fun markAsSynced(syncId: String) {
        flow.value = flow.value.map {
            if (it.syncId == syncId) it.copy(syncStatus = SyncStatus.SYNCED) else it
        }
    }
    override suspend fun deleteBySyncId(syncId: String) {
        flow.value = flow.value.filterNot { it.syncId == syncId }
    }
}

private class InMemoryTimeBlockDao : TimeBlockDao {
    private val flow = MutableStateFlow<List<TimeBlock>>(emptyList())
    private var nextId = 1

    override fun getTimeBlocksByDate(date: Long): Flow<List<TimeBlock>> =
        flow.map { it.filter { tb -> tb.date == date } }
    override fun getAllTimeBlocks(): Flow<List<TimeBlock>> = flow

    override suspend fun insertTimeBlock(timeBlock: TimeBlock) {
        val assigned = if (timeBlock.id == 0) nextId++ else timeBlock.id
        if (assigned >= nextId) nextId = assigned + 1
        flow.value = flow.value.filterNot { it.id == assigned } + timeBlock.copy(id = assigned)
    }

    override suspend fun updateTimeBlock(timeBlock: TimeBlock) {
        flow.value = flow.value.map { if (it.id == timeBlock.id) timeBlock else it }
    }

    override suspend fun deleteTimeBlock(timeBlock: TimeBlock) {
        flow.value = flow.value.filterNot { it.id == timeBlock.id }
    }

    override suspend fun getAllTimeBlocksOnce(): List<TimeBlock> = flow.value
    override suspend fun getTimeBlockBySyncIdOnce(syncId: String): TimeBlock? =
        flow.value.find { it.syncId == syncId }
    override fun observePendingCount(): Flow<Int> =
        flow.map { list -> list.count { it.syncStatus == SyncStatus.PENDING_UPLOAD } }
    override suspend fun markAsSynced(syncId: String) {
        flow.value = flow.value.map {
            if (it.syncId == syncId) it.copy(syncStatus = SyncStatus.SYNCED) else it
        }
    }
    override suspend fun deleteBySyncId(syncId: String) {
        flow.value = flow.value.filterNot { it.syncId == syncId }
    }
}

private class InMemoryPomodoroDao : PomodoroDao {
    private val flow = MutableStateFlow<List<PomodoroRecord>>(emptyList())
    private var nextId = 1

    override fun getRecordsByDate(date: Long): Flow<List<PomodoroRecord>> =
        flow.map { it.filter { p -> p.date == date } }
    override fun getTodayCount(date: Long): Flow<Int> =
        flow.map { it.count { p -> p.date == date } }

    override suspend fun insertRecord(record: PomodoroRecord) {
        val assigned = if (record.id == 0) nextId++ else record.id
        if (assigned >= nextId) nextId = assigned + 1
        flow.value = flow.value.filterNot { it.id == assigned } + record.copy(id = assigned)
    }

    override suspend fun getAllRecordsOnce(): List<PomodoroRecord> = flow.value
    override fun observePendingCount(): Flow<Int> =
        flow.map { list -> list.count { it.syncStatus == SyncStatus.PENDING_UPLOAD } }
    override suspend fun markAsSynced(syncId: String) {
        flow.value = flow.value.map {
            if (it.syncId == syncId) it.copy(syncStatus = SyncStatus.SYNCED) else it
        }
    }
    override suspend fun deleteBySyncId(syncId: String) {
        flow.value = flow.value.filterNot { it.syncId == syncId }
    }
}

private class InMemoryTombstoneDao : SyncTombstoneDao {
    private val flow = MutableStateFlow<List<SyncTombstone>>(emptyList())
    private var nextId = 1

    override fun observeTombstoneCount(): Flow<Int> = flow.map { it.size }
    override suspend fun getAllTombstonesOnce(): List<SyncTombstone> = flow.value

    override suspend fun upsertTombstone(tombstone: SyncTombstone) {
        val filtered = flow.value.filterNot {
            it.entityType == tombstone.entityType && it.syncId == tombstone.syncId
        }
        val assigned = if (tombstone.id == 0) nextId++ else tombstone.id
        flow.value = filtered + tombstone.copy(id = assigned)
    }

    override suspend fun deleteTombstone(entityType: SyncEntityType, syncId: String) {
        flow.value = flow.value.filterNot { it.entityType == entityType && it.syncId == syncId }
    }
}
