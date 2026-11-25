package com.example.notepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.ui.components.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 時間規畫表主畫面（整合每日時間規畫與專注番茄鐘雙分頁，支援日期切換、進度統計、行程增刪改與一鍵連動番茄鐘專注）
 */
@Composable
fun ScheduleScreen(
    taskRepository: TaskRepository,
    initialTab: Int? = null,
    onConsumeInitialTab: () -> Unit = {},
    initialPomodoroTaskTitle: String? = null,
    onStartPomodoroForTask: ((String) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0: ⏰ 時間規畫, 1: 🍅 番茄鐘
    var selectedTab by remember { mutableIntStateOf(0) }
    var pomodoroTaskTitle by remember { mutableStateOf(initialPomodoroTaskTitle ?: "自由專注") }

    LaunchedEffect(initialTab) {
        val targetTab = initialTab ?: return@LaunchedEffect
        selectedTab = targetTab.coerceIn(0, 1)
        onConsumeInitialTab()
    }

    LaunchedEffect(initialPomodoroTaskTitle) {
        if (!initialPomodoroTaskTitle.isNullOrEmpty()) {
            pomodoroTaskTitle = initialPomodoroTaskTitle
        }
    }

    // Date state (in start-of-day millis)
    var selectedDateMillis by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }

    val selectedCal = remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
    }
    val year = selectedCal.get(Calendar.YEAR)
    val month = selectedCal.get(Calendar.MONTH) + 1
    val day = selectedCal.get(Calendar.DAY_OF_MONTH)
    val dayOfWeek = when (selectedCal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> "週日"
        Calendar.MONDAY -> "週一"
        Calendar.TUESDAY -> "週二"
        Calendar.WEDNESDAY -> "週三"
        Calendar.THURSDAY -> "週四"
        Calendar.FRIDAY -> "週五"
        Calendar.SATURDAY -> "週六"
        else -> ""
    }

    val timeBlocks by taskRepository.getTimeBlocksByDate(selectedDateMillis)
        .collectAsState(initial = emptyList())

    val totalCount = timeBlocks.size
    val completedCount = timeBlocks.count { it.isCompleted }
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    var showAddDialog by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<TimeBlock?>(null) }
    var deletingBlock by remember { mutableStateOf<TimeBlock?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 時間規畫表內建分頁列：將「時間規畫」與「番茄鐘」整合於同一入口
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⏰ 時間規畫 ($totalCount)")
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🍅 番茄鐘")
                    }
                }
            )
        }

        if (selectedTab == 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                PomodoroScreen(
                    taskRepository = taskRepository,
                    initialTaskTitle = pomodoroTaskTitle
                )
            }
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = AppButtonThemeDefaults.fabContainerColor(),
                        contentColor = AppButtonThemeDefaults.fabContentColor(),
                        icon = { Icon(Icons.Default.Add, contentDescription = "新增時間段") },
                        text = { Text("安排行程") }
                    )
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- Day Navigator Header ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        val c = Calendar.getInstance().apply {
                                            timeInMillis = selectedDateMillis
                                            add(Calendar.DAY_OF_YEAR, -1)
                                        }
                                        selectedDateMillis = c.timeInMillis
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "前一天",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${year}年${month}月${day}日 ($dayOfWeek)",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "每日時間規畫表",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }

                                    IconButton(onClick = {
                                        val c = Calendar.getInstance().apply {
                                            timeInMillis = selectedDateMillis
                                            add(Calendar.DAY_OF_YEAR, 1)
                                        }
                                        selectedDateMillis = c.timeInMillis
                                    }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "後一天",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                // Progress summary
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "今日完成度：$completedCount / $totalCount",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }

                    // --- Time Blocks List ---
                    if (timeBlocks.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "無時間安排",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "今日尚未安排任何時間規劃",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "點擊右下角「+ 安排行程」按鈕規劃今日時間表",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    } else {
                        items(timeBlocks, key = { it.id }) { block ->
                            TimeBlockCard(
                                timeBlock = block,
                                onCheckedChange = { checked ->
                                    coroutineScope.launch {
                                        try {
                                            taskRepository.updateTimeBlock(block.copy(isCompleted = checked))
                                            showFeedback(
                                                context,
                                                if (checked) "已完成行程「${block.title}」" else "已將「${block.title}」標記為未完成"
                                            )
                                        } catch (e: Exception) {
                                            showFeedback(context, "狀態更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                                        }
                                    }
                                },
                                onEdit = {
                                    editingBlock = block
                                },
                                onDelete = {
                                    deletingBlock = block
                                },
                                onStartPomodoro = {
                                    pomodoroTaskTitle = block.title
                                    selectedTab = 1
                                    onStartPomodoroForTask?.invoke(block.title)
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // --- Add Time Block Dialog ---
    if (showAddDialog) {
        AddTimeBlockDialog(
            dateMillis = selectedDateMillis,
            onDismiss = { showAddDialog = false },
            onSave = { newBlock ->
                coroutineScope.launch {
                    try {
                        taskRepository.insertTimeBlock(newBlock)
                        showFeedback(context, "已成功安排行程")
                        showAddDialog = false
                    } catch (e: Exception) {
                        showFeedback(context, "安排行程失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            }
        )
    }

    // --- Dialog: Edit Time Block ---
    editingBlock?.let { blockToEdit ->
        EditTimeBlockDialog(
            timeBlock = blockToEdit,
            onDismiss = { editingBlock = null },
            onSave = { updated ->
                coroutineScope.launch {
                    try {
                        taskRepository.updateTimeBlock(updated)
                        showFeedback(context, "行程已更新")
                        editingBlock = null
                    } catch (e: Exception) {
                        showFeedback(context, "更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            }
        )
    }

    // --- Dialog: Confirm Delete Time Block ---
    deletingBlock?.let { block ->
        ConfirmDeleteDialog(
            itemName = block.title,
            onConfirm = {
                coroutineScope.launch {
                    try {
                        taskRepository.deleteTimeBlock(block)
                        showFeedback(context, "已刪除行程")
                        deletingBlock = null
                    } catch (e: Exception) {
                        showFeedback(context, "刪除失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onDismiss = { deletingBlock = null }
        )
    }
}

@Composable
fun TimeBlockCard(
    timeBlock: TimeBlock,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onStartPomodoro: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (timeBlock.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = timeBlock.isCompleted,
                onCheckedChange = onCheckedChange
            )

            // Timeline bar / color strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(timeBlock.color))
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${timeBlock.startTime} - ${timeBlock.endTime}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ColorTag(
                        text = timeBlock.tag,
                        tagColor = Color(timeBlock.color)
                    )
                }

                Text(
                    text = timeBlock.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (timeBlock.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (timeBlock.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )

                if (timeBlock.description.isNotEmpty()) {
                    Text(
                        text = timeBlock.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions: Edit, Pomodoro, Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "編輯",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onStartPomodoro,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(text = "🍅", style = MaterialTheme.typography.titleMedium)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "刪除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
