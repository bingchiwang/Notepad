package com.example.notepad.data.sync

import com.example.notepad.data.local.entity.SyncEntityType
import com.example.notepad.data.local.entity.SyncStatus
import com.example.notepad.data.preferences.SyncSettings

/**
 * 雲端同步標準文件封裝（對應 Firebase Firestore 文件結構）
 */
data class CloudSyncDocument(
    val entityType: SyncEntityType,
    val syncId: String,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val payload: Map<String, Any?> = emptyMap()
) {
    /**
     * Firestore 文件鍵值：ENTITYTYPE_SYNCID，確保所有實體類型共用子集合亦不會主鍵衝突
     */
    val documentId: String
        get() = "${entityType.value}_$syncId"
}

/**
 * 本機與雲端資料衝突處理結果（資料同步規則 6）
 */
enum class EntityConflictDecision {
    /** 保留本機最新版本並上傳覆蓋雲端 */
    KEEP_LOCAL_AND_UPLOAD,

    /** 採用雲端較新版本並更新至本機 */
    ACCEPT_REMOTE,

    /** 雲端已執行較新之刪除，同步刪除本機資料 */
    DELETE_LOCAL,

    /** 兩端狀態與時間一致，無需動作 */
    NO_OP
}

/**
 * 本機刪除墓碑與雲端資料的衝突處理結果
 */
enum class TombstoneConflictDecision {
    /** 刪除時間晚於或等於雲端修改時間：將刪除標記推送至雲端並清除本機墓碑 */
    PUSH_DELETE_AND_CLEAR_TOMBSTONE,

    /** 雲端於本機刪除之後有更新編輯：依最新時間優先策略還原雲端版本至本機 */
    RESURRECT_REMOTE_AND_CLEAR_TOMBSTONE
}

/**
 * 雲端資料衝突智慧合併與最新修改優先解析器（資料同步規則 6）
 */
object SyncConflictResolver {

    /**
     * 依據更新時間與本地待同步狀態解析實體衝突
     *
     * 規則：
     * - 若本機狀態為 [SyncStatus.SYNCED]：雲端較新 (`remoteUpdatedAt > localUpdatedAt`) 或雲端標記刪除時採用雲端。
     * - 若本機狀態為 [SyncStatus.PENDING_UPLOAD]（離線雙邊修改衝突）：比較 `updatedAt`，最新修改時間勝出。
     */
    fun resolveEntityConflict(
        localUpdatedAt: Long,
        localSyncStatus: SyncStatus,
        remoteUpdatedAt: Long,
        remoteIsDeleted: Boolean
    ): EntityConflictDecision {
        if (remoteIsDeleted) {
            return if (localSyncStatus == SyncStatus.PENDING_UPLOAD && localUpdatedAt > remoteUpdatedAt) {
                // 本機在遠端刪除後仍有更新編輯 -> 保留本機修改並上傳復原
                EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD
            } else {
                EntityConflictDecision.DELETE_LOCAL
            }
        }

        return when (localSyncStatus) {
            SyncStatus.SYNCED -> {
                if (remoteUpdatedAt > localUpdatedAt) {
                    EntityConflictDecision.ACCEPT_REMOTE
                } else {
                    EntityConflictDecision.NO_OP
                }
            }

            SyncStatus.PENDING_UPLOAD -> {
                if (remoteUpdatedAt > localUpdatedAt) {
                    EntityConflictDecision.ACCEPT_REMOTE
                } else {
                    EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD
                }
            }
        }
    }

    /**
     * 依據刪除時間與遠端更新時間解析墓碑衝突
     */
    fun resolveTombstoneConflict(
        localDeletedAt: Long,
        remoteUpdatedAt: Long?,
        remoteIsDeleted: Boolean
    ): TombstoneConflictDecision {
        if (remoteUpdatedAt == null || remoteIsDeleted) {
            return TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE
        }
        return if (remoteUpdatedAt > localDeletedAt) {
            TombstoneConflictDecision.RESURRECT_REMOTE_AND_CLEAR_TOMBSTONE
        } else {
            TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE
        }
    }
}

/**
 * 單次同步執行結果摘要
 */
data class SyncRunResult(
    val succeeded: Boolean,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val deletedCount: Int = 0,
    val conflictResolvedCount: Int = 0,
    val syncedAt: Long? = null,
    val message: String
)

/**
 * 提供給 UI 呈現的響應式同步狀態模型
 */
data class CloudSyncUiState(
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val pendingCount: Int = 0,
    val isFirebaseConfigured: Boolean = false,
    val syncSettings: SyncSettings = SyncSettings(),
    val lastRunSummary: String = "尚未同步"
)
