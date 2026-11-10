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
import com.example.notepad.data.local.dao.SyncTombstoneDao
import com.example.notepad.data.local.dao.TimeBlockDao
import com.example.notepad.data.local.dao.ToDoDao
import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.local.entity.PomodoroRecord
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.SyncTombstone
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo

/**
 * 應用程式核心 Room 資料庫（v5：新增雲端同步欄位 syncId / syncStatus / updatedAt 與離線刪除墓碑表）
 */
@Database(
    entities = [
        ToDo::class,
        Reminder::class,
        TimeBlock::class,
        PomodoroRecord::class,
        Note::class,
        Category::class,
        SyncTombstone::class
    ],
    version = 5,
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
    abstract fun syncTombstoneDao(): SyncTombstoneDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v3 升級至 v4 之結構遷移：保留既有待辦、提醒、時間規劃與番茄鐘紀錄，
         * 並建立記事 (notes)、分類 (categories) 表與待辦優先級欄位。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN priority INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE reminders ADD COLUMN linkedToDoId INTEGER")

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

        /**
         * v4 升級至 v5 之結構遷移：為既有全部資料實體補齊跨裝置同步識別碼、更新時間與待同步狀態，
         * 並建立離線刪除墓碑資料表 (`sync_tombstones`)。
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // todos
                db.execSQL("ALTER TABLE `todos` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `todos` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `todos` ADD COLUMN `syncStatus` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE `todos` SET `syncId` = 'v4-todo-' || `id`, `updatedAt` = `createdAt` WHERE `syncId` = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_todos_syncId` ON `todos` (`syncId`)")

                // reminders
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `reminders` ADD COLUMN `syncStatus` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE `reminders` SET `syncId` = 'v4-reminder-' || `id`, `updatedAt` = `createdAt` WHERE `syncId` = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminders_syncId` ON `reminders` (`syncId`)")

                // time_blocks
                db.execSQL("ALTER TABLE `time_blocks` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `time_blocks` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `time_blocks` ADD COLUMN `syncStatus` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE `time_blocks` SET `syncId` = 'v4-timeblock-' || `id`, `updatedAt` = `date` WHERE `syncId` = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_time_blocks_syncId` ON `time_blocks` (`syncId`)")

                // pomodoro_records
                db.execSQL("ALTER TABLE `pomodoro_records` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `pomodoro_records` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pomodoro_records` ADD COLUMN `syncStatus` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE `pomodoro_records` SET `syncId` = 'v4-pomodoro-' || `id`, `updatedAt` = `timestamp` WHERE `syncId` = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pomodoro_records_syncId` ON `pomodoro_records` (`syncId`)")

                // categories
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `categories` ADD COLUMN `syncStatus` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE `categories` SET `syncId` = 'v4-category-' || `id`, `updatedAt` = `createdAt` WHERE `syncId` = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_syncId` ON `categories` (`syncId`)")

                // notes (已具備 updatedAt)
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `syncId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `syncStatus` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE `notes` SET `syncId` = 'v4-note-' || `id` WHERE `syncId` = ''")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_notes_syncId` ON `notes` (`syncId`)")

                // sync_tombstones
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_tombstones` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `syncId` TEXT NOT NULL,
                        `deletedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_tombstones_entityType_syncId` ON `sync_tombstones` (`entityType`, `syncId`)"
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
