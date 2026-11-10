package com.example.notepad.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notepad.data.local.entity.SyncEntityType
import com.example.notepad.data.local.entity.SyncTombstone
import kotlinx.coroutines.flow.Flow

/**
 * 離線刪除墓碑紀錄資料存取介面
 */
@Dao
interface SyncTombstoneDao {
    @Query("SELECT COUNT(*) FROM sync_tombstones")
    fun observeTombstoneCount(): Flow<Int>

    @Query("SELECT * FROM sync_tombstones")
    suspend fun getAllTombstonesOnce(): List<SyncTombstone>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTombstone(tombstone: SyncTombstone)

    @Query("DELETE FROM sync_tombstones WHERE entityType = :entityType AND syncId = :syncId")
    suspend fun deleteTombstone(entityType: SyncEntityType, syncId: String)
}
