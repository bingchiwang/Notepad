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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notepad.data.local.entity.Reminder
import com.example.notepad.data.local.entity.TimeBlock
import com.example.notepad.data.local.entity.ToDo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Dialog for creating a Reminder that supports multi-day selection and custom notification time.
 */
@Composable
fun AddMultiDayReminderDialog(
    onDismiss: () -> Unit,
    onSave: (List<Reminder>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("提醒") }
    var selectedColor by remember { mutableLongStateOf(PRESET_COLORS[2].colorLong) } // Amber

    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }

    // Generate upcoming 14 days
    val upcomingDays = remember {
        val list = mutableListOf<Calendar>()
        val cal = Calendar.getInstance()
        for (i in 0 until 14) {
            val c = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            list.add(c)
        }
        list
    }

    // Set of selected day timestamps
    val selectedDaysMillis = remember {
        mutableStateListOf<Long>().apply {
            // Default select today
            add(upcomingDays.first().timeInMillis)
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
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
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
                    placeholder = { Text("例如: 吃維他命、交報告、晨會") },
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
                                Text("-", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            }
                            Text(
                                String.format(Locale.getDefault(), "%02d", hour),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { hour = if (hour < 23) hour + 1 else 0 }) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("分：", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { minute = if (minute >= 5) minute - 5 else 55 }) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            }
                            Text(
                                String.format(Locale.getDefault(), "%02d", minute),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { minute = if (minute <= 50) minute + 5 else 0 }) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Multi-Day Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅 選擇提醒日期 (已選 ${selectedDaysMillis.size} 天)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(onClick = {
                            selectedDaysMillis.clear()
                            upcomingDays.take(7).forEach { selectedDaysMillis.add(it.timeInMillis) }
                        }) {
                            Text("未來 7 天", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Interactive day chips for upcoming 14 days
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(upcomingDays) { calDay ->
                            val millis = calDay.timeInMillis
                            val isSelected = selectedDaysMillis.contains(millis)
                            val m = calDay.get(Calendar.MONTH) + 1
                            val d = calDay.get(Calendar.DAY_OF_MONTH)
                            val dayOfWeekName = when (calDay.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.SUNDAY -> "日"
                                Calendar.MONDAY -> "一"
                                Calendar.TUESDAY -> "二"
                                Calendar.WEDNESDAY -> "三"
                                Calendar.THURSDAY -> "四"
                                Calendar.FRIDAY -> "五"
                                Calendar.SATURDAY -> "六"
                                else -> ""
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            if (selectedDaysMillis.size > 1) {
                                                selectedDaysMillis.remove(millis)
                                            }
                                        } else {
                                            selectedDaysMillis.add(millis)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$m/$d",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "週$dayOfWeekName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }

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
                    Text(
                        text = "📅 提醒日期與時間：",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${year}年${month}月${day}日  ${String.format(Locale.getDefault(), "%02d:%02d", hour, minute)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Adjust Day
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("日：", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = {
                                val c = Calendar.getInstance().apply {
                                    set(year, month - 1, day)
                                    add(Calendar.DAY_OF_MONTH, -1)
                                }
                                year = c.get(Calendar.YEAR)
                                month = c.get(Calendar.MONTH) + 1
                                day = c.get(Calendar.DAY_OF_MONTH)
                            }) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            }
                            Text("$day 日", fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val c = Calendar.getInstance().apply {
                                    set(year, month - 1, day)
                                    add(Calendar.DAY_OF_MONTH, 1)
                                }
                                year = c.get(Calendar.YEAR)
                                month = c.get(Calendar.MONTH) + 1
                                day = c.get(Calendar.DAY_OF_MONTH)
                            }) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            }
                        }

                        // Adjust Hour
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("時間：", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { hour = if (hour > 0) hour - 1 else 23 }) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                            }
                            Text(String.format(Locale.getDefault(), "%02d:%02d", hour, minute), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { hour = if (hour < 23) hour + 1 else 0 }) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = hasDueDate,
                            onCheckedChange = { hasDueDate = it }
                        )
                        Text("設定到期日", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }

                    if (hasDueDate) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 ${year}年${month}月${day}日",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val c = Calendar.getInstance().apply {
                                        set(year, month - 1, day)
                                        add(Calendar.DAY_OF_MONTH, -1)
                                    }
                                    year = c.get(Calendar.YEAR)
                                    month = c.get(Calendar.MONTH) + 1
                                    day = c.get(Calendar.DAY_OF_MONTH)
                                }) {
                                    Text("-", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                                }
                                Text("調天", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = {
                                    val c = Calendar.getInstance().apply {
                                        set(year, month - 1, day)
                                        add(Calendar.DAY_OF_MONTH, 1)
                                    }
                                    year = c.get(Calendar.YEAR)
                                    month = c.get(Calendar.MONTH) + 1
                                    day = c.get(Calendar.DAY_OF_MONTH)
                                }) {
                                    Text("+", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
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
