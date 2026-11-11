package com.example.notepad.reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.notepad.MainActivity
import com.example.notepad.data.local.AppDatabase
import com.example.notepad.data.preferences.NotificationPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 提醒廣播接收器（規範 4.4）
 * - 觸發前再次確認通知總開關與系統通知權限，確保關閉通知或無權限時不發送干擾
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, 0)
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "任務提醒"
        val message = intent.getStringExtra(EXTRA_REMINDER_MESSAGE) ?: "您的設定提醒時間到了！"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationPrefs = NotificationPreferences(context)
                val isEnabled = notificationPrefs.isNotificationsEnabled()

                if (isEnabled && ReminderScheduler.hasNotificationPermission(context)) {
                    showNotification(context, reminderId, title, message)
                }

                // 標記資料庫狀態為已處理
                val db = AppDatabase.getDatabase(context)
                db.reminderDao().markAsFired(reminderId)
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, reminderId: Int, title: String, message: String) {
        if (!ReminderScheduler.hasNotificationPermission(context)) {
            return
        }

        val channelId = "notepad_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // minSdk = 26 (Android 8.0) 起一律建立 NotificationChannel 以確保通知正常顯示
        val channel = NotificationChannel(
            channelId,
            "任務提醒通知",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "提供即時事件與待辦通知"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🔔 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(reminderId, notification)
    }

    companion object {
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_REMINDER_TITLE = "EXTRA_REMINDER_TITLE"
        const val EXTRA_REMINDER_MESSAGE = "EXTRA_REMINDER_MESSAGE"
    }
}
