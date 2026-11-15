package com.example.notepad.widget

import android.content.Intent
import android.os.Binder
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.appwidget.AppWidgetManager
import com.example.notepad.R
import com.example.notepad.data.local.AppDatabase
import kotlinx.coroutines.runBlocking

/**
 * 快速記事桌面小工具列表遠端視圖服務（RemoteViewsService）
 * 負責從本機 Room 資料庫即時載入篩選後的最近記事，並綁定點擊進入編輯之 Fill-In Intent。
 */
class QuickNoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        return QuickNoteRemoteViewsFactory(applicationContext, appWidgetId)
    }
}

private class QuickNoteRemoteViewsFactory(
    private val context: android.content.Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WidgetNoteItem> = emptyList()

    override fun onCreate() {
        // 首次建立時交由系統後續觸發 onDataSetChanged 載入最新資料
    }

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            val database = AppDatabase.getDatabase(context)
            val widgetPrefs = QuickNoteWidgetPreferences(context)
            val filter = widgetPrefs.getFilter(appWidgetId)

            val resolved = runBlocking {
                val notes = database.noteDao().getAllNotesOnce()
                val categories = database.categoryDao().getAllCategoriesOnce()
                QuickNoteWidgetLogic.resolveWidgetState(
                    notes = notes,
                    categories = categories,
                    requestedFilter = filter
                )
            }
            items = resolved.notes
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

        return RemoteViews(context.packageName, R.layout.widget_quick_note_item).apply {
            setInt(R.id.widget_item_color_bar, "setBackgroundColor", item.accentColor.toInt())
            setViewVisibility(R.id.widget_item_pin, if (item.isPinned) View.VISIBLE else View.GONE)
            setTextViewText(R.id.widget_item_title, item.title)

            if (!item.categoryName.isNullOrBlank()) {
                setViewVisibility(R.id.widget_item_category, View.VISIBLE)
                setTextViewText(R.id.widget_item_category, item.categoryName)
            } else {
                setViewVisibility(R.id.widget_item_category, View.GONE)
            }

            setTextViewText(R.id.widget_item_time, item.formattedTime)
            setTextViewText(R.id.widget_item_content, item.contentPreview)

            // 點擊該篇記事可進入編輯頁面
            setOnClickFillInIntent(
                R.id.widget_item_root,
                QuickNoteWidgetContract.buildEditNoteFillInIntent(item.id)
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
