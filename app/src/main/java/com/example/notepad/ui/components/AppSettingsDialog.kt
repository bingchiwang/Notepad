package com.example.notepad.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.notepad.widget.CalendarWidgetDateFontSize

/**
 * 設定管理中心的五大摺疊分類列舉（預設全數收合，點擊分類項目後才展開對應設定）
 */
private enum class SettingsCategory {
    THEME_AND_BUTTON,
    CALENDAR_AND_WIDGET,
    FONT_SCALE,
    NOTIFICATIONS,
    CLOUD_SYNC
}

/**
 * 個人化設定管理中心對話框（採用分類摺疊架構，點擊分類後展開設定）
 *
 * 整合五大分類模組：
 * 1. 主題與按鍵外觀（淺／深／系統模式、自訂按鍵顏色與全域文字高對比自動防重色）
 * 2. 月曆與桌面小工具（小工具月曆日期字體大小、App 月曆視覺樣式與選取底色）
 * 3. 全局字體大小（即時全局縮放與預覽）
 * 4. 提醒與系統通知（通知總開關與 Android 8.0+ / 13+ 權限狀態檢查）
 * 5. Google 雲端硬碟同步（連線狀態檢視、一鍵同步與連線設定入口）
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
    onDismiss: () -> Unit,
    widgetDateFontSize: CalendarWidgetDateFontSize = CalendarWidgetDateFontSize.MEDIUM,
    onWidgetDateFontSizeSelected: (CalendarWidgetDateFontSize) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(ReminderScheduler.hasNotificationPermission(context))
    }

    // 預設所有分類皆為收合狀態（null），使用者點擊任一分類後才展開設定內容
    var expandedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    fun toggleCategory(category: SettingsCategory) {
        expandedCategory = if (expandedCategory == category) null else category
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
        personalization.fontScale != AppFontScale.MEDIUM ||
        widgetDateFontSize != CalendarWidgetDateFontSize.MEDIUM

    // 各分類收合時的目前設定摘要文字
    val themeSummary = buildString {
        append(personalization.themeMode.label)
        append("・")
        if (personalization.buttonColorLong != null) {
            append("自訂按鍵色 (${ColorContrastUtils.formatHexColor(personalization.buttonColorLong)})")
        } else {
            append("預設按鍵色")
        }
    }
    val calendarAndWidgetSummary =
        "小工具字體：${widgetDateFontSize.label}・月曆：${personalization.calendarStyle.label}"
    val fontScaleSummary = "目前比例：${personalization.fontScale.label}"
    val notificationSummary = when {
        !notificationsEnabled -> "已關閉提醒通知"
        !hasPermission -> "已開啟（⚠️ 尚未授與系統通知權限）"
        else -> "已開啟・系統通知權限已就緒"
    }
    val cloudSyncSummary = when {
        syncUiState.isSyncing -> "正在同步資料..."
        syncUiState.isCloudConfigured -> {
            val pendingInfo = if (syncUiState.pendingCount > 0) {
                "待上傳 ${syncUiState.pendingCount} 筆"
            } else {
                "無待同步異動"
            }
            "已連結 Google 帳號・$pendingInfo"
        }
        else -> "尚未連結 Google 帳號"
    }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 頂部標題與一鍵重設
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚙️ 設定中心",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "請點選下方分類展開並調整設定",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

                Spacer(modifier = Modifier.height(2.dp))

                // === 分類 1：主題與按鍵外觀 ===
                SettingsCategorySection(
                    icon = Icons.Default.Palette,
                    title = "主題與按鍵外觀",
                    summary = themeSummary,
                    expanded = expandedCategory == SettingsCategory.THEME_AND_BUTTON,
                    onToggle = { toggleCategory(SettingsCategory.THEME_AND_BUTTON) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
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

                        // 1B. 自訂按鍵顏色（含對比度自動防重色與 Hex 輸入）
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
                    }
                }

                // === 分類 2：月曆與桌面小工具（含小工具月曆日期字體大小調整） ===
                SettingsCategorySection(
                    icon = Icons.Default.CalendarMonth,
                    title = "月曆與桌面小工具",
                    summary = calendarAndWidgetSummary,
                    expanded = expandedCategory == SettingsCategory.CALENDAR_AND_WIDGET,
                    onToggle = { toggleCategory(SettingsCategory.CALENDAR_AND_WIDGET) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 2A. 桌面小工具月曆日期字體大小設定
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
                                        text = "小工具月曆日期字體大小",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${widgetDateFontSize.dayNumberSp}sp",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CalendarWidgetDateFontSize.entries.forEach { sizeOption ->
                                        val selected = sizeOption == widgetDateFontSize
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onWidgetDateFontSizeSelected(sizeOption) },
                                            label = {
                                                Text(
                                                    text = sizeOption.label,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Text(
                                    text = "同步套用至桌面月曆小工具（國曆：${widgetDateFontSize.dayNumberSp}sp ／ 農曆：${widgetDateFontSize.lunarSp}sp）",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // 小工具日期字級即時預覽列
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "14",
                                                fontSize = widgetDateFontSize.dayNumberSp.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD32F2F)
                                            )
                                            Text(
                                                text = "初五",
                                                fontSize = widgetDateFontSize.lunarSp.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "15",
                                                fontSize = widgetDateFontSize.dayNumberSp.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "中秋節",
                                                fontSize = widgetDateFontSize.lunarSp.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "16",
                                                fontSize = widgetDateFontSize.dayNumberSp.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "十七",
                                                fontSize = widgetDateFontSize.lunarSp.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2B. App 內月曆視覺樣式與重點色
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
                                    text = "App 月曆視覺樣式",
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

                                // 迷你月曆即時預覽（驗證週日紅／節慶紅與選日底色防重色反差）
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
                                                    text = "穀雨",
                                                    fontSize = 10.sp,
                                                    color = calendarPreviewPalette.resolveLunarLabelColor(
                                                        isSelected = false,
                                                        hasFestival = false,
                                                        hasSolarTerm = true
                                                    )
                                                )
                                            }
                                        }

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
                }

                // === 分類 3：全局字體大小 ===
                SettingsCategorySection(
                    icon = Icons.Default.FormatSize,
                    title = "全局字體大小",
                    summary = fontScaleSummary,
                    expanded = expandedCategory == SettingsCategory.FONT_SCALE,
                    onToggle = { toggleCategory(SettingsCategory.FONT_SCALE) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    text = "所有記事內容、待辦清單、月曆與提醒字體皆已依此比例調整。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // === 分類 4：提醒與系統通知 ===
                SettingsCategorySection(
                    icon = if (notificationsEnabled) {
                        Icons.Default.Notifications
                    } else {
                        Icons.Default.NotificationsOff
                    },
                    iconTint = if (notificationsEnabled && hasPermission) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    title = "提醒與系統通知",
                    summary = notificationSummary,
                    expanded = expandedCategory == SettingsCategory.NOTIFICATIONS,
                    onToggle = { toggleCategory(SettingsCategory.NOTIFICATIONS) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                Column(modifier = Modifier.weight(1f)) {
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
                                                val settingsIntent =
                                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
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
                }

                // === 分類 5：Google 雲端硬碟同步 ===
                SettingsCategorySection(
                    icon = when {
                        syncUiState.isSyncing -> Icons.Default.CloudSync
                        !syncUiState.isOnline ||
                            !syncUiState.syncSettings.syncEnabled ||
                            !syncUiState.isCloudConfigured -> Icons.Default.CloudOff
                        else -> Icons.Default.CloudDone
                    },
                    iconTint = if (syncUiState.isOnline && syncUiState.isCloudConfigured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    title = "Google 雲端硬碟同步",
                    summary = cloudSyncSummary,
                    expanded = expandedCategory == SettingsCategory.CLOUD_SYNC,
                    onToggle = { toggleCategory(SettingsCategory.CLOUD_SYNC) }
                ) {
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
                            val cloudStateText = if (syncUiState.isCloudConfigured) {
                                val accountLabel = syncUiState.syncSettings.googleAccountEmail.ifBlank { "Google 雲端硬碟" }
                                "Google 雲端硬碟（$accountLabel）"
                            } else {
                                "尚未連結 Google 帳號"
                            }
                            val pendingText = if (syncUiState.pendingCount > 0) {
                                "待上傳 ${syncUiState.pendingCount} 筆"
                            } else {
                                "無待同步異動"
                            }
                            val lastSyncText = syncUiState.syncSettings.lastSyncAt?.let {
                                DateFormatter.formatDateTime(it)
                            } ?: "尚未同步"

                            Text(
                                text = "🌐 網路：$networkStateText  ｜  ☁️ 雲端：$cloudStateText",
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

                Spacer(modifier = Modifier.height(4.dp))

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
 * 可摺疊之設定分類卡片元件（點擊標題列後展開或收合該分類底下的細項設定）
 */
@Composable
private fun SettingsCategorySection(
    icon: ImageVector,
    title: String,
    summary: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (expanded) 1.5.dp else 1.dp,
            color = if (expanded) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        color = if (expanded) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "收合分類" else "展開分類",
                    tint = if (expanded) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    content()
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
