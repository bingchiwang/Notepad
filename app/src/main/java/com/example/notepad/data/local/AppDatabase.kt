package com.example.notepad.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.notepad.data.local.dao.CategoryDao
import com.example.notepad.data.local.dao.NoteDao
import com.example.notepad.data.local.dao.PomodoroDao
import com.example.notepad.data.local.dao.ReminderDao
import com.example.notepad.data.local.dao.TimeBlockDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.local.entity.PomodoroRecord
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo

/**
 * 應用程式核心 Room 資料庫（v4：新增 Note、Category 資料表與 ToDo 優先級）
 */
@Database(
    entities = [
        ToDo::class,
        Reminder::class,
        TimeBlock::class,
        PomodoroRecord::class,
        Note::class,
        Category::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun toDoDao(): ToDoDao
    abstract fun reminderDao(): ReminderDao
    abstract fun timeBlockDao(): TimeBlockDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v3 升級至 v4 之結構遷移：保留既有待辦、提醒、時間規劃與番茄鐘紀錄，
         * 並建立記事 (notes)、分類 (categories) 表與待辦優先級欄位。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 為待辦事項新增 priority 欄位（預設 2 = MEDIUM）
                db.execSQL("ALTER TABLE todos ADD COLUMN priority INTEGER NOT NULL DEFAULT 2")
                // 為提醒事項新增關聯待辦欄位
                db.execSQL("ALTER TABLE reminders ADD COLUMN linkedToDoId INTEGER")

                // 建立分類資料表及名稱唯一索引（規範 4.3）
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_name` ON `categories` (`name`)"
                )

                // 建立記事資料表及分類外鍵關聯（刪除分類時設為 NULL）
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `categoryId` INTEGER,
                        `color` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_notes_categoryId` ON `notes` (`categoryId`)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notepad_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
