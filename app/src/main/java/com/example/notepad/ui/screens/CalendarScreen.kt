package com.example.notepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun CalendarScreen(
    calendarFacade: LunarCalendarFacade,
    taskRepository: TaskRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allToDos by taskRepository.allToDos.collectAsState(initial = emptyList())
    val allReminders by taskRepository.allReminders.collectAsState(initial = emptyList())

    // Current viewed Year & Month (1-based month: 1..12)
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    var currentYear by remember { mutableIntStateOf(todayYear) }
    var currentMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    var editingToDo by remember { mutableStateOf<ToDo?>(null) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }

    var deletingToDo by remember { mutableStateOf<ToDo?>(null) }
    var deletingReminder by remember { mutableStateOf<Reminder?>(null) }

    // Calculate calendar grid for currentYear and currentMonth
    val monthCal = remember(currentYear, currentMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val daysInMonth = remember(currentYear, currentMonth) {
        monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(currentYear, currentMonth) {
        monthCal.get(Calendar.DAY_OF_WEEK)
    }

    fun getDayStartMillis(day: Int): Long {
        val c = Calendar.getInstance()
        c.set(currentYear, currentMonth - 1, day, 0, 0, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    val selectedDayStartMillis = remember(currentYear, currentMonth, selectedDay) {
        getDayStartMillis(selectedDay)
    }
    val selectedDayEndMillis = remember(selectedDayStartMillis) {
        selectedDayStartMillis + 86400000L - 1
    }

    val selectedDayToDos = remember(allToDos, selectedDayStartMillis) {
        allToDos.filter { it.dueDate == selectedDayStartMillis }
    }

    val selectedDayReminders = remember(allReminders, selectedDayStartMillis, selectedDayEndMillis) {
        allReminders.filter { it.triggerAt in selectedDayStartMillis..selectedDayEndMillis }
    }

    val selectedDateInfo = remember(currentYear, currentMonth, selectedDay) {
        val c = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, selectedDay, 12, 0, 0)
        }
        calendarFacade.getCalendarInfo(c.time)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header: Month Navigator & Year-Month Title ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentMonth == 1) {
                            currentYear -= 1
                            currentMonth = 12
                        } else {
                            currentMonth -= 1
                        }
                        selectedDay = 1
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上個月",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${currentYear} 年 ${currentMonth} 月",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = selectedDateInfo.lunarDate.getYearCyclic(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            currentYear = todayYear
                            currentMonth = todayMonth
                            selectedDay = todayDay
                        }) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = "今天",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        IconButton(onClick = {
                            if (currentMonth == 12) {
                                currentYear += 1
                                currentMonth = 1
                            } else {
                                currentMonth += 1
                            }
                            selectedDay = 1
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "下個月",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // --- Calendar Grid ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weekdays.forEachIndexed { index, name ->
                            Text(
                                text = name,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (index) {
                                    0 -> Color(0xFFE53935)
                                    6 -> Color(0xFF1E88E5)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(bottom = 6.dp))

                    val totalSlots = ((firstDayOfWeek - 1 + daysInMonth + 6) / 7) * 7
                    val rows = totalSlots / 7

                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (col in 0 until 7) {
                                val slotIndex = row * 7 + col
                                val dayNumber = slotIndex - (firstDayOfWeek - 1) + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val isSelected = (dayNumber == selectedDay)
                                    val isToday = (currentYear == todayYear && currentMonth == todayMonth && dayNumber == todayDay)
                                    val dayMillis = getDayStartMillis(dayNumber)
                                    val dayEndMillis = dayMillis + 86400000L - 1

                                    val dayTasks = allToDos.filter { it.dueDate == dayMillis }
                                    val dayReminders = allReminders.filter { it.triggerAt in dayMillis..dayEndMillis }
                                    val hasIncompleteTasks = dayTasks.any { !it.isCompleted }
                                    val isDueTomorrow = calendarFacade.isTomorrow(dayMillis) && hasIncompleteTasks

                                    val dayDate = remember(currentYear, currentMonth, dayNumber) {
                                        Calendar.getInstance().apply {
                                            set(currentYear, currentMonth - 1, dayNumber, 12, 0, 0)
                                        }.time
                                    }
                                    val info = remember(dayDate) { calendarFacade.getCalendarInfo(dayDate) }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .then(
                                                if (isDueTomorrow) {
                                                    Modifier.breathingDashedBorder(
                                                        color = Color(0xFFFF3D00),
                                                        cornerRadius = 8f,
                                                        strokeWidth = 2f
                                                    )
                                                } else Modifier
                                            )
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                                    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(
                                                        1.5.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                } else Modifier
                                            )
                                            .clickable { selectedDay = dayNumber },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    col == 0 -> Color(0xFFE53935)
                                                    col == 6 -> Color(0xFF1E88E5)
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )

                                            Text(
                                                text = info.displayLabel,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                fontWeight = if (!info.festival.isNullOrEmpty() || !info.solarTerm.isNullOrEmpty()) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    !info.festival.isNullOrEmpty() -> Color(0xFFD32F2F)
                                                    !info.solarTerm.isNullOrEmpty() -> Color(0xFF388E3C)
                                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            )

                                            // Task and Reminder dots
                                            if (dayTasks.isNotEmpty() || dayReminders.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier.padding(top = 2.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    dayTasks.take(2).forEach { task ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (task.isCompleted) Color.Gray else Color(task.color)
                                                                )
                                                        )
                                                    }
                                                    dayReminders.take(1).forEach { rem ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(rem.color))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(0.85f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Selected Date Detail & Lunar Information Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentYear}年${currentMonth}月${selectedDay}日",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedDateInfo.lunarDate.getFormattedString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!selectedDateInfo.displayFestival.isNullOrEmpty()) {
                            Text(
                                text = "🏮 節慶：${selectedDateInfo.displayFestival}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!selectedDateInfo.solarTerm.isNullOrEmpty()) {
                            Text(
                                text = "🌿 節氣：${selectedDateInfo.solarTerm}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF388E3C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- Selected Date To-Do Items Section ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 該日待做事項 (${selectedDayToDos.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { showAddReminderDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("加提醒", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = { showAddTaskDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("加待辦", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (selectedDayToDos.isEmpty()) {
            item {
                Text(
                    text = "此日尚無到期的待做事項",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        } else {
            items(selectedDayToDos, key = { it.id }) { toDo ->
                val isTomorrow = calendarFacade.isTomorrow(selectedDayStartMillis)
                DeadlineAlertCard(
                    title = toDo.title,
                    description = toDo.description,
                    tag = toDo.tag,
                    tagColor = Color(toDo.color),
                    isDueTomorrow = isTomorrow,
                    isCompleted = toDo.isCompleted,
                    dueDateText = "📅 ${currentMonth}月${selectedDay}日 到期",
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
        }

        // --- Selected Date Reminders Section ---
        item {
            Text(
                text = "🔔 該日提醒事項 (${selectedDayReminders.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (selectedDayReminders.isEmpty()) {
            item {
                Text(
                    text = "此日尚無預定的提醒通知",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        } else {
            items(selectedDayReminders, key = { it.id }) { reminder ->
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val timeStr = sdf.format(Date(reminder.triggerAt))

                ReminderCard(
                    title = reminder.title,
                    message = reminder.message,
                    triggerTimeText = "${currentMonth}月${selectedDay}日 $timeStr",
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
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // --- Dialog: Add New To-Do for Selected Date ---
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var tag by remember { mutableStateOf("日常") }
        var selectedColor by remember { mutableLongStateOf(PRESET_COLORS.first().colorLong) }

        Dialog(onDismissRequest = { showAddTaskDialog = false }) {
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
                        text = "新增待做事項 (${currentMonth}月${selectedDay}日)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("事項標題 *") },
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

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        TextButton(onClick = { showAddTaskDialog = false }) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    coroutineScope.launch {
                                        taskRepository.insertToDo(
                                            ToDo(
                                                title = title.trim(),
                                                description = description.trim(),
                                                dueDate = selectedDayStartMillis,
                                                tag = tag.trim().ifEmpty { "日常" },
                                                color = selectedColor
                                            )
                                        )
                                        showAddTaskDialog = false
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

    // --- Dialog: Add Reminder for Selected Date ---
    if (showAddReminderDialog) {
        AddMultiDayReminderDialog(
            onDismiss = { showAddReminderDialog = false },
            onSave = { reminderList ->
                coroutineScope.launch {
                    reminderList.forEach { reminder ->
                        val newId = taskRepository.insertReminder(reminder)
                        ReminderScheduler.scheduleReminder(context, reminder.copy(id = newId.toInt()))
                    }
                    showAddReminderDialog = false
                }
            }
        )
    }

    // --- Dialog: Edit To-Do ---
    editingToDo?.let { todoToEdit ->
        EditToDoDialog(
            toDo = todoToEdit,
            onDismiss = { editingToDo = null },
            onSave = { updated ->
                coroutineScope.launch {
                    taskRepository.updateToDo(updated)
                    editingToDo = null
                }
            }
        )
    }

    // --- Dialog: Edit Reminder ---
    editingReminder?.let { remToEdit ->
        EditReminderDialog(
            reminder = remToEdit,
            onDismiss = { editingReminder = null },
            onSave = { updated ->
                coroutineScope.launch {
                    taskRepository.updateReminder(updated)
                    ReminderScheduler.cancelReminder(context, updated.id)
                    ReminderScheduler.scheduleReminder(context, updated)
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
    deletingReminder?.let { rem ->
        ConfirmDeleteDialog(
            itemName = rem.title.ifEmpty { "提醒" },
            onConfirm = {
                coroutineScope.launch {
                    ReminderScheduler.cancelReminder(context, rem.id)
                    taskRepository.deleteReminder(rem)
                    deletingReminder = null
                }
            },
            onDismiss = { deletingReminder = null }
        )
    }
}
