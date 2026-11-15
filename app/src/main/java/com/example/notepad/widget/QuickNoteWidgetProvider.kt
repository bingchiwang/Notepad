package com.example.notepad.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.example.notepad.MainActivity
import com.example.notepad.R
import com.example.notepad.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 快速記事桌面小工具提供者（AppWidgetProvider）
 *
 * 支援功能：
 * - 顯示指定分類下（或全部／未分類）的最近記事清單
 * - 點擊頂部「新增」按鈕或空狀態區塊即可直接進入新增記事編輯頁面
 * - 點擊任一記事卡片即可進入該篇記事的編輯頁面
 * - 點擊頂部分類標籤即可開啟設定頁面隨時切換顯示分類
 */
class QuickNoteWidgetProvider : AppWidgetProvider() {

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

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = QuickNoteWidgetPreferences(context)
        appWidgetIds.forEach { appWidgetId ->
            prefs.removeWidget(appWidgetId)
        }
    }

    companion object {
        /**
         * 同步／非同步從本機資料庫讀取最新分類狀態，並更新所有指定的桌面小工具視圖
         */
        suspend fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            val database = AppDatabase.getDatabase(context)
            val notes = database.noteDao().getAllNotesOnce()
            val categories = database.categoryDao().getAllCategoriesOnce()
            val prefs = QuickNoteWidgetPreferences(context)

            appWidgetIds.forEach { appWidgetId ->
                val requestedFilter = prefs.getFilter(appWidgetId)
                val resolved = QuickNoteWidgetLogic.resolveWidgetState(
                    notes = notes,
                    categories = categories,
                    requestedFilter = requestedFilter
                )
                if (resolved.isCategoryMissingFallback) {
                    prefs.setFilter(appWidgetId, resolved.effectiveFilter)
                }

                val views = buildWidgetRemoteViews(
                    context = context,
                    appWidgetId = appWidgetId,
                    filterLabel = resolved.filterLabel,
                    prefillCategoryId = resolved.prefillCategoryId
                )
                appWidgetManager.updateAppWidget(appWidgetId, views)
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_notes_list)
            }
        }

        private fun buildWidgetRemoteViews(
            context: Context,
            appWidgetId: Int,
            filterLabel: String,
            prefillCategoryId: Int?
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_note)

            views.setTextViewText(R.id.widget_category_chip, "$filterLabel ▾")
            views.setTextViewText(
                R.id.widget_empty_title,
                "「$filterLabel」尚無記事"
            )

            // 綁定列表清單服務（RemoteViewsService）與空狀態視圖
            val adapterIntent = Intent(context, QuickNoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.widget_notes_list, adapterIntent)
            views.setEmptyView(R.id.widget_notes_list, R.id.widget_empty_view)

            // 點擊「新增」按鈕或空狀態視圖 -> 進入新增記事畫面（若顯示指定分類則預選該分類）
            val createIntent = QuickNoteWidgetContract.buildCreateNoteIntent(context, prefillCategoryId).apply {
                data = Uri.parse("notepad://widget/create/$appWidgetId")
            }
            val createPendingIntent = PendingIntent.getActivity(
                context,
                10_000 + appWidgetId,
                createIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_button, createPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_view, createPendingIntent)

            // 點擊分類標籤 -> 開啟桌面小工具分類切換設定頁
            val configIntent = Intent(context, QuickNoteWidgetConfigureActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse("notepad://widget/configure/$appWidgetId")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val configPendingIntent = PendingIntent.getActivity(
                context,
                20_000 + appWidgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_category_chip, configPendingIntent)

            // 設定 ListView 點擊項目模板（明確指定 MainActivity，符合 Android 12+/14+ PendingIntent 規範）
            val editTemplateIntent = Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("notepad://widget/edit/$appWidgetId")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
            val editTemplatePendingIntent = PendingIntent.getActivity(
                context,
                30_000 + appWidgetId,
                editTemplateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
            )
            views.setPendingIntentTemplate(R.id.widget_notes_list, editTemplatePendingIntent)

            return views
        }
    }
}

/**
 * 桌面小工具資料即時重新整理協調器
 * 當本機新增、修改、刪除記事／分類，或從雲端同步取得異動後呼叫，確保桌面小工具保持最新內容。
 */
object QuickNoteWidgetUpdater {
    private val updateScope = CoroutineScope(Dispatchers.IO)

    fun notifyDataChanged(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext) ?: return
        val componentName = ComponentName(appContext, QuickNoteWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds.isEmpty()) return

        updateScope.launch {
            QuickNoteWidgetProvider.updateWidgetsInternal(
                context = appContext,
                appWidgetManager = appWidgetManager,
                appWidgetIds = appWidgetIds
            )
        }
    }
}
