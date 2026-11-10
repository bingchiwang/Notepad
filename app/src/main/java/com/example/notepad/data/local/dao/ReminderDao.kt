package com.example.notepad.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.notepad.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isFired ASC, triggerAt ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE triggerAt >= :startMillis AND triggerAt <= :endMillis ORDER BY triggerAt ASC")
    fun getRemindersByDate(startMillis: Long, endMillis: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun getReminderById(id: Int): Flow<Reminder?>

    /**
     * 取得特定任務尚未觸發的關聯提醒，用於任務完成時取消對應通知排程（規範 4.4）
     */
    @Query("SELECT * FROM reminders WHERE linkedToDoId = :toDoId AND isFired = 0")
    suspend fun getPendingRemindersByToDoId(toDoId: Int): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isFired = 1 WHERE id = :id")
    suspend fun markAsFired(id: Int)
}
