package com.example.notepad.data.sync

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
import com.example.notepad.data.preferences.PersonalizationSettings
import com.example.notepad.data.preferences.SyncPreferences
import com.example.notepad.data.preferences.SyncSettings
import com.example.notepad.data.preferences.ThemeMode
import com.example.notepad.data.preferences.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 雲端資料雙向同步核心協調器（全面落實資料同步規則 1 ~ 6）
 *
 * 職責：
 * 1. 統計所有待同步本機異動（新增／更新／離線刪除墓碑／偏好設定）
 * 2. 監測網路連線恢復與本機資料異動，自動觸發或手動執行同步
 * 3. 無網路時保留所有本機修改與待同步狀態，不丟失任何離線變更
 * 4. 雙向合併全部資料（記事、分類、待辦、提醒、時間規劃、番茄鐘與主題／通知設定）
 * 5. 衝突時委派 [SyncConflictResolver] 依最新修改時間 (`updatedAt`) 智慧合併
 * 6. 同步成功後更新本機狀態為 SYNCED 並更新最後同步時間 (`lastSyncAt`)
 */
class CloudSyncManager(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao,
    private val toDoDao: ToDoDao,
    private val reminderDao: ReminderDao,
    private val timeBlockDao: TimeBlockDao,
    private val pomodoroDao: PomodoroDao,
    private val tombstoneDao: SyncTombstoneDao,
    private val syncPreferences: SyncPreferences,
    private val themePreferences: ThemePreferences,
    private val notificationPreferences: NotificationPreferences,
    private val networkObserver: NetworkConnectivityObserver,
    private val remoteDataSource: CloudRemoteDataSource,
    private val onReminderSyncedFromCloud: (suspend (reminder: Reminder, isDeleted: Boolean) -> Unit)? = null
) {
    private val syncMutex = Mutex()
    private var monitorScope: CoroutineScope? = null
    private var autoSyncJob: Job? = null

    private val _isSyncing = MutableStateFlow(false)

    private val totalPendingCountFlow: Flow<Int> = combine(
        noteDao.observePendingCount(),
        categoryDao.observePendingCount(),
        toDoDao.observePendingCount(),
        reminderDao.observePendingCount(),
        timeBlockDao.observePendingCount(),
        pomodoroDao.observePendingCount(),
        tombstoneDao.observeTombstoneCount(),
        syncPreferences.syncSettingsFlow
    ) { counts ->
        val notes = counts[0] as Int
        val categories = counts[1] as Int
        val todos = counts[2] as Int
        val reminders = counts[3] as Int
        val blocks = counts[4] as Int
        val pomodoros = counts[5] as Int
        val tombstones = counts[6] as Int
        val settings = counts[7] as SyncSettings
        notes + categories + todos + reminders + blocks + pomodoros + tombstones +
            if (settings.settingsSyncPending) 1 else 0
    }

    private val _uiState = MutableStateFlow(CloudSyncUiState())
    val uiState: StateFlow<CloudSyncUiState> = _uiState.asStateFlow()

    /**
     * 啟動生命週期狀態觀察與網路恢復自動同步監聽（規則 2 & 4）
     */
    fun startObserving(scope: CoroutineScope) {
        monitorScope = scope

        scope.launch {
            syncPreferences.ensureSyncAccountId()
        }

        // 組合線上狀態、同步狀態、待同步筆數與設定供 UI 即時反映
        scope.launch {
            combine(
                networkObserver.isOnlineFlow,
                _isSyncing,
                totalPendingCountFlow,
                syncPreferences.syncSettingsFlow
            ) { isOnline, isSyncing, pendingCount, settings ->
                CloudSyncUiState(
                    isOnline = isOnline,
                    isSyncing = isSyncing,
                    pendingCount = pendingCount,
                    isFirebaseConfigured = remoteDataSource.isConfigured(settings),
                    syncSettings = settings,
                    lastRunSummary = settings.lastSyncMessage
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // 監聽網路從離線恢復為連線，或啟用且已配置時偵測待同步項目並自動上傳
        var previousOnline = networkObserver.isCurrentlyOnline()
        scope.launch {
            networkObserver.isOnlineFlow.collect { nowOnline ->
                val recovered = !previousOnline && nowOnline
                previousOnline = nowOnline
                if (recovered) {
                    val settings = syncPreferences.getSyncSettingsOnce()
                    if (settings.syncEnabled && remoteDataSource.isConfigured(settings)) {
                        scheduleAutoSync(delayMillis = 300L)
                    }
                }
            }
        }
    }

    /**
     * 當本機資料或設定異動時呼叫（有網路時立即節流同步；無網路時保留待同步狀態）
     */
    fun requestSyncOnDataChanged() {
        val scope = monitorScope ?: return
        scope.launch {
            val settings = syncPreferences.getSyncSettingsOnce()
            if (!settings.syncEnabled || !remoteDataSource.isConfigured(settings)) return@launch
            if (!networkObserver.isCurrentlyOnline()) {
                syncPreferences.recordSyncStatusMessage("離線中：已保留本機修改與待同步狀態")
                return@launch
            }
            scheduleAutoSync(delayMillis = 600L)
        }
    }

    private fun scheduleAutoSync(delayMillis: Long) {
        val scope = monitorScope ?: return
        autoSyncJob?.cancel()
        autoSyncJob = scope.launch {
            delay(delayMillis)
            syncNow(isManual = false)
        }
    }

    /**
     * 執行完整雙向同步（規則 2 ~ 6）
     */
    suspend fun syncNow(isManual: Boolean = true): SyncRunResult = syncMutex.withLock {
        val settings = syncPreferences.getSyncSettingsOnce()
        val accountId = syncPreferences.ensureSyncAccountId()

        if (!settings.syncEnabled) {
            val msg = "雲端同步目前已關閉，資料僅保存於本機"
            if (isManual) syncPreferences.recordSyncStatusMessage(msg)
            return@withLock SyncRunResult(succeeded = false, message = msg)
        }

        if (!remoteDataSource.isConfigured(settings)) {
            val msg = "尚未設定 Firebase 專案（可加入 google-services.json 或手動填寫連線參數），已保留待同步狀態"
            syncPreferences.recordSyncStatusMessage(msg)
            return@withLock SyncRunResult(succeeded = false, message = msg)
        }

        if (!networkObserver.isCurrentlyOnline()) {
            val msg = "無網路連線，已保留本機修改與待同步狀態，待網路恢復後自動上傳"
            syncPreferences.recordSyncStatusMessage(msg)
            return@withLock SyncRunResult(succeeded = false, message = msg)
        }

        _isSyncing.value = true
        return@withLock try {
            val remoteDocs = remoteDataSource.fetchAllDocuments(accountId, settings)
            val remoteByDocId = remoteDocs.associateBy { it.documentId }
            val localTombstones = tombstoneDao.getAllTombstonesOnce()
            val tombstoneByKey = localTombstones.associateBy { "${it.entityType.value}_${it.syncId}" }

            val docsToUpload = mutableListOf<CloudSyncDocument>()
            val syncedIdsToMark = mutableListOf<Pair<SyncEntityType, String>>()
            val tombstonesToClear = mutableListOf<SyncTombstone>()

            var uploaded = 0
            var downloaded = 0
            var deleted = 0
            var conflicts = 0

            // 1. 分類（須優先同步以建立 Note -> Category 外鍵映射）
            val categoryResult = mergeCategories(remoteByDocId, tombstoneByKey)
            docsToUpload += categoryResult.docsToUpload
            syncedIdsToMark += categoryResult.syncedIdsToMark.map { SyncEntityType.CATEGORY to it }
            tombstonesToClear += categoryResult.tombstonesToClear
            downloaded += categoryResult.downloaded
            deleted += categoryResult.deleted
            conflicts += categoryResult.conflicts

            // 建立最新 syncId <-> local id 對照表
            val allLocalCategories = categoryDao.getAllCategoriesOnce()
            val catIdToSyncId = allLocalCategories.associate { it.id to it.syncId }
            val catSyncIdToId = allLocalCategories.associate { it.syncId to it.id }

            // 2. 記事（含 categorySyncId 跨裝置關聯映射）
            val noteResult = mergeNotes(remoteByDocId, tombstoneByKey, catIdToSyncId, catSyncIdToId)
            docsToUpload += noteResult.docsToUpload
            syncedIdsToMark += noteResult.syncedIdsToMark.map { SyncEntityType.NOTE to it }
            tombstonesToClear += noteResult.tombstonesToClear
            downloaded += noteResult.downloaded
            deleted += noteResult.deleted
            conflicts += noteResult.conflicts

            // 3. 待辦事項（須早於提醒同步，確保 Reminder.linkedToDoId 正確對應）
            val todoResult = mergeToDos(remoteByDocId, tombstoneByKey)
            docsToUpload += todoResult.docsToUpload
            syncedIdsToMark += todoResult.syncedIdsToMark.map { SyncEntityType.TODO to it }
            tombstonesToClear += todoResult.tombstonesToClear
            downloaded += todoResult.downloaded
            deleted += todoResult.deleted
            conflicts += todoResult.conflicts

            val allLocalToDos = toDoDao.getAllToDosOnce()
            val todoIdToSyncId = allLocalToDos.associate { it.id to it.syncId }
            val todoSyncIdToId = allLocalToDos.associate { it.syncId to it.id }

            // 4. 提醒事項（同步後連動排程／取消鬧鐘）
            val reminderResult = mergeReminders(remoteByDocId, tombstoneByKey, todoIdToSyncId, todoSyncIdToId)
            docsToUpload += reminderResult.docsToUpload
            syncedIdsToMark += reminderResult.syncedIdsToMark.map { SyncEntityType.REMINDER to it }
            tombstonesToClear += reminderResult.tombstonesToClear
            downloaded += reminderResult.downloaded
            deleted += reminderResult.deleted
            conflicts += reminderResult.conflicts

            // 5. 每日時間規劃表
            val blockResult = mergeTimeBlocks(remoteByDocId, tombstoneByKey)
            docsToUpload += blockResult.docsToUpload
            syncedIdsToMark += blockResult.syncedIdsToMark.map { SyncEntityType.TIME_BLOCK to it }
            tombstonesToClear += blockResult.tombstonesToClear
            downloaded += blockResult.downloaded
            deleted += blockResult.deleted
            conflicts += blockResult.conflicts

            // 6. 番茄鐘紀錄
            val pomodoroResult = mergePomodoros(remoteByDocId, tombstoneByKey)
            docsToUpload += pomodoroResult.docsToUpload
            syncedIdsToMark += pomodoroResult.syncedIdsToMark.map { SyncEntityType.POMODORO to it }
            tombstonesToClear += pomodoroResult.tombstonesToClear
            downloaded += pomodoroResult.downloaded
            deleted += pomodoroResult.deleted
            conflicts += pomodoroResult.conflicts

            // 7. 主題與通知設定
            val settingsMerge = mergeSettings(remoteByDocId, settings)
            settingsMerge.docToUpload?.let { docsToUpload += it }
            if (settingsMerge.downloaded) downloaded++
            if (settingsMerge.conflict) conflicts++

            // 執行批次上傳至 Firebase
            if (docsToUpload.isNotEmpty()) {
                remoteDataSource.upsertDocuments(accountId, settings, docsToUpload)
                uploaded = docsToUpload.size
            }

            // 確認遠端寫入成功後，更新本機 SYNCED 狀態並清理已處理墓碑
            syncedIdsToMark.forEach { (type, syncId) ->
                when (type) {
                    SyncEntityType.CATEGORY -> categoryDao.markAsSynced(syncId)
                    SyncEntityType.NOTE -> noteDao.markAsSynced(syncId)
                    SyncEntityType.TODO -> toDoDao.markAsSynced(syncId)
                    SyncEntityType.REMINDER -> reminderDao.markAsSynced(syncId)
                    SyncEntityType.TIME_BLOCK -> timeBlockDao.markAsSynced(syncId)
                    SyncEntityType.POMODORO -> pomodoroDao.markAsSynced(syncId)
                    SyncEntityType.SETTINGS -> Unit
                }
            }
            tombstonesToClear.forEach { tombstone ->
                tombstoneDao.deleteTombstone(tombstone.entityType, tombstone.syncId)
            }

            val syncedAt = System.currentTimeMillis()
            val summaryMsg = buildString {
                append("同步成功：上傳 $uploaded 筆、下載 $downloaded 筆、刪除 $deleted 筆")
                if (conflicts > 0) append("（依最新時間解決 $conflicts 筆衝突）")
            }
            syncPreferences.recordSyncSuccess(syncedAt, summaryMsg)

            SyncRunResult(
                succeeded = true,
                uploadedCount = uploaded,
                downloadedCount = downloaded,
                deletedCount = deleted,
                conflictResolvedCount = conflicts,
                syncedAt = syncedAt,
                message = summaryMsg
            )
        } catch (e: Exception) {
            val errMsg = "同步暫緩（已保留本機待同步資料）：${e.localizedMessage ?: "連線異常"}"
            syncPreferences.recordSyncStatusMessage(errMsg)
            SyncRunResult(succeeded = false, message = errMsg)
        } finally {
            _isSyncing.value = false
        }
    }

    // === 各實體雙向合併輔助結構與函式 ===

    private data class MergeAccumulator(
        val docsToUpload: MutableList<CloudSyncDocument> = mutableListOf(),
        val syncedIdsToMark: MutableList<String> = mutableListOf(),
        val tombstonesToClear: MutableList<SyncTombstone> = mutableListOf(),
        var downloaded: Int = 0,
        var deleted: Int = 0,
        var conflicts: Int = 0
    )

    private suspend fun mergeCategories(
        remoteByDocId: Map<String, CloudSyncDocument>,
        tombstoneByKey: Map<String, SyncTombstone>
    ): MergeAccumulator {
        val acc = MergeAccumulator()
        val locals = categoryDao.getAllCategoriesOnce()
        val localBySyncId = locals.associateBy { it.syncId }

        for (local in locals) {
            val docId = "${SyncEntityType.CATEGORY.value}_${local.syncId}"
            val remote = remoteByDocId[docId]
            if (remote == null) {
                if (local.syncStatus == SyncStatus.PENDING_UPLOAD) {
                    acc.docsToUpload += local.toCloudDocument()
                    acc.syncedIdsToMark += local.syncId
                } else {
                    acc.docsToUpload += local.toCloudDocument()
                }
            } else {
                val decision = SyncConflictResolver.resolveEntityConflict(
                    localUpdatedAt = local.updatedAt,
                    localSyncStatus = local.syncStatus,
                    remoteUpdatedAt = remote.updatedAt,
                    remoteIsDeleted = remote.isDeleted
                )
                when (decision) {
                    EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD && !remote.isDeleted && local.updatedAt != remote.updatedAt) {
                            acc.conflicts++
                        }
                        acc.docsToUpload += local.toCloudDocument()
                        acc.syncedIdsToMark += local.syncId
                    }

                    EntityConflictDecision.ACCEPT_REMOTE -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.conflicts++
                        val updated = remote.toCategory(existingId = local.id)
                        if (updated != null) {
                            categoryDao.updateCategory(updated)
                            acc.downloaded++
                        }
                    }

                    EntityConflictDecision.DELETE_LOCAL -> {
                        noteDao.clearCategoryFromNotes(local.id, remote.updatedAt)
                        categoryDao.deleteBySyncId(local.syncId)
                        acc.deleted++
                    }

                    EntityConflictDecision.NO_OP -> Unit
                }
            }
        }

        // 處理離線刪除墓碑
        tombstoneByKey.values.filter { it.entityType == SyncEntityType.CATEGORY }.forEach { tombstone ->
            val docId = "${SyncEntityType.CATEGORY.value}_${tombstone.syncId}"
            val remote = remoteByDocId[docId]
            when (SyncConflictResolver.resolveTombstoneConflict(tombstone.deletedAt, remote?.updatedAt, remote?.isDeleted == true)) {
                TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE -> {
                    acc.docsToUpload += CloudSyncDocument(
                        entityType = SyncEntityType.CATEGORY,
                        syncId = tombstone.syncId,
                        updatedAt = tombstone.deletedAt,
                        isDeleted = true
                    )
                    acc.tombstonesToClear += tombstone
                }

                TombstoneConflictDecision.RESURRECT_REMOTE_AND_CLEAR_TOMBSTONE -> {
                    remote?.toCategory(existingId = 0)?.let { restored ->
                        if (categoryDao.getCategoryByName(restored.name) == null) {
                            categoryDao.insertCategory(restored)
                            acc.downloaded++
                            acc.conflicts++
                        }
                    }
                    acc.tombstonesToClear += tombstone
                }
            }
        }

        // 下載僅存在於遠端的新增分類（同時保護分類名稱唯一索引）
        remoteByDocId.values
            .filter { it.entityType == SyncEntityType.CATEGORY && !it.isDeleted }
            .forEach { remote ->
                val key = "${SyncEntityType.CATEGORY.value}_${remote.syncId}"
                if (!localBySyncId.containsKey(remote.syncId) && !tombstoneByKey.containsKey(key)) {
                    val incoming = remote.toCategory(existingId = 0) ?: return@forEach
                    val sameNameLocal = categoryDao.getCategoryByName(incoming.name)
                    if (sameNameLocal == null) {
                        categoryDao.insertCategory(incoming)
                        acc.downloaded++
                    } else if (incoming.updatedAt > sameNameLocal.updatedAt) {
                        categoryDao.updateCategory(
                            sameNameLocal.copy(
                                color = incoming.color,
                                updatedAt = incoming.updatedAt,
                                syncStatus = SyncStatus.SYNCED
                            )
                        )
                        acc.conflicts++
                    }
                }
            }

        return acc
    }

    private suspend fun mergeNotes(
        remoteByDocId: Map<String, CloudSyncDocument>,
        tombstoneByKey: Map<String, SyncTombstone>,
        catIdToSyncId: Map<Int, String>,
        catSyncIdToId: Map<String, Int>
    ): MergeAccumulator {
        val acc = MergeAccumulator()
        val locals = noteDao.getAllNotesOnce()
        val localBySyncId = locals.associateBy { it.syncId }

        for (local in locals) {
            val docId = "${SyncEntityType.NOTE.value}_${local.syncId}"
            val remote = remoteByDocId[docId]
            val catSyncId = local.categoryId?.let { catIdToSyncId[it] }

            if (remote == null) {
                acc.docsToUpload += local.toCloudDocument(catSyncId)
                if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.syncedIdsToMark += local.syncId
            } else {
                when (SyncConflictResolver.resolveEntityConflict(local.updatedAt, local.syncStatus, remote.updatedAt, remote.isDeleted)) {
                    EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD && !remote.isDeleted && local.updatedAt != remote.updatedAt) acc.conflicts++
                        acc.docsToUpload += local.toCloudDocument(catSyncId)
                        acc.syncedIdsToMark += local.syncId
                    }

                    EntityConflictDecision.ACCEPT_REMOTE -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.conflicts++
                        val updated = remote.toNote(existingId = local.id, catSyncIdToId = catSyncIdToId)
                        if (updated != null) {
                            noteDao.updateNote(updated)
                            acc.downloaded++
                        }
                    }

                    EntityConflictDecision.DELETE_LOCAL -> {
                        noteDao.deleteBySyncId(local.syncId)
                        acc.deleted++
                    }

                    EntityConflictDecision.NO_OP -> Unit
                }
            }
        }

        tombstoneByKey.values.filter { it.entityType == SyncEntityType.NOTE }.forEach { tombstone ->
            val remote = remoteByDocId["${SyncEntityType.NOTE.value}_${tombstone.syncId}"]
            when (SyncConflictResolver.resolveTombstoneConflict(tombstone.deletedAt, remote?.updatedAt, remote?.isDeleted == true)) {
                TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE -> {
                    acc.docsToUpload += CloudSyncDocument(
                        entityType = SyncEntityType.NOTE,
                        syncId = tombstone.syncId,
                        updatedAt = tombstone.deletedAt,
                        isDeleted = true
                    )
                    acc.tombstonesToClear += tombstone
                }

                TombstoneConflictDecision.RESURRECT_REMOTE_AND_CLEAR_TOMBSTONE -> {
                    remote?.toNote(existingId = 0, catSyncIdToId = catSyncIdToId)?.let {
                        noteDao.insertNote(it)
                        acc.downloaded++
                        acc.conflicts++
                    }
                    acc.tombstonesToClear += tombstone
                }
            }
        }

        remoteByDocId.values
            .filter { it.entityType == SyncEntityType.NOTE && !it.isDeleted }
            .forEach { remote ->
                val key = "${SyncEntityType.NOTE.value}_${remote.syncId}"
                if (!localBySyncId.containsKey(remote.syncId) && !tombstoneByKey.containsKey(key)) {
                    remote.toNote(existingId = 0, catSyncIdToId = catSyncIdToId)?.let {
                        noteDao.insertNote(it)
                        acc.downloaded++
                    }
                }
            }

        return acc
    }

    private suspend fun mergeToDos(
        remoteByDocId: Map<String, CloudSyncDocument>,
        tombstoneByKey: Map<String, SyncTombstone>
    ): MergeAccumulator {
        val acc = MergeAccumulator()
        val locals = toDoDao.getAllToDosOnce()
        val localBySyncId = locals.associateBy { it.syncId }

        for (local in locals) {
            val remote = remoteByDocId["${SyncEntityType.TODO.value}_${local.syncId}"]
            if (remote == null) {
                acc.docsToUpload += local.toCloudDocument()
                if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.syncedIdsToMark += local.syncId
            } else {
                when (SyncConflictResolver.resolveEntityConflict(local.updatedAt, local.syncStatus, remote.updatedAt, remote.isDeleted)) {
                    EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD && !remote.isDeleted && local.updatedAt != remote.updatedAt) acc.conflicts++
                        acc.docsToUpload += local.toCloudDocument()
                        acc.syncedIdsToMark += local.syncId
                    }

                    EntityConflictDecision.ACCEPT_REMOTE -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.conflicts++
                        remote.toToDo(existingId = local.id)?.let {
                            toDoDao.updateToDo(it)
                            acc.downloaded++
                        }
                    }

                    EntityConflictDecision.DELETE_LOCAL -> {
                        toDoDao.deleteBySyncId(local.syncId)
                        acc.deleted++
                    }

                    EntityConflictDecision.NO_OP -> Unit
                }
            }
        }

        tombstoneByKey.values.filter { it.entityType == SyncEntityType.TODO }.forEach { tombstone ->
            val remote = remoteByDocId["${SyncEntityType.TODO.value}_${tombstone.syncId}"]
            when (SyncConflictResolver.resolveTombstoneConflict(tombstone.deletedAt, remote?.updatedAt, remote?.isDeleted == true)) {
                TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE -> {
                    acc.docsToUpload += CloudSyncDocument(
                        entityType = SyncEntityType.TODO,
                        syncId = tombstone.syncId,
                        updatedAt = tombstone.deletedAt,
                        isDeleted = true
                    )
                    acc.tombstonesToClear += tombstone
                }

                TombstoneConflictDecision.RESURRECT_REMOTE_AND_CLEAR_TOMBSTONE -> {
                    remote?.toToDo(existingId = 0)?.let {
                        toDoDao.insertToDo(it)
                        acc.downloaded++
                        acc.conflicts++
                    }
                    acc.tombstonesToClear += tombstone
                }
            }
        }

        remoteByDocId.values
            .filter { it.entityType == SyncEntityType.TODO && !it.isDeleted }
            .forEach { remote ->
                val key = "${SyncEntityType.TODO.value}_${remote.syncId}"
                if (!localBySyncId.containsKey(remote.syncId) && !tombstoneByKey.containsKey(key)) {
                    remote.toToDo(existingId = 0)?.let {
                        toDoDao.insertToDo(it)
                        acc.downloaded++
                    }
                }
            }

        return acc
    }

    private suspend fun mergeReminders(
        remoteByDocId: Map<String, CloudSyncDocument>,
        tombstoneByKey: Map<String, SyncTombstone>,
        todoIdToSyncId: Map<Int, String>,
        todoSyncIdToId: Map<String, Int>
    ): MergeAccumulator {
        val acc = MergeAccumulator()
        val locals = reminderDao.getAllRemindersOnce()
        val localBySyncId = locals.associateBy { it.syncId }

        for (local in locals) {
            val remote = remoteByDocId["${SyncEntityType.REMINDER.value}_${local.syncId}"]
            val linkedSyncId = local.linkedToDoId?.let { todoIdToSyncId[it] }

            if (remote == null) {
                acc.docsToUpload += local.toCloudDocument(linkedSyncId)
                if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.syncedIdsToMark += local.syncId
            } else {
                when (SyncConflictResolver.resolveEntityConflict(local.updatedAt, local.syncStatus, remote.updatedAt, remote.isDeleted)) {
                    EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD && !remote.isDeleted && local.updatedAt != remote.updatedAt) acc.conflicts++
                        acc.docsToUpload += local.toCloudDocument(linkedSyncId)
                        acc.syncedIdsToMark += local.syncId
                    }

                    EntityConflictDecision.ACCEPT_REMOTE -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.conflicts++
                        remote.toReminder(existingId = local.id, todoSyncIdToId = todoSyncIdToId)?.let { updated ->
                            reminderDao.updateReminder(updated)
                            onReminderSyncedFromCloud?.invoke(updated, false)
                            acc.downloaded++
                        }
                    }

                    EntityConflictDecision.DELETE_LOCAL -> {
                        onReminderSyncedFromCloud?.invoke(local, true)
                        reminderDao.deleteBySyncId(local.syncId)
                        acc.deleted++
                    }

                    EntityConflictDecision.NO_OP -> Unit
                }
            }
        }

        tombstoneByKey.values.filter { it.entityType == SyncEntityType.REMINDER }.forEach { tombstone ->
            val remote = remoteByDocId["${SyncEntityType.REMINDER.value}_${tombstone.syncId}"]
            when (SyncConflictResolver.resolveTombstoneConflict(tombstone.deletedAt, remote?.updatedAt, remote?.isDeleted == true)) {
                TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE -> {
                    acc.docsToUpload += CloudSyncDocument(
                        entityType = SyncEntityType.REMINDER,
                        syncId = tombstone.syncId,
                        updatedAt = tombstone.deletedAt,
                        isDeleted = true
                    )
                    acc.tombstonesToClear += tombstone
                }

                TombstoneConflictDecision.RESURRECT_REMOTE_AND_CLEAR_TOMBSTONE -> {
                    remote?.toReminder(existingId = 0, todoSyncIdToId = todoSyncIdToId)?.let { restored ->
                        val newId = reminderDao.insertReminder(restored).toInt()
                        onReminderSyncedFromCloud?.invoke(restored.copy(id = newId), false)
                        acc.downloaded++
                        acc.conflicts++
                    }
                    acc.tombstonesToClear += tombstone
                }
            }
        }

        remoteByDocId.values
            .filter { it.entityType == SyncEntityType.REMINDER && !it.isDeleted }
            .forEach { remote ->
                val key = "${SyncEntityType.REMINDER.value}_${remote.syncId}"
                if (!localBySyncId.containsKey(remote.syncId) && !tombstoneByKey.containsKey(key)) {
                    remote.toReminder(existingId = 0, todoSyncIdToId = todoSyncIdToId)?.let { inserted ->
                        val newId = reminderDao.insertReminder(inserted).toInt()
                        onReminderSyncedFromCloud?.invoke(inserted.copy(id = newId), false)
                        acc.downloaded++
                    }
                }
            }

        return acc
    }

    private suspend fun mergeTimeBlocks(
        remoteByDocId: Map<String, CloudSyncDocument>,
        tombstoneByKey: Map<String, SyncTombstone>
    ): MergeAccumulator {
        val acc = MergeAccumulator()
        val locals = timeBlockDao.getAllTimeBlocksOnce()
        val localBySyncId = locals.associateBy { it.syncId }

        for (local in locals) {
            val remote = remoteByDocId["${SyncEntityType.TIME_BLOCK.value}_${local.syncId}"]
            if (remote == null) {
                acc.docsToUpload += local.toCloudDocument()
                if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.syncedIdsToMark += local.syncId
            } else {
                when (SyncConflictResolver.resolveEntityConflict(local.updatedAt, local.syncStatus, remote.updatedAt, remote.isDeleted)) {
                    EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD && !remote.isDeleted && local.updatedAt != remote.updatedAt) acc.conflicts++
                        acc.docsToUpload += local.toCloudDocument()
                        acc.syncedIdsToMark += local.syncId
                    }

                    EntityConflictDecision.ACCEPT_REMOTE -> {
                        if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.conflicts++
                        remote.toTimeBlock(existingId = local.id)?.let {
                            timeBlockDao.updateTimeBlock(it)
                            acc.downloaded++
                        }
                    }

                    EntityConflictDecision.DELETE_LOCAL -> {
                        timeBlockDao.deleteBySyncId(local.syncId)
                        acc.deleted++
                    }

                    EntityConflictDecision.NO_OP -> Unit
                }
            }
        }

        tombstoneByKey.values.filter { it.entityType == SyncEntityType.TIME_BLOCK }.forEach { tombstone ->
            val remote = remoteByDocId["${SyncEntityType.TIME_BLOCK.value}_${tombstone.syncId}"]
            when (SyncConflictResolver.resolveTombstoneConflict(tombstone.deletedAt, remote?.updatedAt, remote?.isDeleted == true)) {
                TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE -> {
                    acc.docsToUpload += CloudSyncDocument(
                        entityType = SyncEntityType.TIME_BLOCK,
                        syncId = tombstone.syncId,
                        updatedAt = tombstone.deletedAt,
                        isDeleted = true
                    )
                    acc.tombstonesToClear += tombstone
                }

                TombstoneConflictDecision.RESURRECT_REMOTE_AND_CLEAR_TOMBSTONE -> {
                    remote?.toTimeBlock(existingId = 0)?.let {
                        timeBlockDao.insertTimeBlock(it)
                        acc.downloaded++
                        acc.conflicts++
                    }
                    acc.tombstonesToClear += tombstone
                }
            }
        }

        remoteByDocId.values
            .filter { it.entityType == SyncEntityType.TIME_BLOCK && !it.isDeleted }
            .forEach { remote ->
                val key = "${SyncEntityType.TIME_BLOCK.value}_${remote.syncId}"
                if (!localBySyncId.containsKey(remote.syncId) && !tombstoneByKey.containsKey(key)) {
                    remote.toTimeBlock(existingId = 0)?.let {
                        timeBlockDao.insertTimeBlock(it)
                        acc.downloaded++
                    }
                }
            }

        return acc
    }

    private suspend fun mergePomodoros(
        remoteByDocId: Map<String, CloudSyncDocument>,
        tombstoneByKey: Map<String, SyncTombstone>
    ): MergeAccumulator {
        val acc = MergeAccumulator()
        val locals = pomodoroDao.getAllRecordsOnce()
        val localBySyncId = locals.associateBy { it.syncId }

        for (local in locals) {
            val remote = remoteByDocId["${SyncEntityType.POMODORO.value}_${local.syncId}"]
            if (remote == null) {
                acc.docsToUpload += local.toCloudDocument()
                if (local.syncStatus == SyncStatus.PENDING_UPLOAD) acc.syncedIdsToMark += local.syncId
            } else if (remote.isDeleted && local.syncStatus == SyncStatus.SYNCED) {
                pomodoroDao.deleteBySyncId(local.syncId)
                acc.deleted++
            }
        }

        remoteByDocId.values
            .filter { it.entityType == SyncEntityType.POMODORO && !it.isDeleted }
            .forEach { remote ->
                val key = "${SyncEntityType.POMODORO.value}_${remote.syncId}"
                if (!localBySyncId.containsKey(remote.syncId) && !tombstoneByKey.containsKey(key)) {
                    remote.toPomodoroRecord(existingId = 0)?.let {
                        pomodoroDao.insertRecord(it)
                        acc.downloaded++
                    }
                }
            }

        return acc
    }

    private data class SettingsMergeOutcome(
        val docToUpload: CloudSyncDocument? = null,
        val downloaded: Boolean = false,
        val conflict: Boolean = false
    )

    private suspend fun mergeSettings(
        remoteByDocId: Map<String, CloudSyncDocument>,
        settings: SyncSettings
    ): SettingsMergeOutcome {
        val localPersonalization = themePreferences.personalizationFlow.first()
        val localNotifications = notificationPreferences.isNotificationsEnabled()
        val localUpdatedAt = settings.settingsUpdatedAt
        val localSyncStatus = if (settings.settingsSyncPending) SyncStatus.PENDING_UPLOAD else SyncStatus.SYNCED

        val remote = remoteByDocId["${SyncEntityType.SETTINGS.value}_global"]
        if (remote == null) {
            if (settings.settingsSyncPending || localUpdatedAt > 0L) {
                val uploadTime = if (localUpdatedAt > 0L) localUpdatedAt else System.currentTimeMillis()
                return SettingsMergeOutcome(
                    docToUpload = CloudSyncDocument(
                        entityType = SyncEntityType.SETTINGS,
                        syncId = "global",
                        updatedAt = uploadTime,
                        isDeleted = false,
                        payload = buildSettingsPayload(localPersonalization, localNotifications)
                    )
                )
            }
            return SettingsMergeOutcome()
        }

        return when (SyncConflictResolver.resolveEntityConflict(localUpdatedAt, localSyncStatus, remote.updatedAt, remote.isDeleted)) {
            EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD -> {
                val conflict = settings.settingsSyncPending && localUpdatedAt != remote.updatedAt
                SettingsMergeOutcome(
                    docToUpload = CloudSyncDocument(
                        entityType = SyncEntityType.SETTINGS,
                        syncId = "global",
                        updatedAt = localUpdatedAt,
                        isDeleted = false,
                        payload = buildSettingsPayload(localPersonalization, localNotifications)
                    ),
                    conflict = conflict
                )
            }

            EntityConflictDecision.ACCEPT_REMOTE -> {
                val remoteTheme = ThemeMode.fromString(remote.payload["themeMode"] as? String)
                val remoteButtonColor = (remote.payload["buttonColorLong"] as? Number)?.toLong()
                val remoteCalendarStyle = CalendarStyle.fromString(remote.payload["calendarStyle"] as? String)
                val remoteCalendarAccent = (remote.payload["calendarAccentColorLong"] as? Number)?.toLong()
                val remoteFontScale = AppFontScale.fromString(remote.payload["fontScale"] as? String)
                val remoteNotif = (remote.payload["notificationsEnabled"] as? Boolean) ?: localNotifications

                themePreferences.applyPersonalization(
                    PersonalizationSettings(
                        themeMode = remoteTheme,
                        buttonColorLong = remoteButtonColor,
                        calendarStyle = remoteCalendarStyle,
                        calendarAccentColorLong = remoteCalendarAccent,
                        fontScale = remoteFontScale
                    )
                )
                notificationPreferences.setNotificationsEnabled(remoteNotif)
                syncPreferences.markSettingsSynced(remote.updatedAt)
                SettingsMergeOutcome(downloaded = true, conflict = settings.settingsSyncPending)
            }

            EntityConflictDecision.DELETE_LOCAL,
            EntityConflictDecision.NO_OP -> SettingsMergeOutcome()
        }
    }

    private fun buildSettingsPayload(
        personalization: PersonalizationSettings,
        notificationsEnabled: Boolean
    ): Map<String, Any?> = mapOf(
        "themeMode" to personalization.themeMode.name,
        "buttonColorLong" to personalization.buttonColorLong,
        "calendarStyle" to personalization.calendarStyle.name,
        "calendarAccentColorLong" to personalization.calendarAccentColorLong,
        "fontScale" to personalization.fontScale.name,
        "notificationsEnabled" to notificationsEnabled
    )

    // === 實體 <-> 雲端文件序列化轉換器 ===

    private fun Category.toCloudDocument() = CloudSyncDocument(
        entityType = SyncEntityType.CATEGORY,
        syncId = syncId,
        updatedAt = updatedAt,
        isDeleted = false,
        payload = mapOf(
            "name" to name,
            "color" to color,
            "createdAt" to createdAt
        )
    )

    private fun CloudSyncDocument.toCategory(existingId: Int): Category? {
        val catName = (payload["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return Category(
            id = existingId,
            name = catName,
            color = (payload["color"] as? Number)?.toLong() ?: 0xFF2196F3L,
            createdAt = (payload["createdAt"] as? Number)?.toLong() ?: updatedAt,
            syncId = syncId,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.SYNCED
        )
    }

    private fun Note.toCloudDocument(categorySyncId: String?) = CloudSyncDocument(
        entityType = SyncEntityType.NOTE,
        syncId = syncId,
        updatedAt = updatedAt,
        isDeleted = false,
        payload = mapOf(
            "title" to title,
            "content" to content,
            "isPinned" to isPinned,
            "categorySyncId" to categorySyncId,
            "color" to color,
            "createdAt" to createdAt
        )
    )

    private fun CloudSyncDocument.toNote(
        existingId: Int,
        catSyncIdToId: Map<String, Int>
    ): Note? {
        val catSyncId = payload["categorySyncId"] as? String
        return Note(
            id = existingId,
            title = (payload["title"] as? String) ?: "",
            content = (payload["content"] as? String) ?: "",
            isPinned = (payload["isPinned"] as? Boolean) ?: false,
            categoryId = catSyncId?.let { catSyncIdToId[it] },
            color = (payload["color"] as? Number)?.toLong() ?: 0xFF2196F3L,
            createdAt = (payload["createdAt"] as? Number)?.toLong() ?: updatedAt,
            updatedAt = updatedAt,
            syncId = syncId,
            syncStatus = SyncStatus.SYNCED
        )
    }

    private fun ToDo.toCloudDocument() = CloudSyncDocument(
        entityType = SyncEntityType.TODO,
        syncId = syncId,
        updatedAt = updatedAt,
        isDeleted = false,
        payload = mapOf(
            "title" to title,
            "description" to description,
            "isCompleted" to isCompleted,
            "dueDate" to dueDate,
            "tag" to tag,
            "color" to color,
            "createdAt" to createdAt,
            "priorityLevel" to priority.level
        )
    )

    private fun CloudSyncDocument.toToDo(existingId: Int): ToDo? {
        val todoTitle = (payload["title"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        val priorityLevel = (payload["priorityLevel"] as? Number)?.toInt() ?: Priority.MEDIUM.level
        return ToDo(
            id = existingId,
            title = todoTitle,
            description = (payload["description"] as? String) ?: "",
            isCompleted = (payload["isCompleted"] as? Boolean) ?: false,
            dueDate = (payload["dueDate"] as? Number)?.toLong(),
            tag = (payload["tag"] as? String) ?: "日常",
            color = (payload["color"] as? Number)?.toLong() ?: 0xFF2196F3L,
            createdAt = (payload["createdAt"] as? Number)?.toLong() ?: updatedAt,
            priority = Priority.fromLevel(priorityLevel),
            syncId = syncId,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.SYNCED
        )
    }

    private fun Reminder.toCloudDocument(linkedToDoSyncId: String?) = CloudSyncDocument(
        entityType = SyncEntityType.REMINDER,
        syncId = syncId,
        updatedAt = updatedAt,
        isDeleted = false,
        payload = mapOf(
            "title" to title,
            "message" to message,
            "triggerAt" to triggerAt,
            "tag" to tag,
            "color" to color,
            "isFired" to isFired,
            "createdAt" to createdAt,
            "linkedToDoSyncId" to linkedToDoSyncId
        )
    )

    private fun CloudSyncDocument.toReminder(
        existingId: Int,
        todoSyncIdToId: Map<String, Int>
    ): Reminder? {
        val triggerAt = (payload["triggerAt"] as? Number)?.toLong() ?: return null
        val linkedSyncId = payload["linkedToDoSyncId"] as? String
        return Reminder(
            id = existingId,
            title = (payload["title"] as? String) ?: "",
            message = (payload["message"] as? String) ?: "",
            triggerAt = triggerAt,
            tag = (payload["tag"] as? String) ?: "提醒",
            color = (payload["color"] as? Number)?.toLong() ?: 0xFFFF9800L,
            isFired = (payload["isFired"] as? Boolean) ?: false,
            createdAt = (payload["createdAt"] as? Number)?.toLong() ?: updatedAt,
            linkedToDoId = linkedSyncId?.let { todoSyncIdToId[it] },
            syncId = syncId,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.SYNCED
        )
    }

    private fun TimeBlock.toCloudDocument() = CloudSyncDocument(
        entityType = SyncEntityType.TIME_BLOCK,
        syncId = syncId,
        updatedAt = updatedAt,
        isDeleted = false,
        payload = mapOf(
            "date" to date,
            "startTime" to startTime,
            "endTime" to endTime,
            "title" to title,
            "description" to description,
            "isCompleted" to isCompleted,
            "tag" to tag,
            "color" to color
        )
    )

    private fun CloudSyncDocument.toTimeBlock(existingId: Int): TimeBlock? {
        val blockDate = (payload["date"] as? Number)?.toLong() ?: return null
        val blockTitle = (payload["title"] as? String)?.takeIf { it.isNotBlank() } ?: return null
        return TimeBlock(
            id = existingId,
            date = blockDate,
            startTime = (payload["startTime"] as? String) ?: "09:00",
            endTime = (payload["endTime"] as? String) ?: "10:00",
            title = blockTitle,
            description = (payload["description"] as? String) ?: "",
            isCompleted = (payload["isCompleted"] as? Boolean) ?: false,
            tag = (payload["tag"] as? String) ?: "工作",
            color = (payload["color"] as? Number)?.toLong() ?: 0xFF4CAF50L,
            syncId = syncId,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.SYNCED
        )
    }

    private fun PomodoroRecord.toCloudDocument() = CloudSyncDocument(
        entityType = SyncEntityType.POMODORO,
        syncId = syncId,
        updatedAt = updatedAt,
        isDeleted = false,
        payload = mapOf(
            "timestamp" to timestamp,
            "durationMinutes" to durationMinutes,
            "taskTitle" to taskTitle,
            "date" to date
        )
    )

    private fun CloudSyncDocument.toPomodoroRecord(existingId: Int): PomodoroRecord? {
        val recordDate = (payload["date"] as? Number)?.toLong() ?: return null
        return PomodoroRecord(
            id = existingId,
            timestamp = (payload["timestamp"] as? Number)?.toLong() ?: updatedAt,
            durationMinutes = (payload["durationMinutes"] as? Number)?.toInt() ?: 25,
            taskTitle = (payload["taskTitle"] as? String) ?: "專注時段",
            date = recordDate,
            syncId = syncId,
            updatedAt = updatedAt,
            syncStatus = SyncStatus.SYNCED
        )
    }
}
