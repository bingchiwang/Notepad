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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
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
 * 月曆桌面小工具設定頁面（支援首次加入桌面及從系統長按設定重新配置農曆日期顯示選項）
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

        setContent {
            val personalization by themePreferences.personalizationFlow.collectAsState(
                initial = PersonalizationSettings()
            )
            val useDarkTheme = personalization.themeMode.shouldUseDarkTheme(isSystemInDarkTheme())
            var selectedShowLunar by remember { mutableStateOf(initialShowLunar) }

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

            fun saveAndApply(showLunar: Boolean) {
                lifecycleScope.launch {
                    widgetPrefs.setShowLunarDate(appWidgetId, showLunar)
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
                    showFeedback(
                        this@CalendarWidgetConfigureActivity,
                        if (showLunar) "月曆小工具已啟用農曆日期顯示" else "月曆小工具已設為僅顯示國曆日期"
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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

                        Text(
                            text = "日期均採「置中靠上」排版；您可選擇是否在格內同時顯示農曆日期、節慶與二十四節氣（亦可在桌面點擊頂部「農曆」按鈕隨時切換）。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 即時預覽格子效果（置中靠上 + 農曆切換對比）
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
                                    text = "格子排版即時預覽",
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
                                        lunarText = if (selectedShowLunar) "十月初六" else null,
                                        isToday = true,
                                        sampleEventTitle = "完成企劃書"
                                    )
                                    PreviewDayCellCard(
                                        modifier = Modifier.weight(1f),
                                        dayNumber = "22",
                                        lunarText = if (selectedShowLunar) "小雪" else null,
                                        isToday = false,
                                        isLunarHighlight = true,
                                        sampleEventTitle = "讀書筆記"
                                    )
                                }
                            }
                        }

                        LunarOptionCard(
                            title = "顯示國曆與農曆日期（推薦）",
                            subtitle = "在置中靠上的國曆日號下方顯示農曆初一～三十、傳統節慶與節氣",
                            isSelected = selectedShowLunar,
                            onClick = { selectedShowLunar = true }
                        )

                        LunarOptionCard(
                            title = "僅顯示國曆日期",
                            subtitle = "隱藏農曆文字，保留較大的記事與待辦事項文字呈現空間",
                            isSelected = !selectedShowLunar,
                            onClick = { selectedShowLunar = false }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { saveAndApply(selectedShowLunar) },
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
    isToday: Boolean,
    sampleEventTitle: String,
    modifier: Modifier = Modifier,
    isLunarHighlight: Boolean = false
) {
    Surface(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isToday) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isToday) 1.5.dp else 1.dp,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
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
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (lunarText != null) {
                Text(
                    text = lunarText,
                    fontSize = 10.sp,
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
