package com.example.notepad.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.ToDo
import kotlinx.coroutines.flow.Flow

@Dao
interface ToDoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, priority DESC, dueDate ASC, createdAt DESC")
    fun getAllToDos(): Flow<List<ToDo>>

    /**
     * 僅取得未完成的待辦事項（規範 4.2：已完成任務不得繼續顯示為未完成）
     */
    @Query("SELECT * FROM todos WHERE isCompleted = 0 ORDER BY priority DESC, dueDate ASC, createdAt DESC")
    fun getActiveToDos(): Flow<List<ToDo>>

    /**
     * 僅取得已完成的待辦事項
     */
    @Query("SELECT * FROM todos WHERE isCompleted = 1 ORDER BY createdAt DESC")
    fun getCompletedToDos(): Flow<List<ToDo>>

    /**
     * 依優先級篩選待辦事項（規範 4.2：高、中、低）
     */
    @Query("SELECT * FROM todos WHERE priority = :priority ORDER BY isCompleted ASC, dueDate ASC, createdAt DESC")
    fun getToDosByPriority(priority: Priority): Flow<List<ToDo>>

    @Query("SELECT * FROM todos WHERE dueDate = :date ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getToDosByDate(date: Long): Flow<List<ToDo>>

    @Query("SELECT * FROM todos WHERE dueDate >= :startDate AND dueDate <= :endDate ORDER BY dueDate ASC, createdAt DESC")
    fun getToDosByDateRange(startDate: Long, endDate: Long): Flow<List<ToDo>>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    fun getToDoById(id: Int): Flow<ToDo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToDo(toDo: ToDo)

    @Update
    suspend fun updateToDo(toDo: ToDo)

    @Delete
    suspend fun deleteToDo(toDo: ToDo)

    @Query("SELECT * FROM todos")
    suspend fun getAllToDosOnce(): List<ToDo>

    @Query("SELECT * FROM todos WHERE syncId = :syncId LIMIT 1")
    suspend fun getToDoBySyncIdOnce(syncId: String): ToDo?

    @Query("SELECT COUNT(*) FROM todos WHERE syncStatus = 1")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE todos SET syncStatus = 0 WHERE syncId = :syncId")
    suspend fun markAsSynced(syncId: String)

    @Query("DELETE FROM todos WHERE syncId = :syncId")
    suspend fun deleteBySyncId(syncId: String)
}
