package com.example.notepad.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.notepad.calendar.LunarCalendarFacade
import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.local.entity.RecurringTaskLogic
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.ToDo
import com.example.notepad.data.preferences.PersonalizationSettings
import com.example.notepad.data.repository.NoteRepository
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.reminder.ReminderScheduler
import com.example.notepad.ui.components.*
import com.example.notepad.ui.theme.CalendarThemeResolver
import com.example.notepad.ui.theme.LocalPersonalizationSettings
import com.example.notepad.widget.CalendarWidgetUpdater
import com.example.notepad.widget.ToDoWidgetLogic
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 農曆萬年曆主畫面（支援 6 種自訂月曆樣式、高對比文字保護、節氣節慶顯示與當日待辦／提醒快速管理）
 */
@Composable
fun CalendarScreen(
    calendarFacade: LunarCalendarFacade,
    taskRepository: TaskRepository,
    notificationsEnabled: Boolean = true,
    personalization: PersonalizationSettings = LocalPersonalizationSettings.current,
    noteRepository: NoteRepository? = null,
    onNoteClick: (Int) -> Unit = {},
    onToDoClick: (Int) -> Unit = {},
    onQuickCreateNoteForDate: (Long) -> Unit = {},
    initialSelectedDateMillis: Long? = null,
    onConsumeInitialDate: () -> Unit = {},
    openQuickAddOnLaunch: Boolean = false,
    onConsumeOpenQuickAdd: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val useDarkTheme = personalization.themeMode.shouldUseDarkTheme(isSystemInDarkTheme())
    val activeColorScheme = MaterialTheme.colorScheme
    val calendarPalette = remember(
        personalization.calendarStyle,
        personalization.calendarAccentColorLong,
        useDarkTheme,
        activeColorScheme
    ) {
        CalendarThemeResolver.resolvePalette(
            style = personalization.calendarStyle,
            customAccentColorLong = personalization.calendarAccentColorLong,
            darkTheme = useDarkTheme,
            themeColorScheme = activeColorScheme
        )
    }

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

    var showQuickAddChooser by remember { mutableStateOf(false) }

    val allNotes by (noteRepository?.allNotes?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList<Note>()) })

    // 接收來自月曆桌面小工具的日期跳轉與快速新增指令
    LaunchedEffect(initialSelectedDateMillis, openQuickAddOnLaunch) {
        if (initialSelectedDateMillis != null) {
            val targetCal = Calendar.getInstance().apply { timeInMillis = initialSelectedDateMillis }
            currentYear = targetCal.get(Calendar.YEAR)
            currentMonth = targetCal.get(Calendar.MONTH) + 1
            selectedDay = targetCal.get(Calendar.DAY_OF_MONTH)
            onConsumeInitialDate()
        }
        if (openQuickAddOnLaunch) {
            showQuickAddChooser = true
            onConsumeOpenQuickAdd()
        }
    }

    // 當使用者於 App 月曆切換瀏覽年月時，同步更新桌面月曆小工具顯示之年月與內容
    LaunchedEffect(currentYear, currentMonth) {
        CalendarWidgetUpdater.syncDisplayedMonth(context, currentYear, currentMonth)
    }

    val selectedDayNotes = remember(allNotes, selectedDayStartMillis) {
        allNotes
            .filter { ToDoWidgetLogic.normalizeToLocalMidnight(it.createdAt) == selectedDayStartMillis }
            .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
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
                colors = CardDefaults.cardColors(containerColor = calendarPalette.headerBgColor),
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
                            tint = calendarPalette.headerContentColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${currentYear} 年 ${currentMonth} 月",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = calendarPalette.headerContentColor
                        )
                        Text(
                            text = "農曆 ${selectedDateInfo.lunarDate.getYearCyclic()}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.SansSerif,
                            color = calendarPalette.headerSubContentColor
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
                                tint = calendarPalette.headerContentColor
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
                                tint = calendarPalette.headerContentColor
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
                colors = CardDefaults.cardColors(containerColor = calendarPalette.cardBgColor),
                border = calendarPalette.cardBorderColor?.let { BorderStroke(1.dp, it) },
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
                                    0 -> calendarPalette.sundayHeaderColor
                                    6 -> calendarPalette.saturdayHeaderColor
                                    else -> calendarPalette.weekdayHeaderColor
                                }
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 6.dp),
                        color = calendarPalette.gridBorderColor
                    )

                    val totalSlots = ((firstDayOfWeek - 1 + daysInMonth + 6) / 7) * 7
                    val rows = totalSlots / 7

                    for (row in 0 until rows) {
                        if (row > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 2.dp),
                                thickness = 0.5.dp,
                                color = calendarPalette.gridBorderColor
                            )
                        }
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
                                            .clip(calendarPalette.cellShape)
                                            .then(
                                                if (isDueTomorrow) {
                                                    Modifier.breathingDashedBorder(
                                                        color = Color(0xFFFF3D00),
                                                        cornerRadius = 8f,
                                                        strokeWidth = 2f
                                                    )
                                                } else if (calendarPalette.showCellGridBorder && !isSelected) {
                                                    Modifier.border(
                                                        0.5.dp,
                                                        calendarPalette.gridBorderColor,
                                                        calendarPalette.cellShape
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .background(
                                                when {
                                                    isSelected -> calendarPalette.selectedBgColor
                                                    isToday -> calendarPalette.todayBgColor
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(
                                                        1.5.dp,
                                                        calendarPalette.todayBorderColor,
                                                        calendarPalette.cellShape
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
                                                color = calendarPalette.resolveDayNumberColor(
                                                    isSelected = isSelected,
                                                    dayOfWeekIndex = col
                                                )
                                            )

                                            Text(
                                                text = info.displayLabel,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.SansSerif,
                                                maxLines = 1,
                                                fontWeight = if (!info.festival.isNullOrEmpty() || !info.solarTerm.isNullOrEmpty()) FontWeight.Bold else FontWeight.Normal,
                                                color = calendarPalette.resolveLunarLabelColor(
                                                    isSelected = isSelected,
                                                    hasFestival = !info.festival.isNullOrEmpty(),
                                                    hasSolarTerm = !info.solarTerm.isNullOrEmpty()
                                                )
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
                colors = CardDefaults.cardColors(containerColor = calendarPalette.detailCardBgColor),
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
                            fontWeight = FontWeight.Bold,
                            color = calendarPalette.detailTextColor
                        )
                        Text(
                            text = "農曆 ${selectedDateInfo.lunarDate.getFormattedString()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.SansSerif,
                            color = calendarPalette.detailPrimaryColor,
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
                                color = calendarPalette.detailFestivalColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (!selectedDateInfo.solarTerm.isNullOrEmpty()) {
                            Text(
                                text = "🌿 節氣：${selectedDateInfo.solarTerm}",
                                style = MaterialTheme.typography.bodySmall,
                                color = calendarPalette.detailSolarTermColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- Selected Date Notes Section ---
        if (noteRepository != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 該日記事 (${selectedDayNotes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    FilledTonalButton(
                        onClick = { onQuickCreateNoteForDate(selectedDayStartMillis) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("加記事", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (selectedDayNotes.isEmpty()) {
                item {
                    Text(
                        text = "此日尚無建立記事",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            } else {
                items(selectedDayNotes, key = { "cal_note_${it.id}" }) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNoteClick(note.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(note.color))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = note.title.ifBlank { "（無標題）" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content.replace('\n', ' '),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "更新於 ${DateFormatter.formatTime(note.updatedAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "編輯記事",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                        colors = AppButtonThemeDefaults.filledColors(),
                        border = AppButtonThemeDefaults.filledBorder(),
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
                val recurrenceLabel = RecurringTaskLogic.formatRuleLabel(
                    recurrenceType = toDo.recurrenceType,
                    dayOfWeek = toDo.recurrenceDayOfWeek,
                    dayOfMonth = toDo.recurrenceDayOfMonth
                )
                DeadlineAlertCard(
                    title = toDo.title,
                    description = toDo.description,
                    tag = toDo.tag,
                    tagColor = Color(toDo.color),
                    isDueTomorrow = isTomorrow,
                    priority = toDo.priority,
                    isCompleted = toDo.isCompleted,
                    dueDateText = "📅 ${currentMonth}月${selectedDay}日 到期",
                    recurrenceText = recurrenceLabel.takeIf { it.isNotEmpty() },
                    onClick = { onToDoClick(toDo.id) },
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            try {
                                val outcome = ReminderScheduler.updateToDoCompletionAndSyncReminders(
                                    context = context,
                                    taskRepository = taskRepository,
                                    toDo = toDo,
                                    isCompleted = checked
                                )
                                val feedback = ReminderScheduler.formatCompletionFeedback(
                                    title = toDo.title,
                                    isCompleted = checked,
                                    outcome = outcome
                                )
                                showFeedback(context, feedback)
                            } catch (e: Exception) {
                                showFeedback(context, "狀態更新失敗: ${e.localizedMessage ?: "發生錯誤"}")
                            }
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
                val timeStr = DateFormatter.formatTime(reminder.triggerAt)

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
        AddToDoDialog(
            initialDueDateMillis = selectedDayStartMillis,
            dialogTitle = "新增待做事項 (${currentMonth}月${selectedDay}日)",
            onDismiss = { showAddTaskDialog = false },
            onSave = { newToDo ->
                coroutineScope.launch {
                    try {
                        taskRepository.insertToDo(newToDo)
                        showFeedback(context, "已成功新增待辦事項")
                        showAddTaskDialog = false
                    } catch (e: Exception) {
                        showFeedback(context, "新增失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            }
        )
    }

    // --- Dialog: Add Reminder for Selected Date ---
    if (showAddReminderDialog) {
        AddMultiDayReminderDialog(
            initialDateMillis = selectedDayStartMillis,
            availableToDos = allToDos,
            notificationsEnabled = notificationsEnabled,
            onDismiss = { showAddReminderDialog = false },
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
                        showFeedback(context, "已成功建立 ${reminderList.size} 個提醒事項")
                        showAddReminderDialog = false
                    } catch (e: Exception) {
                        showFeedback(context, "建立提醒失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
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
                    try {
                        taskRepository.updateToDo(updated)
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
    editingReminder?.let { remToEdit ->
        EditReminderDialog(
            reminder = remToEdit,
            notificationsEnabled = notificationsEnabled,
            onDismiss = { editingReminder = null },
            onSave = { updated ->
                coroutineScope.launch {
                    try {
                        taskRepository.updateReminder(updated)
                        ReminderScheduler.cancelReminder(context, updated.id)
                        ReminderScheduler.scheduleReminder(
                            context = context,
                            reminder = updated,
                            notificationsEnabled = notificationsEnabled
                        )
                        showFeedback(context, "提醒事項已更新")
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
    deletingReminder?.let { rem ->
        ConfirmDeleteDialog(
            itemName = rem.title.ifEmpty { "提醒" },
            onConfirm = {
                coroutineScope.launch {
                    try {
                        ReminderScheduler.cancelReminder(context, rem.id)
                        taskRepository.deleteReminder(rem)
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

    // --- Dialog: Quick Create Note or To-Do (Triggered from Calendar Widget '+ 新增') ---
    if (showQuickAddChooser) {
        AlertDialog(
            onDismissRequest = { showQuickAddChooser = false },
            title = {
                Text(
                    text = "快速建立 (${currentMonth}月${selectedDay}日)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("請選擇要為此日期快速建立記事或待辦事項：")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuickAddChooser = false
                        showAddTaskDialog = true
                    },
                    colors = AppButtonThemeDefaults.filledColors(),
                    border = AppButtonThemeDefaults.filledBorder()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新增待辦")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showQuickAddChooser = false }) {
                        Text("取消")
                    }
                    FilledTonalButton(
                        onClick = {
                            showQuickAddChooser = false
                            onQuickCreateNoteForDate(selectedDayStartMillis)
                        }
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新增記事")
                    }
                }
            }
        )
    }
}
