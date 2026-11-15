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
     * 移除已從桌面刪除之月曆小工具設定資料
     */
    fun removeWidget(appWidgetId: Int) {
        prefs.edit()
            .remove(yearKey(appWidgetId))
            .remove(monthKey(appWidgetId))
            .apply()
    }

    private fun yearKey(appWidgetId: Int): String = "cal_year_$appWidgetId"

    private fun monthKey(appWidgetId: Int): String = "cal_month_$appWidgetId"

    companion object {
        private const val PREFS_NAME = "calendar_widget_prefs"
        private const val UNSET_VALUE = -1
    }
}
