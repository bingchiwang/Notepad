package com.example.notepad.widget

import android.content.Context

/**
 * 桌面快速記事小工具之個別實例偏好儲存（SharedPreferences）
 * 支援在同一桌面放置多個小工具，各自顯示不同指定分類之記事。
 */
class QuickNoteWidgetPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 讀取指定小工具 ID 所設定的顯示分類（預設顯示全部記事）
     */
    fun getFilter(appWidgetId: Int): WidgetCategoryFilter {
        val raw = prefs.getString(keyForWidget(appWidgetId), null)
        return QuickNoteWidgetLogic.decodeFilter(raw)
    }

    /**
     * 儲存指定小工具 ID 的顯示分類篩選設定
     */
    fun setFilter(appWidgetId: Int, filter: WidgetCategoryFilter) {
        prefs.edit()
            .putString(keyForWidget(appWidgetId), QuickNoteWidgetLogic.encodeFilter(filter))
            .apply()
    }

    /**
     * 移除已從桌面刪除之小工具設定資料
     */
    fun removeWidget(appWidgetId: Int) {
        prefs.edit()
            .remove(keyForWidget(appWidgetId))
            .apply()
    }

    private fun keyForWidget(appWidgetId: Int): String = "widget_filter_$appWidgetId"

    companion object {
        private const val PREFS_NAME = "quick_note_widget_prefs"
    }
}
