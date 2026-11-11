package com.example.notepad.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notepad.data.preferences.ThemeMode
import com.example.notepad.reminder.ReminderScheduler

/**
 * 應用程式偏好設定對話框（規範 4.4 通知總開關 & 權限處理 + 規範 4.5 自訂主題切換）
 */
@Composable
fun AppSettingsDialog(
    currentThemeMode: ThemeMode,
    notificationsEnabled: Boolean,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onNotificationsToggled: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ReminderScheduler.hasNotificationPermission(context))
    }

    // 從 Android 8.0+ 系統通知設定返回 App 時，自動重新確認通知權限與總開關狀態
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ReminderScheduler.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            showFeedback(context, "已取得系統通知權限")
        } else {
            showFeedback(context, "通知權限尚未開啟，請至系統設定授權")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚙️ 外觀主題與通知設定",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // === 4.5 自訂主題切換 ===
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🎨 顯示主題（即時同步所有頁面並永久保存）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ThemeMode.entries.forEach { mode ->
                        val isSelected = mode == currentThemeMode
                        val icon = when (mode) {
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        }
                        val description = when (mode) {
                            ThemeMode.LIGHT -> "固定使用明亮舒適的淺色介面"
                            ThemeMode.DARK -> "固定使用護眼省電的深色介面"
                            ThemeMode.SYSTEM -> "自動依據手機系統設定切換淺色或深色"
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeModeSelected(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = mode.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onThemeModeSelected(mode) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // === 4.4 提醒通知總開關與權限 ===
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🔔 提醒通知設定",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (notificationsEnabled) {
                                        Icons.Default.Notifications
                                    } else {
                                        Icons.Default.NotificationsOff
                                    },
                                    contentDescription = "通知總開關",
                                    tint = if (notificationsEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Column {
                                    Text(
                                        text = "啟用提醒通知功能",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (notificationsEnabled) {
                                            "開啟中：將在預定時間準時發送提醒"
                                        } else {
                                            "已關閉：停止發送通知並暫停排程"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = onNotificationsToggled
                            )
                        }
                    }

                    // Android 13+ 通知權限狀態檢查與授權入口
                    if (!hasPermission) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⚠️ 尚未授與系統通知權限，提醒時間到時將無法顯示通知。",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        ) {
                                            Text("申請通知權限")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(settingsIntent)
                                        }
                                    ) {
                                        Text("開啟系統設定")
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "✅ Android 系統通知權限已就緒",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("完成")
                    }
                }
            }
        }
    }
}
