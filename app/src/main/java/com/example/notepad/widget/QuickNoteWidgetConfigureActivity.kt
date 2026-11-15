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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.notepad.data.local.AppDatabase
import com.example.notepad.data.preferences.PersonalizationSettings
import com.example.notepad.data.preferences.ThemePreferences
import com.example.notepad.ui.components.showFeedback
import com.example.notepad.ui.theme.NotepadTheme
import kotlinx.coroutines.launch

/**
 * 快速記事桌面小工具設定頁面（支援首次加入桌面及後續點擊分類標籤重新設定顯示分類）
 */
class QuickNoteWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 預設結果設為 CANCELED，確保使用者按返回鍵取消新增時不會在桌面產生空白小工具
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val database = AppDatabase.getDatabase(this)
        val themePreferences = ThemePreferences(this)
        val widgetPrefs = QuickNoteWidgetPreferences(this)
        val initialFilter = widgetPrefs.getFilter(appWidgetId)

        setContent {
            val personalization by themePreferences.personalizationFlow.collectAsState(
                initial = PersonalizationSettings()
            )
            val useDarkTheme = personalization.themeMode.shouldUseDarkTheme(isSystemInDarkTheme())
            val categories by database.categoryDao().getAllCategories().collectAsState(initial = emptyList())
            val notes by database.noteDao().getAllNotes().collectAsState(initial = emptyList())

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
                                imageVector = Icons.Default.Widgets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "設定快速記事小工具分類",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "請選擇此桌面小工具要顯示的記事分類。設定後點擊小工具上的「新增」也會自動帶入所選分類。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        fun applySelection(filter: WidgetCategoryFilter, label: String) {
                            lifecycleScope.launch {
                                widgetPrefs.setFilter(appWidgetId, filter)
                                val appWidgetManager = AppWidgetManager.getInstance(this@QuickNoteWidgetConfigureActivity)
                                if (appWidgetManager != null) {
                                    QuickNoteWidgetProvider.updateWidgetsInternal(
                                        context = this@QuickNoteWidgetConfigureActivity,
                                        appWidgetManager = appWidgetManager,
                                        appWidgetIds = intArrayOf(appWidgetId)
                                    )
                                }
                                val resultValue = Intent().apply {
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                }
                                setResult(RESULT_OK, resultValue)
                                showFeedback(
                                    this@QuickNoteWidgetConfigureActivity,
                                    "小工具已設定顯示「$label」"
                                )
                                finish()
                            }
                        }

                        val uncategorizedCount = remember(notes) { notes.count { it.categoryId == null } }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                CategoryOptionCard(
                                    title = "全部記事",
                                    subtitle = "顯示所有分類與未分類的最近記事（共 ${notes.size} 筆）",
                                    dotColor = MaterialTheme.colorScheme.primary,
                                    isSelected = initialFilter is WidgetCategoryFilter.All,
                                    onClick = {
                                        applySelection(WidgetCategoryFilter.All, "全部記事")
                                    }
                                )
                            }

                            item {
                                CategoryOptionCard(
                                    title = "未分類",
                                    subtitle = "僅顯示尚未歸類的記事（共 $uncategorizedCount 筆）",
                                    dotColor = MaterialTheme.colorScheme.outline,
                                    isSelected = initialFilter is WidgetCategoryFilter.Uncategorized,
                                    onClick = {
                                        applySelection(WidgetCategoryFilter.Uncategorized, "未分類")
                                    }
                                )
                            }

                            if (categories.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderOpen,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = "目前尚無自訂分類，可先選擇「全部記事」或稍後在記事頁新增分類再回來切換。",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(categories, key = { it.id }) { category ->
                                    val count = notes.count { it.categoryId == category.id }
                                    val isSelected = initialFilter is WidgetCategoryFilter.CategoryFilter &&
                                        initialFilter.categoryId == category.id
                                    CategoryOptionCard(
                                        title = category.name,
                                        subtitle = "顯示「${category.name}」分類中的最近記事（共 $count 筆）",
                                        dotColor = Color(category.color),
                                        isSelected = isSelected,
                                        onClick = {
                                            applySelection(
                                                WidgetCategoryFilter.CategoryFilter(category.id),
                                                category.name
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryOptionCard(
    title: String,
    subtitle: String,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "目前顯示中",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
