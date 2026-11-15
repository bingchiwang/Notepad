package com.example.notepad.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.example.notepad.data.local.AppDatabase
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.reminder.ReminderScheduler
import kotlinx.coroutines.flow.first

/**
 * 桌面小工具共用待辦操作協調器（落實 DRY：同時服務待辦小工具與今日行程小工具）
 */
object WidgetTaskActions {

    /**
     * 從桌面小工具直接將待辦任務標記為完成，同步取消關聯尚未觸發之提醒鬧鐘，
     * 並立即刷新「待辦事項小工具」與「今日行程小工具」。
     */
    suspend fun completeToDo(context: Context, todoId: Int) {
        val appContext = context.applicationContext
        val database = AppDatabase.getDatabase(appContext)
        val toDo = database.toDoDao().getToDoById(todoId).first() ?: return
        if (toDo.isCompleted) return

        val taskRepository = TaskRepository(
            toDoDao = database.toDoDao(),
            reminderDao = database.reminderDao(),
            timeBlockDao = database.timeBlockDao(),
            pomodoroDao = database.pomodoroDao(),
            syncTombstoneDao = database.syncTombstoneDao()
        )
        ReminderScheduler.updateToDoCompletionAndSyncReminders(
            context = appContext,
            taskRepository = taskRepository,
            toDo = toDo,
            isCompleted = true
        )

        // 同步即時刷新所有受影響的小工具（await 確保 Broadcast goAsync() 結束前完成刷新）
        ToDoWidgetUpdater.refreshAllBlocking(appContext)
        TodayScheduleWidgetUpdater.refreshAllBlocking(appContext)
        CalendarWidgetUpdater.refreshAllBlocking(appContext)

        Handler(Looper.getMainLooper()).post {
            runCatching {
                Toast.makeText(appContext, "已完成：「${toDo.title}」", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
