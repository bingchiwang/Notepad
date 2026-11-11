package com.example.notepad.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.ToDo
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.reminder.ReminderScheduler
import com.example.notepad.ui.components.*
import kotlinx.coroutines.launch

/**
 * 雙軌任務與提醒主畫面（規範 4.2 待辦事項 & 4.4 提醒通知）
 * - 進行中 / 已完成明確隔離（已完成任務不會顯示於未完成列表）
 * - 支援優先級（高、中、低）排序與色彩徽章顯示
 * - 任務完成時自動取消尚未觸發的關聯提醒通知
 */
@Composable
fun TaskScreen(
    taskRepository: TaskRepository,
    calendarFacade: LunarCalendarFacade,
    onTaskClick: (Int) -> Unit,
    onStartPomodoro: ((String) -> Unit)? = null,
    notificationsEnabled: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val toDos by taskRepository.allToDos.collectAsState(initial = emptyList())
    val activeToDos by taskRepository.activeToDos.collectAsState(initial = emptyList())
    val completedToDos by taskRepository.completedToDos.collectAsState(initial = emptyList())
    val reminders by taskRepository.allReminders.collectAsState(initial = emptyList())

    // 0: 📋 待做事件, 1: 🔔 提醒事項
    var selectedTrack by remember { mutableIntStateOf(0) }

    // 1: 進行中（預設，確保已完成任務不顯示為未完成）, 2: 已完成, 0: 全部
    var todoFilterMode by remember { mutableIntStateOf(1) }
    var selectedPriorityFilter by remember { mutableStateOf<Priority?>(null) }

    var showAddToDoDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    // State for editing
    var editingToDo by remember { mutableStateOf<ToDo?>(null) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    // State for deletion confirmation
    var deletingToDo by remember { mutableStateOf<ToDo?>(null) }
    var deletingReminder by remember { mutableStateOf<Reminder?>(null) }

    // Android 13+ 通知權限申請
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showFeedback(context, "通知權限尚未授權，可於右上角設定重新申請")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderScheduler.hasNotificationPermission(context)
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val filteredToDos = remember(activeToDos, completedToDos, toDos, todoFilterMode, selectedPriorityFilter) {
        val statusFiltered = when (todoFilterMode) {
            1 -> activeToDos
            2 -> completedToDos
            else -> toDos
        }
        val priority = selectedPriorityFilter
        if (priority != null) {
            statusFiltered.filter { it.priority == priority }
        } else {
            statusFiltered
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
                containerColor = AppButtonThemeDefaults.fabContainerColor(),
                contentColor = AppButtonThemeDefaults.fabContentColor(),
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Text("📋 待做事件 (${activeToDos.size})")
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
                // 完成狀態篩選（進行中與已完成分離）
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = todoFilterMode == 1,
                            onClick = { todoFilterMode = 1 },
                            label = { Text("進行中 (${activeToDos.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = todoFilterMode == 2,
                            onClick = { todoFilterMode = 2 },
                            label = { Text("已完成 (${completedToDos.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = todoFilterMode == 0,
                            onClick = { todoFilterMode = 0 },
                            label = { Text("全部 (${toDos.size})") }
                        )
                    }
                }

                // 優先級篩選晶片（高 / 中 / 低）
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = "優先級：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedPriorityFilter == null,
                            onClick = { selectedPriorityFilter = null },
                            label = { Text("不限", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    items(Priority.entries) { priority ->
                        FilterChip(
                            selected = selectedPriorityFilter == priority,
                            onClick = {
                                selectedPriorityFilter = if (selectedPriorityFilter == priority) null else priority
                            },
                            label = {
                                Text(
                                    text = priority.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedPriorityFilter == priority) {
                                        Color(priority.colorLong)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        )
                    }
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
                                1 -> "目前沒有進行中的待做事項 🎉"
                                2 -> "尚未有任何已完成的任務"
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
                                "📅 到期日: ${DateFormatter.formatDate(it)}"
                            }

                            DeadlineAlertCard(
                                title = toDo.title,
                                description = toDo.description,
                                tag = toDo.tag,
                                tagColor = Color(toDo.color),
                                isDueTomorrow = isDueTomorrow,
                                priority = toDo.priority,
                                isCompleted = toDo.isCompleted,
                                dueDateText = dueDateString,
                                onClick = { onTaskClick(toDo.id) },
                                onCheckedChange = { checked ->
                                    coroutineScope.launch {
                                        try {
                                            val cancelledCount = ReminderScheduler.updateToDoCompletionAndSyncReminders(
                                                context = context,
                                                taskRepository = taskRepository,
                                                toDo = toDo,
                                                isCompleted = checked
                                            )
                                            val feedback = when {
                                                checked && cancelledCount > 0 ->
                                                    "已完成「${toDo.title}」，並取消 $cancelledCount 個關聯提醒"
                                                checked -> "已完成「${toDo.title}」"
                                                else -> "已將「${toDo.title}」標記為進行中"
                                            }
                                            showFeedback(context, feedback)
                                        } catch (e: Exception) {
                                            showFeedback(context, "狀態更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                                        }
                                    }
                                },
                                onEdit = { editingToDo = toDo },
                                onDelete = { deletingToDo = toDo }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            } else {
                // === Track 1: Reminders ===
                if (!notificationsEnabled) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "通知已關閉",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "提醒通知功能目前已關閉，不會發送新通知（可於右上角設定重新開啟）。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

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
                                text = "點擊右下角「+ 新增提醒」可自由選擇任意多天與通知時間",
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
                            val triggerTimeStr = DateFormatter.formatDateTime(reminder.triggerAt)

                            ReminderCard(
                                title = reminder.title,
                                message = reminder.message,
                                triggerTimeText = triggerTimeStr,
                                tag = reminder.tag,
                                tagColor = Color(reminder.color),
                                isFired = reminder.isFired,
                                onEdit = { editingReminder = reminder },
                                onDelete = { deletingReminder = reminder }
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
        AddToDoDialog(
            onDismiss = { showAddToDoDialog = false },
            onSave = { newToDo ->
                coroutineScope.launch {
                    try {
                        taskRepository.insertToDo(newToDo)
                        if (todoFilterMode == 2) {
                            todoFilterMode = 1
                        }
                        showFeedback(context, "已成功新增待辦事項")
                        showAddToDoDialog = false
                    } catch (e: Exception) {
                        showFeedback(context, "新增失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            }
        )
    }

    // --- Dialog: Add Multi-Day Reminder ---
    if (showAddReminderDialog) {
        AddMultiDayReminderDialog(
            availableToDos = toDos,
            notificationsEnabled = notificationsEnabled,
            onDismiss = { showAddReminderDialog = false },
            onSave = { reminderList ->
                coroutineScope.launch {
                    try {
                        var scheduledCount = 0
                        reminderList.forEach { reminder ->
                            val newId = taskRepository.insertReminder(reminder)
                            val scheduled = ReminderScheduler.scheduleReminder(
                                context = context,
                                reminder = reminder.copy(id = newId.toInt()),
                                notificationsEnabled = notificationsEnabled
                            )
                            if (scheduled) scheduledCount++
                        }
                        val feedback = if (notificationsEnabled) {
                            "已成功建立 ${reminderList.size} 個提醒（已排程 $scheduledCount 筆通知）"
                        } else {
                            "已保存 ${reminderList.size} 個提醒（通知功能關閉中）"
                        }
                        showFeedback(context, feedback)
                        showAddReminderDialog = false
                    } catch (e: Exception) {
                        showFeedback(context, "建立提醒失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
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
                    try {
                        taskRepository.updateToDo(updatedToDo)
                        showFeedback(context, "待辦事項已更新")
                        editingToDo = null
                    } catch (e: Exception) {
                        showFeedback(context, "更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            }
        )
    }

    // --- Dialog: Edit Reminder ---
    editingReminder?.let { reminderToEdit ->
        EditReminderDialog(
            reminder = reminderToEdit,
            notificationsEnabled = notificationsEnabled,
            onDismiss = { editingReminder = null },
            onSave = { updatedReminder ->
                coroutineScope.launch {
                    try {
                        taskRepository.updateReminder(updatedReminder)
                        ReminderScheduler.cancelReminder(context, updatedReminder.id)
                        val scheduled = ReminderScheduler.scheduleReminder(
                            context = context,
                            reminder = updatedReminder,
                            notificationsEnabled = notificationsEnabled
                        )
                        showFeedback(
                            context,
                            if (scheduled) "提醒事項已更新並重新排程" else "提醒事項已更新（通知目前暫停）"
                        )
                        editingReminder = null
                    } catch (e: Exception) {
                        showFeedback(context, "更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
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
                    try {
                        val pending = taskRepository.getPendingRemindersByToDoId(todo.id)
                        pending.forEach { ReminderScheduler.cancelReminder(context, it.id) }
                        taskRepository.deleteToDo(todo)
                        showFeedback(context, "已刪除待辦事項")
                        deletingToDo = null
                    } catch (e: Exception) {
                        showFeedback(context, "刪除失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
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
                    try {
                        ReminderScheduler.cancelReminder(context, reminder.id)
                        taskRepository.deleteReminder(reminder)
                        showFeedback(context, "已刪除提醒事項")
                        deletingReminder = null
                    } catch (e: Exception) {
                        showFeedback(context, "刪除失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onDismiss = { deletingReminder = null }
        )
    }
}

/**
 * 待辦事項詳情頁面（顯示優先級、分開管理截止日與獨立提醒、支援完成同步取消提醒）
 */
@Composable
fun TaskDetailScreen(
    taskId: Int,
    taskRepository: TaskRepository,
    notificationsEnabled: Boolean = true,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val toDos by taskRepository.allToDos.collectAsState(initial = emptyList())
    val reminders by taskRepository.allReminders.collectAsState(initial = emptyList())
    val task = toDos.find { it.id == taskId }
    val linkedReminders = remember(reminders, taskId) {
        reminders.filter { it.linkedToDoId == taskId }
    }

    var editingTask by remember { mutableStateOf<ToDo?>(null) }
    var deletingTask by remember { mutableStateOf<ToDo?>(null) }
    var showAddLinkedReminder by remember { mutableStateOf(false) }

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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                        ColorTag(text = task.tag, tagColor = Color(task.color))
                        PriorityBadge(priority = task.priority)
                    }
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
                    Text(
                        text = "📅 截止日期: ${DateFormatter.formatDate(it)}",
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
                                try {
                                    val cancelled = ReminderScheduler.updateToDoCompletionAndSyncReminders(
                                        context = context,
                                        taskRepository = taskRepository,
                                        toDo = task,
                                        isCompleted = checked
                                    )
                                    val msg = if (checked && cancelled > 0) {
                                        "任務已完成，已自動取消 $cancelled 個關聯提醒"
                                    } else if (checked) {
                                        "任務已標記完成"
                                    } else {
                                        "任務已恢復為進行中"
                                    }
                                    showFeedback(context, msg)
                                } catch (e: Exception) {
                                    showFeedback(context, "更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                                }
                            }
                        }
                    )
                    Text(
                        text = if (task.isCompleted) "已完成" else "尚未完成",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔔 關聯提醒排程 (${linkedReminders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(onClick = { showAddLinkedReminder = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("設定提醒時間")
                    }
                }

                if (linkedReminders.isEmpty()) {
                    Text(
                        text = "尚未綁定提醒通知（截止日期與提醒排程可分開獨立管理）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        linkedReminders.forEach { rem ->
                            ReminderCard(
                                title = rem.title,
                                message = rem.message,
                                triggerTimeText = DateFormatter.formatDateTime(rem.triggerAt),
                                tag = rem.tag,
                                tagColor = Color(rem.color),
                                isFired = rem.isFired
                            )
                        }
                    }
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
                    try {
                        taskRepository.updateToDo(updated)
                        showFeedback(context, "待辦事項已更新")
                        editingTask = null
                    } catch (e: Exception) {
                        showFeedback(context, "更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            }
        )
    }

    deletingTask?.let { t ->
        ConfirmDeleteDialog(
            itemName = t.title,
            onConfirm = {
                coroutineScope.launch {
                    try {
                        val pending = taskRepository.getPendingRemindersByToDoId(t.id)
                        pending.forEach { ReminderScheduler.cancelReminder(context, it.id) }
                        taskRepository.deleteToDo(t)
                        showFeedback(context, "已刪除待辦事項")
                        deletingTask = null
                        onNavigateBack?.invoke()
                    } catch (e: Exception) {
                        showFeedback(context, "刪除失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onDismiss = { deletingTask = null }
        )
    }

    if (showAddLinkedReminder && task != null) {
        AddMultiDayReminderDialog(
            initialDateMillis = task.dueDate,
            initialTitle = task.title,
            initialLinkedToDoId = task.id,
            availableToDos = listOf(task),
            notificationsEnabled = notificationsEnabled,
            onDismiss = { showAddLinkedReminder = false },
            onSave = { reminderList ->
                coroutineScope.launch {
                    try {
                        reminderList.forEach { reminder ->
                            val newId = taskRepository.insertReminder(reminder)
                            ReminderScheduler.scheduleReminder(
                                context = context,
                                reminder = reminder.copy(id = newId.toInt()),
                                notificationsEnabled = notificationsEnabled
                            )
                        }
                        showFeedback(context, "已為此任務設定 ${reminderList.size} 筆獨立提醒")
                        showAddLinkedReminder = false
                    } catch (e: Exception) {
                        showFeedback(context, "設定失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            }
        )
    }
}
