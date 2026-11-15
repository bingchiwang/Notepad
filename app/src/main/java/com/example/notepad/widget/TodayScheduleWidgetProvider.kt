package com.example.notepad.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.notepad.R
import com.example.notepad.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 今日行程桌面小工具提供者（AppWidgetProvider）
 *
 * 支援功能：
 * - 顯示今日完成進度（整合今日時間規劃與今日到期待辦之進度條及百分比）
 * - 顯示今天到期的待辦任務（支援直接勾選完成與進入詳情頁）
 * - 顯示即將到來的提醒通知（點擊可直達提醒事項分軌）
 */
class TodayScheduleWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_COMPLETE_TODAY_TODO,
            ACTION_LEGACY_TODAY_TEMPLATE -> {
                val subAction = intent.getStringExtra(EXTRA_ITEM_ACTION) ?: intent.action
                val todoId = intent.getIntExtra(
                    ToDoWidgetProvider.EXTRA_TODO_ID,
                    QuickNoteWidgetContract.INVALID_ID
                )

                when (subAction) {
                    ACTION_COMPLETE_TODAY_TODO,
                    ToDoWidgetProvider.ACTION_COMPLETE_TODO -> {
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
                    ToDoWidgetProvider.ACTION_OPEN_TODO_DETAIL -> {
                        if (todoId <= 0) return
                        runCatching {
                            context.startActivity(
                                QuickNoteWidgetContract.buildOpenToDoDetailIntent(context, todoId)
                            )
                        }.onFailure { e ->
                            Log.w("TodayScheduleWidget", "無法開啟待辦詳情頁: ${e.message}")
                        }
                    }
                    ACTION_OPEN_REMINDERS -> {
                        runCatching {
                            context.startActivity(
                                QuickNoteWidgetContract.buildOpenRemindersTrackIntent(context)
                            )
                        }.onFailure { e ->
                            Log.w("TodayScheduleWidget", "無法開啟提醒分頁: ${e.message}")
                        }
                    }
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
        const val ACTION_COMPLETE_TODAY_TODO = "com.example.notepad.ACTION_WIDGET_TODAY_COMPLETE_TODO"
        private const val ACTION_OPEN_REMINDERS = "com.example.notepad.ACTION_WIDGET_TODAY_OPEN_REMINDERS"
        private const val ACTION_LEGACY_TODAY_TEMPLATE = "com.example.notepad.ACTION_WIDGET_TODAY_TEMPLATE"
        private const val EXTRA_ITEM_ACTION = "com.example.notepad.EXTRA_TODO_ITEM_ACTION"

        fun buildOpenRemindersFillInIntent(): Intent {
            return Intent().apply {
                putExtra(EXTRA_ITEM_ACTION, ACTION_OPEN_REMINDERS)
            }
        }

        /**
         * 讀取今日排程、待辦與提醒並重新繪製指定的今日行程桌面小工具
         */
        suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            val database = AppDatabase.getDatabase(context)
            val todos = database.toDoDao().getAllToDosOnce()
            val timeBlocks = database.timeBlockDao().getAllTimeBlocksOnce()
            val reminders = database.reminderDao().getAllRemindersOnce()

            val resolved = TodayScheduleWidgetLogic.resolveTodaySchedule(
                todos = todos,
                timeBlocks = timeBlocks,
                reminders = reminders
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = buildWidgetRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    resolved = resolved
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    @Suppress("DEPRECATION")
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_today_list)
                }
            }
        }

        private fun buildWidgetRemoteViews(
            context: Context,
            appWidgetId: Int,
            resolved: ResolvedTodayScheduleState
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_today_schedule)

            views.setTextViewText(R.id.widget_today_date_chip, resolved.headerDateText)
            views.setTextViewText(R.id.widget_today_progress_text, resolved.progress.summaryText)
            views.setProgressBar(R.id.widget_today_progress_bar, 100, resolved.progress.percent, false)

            // 點擊「+ 新增」按鈕 -> 開啟待辦新增視窗
            val addIntent = QuickNoteWidgetContract.buildOpenToDoTabIntent(
                context = context,
                openAddDialog = true
            ).apply {
                data = Uri.parse("notepad://widget/today/add/$appWidgetId")
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                90_000 + appWidgetId,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_today_add_button, addPendingIntent)

            // 點擊日期膠囊或今日完成進度卡 -> 開啟每日時間規劃表
            val scheduleIntent = QuickNoteWidgetContract.buildOpenScheduleTabIntent(context).apply {
                data = Uri.parse("notepad://widget/today/schedule/$appWidgetId")
            }
            val schedulePendingIntent = PendingIntent.getActivity(
                context,
                100_000 + appWidgetId,
                scheduleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_today_date_chip, schedulePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_today_progress_card, schedulePendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val collectionBuilder = RemoteViews.RemoteCollectionItems.Builder()
                resolved.rows.forEach { row ->
                    val rowViews = TodayScheduleWidgetRowBinder.bindRow(context, row).apply {
                        when (row) {
                            is TodayScheduleRow.DueTodayToDoRow -> {
                                val itemId = row.item.id
                                val completeIntent = Intent(context, TodayScheduleWidgetProvider::class.java).apply {
                                    action = ACTION_COMPLETE_TODAY_TODO
                                    data = Uri.parse("notepad://widget/today/complete/$itemId")
                                    putExtra(ToDoWidgetProvider.EXTRA_TODO_ID, itemId)
                                }
                                val completePendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    110_000 + itemId,
                                    completeIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                setOnClickPendingIntent(R.id.widget_todo_checkbox_container, completePendingIntent)

                                val detailIntent = QuickNoteWidgetContract.buildOpenToDoDetailIntent(
                                    context = context,
                                    taskId = itemId
                                ).apply {
                                    data = Uri.parse("notepad://widget/today/detail/$itemId")
                                }
                                val detailPendingIntent = PendingIntent.getActivity(
                                    context,
                                    120_000 + itemId,
                                    detailIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                setOnClickPendingIntent(R.id.widget_todo_content_area, detailPendingIntent)
                            }
                            is TodayScheduleRow.UpcomingReminderRow -> {
                                val reminderId = row.reminder.id
                                val reminderIntent = QuickNoteWidgetContract.buildOpenRemindersTrackIntent(context).apply {
                                    data = Uri.parse("notepad://widget/today/reminder/$reminderId")
                                }
                                val reminderPendingIntent = PendingIntent.getActivity(
                                    context,
                                    130_000 + reminderId,
                                    reminderIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                setOnClickPendingIntent(R.id.widget_reminder_item_root, reminderPendingIntent)
                            }
                            is TodayScheduleRow.SectionHeader,
                            is TodayScheduleRow.EmptyNoticeRow -> Unit
                        }
                    }
                    collectionBuilder.addItem(row.stableId, rowViews)
                }
                collectionBuilder.setHasStableIds(true)
                collectionBuilder.setViewTypeCount(TodayScheduleWidgetRowBinder.VIEW_TYPE_COUNT)
                views.setRemoteAdapter(R.id.widget_today_list, collectionBuilder.build())
            } else {
                val adapterIntent = Intent(context, TodayScheduleWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
                }
                @Suppress("DEPRECATION")
                views.setRemoteAdapter(R.id.widget_today_list, adapterIntent)

                val templateIntent = Intent(context, TodayScheduleWidgetProvider::class.java).apply {
                    action = ACTION_LEGACY_TODAY_TEMPLATE
                    data = Uri.parse("notepad://widget/today/template/$appWidgetId")
                }
                val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
                val templatePendingIntent = PendingIntent.getBroadcast(
                    context,
                    140_000 + appWidgetId,
                    templateIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
                )
                views.setPendingIntentTemplate(R.id.widget_today_list, templatePendingIntent)
            }

            return views
        }
    }
}

/**
 * 今日行程桌面小工具即時重新整理協調器
 */
object TodayScheduleWidgetUpdater {
    private val updateScope = CoroutineScope(Dispatchers.IO)

    fun notifyDataChanged(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return
        val componentName = ComponentName(appContext, TodayScheduleWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        updateScope.launch {
            TodayScheduleWidgetProvider.updateWidgetsInternal(
                context = appContext,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds
            )
        }
    }

    suspend fun refreshAllBlocking(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return
        val componentName = ComponentName(appContext, TodayScheduleWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        TodayScheduleWidgetProvider.updateWidgetsInternal(
            context = appContext,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds
        )
    }
}
