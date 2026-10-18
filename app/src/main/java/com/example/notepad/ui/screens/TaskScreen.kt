package com.example.notepad.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.local.entity.ToDo
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.ui.components.ColorPickerRow
import com.example.notepad.ui.components.ColorTag
import com.example.notepad.ui.components.DeadlineAlertCard
import com.example.notepad.ui.components.PRESET_COLORS
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TaskScreen(
    taskRepository: TaskRepository,
    calendarFacade: LunarCalendarFacade,
    onTaskClick: (Int) -> Unit,
    onStartPomodoro: ((String) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val toDos by taskRepository.allToDos.collectAsState(initial = emptyList())

    var filterMode by remember { mutableIntStateOf(0) } // 0: 全部, 1: 進行中, 2: 已完成
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredToDos = remember(toDos, filterMode) {
        when (filterMode) {
            1 -> toDos.filter { !it.isCompleted }
            2 -> toDos.filter { it.isCompleted }
            else -> toDos
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "新增待做事項") },
                text = { Text("新增待辦") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMode == 0,
                    onClick = { filterMode = 0 },
                    label = { Text("全部 (${toDos.size})") }
                )
                FilterChip(
                    selected = filterMode == 1,
                    onClick = { filterMode = 1 },
                    label = { Text("進行中 (${toDos.count { !it.isCompleted }})") }
                )
                FilterChip(
                    selected = filterMode == 2,
                    onClick = { filterMode = 2 },
                    label = { Text("已完成 (${toDos.count { it.isCompleted }})") }
                )
            }

            if (filteredToDos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (filterMode) {
                            1 -> "太棒了！目前沒有未完成的待做事項 🎉"
                            2 -> "尚未有任何完成的紀錄"
                            else -> "目前尚無任何待做事項，點擊右下角按鈕新增！"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredToDos, key = { it.id }) { toDo ->
                        val isDueTomorrow = toDo.dueDate?.let { calendarFacade.isTomorrow(it) } ?: false
                        val dueDateString = toDo.dueDate?.let {
                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                            "📅 到期日: ${sdf.format(Date(it))}"
                        }

                        DeadlineAlertCard(
                            title = toDo.title,
                            description = toDo.description,
                            tag = toDo.tag,
                            tagColor = Color(toDo.color),
                            isDueTomorrow = isDueTomorrow,
                            isCompleted = toDo.isCompleted,
                            dueDateText = dueDateString,
                            onClick = { onTaskClick(toDo.id) },
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    taskRepository.updateToDo(toDo.copy(isCompleted = checked))
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    taskRepository.deleteToDo(toDo)
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }
    }

    // Add ToDo Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var tag by remember { mutableStateOf("日常") }
        var selectedColor by remember { mutableLongStateOf(PRESET_COLORS.first().colorLong) }
        var setDueTomorrow by remember { mutableStateOf(false) }

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
                        text = "新增待做事項",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("標題 *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("詳細說明 (選填)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tag,
                        onValueChange = { tag = it },
                        label = { Text("分類標籤") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Tomorrow Due Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = setDueTomorrow,
                            onCheckedChange = { setDueTomorrow = it }
                        )
                        Text(
                            text = "設為明日到期（觸發橘紅呼吸外框警示）",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                                    val dueMillis = if (setDueTomorrow) {
                                        Calendar.getInstance().apply {
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                            add(Calendar.DAY_OF_YEAR, 1)
                                        }.timeInMillis
                                    } else {
                                        null
                                    }

                                    coroutineScope.launch {
                                        taskRepository.insertToDo(
                                            ToDo(
                                                title = title.trim(),
                                                description = description.trim(),
                                                dueDate = dueMillis,
                                                tag = tag.trim().ifEmpty { "日常" },
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
fun TaskDetailScreen(
    taskId: Int,
    taskRepository: TaskRepository
) {
    val coroutineScope = rememberCoroutineScope()
    val toDos by taskRepository.allToDos.collectAsState(initial = emptyList())
    val task = toDos.find { it.id == taskId }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (task != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorTag(text = task.tag, tagColor = Color(task.color))
                    IconButton(onClick = {
                        coroutineScope.launch {
                            taskRepository.deleteToDo(task)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "刪除事項",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                task.dueDate?.let {
                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    Text(
                        text = "📅 到期日: ${sdf.format(Date(it))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { checked ->
                            coroutineScope.launch {
                                taskRepository.updateToDo(task.copy(isCompleted = checked))
                            }
                        }
                    )
                    Text(
                        text = if (task.isCompleted) "已標記為完成" else "尚未完成",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "請於左側清單選擇待辦事項",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
