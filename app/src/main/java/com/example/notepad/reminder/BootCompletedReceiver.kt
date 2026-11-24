package com.example.notepad.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notepad.widget.CalendarWidgetUpdater
import com.example.notepad.widget.ToDoWidgetUpdater
import com.example.notepad.widget.TodayScheduleWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 系統開機與應用程式更新廣播接收器（規範 3.4）
 *
 * 監聽：
 * - [Intent.ACTION_BOOT_COMPLETED]：裝置重新開機完成（`AlarmManager` 排程於關機時會全數清除）
 * - [Intent.ACTION_MY_PACKAGE_REPLACED]：應用程式覆蓋更新完成（更新安裝會重置既定鬧鐘）
 * - 部分機型之 `QUICKBOOT_POWERON` 快速開機事件
 *
 * 收到廣播後於背景非同步從 Room 資料庫撈取未觸發且未過期的提醒重新向 `AlarmManager` 排程，
 * 並同步刷新桌面待辦、今日行程與月曆小工具狀態。
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (!isSupportedBootAction(action)) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val count = ReminderScheduler.rescheduleUpcomingReminders(appContext)
                Log.i(TAG, "收到系統事件 $action，已重新排程 $count 筆未來提醒")

                ToDoWidgetUpdater.notifyDataChanged(appContext)
                TodayScheduleWidgetUpdater.notifyDataChanged(appContext)
                CalendarWidgetUpdater.notifyDataChanged(appContext)
            } catch (e: Exception) {
                Log.w(TAG, "重新排程提醒時發生異常: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"

        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON
        )

        /**
         * 判斷廣播動作是否為合法之重開機／App 更新復原事件
         */
        fun isSupportedBootAction(action: String?): Boolean {
            return action != null && action in SUPPORTED_ACTIONS
        }
    }
}
