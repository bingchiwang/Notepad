package com.example.notepad.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.notepad.data.local.entity.ToDo
import kotlinx.coroutines.flow.Flow

@Dao
interface ToDoDao {
    @Query("SELECT * FROM todos ORDER BY isCompleted ASC, dueDate ASC, createdAt DESC")
    fun getAllToDos(): Flow<List<ToDo>>

    @Query("SELECT * FROM todos WHERE dueDate = :date ORDER BY isCompleted ASC, createdAt DESC")
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
}
