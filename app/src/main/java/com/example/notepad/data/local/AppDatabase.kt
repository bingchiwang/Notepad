package com.example.notepad.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notepad.data.local.dao.ReminderDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.ToDo

@Database(entities = [ToDo::class, Reminder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun toDoDao(): ToDoDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notepad_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
