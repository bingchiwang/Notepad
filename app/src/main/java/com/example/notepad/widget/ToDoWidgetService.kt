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
 * 待辦事項桌面小工具列表遠端視圖服務（供 Android 8.0–11 裝置使用）
 * 負責從本機 Room 資料庫即時載入尚未完成的待辦事項，並綁定直接勾選完成與查看詳情之 Fill-In Intent。
 */
class ToDoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ToDoRemoteViewsFactory(applicationContext)
    }
}

private class ToDoRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WidgetToDoItem> = emptyList()

    override fun onCreate() {
        // 初次建立後由系統觸發 onDataSetChanged 載入最新資料
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            val database = AppDatabase.getDatabase(context)
            val resolved = runBlocking {
                val todos = database.toDoDao().getAllToDosOnce()
                ToDoWidgetLogic.resolveActiveTasks(todos = todos)
            }
            items = resolved.items
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position !in items.indices) return null
        val item = items[position]
        return ToDoWidgetRowBinder.bindRow(context, item).apply {
            setOnClickFillInIntent(
                R.id.widget_todo_checkbox_container,
                ToDoWidgetProvider.buildCompleteFillInIntent(item.id)
            )
            setOnClickFillInIntent(
                R.id.widget_todo_content_area,
                ToDoWidgetProvider.buildOpenDetailFillInIntent(item.id)
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return items.getOrNull(position)?.id?.toLong() ?: position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
