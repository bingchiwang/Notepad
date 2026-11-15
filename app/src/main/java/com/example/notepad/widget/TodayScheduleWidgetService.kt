package com.example.notepad.widget

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.notepad.R
import com.example.notepad.data.local.AppDatabase
import kotlinx.coroutines.runBlocking

/**
 * 今日行程桌面小工具列表遠端視圖服務（供 Android 8.0–11 裝置使用）
 */
class TodayScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayScheduleRemoteViewsFactory(applicationContext)
    }
}

private class TodayScheduleRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<TodayScheduleRow> = emptyList()

    override fun onCreate() {
        // 初次建立後由系統觸發 onDataSetChanged 載入資料
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            val database = AppDatabase.getDatabase(context)
            val resolved = runBlocking {
                val todos = database.toDoDao().getAllToDosOnce()
                val timeBlocks = database.timeBlockDao().getAllTimeBlocksOnce()
                val reminders = database.reminderDao().getAllRemindersOnce()
                TodayScheduleWidgetLogic.resolveTodaySchedule(
                    todos = todos,
                    timeBlocks = timeBlocks,
                    reminders = reminders
                )
            }
            rows = resolved.rows
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in rows.indices) return null
        val row = rows[position]
        return TodayScheduleWidgetRowBinder.bindRow(context, row).apply {
            when (row) {
                is TodayScheduleRow.DueTodayToDoRow -> {
                    setOnClickFillInIntent(
                        R.id.widget_todo_checkbox_container,
                        ToDoWidgetProvider.buildCompleteFillInIntent(row.item.id)
                    )
                    setOnClickFillInIntent(
                        R.id.widget_todo_content_area,
                        ToDoWidgetProvider.buildOpenDetailFillInIntent(row.item.id)
                    )
                }
                is TodayScheduleRow.UpcomingReminderRow -> {
                    setOnClickFillInIntent(
                        R.id.widget_reminder_item_root,
                        TodayScheduleWidgetProvider.buildOpenRemindersFillInIntent()
                    )
                }
                is TodayScheduleRow.SectionHeader,
                is TodayScheduleRow.EmptyNoticeRow -> Unit
            }
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = TodayScheduleWidgetRowBinder.VIEW_TYPE_COUNT

    override fun getItemId(position: Int): Long {
        return rows.getOrNull(position)?.stableId ?: position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
