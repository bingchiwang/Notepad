package com.example.notepad.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notepad.data.local.entity.PomodoroRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_records WHERE date = :date ORDER BY timestamp DESC")
    fun getRecordsByDate(date: Long): Flow<List<PomodoroRecord>>

    @Query("SELECT COUNT(*) FROM pomodoro_records WHERE date = :date")
    fun getTodayCount(date: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PomodoroRecord)

    @Query("SELECT * FROM pomodoro_records")
    suspend fun getAllRecordsOnce(): List<PomodoroRecord>

    @Query("SELECT COUNT(*) FROM pomodoro_records WHERE syncStatus = 1")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE pomodoro_records SET syncStatus = 0 WHERE syncId = :syncId")
    suspend fun markAsSynced(syncId: String)

    @Query("DELETE FROM pomodoro_records WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
