package com.example.notepad.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.ToDo
import com.example.notepad.data.preferences.NotificationPreferences
import com.example.notepad.data.repository.TaskRepository
import kotlinx.coroutines.flow.first

/**
 * 系統提醒排程管理器（規範 4.4）
 * - 驗證時間有效性（不可排程過去時間）
 * - 遵守全域通知總開關控制
 * - 支援任務完成時自動取消關聯之尚未觸發提醒
 */
object ReminderScheduler {

    private const val ACTION_REMINDER = "com.example.notepad.ACTION_REMINDER_NOTIFICATION"

    /**
     * 純邏輯檢核是否允許排程提醒（方便單元測試驗證規範 4.4）
     */
    fun isReminderScheduleAllowed(
        triggerAt: Long,
        notificationsEnabled: Boolean,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return notificationsEnabled && triggerAt > currentTimeMillis
    }

    /**
     * 檢查目前是否具備系統通知權限（Android 13+ 需 POST_NOTIFICATIONS）
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 排定單一系統提醒（若通知總開關關閉或時間已過期，則不安排排程）
     * @return true 表示已成功送出排程，false 表示因關閉或過期而未排程
     */
    fun scheduleReminder(
        context: Context,
        reminder: Reminder,
        notificationsEnabled: Boolean = true
    ): Boolean {
        if (!isReminderScheduleAllowed(reminder.triggerAt, notificationsEnabled)) {
            return false
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_REMINDER_MESSAGE, reminder.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.triggerAt,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.triggerAt,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAt,
                    pendingIntent
                )
            }
            true
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule exact alarm: ${e.message}")
            false
        }
    }

    /**
     * 取消指定 ID 之鬧鐘通知排程
     */
    fun cancelReminder(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * 切換任務完成狀態並連動取消尚未觸發的關聯提醒（規範 4.4）
     * @return 本次被自動取消的關聯提醒數量
     */
    suspend fun updateToDoCompletionAndSyncReminders(
        context: Context,
        taskRepository: TaskRepository,
        toDo: ToDo,
        isCompleted: Boolean
    ): Int {
        taskRepository.updateToDo(toDo.copy(isCompleted = isCompleted))
        if (!isCompleted) return 0

        val pendingReminders = taskRepository.getPendingRemindersByToDoId(toDo.id)
        pendingReminders.forEach { reminder ->
            cancelReminder(context, reminder.id)
            taskRepository.markReminderAsFired(reminder.id)
        }
        return pendingReminders.size
    }

    /**
     * 切換全域通知總開關並同步更新系統排程（規範 4.4）
     * - 關閉時：立即取消所有待觸發鬧鐘排程
     * - 開啟時：重新排程尚未觸發且未過期的未來提醒
     */
    suspend fun toggleNotificationsMasterSwitch(
        context: Context,
        taskRepository: TaskRepository,
        notificationPreferences: NotificationPreferences,
        enabled: Boolean
    ) {
        notificationPreferences.setNotificationsEnabled(enabled)
        val allReminders = taskRepository.allReminders.first()
        val now = System.currentTimeMillis()

        if (!enabled) {
            allReminders.filter { !it.isFired }.forEach { reminder ->
                cancelReminder(context, reminder.id)
            }
        } else {
            allReminders.filter { !it.isFired && it.triggerAt > now }.forEach { reminder ->
                scheduleReminder(context, reminder, notificationsEnabled = true)
            }
        }
    }
}
