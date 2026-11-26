package com.example.notepad.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.notepad.data.preferences.PersonalizationSettings
import com.example.notepad.data.preferences.ThemePreferences
import com.example.notepad.ui.components.AppButtonThemeDefaults
import com.example.notepad.ui.components.showFeedback
import com.example.notepad.ui.theme.NotepadTheme
import kotlinx.coroutines.launch

/**
 * 月曆桌面小工具設定頁面（支援首次加入桌面、長按設定或點擊小工具左上角圖示配置日期字體大小與農曆顯示）
 */
class CalendarWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val themePreferences = ThemePreferences(this)
        val widgetPrefs = CalendarWidgetPreferences(this)
        val initialShowLunar = widgetPrefs.getShowLunarDate(appWidgetId)
        val initialDateFontSize = widgetPrefs.getDateFontSize(appWidgetId)

        setContent {
            val personalization by themePreferences.personalizationFlow.collectAsState(
                initial = PersonalizationSettings()
            )
            val useDarkTheme = personalization.themeMode.shouldUseDarkTheme(isSystemInDarkTheme())
            var selectedShowLunar by remember { mutableStateOf(initialShowLunar) }
            var selectedDateFontSize by remember { mutableStateOf(initialDateFontSize) }

            LaunchedEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT
                    ) { useDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT
                    ) { useDarkTheme }
                )
            }

            fun saveAndApply(
                showLunar: Boolean,
                dateFontSize: CalendarWidgetDateFontSize
            ) {
                lifecycleScope.launch {
                    widgetPrefs.setShowLunarDate(appWidgetId, showLunar)
                    widgetPrefs.setDateFontSize(appWidgetId, dateFontSize)
                    val appWidgetManager = AppWidgetManager.getInstance(this@CalendarWidgetConfigureActivity)
                    if (appWidgetManager != null) {
                        CalendarWidgetProvider.updateWidgetsInternal(
                            context = this@CalendarWidgetConfigureActivity,
                            appWidgetManager = appWidgetManager,
                            appWidgetIds = intArrayOf(appWidgetId)
                        )
                    }
                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(RESULT_OK, resultValue)
                    val lunarSummary = if (showLunar) "顯示農曆" else "僅國曆"
                    showFeedback(
                        this@CalendarWidgetConfigureActivity,
                        "已套用月曆小工具設定（日期字體：${dateFontSize.label}・$lunarSummary）"
                    )
                    finish()
                }
            }

            NotepadTheme(
                darkTheme = useDarkTheme,
                personalization = personalization
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .displayCutoutPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "取消"
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "設定月曆桌面小工具",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "可自由調整小工具月曆的「日期字體大小」及是否顯示「台灣農曆、節慶與二十四節氣」。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 即時預覽格子效果（日期字級縮放 + 農曆切換對比）
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "格子排版與字級即時預覽（目前：${selectedDateFontSize.label}）",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        PreviewDayCellCard(
                                            modifier = Modifier.weight(1f),
                                            dayNumber = "15",
                                            lunarText = if (selectedShowLunar) "初六" else null,
                                            dateFontSize = selectedDateFontSize,
                                            isToday = false,
                                            isSunday = true,
                                            sampleEventTitle = "週日家庭聚餐"
                                        )
                                        PreviewDayCellCard(
                                            modifier = Modifier.weight(1f),
                                            dayNumber = "16",
                                            lunarText = if (selectedShowLunar) "初七" else null,
                                            dateFontSize = selectedDateFontSize,
                                            isToday = true,
                                            sampleEventTitle = "完成企劃書"
                                        )
                                        PreviewDayCellCard(
                                            modifier = Modifier.weight(1f),
                                            dayNumber = "17",
                                            lunarText = if (selectedShowLunar) "中秋節" else null,
                                            dateFontSize = selectedDateFontSize,
                                            isToday = false,
                                            isLunarHighlight = true,
                                            sampleEventTitle = "讀書筆記"
                                        )
                                    }
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }

                            // 日期字體大小設定區塊
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
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
                                            text = "日期字體大小",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CalendarWidgetDateFontSize.entries.forEach { sizeOption ->
                                            val isSelected = sizeOption == selectedDateFontSize
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedDateFontSize = sizeOption },
                                                label = {
                                                    Text(
                                                        text = sizeOption.label,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "國曆日期：${selectedDateFontSize.dayNumberSp}sp  ｜  農曆副標：${selectedDateFontSize.lunarSp}sp",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            LunarOptionCard(
                                title = "顯示國曆與農曆日期（推薦）",
                                subtitle = "在置中靠上的國曆日號下方顯示農曆初一～三十、台灣節慶與節氣",
                                isSelected = selectedShowLunar,
                                onClick = { selectedShowLunar = true }
                            )

                            LunarOptionCard(
                                title = "僅顯示國曆日期",
                                subtitle = "隱藏農曆文字，保留較大的記事與待辦事項文字呈現空間",
                                isSelected = !selectedShowLunar,
                                onClick = { selectedShowLunar = false }
                            )
                        }

                        Button(
                            onClick = { saveAndApply(selectedShowLunar, selectedDateFontSize) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = AppButtonThemeDefaults.filledColors(),
                            border = AppButtonThemeDefaults.filledBorder(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "套用並完成設定",
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewDayCellCard(
    dayNumber: String,
    lunarText: String?,
    dateFontSize: CalendarWidgetDateFontSize,
    isToday: Boolean,
    sampleEventTitle: String,
    modifier: Modifier = Modifier,
    isLunarHighlight: Boolean = false,
    isSunday: Boolean = false
) {
    Surface(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isToday) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            Color.Transparent
        },
        border = if (isToday) {
            BorderStroke(width = 1.5.dp, color = MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = dayNumber,
                fontSize = dateFontSize.dayNumberSp.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSunday -> Color(0xFFD32F2F)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center
            )
            if (lunarText != null) {
                Text(
                    text = lunarText,
                    fontSize = dateFontSize.lunarSp.sp,
                    color = if (isLunarHighlight) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = sampleEventTitle,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LunarOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "已選取",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
