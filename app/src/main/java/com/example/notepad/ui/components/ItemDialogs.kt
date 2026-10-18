package com.example.notepad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
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
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Header navigator for month and year navigation in custom calendar pickers.
 */
@Composable
fun MonthNavigatorHeader(
    year: Int,
    month: Int,
    onPrevYear: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onNextYear: () -> Unit,
    onTodayClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevYear, modifier = Modifier.size(36.dp)) {
                Text("«", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            IconButton(onClick = onPrevMonth, modifier = Modifier.size(36.dp)) {
                Text("‹", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${year}年 ${month}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            AssistChip(
                onClick = onTodayClick,
                label = { Text("本月", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(28.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                Text("›", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            IconButton(onClick = onNextYear, modifier = Modifier.size(36.dp)) {
                Text("»", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

/**
 * 7-column Monthly Calendar Grid for flexible multi-date or single-date selection.
 */
@Composable
fun MonthCalendarGrid(
    viewYear: Int,
    viewMonth: Int, // 1..12
    selectedDatesMillis: Set<Long>,
    onDateToggled: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf("日", "一", "二", "三", "四", "五", "六")

    val cal = remember(viewYear, viewMonth) {
        Calendar.getInstance().apply {
            set(viewYear, viewMonth - 1, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val offset = firstDayOfWeek - 1 // 0..6

    val todayCal = remember { Calendar.getInstance() }
    val isTodayMonth = (todayCal.get(Calendar.YEAR) == viewYear && (todayCal.get(Calendar.MONTH) + 1) == viewMonth)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Weekday header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekdays.forEachIndexed { index, name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (index == 0 || index == 6) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), thickness = 0.5.dp)

        val totalCells = offset + daysInMonth
        val numRows = (totalCells + 6) / 7

        for (row in 0 until numRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - offset + 1
                    if (dayNum in 1..daysInMonth) {
                        val dayMillis = remember(viewYear, viewMonth, dayNum) {
                            Calendar.getInstance().apply {
                                set(viewYear, viewMonth - 1, dayNum, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                        val isSelected = selectedDatesMillis.contains(dayMillis)
                        val isToday = isTodayMonth && (dayNum == todayDay)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateToggled(dayMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$dayNum",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    col == 0 || col == 6 -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Safely clamps day of month when month or year changes.
 */
private fun clampDay(y: Int, m: Int, d: Int): Int {
    val maxDays = Calendar.getInstance().apply {
        set(y, m - 1, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)
    return d.coerceIn(1, maxDays)
}

/**
 * Dialog for creating a Reminder that supports multi-day selection and custom notification time.
 * Date selection is unrestricted across any month or year.
 */
@Composable
fun AddMultiDayReminderDialog(
    initialDateMillis: Long? = null,
    onDismiss: () -> Unit,
    onSave: (List<Reminder>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("提醒") }
    var selectedColor by remember { mutableLongStateOf(PRESET_COLORS[2].colorLong) } // Amber

    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }

    val todayCal = remember { Calendar.getInstance() }
    val todayStartMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val initialCal = remember(initialDateMillis) {
        Calendar.getInstance().apply {
            if (initialDateMillis != null) {
                timeInMillis = initialDateMillis
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    var viewYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var viewMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH) + 1) }

    // Set of selected day timestamps
    val selectedDaysMillis = remember {
        mutableStateListOf<Long>().apply {
            add(initialCal.timeInMillis)
        }
    }

    val quickTimePresets = listOf(
        "08:00" to Pair(8, 0),
        "09:00" to Pair(9, 0),
        "12:00" to Pair(12, 0),
        "14:00" to Pair(14, 0),
        "18:00" to Pair(18, 0),
        "20:00" to Pair(20, 0)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🔔 新增多日提醒",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("提醒標題 *") },
                    placeholder = { Text("例如: 吃維他命、交報告、繳費") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("提醒備註 (選填)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Time Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "⏰ 提醒時間：${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Quick time presets
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickTimePresets) { (label, pair) ->
                            val isSelected = hour == pair.first && minute == pair.second
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    hour = pair.first
                                    minute = pair.second
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Stepper for custom Hour & Minute
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("時：", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { hour = if (hour > 0) hour - 1 else 23 }) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Text(
                                String.format(Locale.getDefault(), "%02d", hour),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { hour = if (hour < 23) hour + 1 else 0 }) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("分：", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { minute = if (minute >= 5) minute - 5 else 55 }) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                            Text(
                                String.format(Locale.getDefault(), "%02d", minute),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { minute = if (minute <= 50) minute + 5 else 0 }) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Multi-Day Selection Section (Free date selection without limits)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 自由選擇提醒日期",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "已選 ${selectedDaysMillis.size} 天",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Quick date presets
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    if (!selectedDaysMillis.contains(todayStartMillis)) {
                                        selectedDaysMillis.add(todayStartMillis)
                                    }
                                    viewYear = todayCal.get(Calendar.YEAR)
                                    viewMonth = todayCal.get(Calendar.MONTH) + 1
                                },
                                label = { Text("今天", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val tomorrow = Calendar.getInstance().apply {
                                        timeInMillis = todayStartMillis
                                        add(Calendar.DAY_OF_YEAR, 1)
                                    }.timeInMillis
                                    if (!selectedDaysMillis.contains(tomorrow)) {
                                        selectedDaysMillis.add(tomorrow)
                                    }
                                },
                                label = { Text("明天", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    for (i in 0 until 7) {
                                        val c = Calendar.getInstance().apply {
                                            timeInMillis = todayStartMillis
                                            add(Calendar.DAY_OF_YEAR, i)
                                        }.timeInMillis
                                        if (!selectedDaysMillis.contains(c)) {
                                            selectedDaysMillis.add(c)
                                        }
                                    }
                                },
                                label = { Text("未來 7 天", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    for (i in 0 until 30) {
                                        val c = Calendar.getInstance().apply {
                                            timeInMillis = todayStartMillis
                                            add(Calendar.DAY_OF_YEAR, i)
                                        }.timeInMillis
                                        if (!selectedDaysMillis.contains(c)) {
                                            selectedDaysMillis.add(c)
                                        }
                                    }
                                },
                                label = { Text("未來 30 天", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        if (selectedDaysMillis.isNotEmpty()) {
                            item {
                                FilterChip(
                                    selected = false,
                                    onClick = { selectedDaysMillis.clear() },
                                    label = { Text("清空", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }

                    // Month navigator for the calendar
                    MonthNavigatorHeader(
                        year = viewYear,
                        month = viewMonth,
                        onPrevYear = { viewYear -= 1 },
                        onPrevMonth = {
                            if (viewMonth == 1) {
                                viewYear -= 1
                                viewMonth = 12
                            } else {
                                viewMonth -= 1
                            }
                        },
                        onNextMonth = {
                            if (viewMonth == 12) {
                                viewYear += 1
                                viewMonth = 1
                            } else {
                                viewMonth += 1
                            }
                        },
                        onNextYear = { viewYear += 1 },
                        onTodayClick = {
                            viewYear = todayCal.get(Calendar.YEAR)
                            viewMonth = todayCal.get(Calendar.MONTH) + 1
                        }
                    )

                    // Month Calendar Grid (Click any date to toggle on/off)
                    MonthCalendarGrid(
                        viewYear = viewYear,
                        viewMonth = viewMonth,
                        selectedDatesMillis = selectedDaysMillis.toSet(),
                        onDateToggled = { millis ->
                            if (selectedDaysMillis.contains(millis)) {
                                selectedDaysMillis.remove(millis)
                            } else {
                                selectedDaysMillis.add(millis)
                            }
                        }
                    )

                    // List of currently selected dates with remove chips
                    if (selectedDaysMillis.isNotEmpty()) {
                        Text(
                            text = "已選定日期 (點擊 ✕ 移除，點擊文字跳至該月)：",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd (E)", Locale.TAIWAN) }
                        val sortedDates = selectedDaysMillis.sorted()

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(sortedDates) { dateMillis ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        val c = Calendar.getInstance().apply { timeInMillis = dateMillis }
                                        viewYear = c.get(Calendar.YEAR)
                                        viewMonth = c.get(Calendar.MONTH) + 1
                                    },
                                    label = {
                                        Text(
                                            text = dateFormat.format(Date(dateMillis)),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { selectedDaysMillis.remove(dateMillis) },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "移除",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "⚠️ 請點擊上方月曆自由選擇要提醒的日期",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider()

                // Tag & Color
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
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && selectedDaysMillis.isNotEmpty()) {
                                val reminders = selectedDaysMillis.map { dayMillis ->
                                    val triggerCal = Calendar.getInstance().apply {
                                        timeInMillis = dayMillis
                                        set(Calendar.HOUR_OF_DAY, hour)
                                        set(Calendar.MINUTE, minute)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    Reminder(
                                        title = title.trim(),
                                        message = message.trim(),
                                        triggerAt = triggerCal.timeInMillis,
                                        tag = tag.trim().ifEmpty { "提醒" },
                                        color = selectedColor
                                    )
                                }
                                onSave(reminders)
                            }
                        },
                        enabled = title.isNotBlank() && selectedDaysMillis.isNotEmpty()
                    ) {
                        Text("建立 ${selectedDaysMillis.size} 個提醒")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for editing an existing Reminder (content and date/time).
 * Date can be freely adjusted by year/month/day or directly picked from interactive calendar.
 */
@Composable
fun EditReminderDialog(
    reminder: Reminder,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    var title by remember { mutableStateOf(reminder.title) }
    var message by remember { mutableStateOf(reminder.message) }
    var tag by remember { mutableStateOf(reminder.tag) }
    var selectedColor by remember { mutableLongStateOf(reminder.color) }

    val initialCal = remember(reminder.triggerAt) {
        Calendar.getInstance().apply { timeInMillis = reminder.triggerAt }
    }

    var year by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH) + 1) }
    var day by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    var showCalendarPicker by remember { mutableStateOf(false) }
    val todayCal = remember { Calendar.getInstance() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "✏️ 編輯提醒事項",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("提醒標題 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("詳細備註") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date & Time Adjustment
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 提醒日期與時間：",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showCalendarPicker = !showCalendarPicker }) {
                            Text(
                                if (showCalendarPicker) "收起月曆" else "展開月曆選擇",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Text(
                        text = "${year}年${month}月${day}日  ${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    // Optional Interactive Calendar Picker
                    if (showCalendarPicker) {
                        MonthNavigatorHeader(
                            year = year,
                            month = month,
                            onPrevYear = {
                                year -= 1
                                day = clampDay(year, month, day)
                            },
                            onPrevMonth = {
                                if (month == 1) {
                                    year -= 1
                                    month = 12
                                } else {
                                    month -= 1
                                }
                                day = clampDay(year, month, day)
                            },
                            onNextMonth = {
                                if (month == 12) {
                                    year += 1
                                    month = 1
                                } else {
                                    month += 1
                                }
                                day = clampDay(year, month, day)
                            },
                            onNextYear = {
                                year += 1
                                day = clampDay(year, month, day)
                            },
                            onTodayClick = {
                                year = todayCal.get(Calendar.YEAR)
                                month = todayCal.get(Calendar.MONTH) + 1
                                day = todayCal.get(Calendar.DAY_OF_MONTH)
                            }
                        )

                        val currentSelectedMillis = remember(year, month, day) {
                            Calendar.getInstance().apply {
                                set(year, month - 1, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }

                        MonthCalendarGrid(
                            viewYear = year,
                            viewMonth = month,
                            selectedDatesMillis = setOf(currentSelectedMillis),
                            onDateToggled = { clickedMillis ->
                                val c = Calendar.getInstance().apply { timeInMillis = clickedMillis }
                                year = c.get(Calendar.YEAR)
                                month = c.get(Calendar.MONTH) + 1
                                day = c.get(Calendar.DAY_OF_MONTH)
                            }
                        )
                    }

                    // Stepper for Year, Month, Day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Year
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("年：", style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = {
                                year -= 1
                                day = clampDay(year, month, day)
                            }, modifier = Modifier.size(28.dp)) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Text("$year", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = {
                                year += 1
                                day = clampDay(year, month, day)
                            }, modifier = Modifier.size(28.dp)) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Month
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("月：", style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = {
                                if (month == 1) {
                                    year -= 1
                                    month = 12
                                } else {
                                    month -= 1
                                }
                                day = clampDay(year, month, day)
                            }, modifier = Modifier.size(28.dp)) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Text(String.format(Locale.getDefault(), "%02d", month), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = {
                                if (month == 12) {
                                    year += 1
                                    month = 1
                                } else {
                                    month += 1
                                }
                                day = clampDay(year, month, day)
                            }, modifier = Modifier.size(28.dp)) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Day
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("日：", style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = {
                                val c = Calendar.getInstance().apply {
                                    set(year, month - 1, day)
                                    add(Calendar.DAY_OF_MONTH, -1)
                                }
                                year = c.get(Calendar.YEAR)
                                month = c.get(Calendar.MONTH) + 1
                                day = c.get(Calendar.DAY_OF_MONTH)
                            }, modifier = Modifier.size(28.dp)) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Text(String.format(Locale.getDefault(), "%02d", day), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = {
                                val c = Calendar.getInstance().apply {
                                    set(year, month - 1, day)
                                    add(Calendar.DAY_OF_MONTH, 1)
                                }
                                year = c.get(Calendar.YEAR)
                                month = c.get(Calendar.MONTH) + 1
                                day = c.get(Calendar.DAY_OF_MONTH)
                            }, modifier = Modifier.size(28.dp)) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Stepper for Hour, Minute
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("時：", style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = { hour = if (hour > 0) hour - 1 else 23 }, modifier = Modifier.size(28.dp)) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Text(String.format(Locale.getDefault(), "%02d", hour), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = { hour = if (hour < 23) hour + 1 else 0 }, modifier = Modifier.size(28.dp)) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Minute
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("分：", style = MaterialTheme.typography.labelSmall)
                            IconButton(onClick = { minute = if (minute >= 5) minute - 5 else 55 }, modifier = Modifier.size(28.dp)) {
                                Text("-", fontWeight = FontWeight.Bold)
                            }
                            Text(String.format(Locale.getDefault(), "%02d", minute), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = { minute = if (minute <= 50) minute + 5 else 0 }, modifier = Modifier.size(28.dp)) {
                                Text("+", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("分類標籤") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val triggerCal = Calendar.getInstance().apply {
                                    set(year, month - 1, day, hour, minute, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onSave(
                                    reminder.copy(
                                        title = title.trim(),
                                        message = message.trim(),
                                        triggerAt = triggerCal.timeInMillis,
                                        tag = tag.trim().ifEmpty { "提醒" },
                                        color = selectedColor
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("更新儲存")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for editing an existing To-Do (title, description, due date, tag, color).
 * Due date can be freely adjusted by year/month/day or directly picked from interactive calendar.
 */
@Composable
fun EditToDoDialog(
    toDo: ToDo,
    onDismiss: () -> Unit,
    onSave: (ToDo) -> Unit
) {
    var title by remember { mutableStateOf(toDo.title) }
    var description by remember { mutableStateOf(toDo.description) }
    var tag by remember { mutableStateOf(toDo.tag) }
    var selectedColor by remember { mutableLongStateOf(toDo.color) }

    val initialCal = remember(toDo.dueDate) {
        Calendar.getInstance().apply {
            if (toDo.dueDate != null) {
                timeInMillis = toDo.dueDate
            }
        }
    }

    var hasDueDate by remember { mutableStateOf(toDo.dueDate != null) }
    var year by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var month by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH) + 1) }
    var day by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var showCalendarPicker by remember { mutableStateOf(false) }

    val todayCal = remember { Calendar.getInstance() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "✏️ 編輯待做事項",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("事項名稱 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("詳細說明") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Due Date section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = hasDueDate,
                                onCheckedChange = { hasDueDate = it }
                            )
                            Text("設定到期日", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }

                        if (hasDueDate) {
                            TextButton(onClick = { showCalendarPicker = !showCalendarPicker }) {
                                Text(
                                    if (showCalendarPicker) "收起月曆" else "展開月曆選擇",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    if (hasDueDate) {
                        Text(
                            text = "📅 ${year}年${month}月${day}日",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (showCalendarPicker) {
                            MonthNavigatorHeader(
                                year = year,
                                month = month,
                                onPrevYear = {
                                    year -= 1
                                    day = clampDay(year, month, day)
                                },
                                onPrevMonth = {
                                    if (month == 1) {
                                        year -= 1
                                        month = 12
                                    } else {
                                        month -= 1
                                    }
                                    day = clampDay(year, month, day)
                                },
                                onNextMonth = {
                                    if (month == 12) {
                                        year += 1
                                        month = 1
                                    } else {
                                        month += 1
                                    }
                                    day = clampDay(year, month, day)
                                },
                                onNextYear = {
                                    year += 1
                                    day = clampDay(year, month, day)
                                },
                                onTodayClick = {
                                    year = todayCal.get(Calendar.YEAR)
                                    month = todayCal.get(Calendar.MONTH) + 1
                                    day = todayCal.get(Calendar.DAY_OF_MONTH)
                                }
                            )

                            val currentSelectedMillis = remember(year, month, day) {
                                Calendar.getInstance().apply {
                                    set(year, month - 1, day, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }

                            MonthCalendarGrid(
                                viewYear = year,
                                viewMonth = month,
                                selectedDatesMillis = setOf(currentSelectedMillis),
                                onDateToggled = { clickedMillis ->
                                    val c = Calendar.getInstance().apply { timeInMillis = clickedMillis }
                                    year = c.get(Calendar.YEAR)
                                    month = c.get(Calendar.MONTH) + 1
                                    day = c.get(Calendar.DAY_OF_MONTH)
                                }
                            )
                        }

                        // Year, Month, Day steppers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Year
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("年：", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = {
                                    year -= 1
                                    day = clampDay(year, month, day)
                                }, modifier = Modifier.size(28.dp)) {
                                    Text("-", fontWeight = FontWeight.Bold)
                                }
                                Text("$year", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = {
                                    year += 1
                                    day = clampDay(year, month, day)
                                }, modifier = Modifier.size(28.dp)) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Month
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("月：", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = {
                                    if (month == 1) {
                                        year -= 1
                                        month = 12
                                    } else {
                                        month -= 1
                                    }
                                    day = clampDay(year, month, day)
                                }, modifier = Modifier.size(28.dp)) {
                                    Text("-", fontWeight = FontWeight.Bold)
                                }
                                Text(String.format(Locale.getDefault(), "%02d", month), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = {
                                    if (month == 12) {
                                        year += 1
                                        month = 1
                                    } else {
                                        month += 1
                                    }
                                    day = clampDay(year, month, day)
                                }, modifier = Modifier.size(28.dp)) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Day
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("日：", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = {
                                    val c = Calendar.getInstance().apply {
                                        set(year, month - 1, day)
                                        add(Calendar.DAY_OF_MONTH, -1)
                                    }
                                    year = c.get(Calendar.YEAR)
                                    month = c.get(Calendar.MONTH) + 1
                                    day = c.get(Calendar.DAY_OF_MONTH)
                                }, modifier = Modifier.size(28.dp)) {
                                    Text("-", fontWeight = FontWeight.Bold)
                                }
                                Text(String.format(Locale.getDefault(), "%02d", day), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                IconButton(onClick = {
                                    val c = Calendar.getInstance().apply {
                                        set(year, month - 1, day)
                                        add(Calendar.DAY_OF_MONTH, 1)
                                    }
                                    year = c.get(Calendar.YEAR)
                                    month = c.get(Calendar.MONTH) + 1
                                    day = c.get(Calendar.DAY_OF_MONTH)
                                }, modifier = Modifier.size(28.dp)) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("分類標籤") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val dueMillis = if (hasDueDate) {
                                    Calendar.getInstance().apply {
                                        set(year, month - 1, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                } else null

                                onSave(
                                    toDo.copy(
                                        title = title.trim(),
                                        description = description.trim(),
                                        dueDate = dueMillis,
                                        tag = tag.trim().ifEmpty { "日常" },
                                        color = selectedColor
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("更新儲存")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for editing an existing Schedule Time Block.
 */
@Composable
fun EditTimeBlockDialog(
    timeBlock: TimeBlock,
    onDismiss: () -> Unit,
    onSave: (TimeBlock) -> Unit
) {
    var title by remember { mutableStateOf(timeBlock.title) }
    var startTime by remember { mutableStateOf(timeBlock.startTime) }
    var endTime by remember { mutableStateOf(timeBlock.endTime) }
    var description by remember { mutableStateOf(timeBlock.description) }
    var tag by remember { mutableStateOf(timeBlock.tag) }
    var selectedColor by remember { mutableLongStateOf(timeBlock.color) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✏️ 編輯行程區塊",
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
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("結束時間") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("分類標籤") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    label = { Text("備註內容") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(
                                    timeBlock.copy(
                                        title = title.trim(),
                                        startTime = startTime.trim(),
                                        endTime = endTime.trim(),
                                        description = description.trim(),
                                        tag = tag.trim().ifEmpty { "工作" },
                                        color = selectedColor
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("更新儲存")
                    }
                }
            }
        }
    }
}
