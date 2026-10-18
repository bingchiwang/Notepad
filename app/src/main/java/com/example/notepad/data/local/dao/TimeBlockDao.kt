package com.example.notepad.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.notepad.data.local.entity.TimeBlock
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeBlockDao {
    @Query("SELECT * FROM time_blocks WHERE date = :date ORDER BY startTime ASC")
    fun getTimeBlocksByDate(date: Long): Flow<List<TimeBlock>>

    @Query("SELECT * FROM time_blocks ORDER BY date DESC, startTime ASC")
    fun getAllTimeBlocks(): Flow<List<TimeBlock>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeBlock(timeBlock: TimeBlock)

    @Update
    suspend fun updateTimeBlock(timeBlock: TimeBlock)

    @Delete
    suspend fun deleteTimeBlock(timeBlock: TimeBlock)
}
