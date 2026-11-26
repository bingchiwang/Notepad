package com.example.notepad.widget

import android.content.Context

/**
 * 月曆桌面小工具個別實例顯示月份狀態儲存（SharedPreferences）
 * 支援在桌面放置多個不同月份的月曆小工具實例並保存左右切換後的年月。
 */
class CalendarWidgetPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 讀取指定小工具目前顯示的年與月（未設定時回傳今日所屬年月）
     */
    fun getDisplayedMonth(
        appWidgetId: Int,
        defaultTodayMillis: Long = System.currentTimeMillis()
    ): Pair<Int, Int> {
        val year = prefs.getInt(yearKey(appWidgetId), UNSET_VALUE)
        val month = prefs.getInt(monthKey(appWidgetId), UNSET_VALUE)
        return if (year > 0 && month in 1..12) {
            year to month
        } else {
            CalendarWidgetLogic.currentYearMonth(defaultTodayMillis)
        }
    }

    /**
     * 儲存指定小工具顯示中的年與月
     */
    fun setDisplayedMonth(appWidgetId: Int, year: Int, month: Int) {
        prefs.edit()
            .putInt(yearKey(appWidgetId), year)
            .putInt(monthKey(appWidgetId), month)
            .apply()
    }

    /**
     * 將所有已放置於桌面的月曆小工具同步至 App 內目前顯示的年與月
     */
    fun syncAllWidgetsDisplayedMonth(appWidgetIds: IntArray, year: Int, month: Int) {
        if (appWidgetIds.isEmpty()) return
        val editor = prefs.edit()
        appWidgetIds.forEach { appWidgetId ->
            editor.putInt(yearKey(appWidgetId), year)
            editor.putInt(monthKey(appWidgetId), month)
        }
        editor.apply()
    }

    /**
     * 將指定小工具的顯示月份往前或往後移動 [deltaMonths] 個月
     */
    fun shiftDisplayedMonth(
        appWidgetId: Int,
        deltaMonths: Int,
        defaultTodayMillis: Long = System.currentTimeMillis()
    ): Pair<Int, Int> {
        val (currentYear, currentMonth) = getDisplayedMonth(appWidgetId, defaultTodayMillis)
        val (nextYear, nextMonth) = CalendarWidgetLogic.shiftMonth(currentYear, currentMonth, deltaMonths)
        setDisplayedMonth(appWidgetId, nextYear, nextMonth)
        return nextYear to nextMonth
    }

    /**
     * 重設指定小工具回到今日所屬月份
     */
    fun resetToCurrentMonth(
        appWidgetId: Int,
        defaultTodayMillis: Long = System.currentTimeMillis()
    ): Pair<Int, Int> {
        val (todayYear, todayMonth) = CalendarWidgetLogic.currentYearMonth(defaultTodayMillis)
        setDisplayedMonth(appWidgetId, todayYear, todayMonth)
        return todayYear to todayMonth
    }

    /**
     * 讀取指定小工具是否顯示農曆日期／節慶／節氣（預設顯示）
     */
    fun getShowLunarDate(appWidgetId: Int): Boolean {
        return prefs.getBoolean(lunarKey(appWidgetId), DEFAULT_SHOW_LUNAR)
    }

    /**
     * 儲存指定小工具是否顯示農曆日期之設定
     */
    fun setShowLunarDate(appWidgetId: Int, showLunar: Boolean) {
        prefs.edit()
            .putBoolean(lunarKey(appWidgetId), showLunar)
            .apply()
    }

    /**
     * 一鍵切換指定小工具的農曆顯示開關並回傳切換後狀態
     */
    fun toggleShowLunarDate(appWidgetId: Int): Boolean {
        val nextValue = !getShowLunarDate(appWidgetId)
        setShowLunarDate(appWidgetId, nextValue)
        return nextValue
    }

    /**
     * 讀取全域預設的小工具月曆日期字體大小（供 App 設定頁與新增小工具時讀取）
     */
    fun getGlobalDateFontSize(): CalendarWidgetDateFontSize {
        val raw = prefs.getString(KEY_DEFAULT_DATE_FONT_SIZE, CalendarWidgetDateFontSize.MEDIUM.name)
        return CalendarWidgetDateFontSize.fromString(raw)
    }

    /**
     * 讀取指定月曆小工具實例的日期字體大小（未個別設定時自動回退至全域預設值）
     */
    fun getDateFontSize(appWidgetId: Int): CalendarWidgetDateFontSize {
        val instanceRaw = prefs.getString(fontSizeKey(appWidgetId), null)
        return if (instanceRaw != null) {
            CalendarWidgetDateFontSize.fromString(instanceRaw)
        } else {
            getGlobalDateFontSize()
        }
    }

    /**
     * 儲存指定月曆小工具實例的日期字體大小，並同步更新全域預設字級
     */
    fun setDateFontSize(appWidgetId: Int, fontSize: CalendarWidgetDateFontSize) {
        prefs.edit()
            .putString(fontSizeKey(appWidgetId), fontSize.name)
            .putString(KEY_DEFAULT_DATE_FONT_SIZE, fontSize.name)
            .apply()
    }

    /**
     * 儲存全域預設日期字體大小，並同步套用至目前桌面上所有月曆小工具實例
     */
    fun setGlobalDateFontSize(appWidgetIds: IntArray, fontSize: CalendarWidgetDateFontSize) {
        val editor = prefs.edit()
            .putString(KEY_DEFAULT_DATE_FONT_SIZE, fontSize.name)
        appWidgetIds.forEach { appWidgetId ->
            editor.putString(fontSizeKey(appWidgetId), fontSize.name)
        }
        editor.apply()
    }

    /**
     * 移除已從桌面刪除之月曆小工具設定資料
     */
    fun removeWidget(appWidgetId: Int) {
        prefs.edit()
            .remove(yearKey(appWidgetId))
            .remove(monthKey(appWidgetId))
            .remove(lunarKey(appWidgetId))
            .remove(fontSizeKey(appWidgetId))
            .apply()
    }

    private fun yearKey(appWidgetId: Int): String = "cal_year_$appWidgetId"

    private fun monthKey(appWidgetId: Int): String = "cal_month_$appWidgetId"

    private fun lunarKey(appWidgetId: Int): String = "cal_lunar_$appWidgetId"

    private fun fontSizeKey(appWidgetId: Int): String = "cal_font_size_$appWidgetId"

    companion object {
        private const val PREFS_NAME = "calendar_widget_prefs"
        private const val UNSET_VALUE = -1
        private const val KEY_DEFAULT_DATE_FONT_SIZE = "cal_default_date_font_size"
        const val DEFAULT_SHOW_LUNAR = true
    }
}
