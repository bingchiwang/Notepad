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
import com.example.notepad.ui.components.ColorPickerRow
import com.example.notepad.ui.components.ColorTag
import com.example.notepad.ui.components.PRESET_COLORS
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun ScheduleScreen(
    taskRepository: TaskRepository,
    onStartPomodoroForTask: ((String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()

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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
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
                                    text = "每日時間規劃表",
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
                                taskRepository.updateTimeBlock(block.copy(isCompleted = checked))
                            }
                        },
                        onDelete = {
                            coroutineScope.launch {
                                taskRepository.deleteTimeBlock(block)
                            }
                        },
                        onStartPomodoro = {
                            onStartPomodoroForTask?.invoke(block.title)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // --- Add Time Block Dialog ---
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var startTime by remember { mutableStateOf("09:00") }
        var endTime by remember { mutableStateOf("10:00") }
        var tag by remember { mutableStateOf("工作") }
        var description by remember { mutableStateOf("") }
        var selectedColor by remember { mutableLongStateOf(PRESET_COLORS[1].colorLong) } // Forest Green

        val quickTags = listOf("工作", "學習", "會議", "運動", "閱讀", "休息", "生活")

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "安排每日時間區塊",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("活動名稱 *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("開始時間") },
                            placeholder = { Text("09:00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("結束時間") },
                            placeholder = { Text("10:00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Quick Tag Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "分類標籤：",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickTags.take(4).forEach { t ->
                                FilterChip(
                                    selected = tag == t,
                                    onClick = { tag = t },
                                    label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Color Picker
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "色彩標記：",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        ColorPickerRow(
                            selectedColor = selectedColor,
                            onColorSelected = { selectedColor = it }
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("備註 (選填)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    coroutineScope.launch {
                                        taskRepository.insertTimeBlock(
                                            TimeBlock(
                                                date = selectedDateMillis,
                                                startTime = startTime.trim(),
                                                endTime = endTime.trim(),
                                                title = title.trim(),
                                                description = description.trim(),
                                                tag = tag.trim().ifEmpty { "日程" },
                                                color = selectedColor
                                            )
                                        )
                                        showAddDialog = false
                                    }
                                }
                            },
                            enabled = title.isNotBlank()
                        ) {
                            Text("儲存")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeBlockCard(
    timeBlock: TimeBlock,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onStartPomodoro: () -> Unit,
    modifier: Modifier = Modifier
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

            // Quick Pomodoro & Delete action
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStartPomodoro,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(text = "🍅", style = MaterialTheme.typography.titleMedium)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
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
