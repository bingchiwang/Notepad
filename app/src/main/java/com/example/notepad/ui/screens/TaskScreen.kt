package com.example.notepad.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.ToDo
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.reminder.ReminderScheduler
import com.example.notepad.ui.components.*
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val toDos by taskRepository.allToDos.collectAsState(initial = emptyList())
    val reminders by taskRepository.allReminders.collectAsState(initial = emptyList())

    // 0: 📋 待做事件, 1: 🔔 提醒事項
    var selectedTrack by remember { mutableIntStateOf(0) }

    var todoFilterMode by remember { mutableIntStateOf(0) } // 0: 全部, 1: 進行中, 2: 已完成
    var showAddToDoDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    // State for editing
    var editingToDo by remember { mutableStateOf<ToDo?>(null) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    // State for deletion confirmation
    var deletingToDo by remember { mutableStateOf<ToDo?>(null) }
    var deletingReminder by remember { mutableStateOf<Reminder?>(null) }

    // Notification permission launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val filteredToDos = remember(toDos, todoFilterMode) {
        when (todoFilterMode) {
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
                onClick = {
                    if (selectedTrack == 0) {
                        showAddToDoDialog = true
                    } else {
                        showAddReminderDialog = true
                    }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "新增") },
                text = { Text(if (selectedTrack == 0) "新增待辦" else "新增提醒") }
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
            // --- Dual-Track Navigation Segment ---
            PrimaryTabRow(
                selectedTabIndex = selectedTrack,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTrack == 0,
                    onClick = { selectedTrack = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📋 待做事件 (${toDos.count { !it.isCompleted }})")
                        }
                    }
                )
                Tab(
                    selected = selectedTrack == 1,
                    onClick = { selectedTrack = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔔 提醒事項 (${reminders.count { !it.isFired }})")
                        }
                    }
                )
            }

            // === Track 0: To-Dos ===
            if (selectedTrack == 0) {
                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = todoFilterMode == 0,
                        onClick = { todoFilterMode = 0 },
                        label = { Text("全部 (${toDos.size})") }
                    )
                    FilterChip(
                        selected = todoFilterMode == 1,
                        onClick = { todoFilterMode = 1 },
                        label = { Text("進行中 (${toDos.count { !it.isCompleted }})") }
                    )
                    FilterChip(
                        selected = todoFilterMode == 2,
                        onClick = { todoFilterMode = 2 },
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
                            text = when (todoFilterMode) {
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
                                onEdit = {
                                    editingToDo = toDo
                                },
                                onDelete = {
                                    deletingToDo = toDo
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            } else {
                // === Track 1: Reminders ===
                if (reminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "目前尚未設定任何提醒事項",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "點擊右下角「+ 新增提醒」可一次設定多天與通知時間",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reminders, key = { it.id }) { reminder ->
                            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                            val triggerTimeStr = sdf.format(Date(reminder.triggerAt))

                            ReminderCard(
                                title = reminder.title,
                                message = reminder.message,
                                triggerTimeText = triggerTimeStr,
                                tag = reminder.tag,
                                tagColor = Color(reminder.color),
                                isFired = reminder.isFired,
                                onEdit = {
                                    editingReminder = reminder
                                },
                                onDelete = {
                                    deletingReminder = reminder
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    // --- Dialog: Add New ToDo ---
    if (showAddToDoDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var tag by remember { mutableStateOf("日常") }
        var selectedColor by remember { mutableLongStateOf(PRESET_COLORS.first().colorLong) }
        var setDueTomorrow by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddToDoDialog = false }) {
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
                        TextButton(onClick = { showAddToDoDialog = false }) {
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
                                        showAddToDoDialog = false
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

    // --- Dialog: Add Multi-Day Reminder ---
    if (showAddReminderDialog) {
        AddMultiDayReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onSave = { reminderList ->
                coroutineScope.launch {
                    reminderList.forEach { reminder ->
                        val newId = taskRepository.insertReminder(reminder)
                        // Schedule alarm notification
                        ReminderScheduler.scheduleReminder(
                            context,
                            reminder.copy(id = newId.toInt())
                        )
                    }
                    showAddReminderDialog = false
                }
            }
        )
    }

    // --- Dialog: Edit ToDo ---
    editingToDo?.let { todoToEdit ->
        EditToDoDialog(
            toDo = todoToEdit,
            onDismiss = { editingToDo = null },
            onSave = { updatedToDo ->
                coroutineScope.launch {
                    taskRepository.updateToDo(updatedToDo)
                    editingToDo = null
                }
            }
        )
    }

    // --- Dialog: Edit Reminder ---
    editingReminder?.let { reminderToEdit ->
        EditReminderDialog(
            reminder = reminderToEdit,
            onDismiss = { editingReminder = null },
            onSave = { updatedReminder ->
                coroutineScope.launch {
                    taskRepository.updateReminder(updatedReminder)
                    // Reschedule alarm
                    ReminderScheduler.cancelReminder(context, updatedReminder.id)
                    ReminderScheduler.scheduleReminder(context, updatedReminder)
                    editingReminder = null
                }
            }
        )
    }

    // --- Dialog: Confirm Delete ToDo ---
    deletingToDo?.let { todo ->
        ConfirmDeleteDialog(
            itemName = todo.title,
            onConfirm = {
                coroutineScope.launch {
                    taskRepository.deleteToDo(todo)
                    deletingToDo = null
                }
            },
            onDismiss = { deletingToDo = null }
        )
    }

    // --- Dialog: Confirm Delete Reminder ---
    deletingReminder?.let { reminder ->
        ConfirmDeleteDialog(
            itemName = reminder.title.ifEmpty { "提醒" },
            onConfirm = {
                coroutineScope.launch {
                    ReminderScheduler.cancelReminder(context, reminder.id)
                    taskRepository.deleteReminder(reminder)
                    deletingReminder = null
                }
            },
            onDismiss = { deletingReminder = null }
        )
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

    var editingTask by remember { mutableStateOf<ToDo?>(null) }
    var deletingTask by remember { mutableStateOf<ToDo?>(null) }

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
                    Row {
                        IconButton(onClick = { editingTask = task }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "編輯事項",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { deletingTask = task }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "刪除事項",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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

    editingTask?.let { t ->
        EditToDoDialog(
            toDo = t,
            onDismiss = { editingTask = null },
            onSave = { updated ->
                coroutineScope.launch {
                    taskRepository.updateToDo(updated)
                    editingTask = null
                }
            }
        )
    }

    deletingTask?.let { t ->
        ConfirmDeleteDialog(
            itemName = t.title,
            onConfirm = {
                coroutineScope.launch {
                    taskRepository.deleteToDo(t)
                    deletingTask = null
                }
            },
            onDismiss = { deletingTask = null }
        )
    }
}
