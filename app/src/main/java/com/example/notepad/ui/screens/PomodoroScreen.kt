package com.example.notepad.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.notepad.data.local.entity.PomodoroRecord
import com.example.notepad.data.repository.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PomodoroMode(val title: String, val minutes: Int, val color: Color) {
    FOCUS("專注工作", 25, Color(0xFFE53935)),
    SHORT_BREAK("短暫休息", 5, Color(0xFF43A047)),
    LONG_BREAK("深度休息", 15, Color(0xFF1E88E5))
}

@Composable
fun PomodoroScreen(
    taskRepository: TaskRepository,
    initialTaskTitle: String? = null
) {
    val coroutineScope = rememberCoroutineScope()

    // Today's midnight millis
    val todayMillis = remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        cal.timeInMillis
    }

    val todayCount by taskRepository.getTodayPomodoroCount(todayMillis)
        .collectAsState(initial = 0)
    val todayRecords by taskRepository.getPomodoroRecordsByDate(todayMillis)
        .collectAsState(initial = emptyList())
    val allToDos by taskRepository.allToDos
        .collectAsState(initial = emptyList())

    var currentMode by remember { mutableStateOf(PomodoroMode.FOCUS) }
    var isRunning by remember { mutableStateOf(false) }
    var timeLeftSeconds by remember { mutableIntStateOf(currentMode.minutes * 60) }
    var taskTitle by remember { mutableStateOf(initialTaskTitle ?: "自由專注") }
    var showSelectTaskDialog by remember { mutableStateOf(false) }

    // When initialTaskTitle changes
    LaunchedEffect(initialTaskTitle) {
        if (!initialTaskTitle.isNullOrEmpty()) {
            taskTitle = initialTaskTitle
        }
    }

    // Switch mode
    fun selectMode(mode: PomodoroMode) {
        currentMode = mode
        isRunning = false
        timeLeftSeconds = mode.minutes * 60
    }

    // Timer countdown effect
    LaunchedEffect(isRunning, timeLeftSeconds) {
        if (isRunning && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds -= 1
        } else if (isRunning && timeLeftSeconds <= 0) {
            isRunning = false
            if (currentMode == PomodoroMode.FOCUS) {
                // Save completed pomodoro record
                coroutineScope.launch {
                    taskRepository.insertPomodoroRecord(
                        PomodoroRecord(
                            timestamp = System.currentTimeMillis(),
                            durationMinutes = PomodoroMode.FOCUS.minutes,
                            taskTitle = taskTitle,
                            date = todayMillis
                        )
                    )
                }
                // Switch to short break
                selectMode(PomodoroMode.SHORT_BREAK)
            } else {
                // Switch back to focus
                selectMode(PomodoroMode.FOCUS)
            }
        }
    }

    val totalSeconds = currentMode.minutes * 60
    val progress = (timeLeftSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "pomodoro_progress")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Mode Selector Segmented Chips ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PomodoroMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectMode(mode) },
                        label = {
                            Text(
                                text = "${mode.title} (${mode.minutes}m)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = mode.color.copy(alpha = 0.2f),
                            selectedLabelColor = mode.color
                        )
                    )
                }
            }
        }

        // --- Current Task Association Card ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSelectTaskDialog = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🎯", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "正在專注的目標",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = taskTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    TextButton(onClick = { showSelectTaskDialog = true }) {
                        Text("變更")
                    }
                }
            }
        }

        // --- Circular Progress Timer Dial ---
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp,
                    trackColor = Color.Transparent
                )

                // Active Progress
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = currentMode.color,
                    strokeWidth = 12.dp,
                    trackColor = Color.Transparent
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val minutes = timeLeftSeconds / 60
                    val seconds = timeLeftSeconds % 60
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isRunning) "計時中..." else "已暫停",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunning) currentMode.color else MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // --- Timer Control Buttons ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                OutlinedIconButton(
                    onClick = {
                        isRunning = false
                        timeLeftSeconds = currentMode.minutes * 60
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重設",
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Play / Pause Button
                Button(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier.height(56.dp).width(140.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = currentMode.color),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "暫停" else "開始",
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "暫停" else "開始",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Skip Button
                OutlinedIconButton(
                    onClick = {
                        isRunning = false
                        if (currentMode == PomodoroMode.FOCUS) {
                            selectMode(PomodoroMode.SHORT_BREAK)
                        } else {
                            selectMode(PomodoroMode.FOCUS)
                        }
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "跳過",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // --- Today's Focus Stats Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🍅 今日完成",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$todayCount 顆番茄",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⏱️ 專注時長",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${todayCount * 25} 分鐘",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // --- Today's Sessions History ---
        if (todayRecords.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = "📜 今日專注紀錄",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(todayRecords, key = { it.id }) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🍅", fontSize = 18.sp)
                            Column {
                                Text(
                                    text = record.taskTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                Text(
                                    text = "完成於 ${timeFormat.format(Date(record.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Text(
                            text = "+${record.durationMinutes} 分鐘",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // --- Dialog: Select or Input Focus Task ---
    if (showSelectTaskDialog) {
        var customInput by remember { mutableStateOf("") }
        val uncompletedTasks = remember(allToDos) { allToDos.filter { !it.isCompleted } }

        Dialog(onDismissRequest = { showSelectTaskDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "設定專注目標",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { customInput = it },
                        label = { Text("自訂專注事項") },
                        placeholder = { Text("例如: 閱讀 30 頁書、複習演算法") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uncompletedTasks.isNotEmpty()) {
                        Text(
                            text = "或從待做清單中選取：",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(uncompletedTasks) { todo ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            taskTitle = todo.title
                                            showSelectTaskDialog = false
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "📋 ${todo.title}",
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSelectTaskDialog = false }) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customInput.isNotBlank()) {
                                    taskTitle = customInput.trim()
                                }
                                showSelectTaskDialog = false
                            }
                        ) {
                            Text("確認")
                        }
                    }
                }
            }
        }
    }
}
