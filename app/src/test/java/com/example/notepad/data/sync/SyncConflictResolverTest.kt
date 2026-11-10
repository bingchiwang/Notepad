package com.example.notepad.data.sync

import com.example.notepad.data.local.entity.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 雲端資料衝突解決策略單元測試（驗證規則 6：智慧合併與最新修改優先）
 */
class SyncConflictResolverTest {

    @Test
    fun `resolveEntityConflict synced local accepts newer remote`() {
        val decision = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 1000L,
            localSyncStatus = SyncStatus.SYNCED,
            remoteUpdatedAt = 2000L,
            remoteIsDeleted = false
        )
        assertEquals(EntityConflictDecision.ACCEPT_REMOTE, decision)
    }

    @Test
    fun `resolveEntityConflict synced local does nothing when remote is equal or older`() {
        val equalDecision = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 2000L,
            localSyncStatus = SyncStatus.SYNCED,
            remoteUpdatedAt = 2000L,
            remoteIsDeleted = false
        )
        val olderDecision = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 2000L,
            localSyncStatus = SyncStatus.SYNCED,
            remoteUpdatedAt = 1500L,
            remoteIsDeleted = false
        )
        assertEquals(EntityConflictDecision.NO_OP, equalDecision)
        assertEquals(EntityConflictDecision.NO_OP, olderDecision)
    }

    @Test
    fun `resolveEntityConflict synced local deletes when remote is marked deleted`() {
        val decision = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 5000L,
            localSyncStatus = SyncStatus.SYNCED,
            remoteUpdatedAt = 4000L,
            remoteIsDeleted = true
        )
        assertEquals(EntityConflictDecision.DELETE_LOCAL, decision)
    }

    @Test
    fun `resolveEntityConflict pending local keeps local when newer or equal`() {
        val newerLocal = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 3000L,
            localSyncStatus = SyncStatus.PENDING_UPLOAD,
            remoteUpdatedAt = 2000L,
            remoteIsDeleted = false
        )
        val equalTime = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 3000L,
            localSyncStatus = SyncStatus.PENDING_UPLOAD,
            remoteUpdatedAt = 3000L,
            remoteIsDeleted = false
        )
        assertEquals(EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD, newerLocal)
        assertEquals(EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD, equalTime)
    }

    @Test
    fun `resolveEntityConflict pending local accepts remote when remote is strictly newer`() {
        val decision = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 2000L,
            localSyncStatus = SyncStatus.PENDING_UPLOAD,
            remoteUpdatedAt = 2500L,
            remoteIsDeleted = false
        )
        assertEquals(EntityConflictDecision.ACCEPT_REMOTE, decision)
    }

    @Test
    fun `resolveEntityConflict pending local edited after remote deletion keeps local`() {
        val decision = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 5000L,
            localSyncStatus = SyncStatus.PENDING_UPLOAD,
            remoteUpdatedAt = 4000L,
            remoteIsDeleted = true
        )
        assertEquals(EntityConflictDecision.KEEP_LOCAL_AND_UPLOAD, decision)
    }

    @Test
    fun `resolveEntityConflict pending local older than remote deletion deletes local`() {
        val decision = SyncConflictResolver.resolveEntityConflict(
            localUpdatedAt = 3000L,
            localSyncStatus = SyncStatus.PENDING_UPLOAD,
            remoteUpdatedAt = 4000L,
            remoteIsDeleted = true
        )
        assertEquals(EntityConflictDecision.DELETE_LOCAL, decision)
    }

    @Test
    fun `resolveTombstoneConflict resurrects remote only if edited strictly after deletion`() {
        assertEquals(
            TombstoneConflictDecision.RESURRECT_REMOTE_AND_CLEAR_TOMBSTONE,
            SyncConflictResolver.resolveTombstoneConflict(
                localDeletedAt = 2000L,
                remoteUpdatedAt = 3000L,
                remoteIsDeleted = false
            )
        )
        assertEquals(
            TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE,
            SyncConflictResolver.resolveTombstoneConflict(
                localDeletedAt = 3000L,
                remoteUpdatedAt = 2000L,
                remoteIsDeleted = false
            )
        )
        assertEquals(
            TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE,
            SyncConflictResolver.resolveTombstoneConflict(
                localDeletedAt = 3000L,
                remoteUpdatedAt = null,
                remoteIsDeleted = false
            )
        )
        assertEquals(
            TombstoneConflictDecision.PUSH_DELETE_AND_CLEAR_TOMBSTONE,
            SyncConflictResolver.resolveTombstoneConflict(
                localDeletedAt = 3000L,
                remoteUpdatedAt = 5000L,
                remoteIsDeleted = true
            )
        )
    }
}
