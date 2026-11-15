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
import androidx.core.content.ContextCompat
import com.example.notepad.R
import com.example.notepad.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 月曆桌面小工具提供者（AppWidgetProvider）
 *
 * 支援功能：
 * - 顯示完整月份與星期標題，並以藍色背景／外框明顯標示今天日期
 * - 直接於日期格內顯示最多 2 筆記事／待辦文字標籤（不再使用小圓點），不同底色區分記事與待辦
 * - 同一天超過 2 筆時顯示「＋N 項」，超長標題自動省略呈現
 * - 支援上一個月／下一個月切換與點擊年月標題回到當月
 * - 完整點擊互動：點擊日期看該日清單、點擊記事進編輯頁、點擊待辦看詳情／編輯、點擊新增快速建立記事或待辦
 */
class CalendarWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val deltaMonths = when (intent.action) {
            ACTION_PREV_MONTH -> -1
            ACTION_NEXT_MONTH -> 1
            ACTION_RESET_MONTH -> 0
            else -> return
        }

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferences = CalendarWidgetPreferences(appContext)
                if (deltaMonths == 0) {
                    preferences.resetToCurrentMonth(appWidgetId)
                } else {
                    preferences.shiftDisplayedMonth(appWidgetId, deltaMonths)
                }
                val manager = AppWidgetManager.getInstance(appContext) ?: return@launch
                updateWidgetsInternal(appContext, manager, intArrayOf(appWidgetId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsInternal(appContext, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val preferences = CalendarWidgetPreferences(context)
        appWidgetIds.forEach { appWidgetId ->
            preferences.removeWidget(appWidgetId)
        }
        super.onDeleted(context, appWidgetIds)
    }

    companion object {
        const val ACTION_PREV_MONTH = "com.example.notepad.ACTION_WIDGET_CAL_PREV_MONTH"
        const val ACTION_NEXT_MONTH = "com.example.notepad.ACTION_WIDGET_CAL_NEXT_MONTH"
        const val ACTION_RESET_MONTH = "com.example.notepad.ACTION_WIDGET_CAL_RESET_MONTH"

        private val WEEK_ROW_IDS = intArrayOf(
            R.id.widget_cal_week_0,
            R.id.widget_cal_week_1,
            R.id.widget_cal_week_2,
            R.id.widget_cal_week_3,
            R.id.widget_cal_week_4,
            R.id.widget_cal_week_5
        )

        /**
         * 讀取本地記事與待辦任務並重新繪製指定月曆桌面小工具
         */
        suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            val database = AppDatabase.getDatabase(context)
            val notes = database.noteDao().getAllNotesOnce()
            val todos = database.toDoDao().getAllToDosOnce()
            val preferences = CalendarWidgetPreferences(context)
            val nowMillis = System.currentTimeMillis()

            appWidgetIds.forEach { appWidgetId ->
                val (displayYear, displayMonth) = preferences.getDisplayedMonth(
                    appWidgetId = appWidgetId,
                    defaultTodayMillis = nowMillis
                )
                val resolvedState = CalendarWidgetLogic.resolveMonthGrid(
                    year = displayYear,
                    month = displayMonth,
                    notes = notes,
                    todos = todos,
                    todayMillis = nowMillis
                )
                val views = buildWidgetRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    resolved = resolvedState
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun buildWidgetRemoteViews(
            context: Context,
            appWidgetId: Int,
            resolved: ResolvedCalendarWidgetState
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)

            views.setTextViewText(R.id.widget_cal_month_title, resolved.headerTitle)

            // 左右切換月份與點擊年月標題回到當月
            views.setOnClickPendingIntent(
                R.id.widget_cal_prev_button,
                buildMonthActionPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    action = ACTION_PREV_MONTH,
                    requestCode = 200_000 + appWidgetId * 10 + 1,
                    uriSuffix = "prev"
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_cal_next_button,
                buildMonthActionPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    action = ACTION_NEXT_MONTH,
                    requestCode = 200_000 + appWidgetId * 10 + 2,
                    uriSuffix = "next"
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_cal_month_title,
                buildMonthActionPendingIntent(
                    context = context,
                    appWidgetId = appWidgetId,
                    action = ACTION_RESET_MONTH,
                    requestCode = 200_000 + appWidgetId * 10 + 3,
                    uriSuffix = "reset"
                )
            )

            // 點擊「+ 新增」按鈕 -> 開啟月曆頁快速建立記事或待辦對話框
            val quickAddIntent = QuickNoteWidgetContract.buildOpenCalendarDayIntent(
                context = context,
                dateMillis = resolved.quickAddDefaultDateMillis,
                openQuickAdd = true
            ).apply {
                data = Uri.parse("notepad://widget/calendar/add/$appWidgetId")
            }
            val quickAddPendingIntent = PendingIntent.getActivity(
                context,
                210_000 + appWidgetId,
                quickAddIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_cal_add_button, quickAddPendingIntent)

            // 填入 6 週 x 7 天 月曆格網（共 42 格）
            WEEK_ROW_IDS.forEachIndexed { weekIndex, weekRowId ->
                views.removeAllViews(weekRowId)
                for (dayInWeek in 0 until 7) {
                    val cellIndex = weekIndex * 7 + dayInWeek
                    val cellState = resolved.cells.getOrNull(cellIndex) ?: continue
                    val cellViews = buildDayCellRemoteViews(
                        context = context,
                        appWidgetId = appWidgetId,
                        cellIndex = cellIndex,
                        cell = cellState
                    )
                    views.addView(weekRowId, cellViews)
                }
            }

            return views
        }

        private fun buildDayCellRemoteViews(
            context: Context,
            appWidgetId: Int,
            cellIndex: Int,
            cell: CalendarDayCellState
        ): RemoteViews {
            val cellViews = RemoteViews(context.packageName, R.layout.widget_calendar_day_cell)

            // 今天以藍色底＋外框標示，其餘依是否屬於當月調整背景與日號顏色
            val cellBackgroundRes = when {
                cell.isToday -> R.drawable.widget_cal_today_bg
                cell.isCurrentMonth -> R.drawable.widget_cal_cell_bg
                else -> R.drawable.widget_cal_cell_other_bg
            }
            val dayNumberColorRes = when {
                cell.isToday -> R.color.widget_cal_today_text
                cell.isCurrentMonth -> R.color.widget_text_primary
                else -> R.color.widget_cal_text_other
            }
            cellViews.setInt(R.id.widget_cal_cell_root, "setBackgroundResource", cellBackgroundRes)
            cellViews.setTextViewText(R.id.widget_cal_day_number, cell.dayOfMonth.toString())
            cellViews.setTextColor(
                R.id.widget_cal_day_number,
                ContextCompat.getColor(context, dayNumberColorRes)
            )

            // 點擊日期格 -> 開啟該日期的記事與待辦清單
            val openDayIntent = QuickNoteWidgetContract.buildOpenCalendarDayIntent(
                context = context,
                dateMillis = cell.dayStartMillis,
                openQuickAdd = false
            ).apply {
                data = Uri.parse("notepad://widget/calendar/day/$appWidgetId/${cell.dayStartMillis}")
            }
            val openDayPendingIntent = PendingIntent.getActivity(
                context,
                300_000 + appWidgetId * 64 + cellIndex,
                openDayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            cellViews.setOnClickPendingIntent(R.id.widget_cal_cell_root, openDayPendingIntent)

            // 最多直接顯示 2 筆文字標籤；點擊記事進編輯頁，點擊待辦看詳情／編輯
            bindEventPill(
                context = context,
                cellViews = cellViews,
                pillViewId = R.id.widget_cal_event_1,
                appWidgetId = appWidgetId,
                cellIndex = cellIndex,
                slotIndex = 0,
                dayStartMillis = cell.dayStartMillis,
                event = cell.visibleEvents.getOrNull(0)
            )
            bindEventPill(
                context = context,
                cellViews = cellViews,
                pillViewId = R.id.widget_cal_event_2,
                appWidgetId = appWidgetId,
                cellIndex = cellIndex,
                slotIndex = 1,
                dayStartMillis = cell.dayStartMillis,
                event = cell.visibleEvents.getOrNull(1)
            )

            // 超過 2 筆時顯示「＋N 項」（例如「＋3 項」），點擊同樣開啟該日完整清單
            val overflowLabel = cell.overflowLabel
            if (overflowLabel != null) {
                cellViews.setViewVisibility(R.id.widget_cal_more_badge, View.VISIBLE)
                cellViews.setTextViewText(R.id.widget_cal_more_badge, overflowLabel)
                cellViews.setOnClickPendingIntent(R.id.widget_cal_more_badge, openDayPendingIntent)
            } else {
                cellViews.setViewVisibility(R.id.widget_cal_more_badge, View.GONE)
            }

            return cellViews
        }

        private fun bindEventPill(
            context: Context,
            cellViews: RemoteViews,
            pillViewId: Int,
            appWidgetId: Int,
            cellIndex: Int,
            slotIndex: Int,
            dayStartMillis: Long,
            event: CalendarDayEvent?
        ) {
            if (event == null) {
                cellViews.setViewVisibility(pillViewId, View.GONE)
                return
            }

            cellViews.setViewVisibility(pillViewId, View.VISIBLE)
            cellViews.setTextViewText(pillViewId, event.title)

            val (bgRes, textColorRes) = when (event.type) {
                CalendarEventType.NOTE -> R.drawable.widget_cal_note_pill_bg to R.color.widget_cal_note_text
                CalendarEventType.TODO -> if (event.isCompleted) {
                    R.drawable.widget_cal_todo_done_pill_bg to R.color.widget_cal_todo_done_text
                } else {
                    R.drawable.widget_cal_todo_pill_bg to R.color.widget_cal_todo_text
                }
            }
            cellViews.setInt(pillViewId, "setBackgroundResource", bgRes)
            cellViews.setTextColor(pillViewId, ContextCompat.getColor(context, textColorRes))

            val clickIntent = when (event.type) {
                CalendarEventType.NOTE -> QuickNoteWidgetContract.buildEditNoteIntent(
                    context = context,
                    noteId = event.id
                ).apply {
                    data = Uri.parse("notepad://widget/calendar/note/$appWidgetId/$dayStartMillis/${event.id}/$slotIndex")
                }
                CalendarEventType.TODO -> QuickNoteWidgetContract.buildOpenToDoDetailIntent(
                    context = context,
                    taskId = event.id
                ).apply {
                    data = Uri.parse("notepad://widget/calendar/todo/$appWidgetId/$dayStartMillis/${event.id}/$slotIndex")
                }
            }
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                500_000 + (appWidgetId % 50) * 200 + cellIndex * 2 + slotIndex,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            cellViews.setOnClickPendingIntent(pillViewId, clickPendingIntent)
        }

        private fun buildMonthActionPendingIntent(
            context: Context,
            appWidgetId: Int,
            action: String,
            requestCode: Int,
            uriSuffix: String
        ): PendingIntent {
            val intent = Intent(context, CalendarWidgetProvider::class.java).apply {
                this.action = action
                data = Uri.parse("notepad://widget/calendar/$uriSuffix/$appWidgetId")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

/**
 * 月曆桌面小工具即時重新整理協調器
 */
object CalendarWidgetUpdater {
    private val updateScope = CoroutineScope(Dispatchers.IO)

    /**
     * 記事、待辦資料變動時非同步觸發月曆桌面小工具更新
     */
    fun notifyDataChanged(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return
        val componentName = ComponentName(appContext, CalendarWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        updateScope.launch {
            CalendarWidgetProvider.updateWidgetsInternal(
                context = appContext,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds
            )
        }
    }

    /**
     * 供桌面小工具背景動作於結束 Broadcast 前同步等待月曆小工具刷新完成
     */
    suspend fun refreshAllBlocking(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return
        val componentName = ComponentName(appContext, CalendarWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        CalendarWidgetProvider.updateWidgetsInternal(
            context = appContext,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds
        )
    }
}
