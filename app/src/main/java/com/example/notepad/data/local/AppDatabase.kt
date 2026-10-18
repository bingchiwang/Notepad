package com.example.notepad.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.notepad.data.local.dao.PomodoroDao
import com.example.notepad.data.local.dao.ReminderDao
import com.example.notepad.data.local.dao.TimeBlockDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.PomodoroRecord
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo

@Database(
    entities = [ToDo::class, Reminder::class, TimeBlock::class, PomodoroRecord::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun toDoDao(): ToDoDao
    abstract fun reminderDao(): ReminderDao
    abstract fun timeBlockDao(): TimeBlockDao
    abstract fun pomodoroDao(): PomodoroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notepad_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
