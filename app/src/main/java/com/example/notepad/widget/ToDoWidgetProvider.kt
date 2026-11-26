package com.example.notepad.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.notepad.R
import com.example.notepad.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 待辦事項桌面小工具提供者（AppWidgetProvider）
 *
 * 支援功能：
 * - 僅顯示尚未完成的待辦任務，並標示高／中／低優先級與截止日期
 * - 直接點擊核取方塊將任務標記完成，並同步取消尚未觸發的關聯提醒排程
 * - 點擊任務標題列可進入該任務詳情頁；點擊頂部「新增」可快速新增待辦事項
 */
class ToDoWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_COMPLETE_TODO) {
            val todoId = intent.getIntExtra(EXTRA_TODO_ID, QuickNoteWidgetContract.INVALID_ID)
            if (todoId <= 0) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    WidgetTaskActions.completeToDo(context.applicationContext, todoId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TODO = "com.example.notepad.ACTION_WIDGET_COMPLETE_TODO"
        const val EXTRA_TODO_ID = "com.example.notepad.EXTRA_WIDGET_TODO_TARGET_ID"

        /**
         * 從本機資料庫載入尚未完成的待辦任務，並更新所有指定桌面小工具實例
         */
        suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            val database = AppDatabase.getDatabase(context)
            val todos = database.toDoDao().getAllToDosOnce()
            val resolved = ToDoWidgetLogic.resolveActiveTasks(todos = todos)

            appWidgetIds.forEach { appWidgetId ->
                val views = buildWidgetRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    resolved = resolved
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun buildWidgetRemoteViews(
            context: Context,
            appWidgetId: Int,
            resolved: ResolvedToDoWidgetState
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_todo)
            views.setTextViewText(R.id.widget_todo_count_chip, "${resolved.activeCount} 項待辦")

            // 點擊「+ 新增」開啟待辦分頁並自動彈出新增任務對話框
            val addIntent = QuickNoteWidgetContract.buildOpenToDoTabIntent(
                context = context,
                openAddDialog = true
            ).apply {
                data = Uri.parse("notepad://widget/todo/add/$appWidgetId")
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                40_000 + appWidgetId,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_todo_add_button, addPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_todo_empty_view, addPendingIntent)

            // 點擊頂部待辦數量膠囊 -> 開啟待辦分頁主畫面
            val openListIntent = QuickNoteWidgetContract.buildOpenToDoTabIntent(
                context = context,
                openAddDialog = false
            ).apply {
                data = Uri.parse("notepad://widget/todo/list/$appWidgetId")
            }
            val openListPendingIntent = PendingIntent.getActivity(
                context,
                50_000 + appWidgetId,
                openListIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_todo_count_chip, openListPendingIntent)

            // Android 12+（minSdk = 31）使用 RemoteCollectionItems 直接綁定廣播勾選與詳情頁導航
            if (resolved.items.isEmpty()) {
                views.setViewVisibility(R.id.widget_todo_list, View.GONE)
                views.setViewVisibility(R.id.widget_todo_empty_view, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_todo_list, View.VISIBLE)
                views.setViewVisibility(R.id.widget_todo_empty_view, View.GONE)

                val collectionBuilder = RemoteViews.RemoteCollectionItems.Builder()
                resolved.items.forEach { item ->
                    val rowViews = ToDoWidgetRowBinder.bindRow(context, item).apply {
                        val completeIntent = Intent(context, ToDoWidgetProvider::class.java).apply {
                            action = ACTION_COMPLETE_TODO
                            data = Uri.parse("notepad://widget/todo/complete/${item.id}")
                            putExtra(EXTRA_TODO_ID, item.id)
                        }
                        val completePendingIntent = PendingIntent.getBroadcast(
                            context,
                            60_000 + item.id,
                            completeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_todo_checkbox_container, completePendingIntent)

                        val detailIntent = QuickNoteWidgetContract.buildOpenToDoDetailIntent(
                            context = context,
                            taskId = item.id
                        ).apply {
                            data = Uri.parse("notepad://widget/todo/detail/${item.id}")
                        }
                        val detailPendingIntent = PendingIntent.getActivity(
                            context,
                            70_000 + item.id,
                            detailIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_todo_content_area, detailPendingIntent)
                    }
                    collectionBuilder.addItem(item.id.toLong(), rowViews)
                }
                collectionBuilder.setHasStableIds(true)
                collectionBuilder.setViewTypeCount(1)
                views.setRemoteAdapter(R.id.widget_todo_list, collectionBuilder.build())
            }

            return views
        }
    }
}

/**
 * 待辦事項桌面小工具資料即時重新整理協調器
 * 當本機待辦異動、任務完成或雲端同步取得異動後呼叫，確保所有桌面待辦小工具保持最新狀態。
 */
object ToDoWidgetUpdater {
    private val updateScope = CoroutineScope(Dispatchers.IO)

    suspend fun refreshAllBlocking(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return
        val componentName = ComponentName(appContext, ToDoWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        ToDoWidgetProvider.updateWidgetsInternal(
            context = appContext,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds
        )
    }

    fun notifyDataChanged(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return
        val componentName = ComponentName(appContext, ToDoWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        updateScope.launch {
            ToDoWidgetProvider.updateWidgetsInternal(
                context = appContext,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds
            )
        }
    }
}
