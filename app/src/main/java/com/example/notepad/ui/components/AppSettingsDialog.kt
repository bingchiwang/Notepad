package com.example.notepad.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.notepad.data.preferences.AppFontScale
import com.example.notepad.data.preferences.CalendarStyle
import com.example.notepad.data.preferences.PERSONALIZATION_COLOR_PRESETS
import com.example.notepad.data.preferences.PersonalizationSettings
import com.example.notepad.data.preferences.ThemeMode
import com.example.notepad.data.sync.CloudSyncUiState
import com.example.notepad.reminder.ReminderScheduler
import com.example.notepad.ui.theme.CalendarThemeResolver
import com.example.notepad.ui.theme.ColorContrastUtils
import com.example.notepad.ui.theme.LocalCustomButtonColors

/**
 * 個人化設定管理中心對話框
 *
 * 整合四大核心管理模組：
 * 1. 主題設定（淺／深／系統模式、自訂按鍵顏色、自訂月曆樣式與全域文字高對比自動防重色）
 * 2. 字體大小調整（即時全局縮放與預覽）
 * 3. 通知開關與 Android 8.0+ / 13+ 權限狀態檢查
 * 4. 雲端同步狀態檢視、一鍵同步與連線設定入口
 */
@Composable
fun AppSettingsDialog(
    personalization: PersonalizationSettings,
    notificationsEnabled: Boolean,
    syncUiState: CloudSyncUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onButtonColorSelected: (Long?) -> Unit,
    onCalendarStyleSelected: (CalendarStyle) -> Unit,
    onCalendarAccentColorSelected: (Long?) -> Unit,
    onFontScaleSelected: (AppFontScale) -> Unit,
    onResetPersonalization: () -> Unit,
    onNotificationsToggled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onOpenCloudSyncSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ReminderScheduler.hasNotificationPermission(context))
    }

    // 自訂 Hex 色碼輸入狀態
    var hexColorInput by remember(personalization.buttonColorLong) {
        mutableStateOf(
            personalization.buttonColorLong?.let { ColorContrastUtils.formatHexColor(it) } ?: ""
        )
    }
    var hexInputError by remember { mutableStateOf<String?>(null) }

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
        hasPermission = granted && ReminderScheduler.hasNotificationPermission(context)
        if (hasPermission) {
            showFeedback(context, "已取得系統通知權限")
        } else {
            showFeedback(context, "通知權限尚未開啟，請至系統設定授權")
        }
    }

    val useDarkTheme = personalization.themeMode.shouldUseDarkTheme(isSystemInDarkTheme())
    val activeColorScheme = MaterialTheme.colorScheme
    val calendarPreviewPalette = remember(
        personalization.calendarStyle,
        personalization.calendarAccentColorLong,
        useDarkTheme,
        activeColorScheme
    ) {
        CalendarThemeResolver.resolvePalette(
            style = personalization.calendarStyle,
            customAccentColorLong = personalization.calendarAccentColorLong,
            darkTheme = useDarkTheme,
            themeColorScheme = activeColorScheme
        )
    }

    val hasAnyCustomStyle = personalization.buttonColorLong != null ||
        personalization.calendarStyle != CalendarStyle.CLASSIC ||
        personalization.calendarAccentColorLong != null ||
        personalization.fontScale != AppFontScale.MEDIUM

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 頂部標題與一鍵重設
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚙️ 管理個人化設定",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (hasAnyCustomStyle) {
                        TextButton(onClick = onResetPersonalization) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "重設個人化設定",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重設", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // === 1. 主題設定（顯示模式 + 按鍵顏色 + 月曆樣式） ===
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "主題與外觀風格（含自動防重色對比保護）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 1A. 淺色 / 深色 / 跟隨系統切換
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = mode == personalization.themeMode
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
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onThemeModeSelected(mode) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = mode.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
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

                    // 1B. 自訂按鍵顏色（含白/黃/黑等對比度實測與 Hex 輸入）
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "自訂按鍵顏色",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (personalization.buttonColorLong != null) {
                                    TextButton(
                                        onClick = {
                                            hexInputError = null
                                            onButtonColorSelected(null)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("恢復主題預設", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            PresetSwatchRow(
                                selectedColorLong = personalization.buttonColorLong,
                                onColorSelected = {
                                    hexInputError = null
                                    onButtonColorSelected(it)
                                }
                            )

                            // 自訂 Hex 色碼輸入
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = hexColorInput,
                                    onValueChange = {
                                        hexColorInput = it
                                        hexInputError = null
                                    },
                                    label = { Text("自訂色碼 (#RRGGBB)") },
                                    placeholder = { Text("#2196F3 或 #FFFFFF") },
                                    isError = hexInputError != null,
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        val parsed = ColorContrastUtils.parseHexColorOrNull(hexColorInput)
                                        if (parsed == null) {
                                            hexInputError = "格式需為 6 碼十六進位（例如 #FFEB3B）"
                                            showFeedback(context, hexInputError!!)
                                        } else {
                                            hexInputError = null
                                            onButtonColorSelected(parsed)
                                        }
                                    }
                                ) {
                                    Text("套用")
                                }
                            }
                            hexInputError?.let { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            // 即時按鍵與文字反色預覽
                            val customBtnColors = LocalCustomButtonColors.current
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        showFeedback(context, "按鍵對比效果清晰可辨！")
                                    },
                                    colors = AppButtonThemeDefaults.filledColors(),
                                    border = AppButtonThemeDefaults.filledBorder(),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("按鍵效果預覽", style = MaterialTheme.typography.labelMedium)
                                }
                                Text(
                                    text = if (customBtnColors.contentColor == ColorContrastUtils.DARK_INK) {
                                        "淺色按鍵已自動搭配深墨文字"
                                    } else {
                                        "深色按鍵已自動搭配亮白文字"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 1C. 自訂月曆樣式與重點色 + 即時對比預覽
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "自訂月曆樣式",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(CalendarStyle.entries) { style ->
                                    val selected = style == personalization.calendarStyle
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onCalendarStyleSelected(style) },
                                        label = { Text(style.label) }
                                    )
                                }
                            }
                            Text(
                                text = personalization.calendarStyle.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "自訂月曆選取底色（可選）",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (personalization.calendarAccentColorLong != null) {
                                    TextButton(
                                        onClick = { onCalendarAccentColorSelected(null) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("依樣式預設", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            PresetSwatchRow(
                                selectedColorLong = personalization.calendarAccentColorLong,
                                onColorSelected = { onCalendarAccentColorSelected(it) }
                            )

                            // 迷你月曆即時預覽（驗證節慶紅/星期紅與選日底色不會同色隱形）
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = calendarPreviewPalette.cardBgColor,
                                border = calendarPreviewPalette.cardBorderColor?.let { BorderStroke(1.dp, it) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 預覽 A：今日／未選節慶格
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(calendarPreviewPalette.cellShape)
                                            .background(calendarPreviewPalette.todayBgColor)
                                            .border(
                                                1.dp,
                                                calendarPreviewPalette.todayBorderColor,
                                                calendarPreviewPalette.cellShape
                                            )
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "14",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = calendarPreviewPalette.resolveDayNumberColor(
                                                    isSelected = false,
                                                    dayOfWeekIndex = 0
                                                )
                                            )
                                            Text(
                                                text = "端陽節氣",
                                                fontSize = 10.sp,
                                                color = calendarPreviewPalette.resolveLunarLabelColor(
                                                    isSelected = false,
                                                    hasFestival = false,
                                                    hasSolarTerm = true
                                                )
                                            )
                                        }
                                    }

                                    // 預覽 B：選中狀態＋節慶標籤（背景色若偏紅／黃／黑仍清楚反色）
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(calendarPreviewPalette.cellShape)
                                            .background(calendarPreviewPalette.selectedBgColor)
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "15",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = calendarPreviewPalette.resolveDayNumberColor(
                                                    isSelected = true,
                                                    dayOfWeekIndex = 0
                                                )
                                            )
                                            Text(
                                                text = "端午節",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = calendarPreviewPalette.resolveLunarLabelColor(
                                                    isSelected = true,
                                                    hasFestival = true,
                                                    hasSolarTerm = false
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // === 2. 字體大小調整 ===
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "字體大小（全應用即時縮放）",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AppFontScale.entries) { fontScaleOption ->
                            FilterChip(
                                selected = fontScaleOption == personalization.fontScale,
                                onClick = { onFontScaleSelected(fontScaleOption) },
                                label = { Text(fontScaleOption.label) }
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "字體大小預覽標題（${personalization.fontScale.label}）",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "所有記事內容、待辦清單、農曆日曆與提醒字體皆已依此比例調整。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // === 3. 通知開關與授權管理 ===
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🔔 提醒通知開關",
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
                                            colors = AppButtonThemeDefaults.filledColors(),
                                            border = AppButtonThemeDefaults.filledBorder(),
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

                // === 4. 雲端同步狀態與快速控制 ===
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                syncUiState.isSyncing -> Icons.Default.CloudSync
                                !syncUiState.isOnline || !syncUiState.syncSettings.syncEnabled -> Icons.Default.CloudOff
                                else -> Icons.Default.CloudDone
                            },
                            contentDescription = null,
                            tint = if (syncUiState.isOnline && syncUiState.isFirebaseConfigured) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "雲端資料同步狀態",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val networkStateText = if (syncUiState.isOnline) "已連線" else "離線中（已保留本機異動）"
                            val firebaseStateText = if (syncUiState.isFirebaseConfigured) "已就緒" else "尚未配置 Firebase 專案"
                            val pendingText = if (syncUiState.pendingCount > 0) {
                                "待上傳 ${syncUiState.pendingCount} 筆"
                            } else {
                                "無待同步異動"
                            }
                            val lastSyncText = syncUiState.syncSettings.lastSyncAt?.let {
                                DateFormatter.formatDateTime(it)
                            } ?: "尚未同步"

                            Text(
                                text = "🌐 網路：$networkStateText  ｜  🔥 雲端：$firebaseStateText",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "⏳ 待同步：$pendingText  ｜  🕒 最後同步：$lastSyncText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "📝 ${syncUiState.lastRunSummary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSyncNow,
                                    enabled = !syncUiState.isSyncing,
                                    colors = AppButtonThemeDefaults.filledColors(),
                                    border = AppButtonThemeDefaults.filledBorder(),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (syncUiState.isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = LocalCustomButtonColors.current.contentColor
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("同步中...", style = MaterialTheme.typography.labelMedium)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("立即同步", style = MaterialTheme.typography.labelMedium)
                                    }
                                }

                                OutlinedButton(
                                    onClick = onOpenCloudSyncSettings,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("連線設定", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = AppButtonThemeDefaults.filledColors(),
                        border = AppButtonThemeDefaults.filledBorder()
                    ) {
                        Text("完成")
                    }
                }
            }
        }
    }
}

/**
 * 可共用之精選色票選擇列（含淺色與深色色塊，並自動計算打勾圖示清晰反色）
 */
@Composable
private fun PresetSwatchRow(
    selectedColorLong: Long?,
    onColorSelected: (Long) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        items(PERSONALIZATION_COLOR_PRESETS) { preset ->
            val swatchColor = Color(preset.colorLong)
            val isSelected = selectedColorLong == preset.colorLong
            val borderColor = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            }
            val borderWidth = if (isSelected) 2.5.dp else 1.dp

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(borderWidth, borderColor, CircleShape)
                    .clickable { onColorSelected(preset.colorLong) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = preset.name,
                        tint = ColorContrastUtils.bestOnBackgroundColor(swatchColor),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
