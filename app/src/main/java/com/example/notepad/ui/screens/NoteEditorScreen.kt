package com.example.notepad.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.local.entity.RecurringTaskLogic
import com.example.notepad.data.preferences.NoteDraft
import com.example.notepad.data.preferences.NoteDraftPreferences
import com.example.notepad.data.repository.NoteChecklistLogic
import com.example.notepad.data.repository.NoteRepository
import com.example.notepad.data.repository.TaskRepository
import com.example.notepad.ui.components.AddTimeBlockDialog
import com.example.notepad.ui.components.AddToDoDialog
import com.example.notepad.ui.components.AppButtonThemeDefaults
import com.example.notepad.ui.components.ColorPickerRow
import com.example.notepad.ui.components.ConfirmDeleteDialog
import com.example.notepad.ui.components.DateFormatter
import com.example.notepad.ui.components.PRESET_COLORS
import com.example.notepad.ui.components.showFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 記事新增與編輯畫面（遵循規範 4.1 記事管理規範）
 * - 標題及內文均允許為空白
 * - 修改後自動更新修改時間 (updatedAt)
 * - 自動保存尚未儲存的內容至 DataStore 草稿，切換頁面或重開 App 皆不會遺失
 * - 支援 Markdown 風格核取方塊清單（`- [ ]` / `- [x]`）互動勾選、Enter 自動延續、
 *   以及選取文字／清單項目一鍵轉為待辦事項（ToDo）或排入今日時間規劃（TimeBlock）
 */
@Composable
fun NoteEditorScreen(
    noteId: Int?,
    noteRepository: NoteRepository,
    noteDraftPreferences: NoteDraftPreferences,
    taskRepository: TaskRepository? = null,
    onNavigateBack: () -> Unit,
    initialCategoryId: Int? = null,
    initialCreatedAtMillis: Long? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val categories by noteRepository.allCategories.collectAsState(initial = emptyList())

    var loadedNote by remember { mutableStateOf<Note?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var restoredFromDraft by remember { mutableStateOf(false) }
    var hasSavedOrDiscarded by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var contentFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedColor by remember { mutableLongStateOf(PRESET_COLORS.first().colorLong) }
    var isPinned by remember { mutableStateOf(false) }

    var baselineTitle by remember { mutableStateOf("") }
    var baselineContent by remember { mutableStateOf("") }
    var baselineCategoryId by remember { mutableStateOf<Int?>(null) }
    var baselineColor by remember { mutableLongStateOf(PRESET_COLORS.first().colorLong) }
    var baselinePinned by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }
    var pendingConvertToDoTitle by remember { mutableStateOf<String?>(null) }
    var pendingConvertTimeBlockTitle by remember { mutableStateOf<String?>(null) }

    // 載入既有記事或恢復草稿
    LaunchedEffect(noteId) {
        val existingNote = if (noteId != null) {
            noteRepository.getNoteById(noteId).first()
        } else null
        loadedNote = existingNote

        if (existingNote != null) {
            baselineTitle = existingNote.title
            baselineContent = existingNote.content
            baselineCategoryId = existingNote.categoryId
            baselineColor = existingNote.color
            baselinePinned = existingNote.isPinned
        }

        val draft = noteDraftPreferences.getDraftOnce()
        val matchesCurrentDraft = draft.hasActiveDraft && draft.editingNoteId == noteId

        if (matchesCurrentDraft) {
            title = draft.title
            content = draft.content
            contentFieldValue = TextFieldValue(draft.content, TextRange(draft.content.length))
            selectedCategoryId = draft.categoryId
            selectedColor = draft.color
            isPinned = draft.isPinned
            restoredFromDraft = true
        } else if (existingNote != null) {
            title = existingNote.title
            content = existingNote.content
            contentFieldValue = TextFieldValue(existingNote.content, TextRange(existingNote.content.length))
            selectedCategoryId = existingNote.categoryId
            selectedColor = existingNote.color
            isPinned = existingNote.isPinned
        } else if (initialCategoryId != null) {
            // 從指定分類的小工具點擊新增時，預帶對應分類標籤
            selectedCategoryId = initialCategoryId
            baselineCategoryId = initialCategoryId
        }

        isInitialized = true
    }

    val hasUnsavedChanges = isInitialized && (
        title != baselineTitle ||
            content != baselineContent ||
            selectedCategoryId != baselineCategoryId ||
            selectedColor != baselineColor ||
            isPinned != baselinePinned
        )

    // 即時自動保存草稿，防止切換頁面或背景回收造成資料遺失
    LaunchedEffect(isInitialized, title, content, selectedCategoryId, selectedColor, isPinned) {
        if (!isInitialized || hasSavedOrDiscarded) return@LaunchedEffect
        delay(120L)
        if (hasUnsavedChanges) {
            noteDraftPreferences.saveDraft(
                NoteDraft(
                    hasActiveDraft = true,
                    editingNoteId = noteId,
                    title = title,
                    content = content,
                    categoryId = selectedCategoryId,
                    color = selectedColor,
                    isPinned = isPinned
                )
            )
        } else if (restoredFromDraft) {
            noteDraftPreferences.clearDraft()
            restoredFromDraft = false
        }
    }

    val checklistItems = remember(content) {
        NoteChecklistLogic.parseChecklistItems(content)
    }
    val selectedTextTitle = remember(contentFieldValue) {
        if (contentFieldValue.selection.collapsed) {
            ""
        } else {
            NoteChecklistLogic.extractActionableTitle(
                content = contentFieldValue.text,
                selectionStart = contentFieldValue.selection.start,
                selectionEnd = contentFieldValue.selection.end
            )
        }
    }
    val cursorLineTitle = remember(contentFieldValue) {
        NoteChecklistLogic.extractActionableTitle(
            content = contentFieldValue.text,
            selectionStart = contentFieldValue.selection.start,
            selectionEnd = contentFieldValue.selection.end
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // === 編輯器頂部工具列 ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                if (hasUnsavedChanges && !hasSavedOrDiscarded) {
                                    noteDraftPreferences.saveDraft(
                                        NoteDraft(
                                            hasActiveDraft = true,
                                            editingNoteId = noteId,
                                            title = title,
                                            content = content,
                                            categoryId = selectedCategoryId,
                                            color = selectedColor,
                                            isPinned = isPinned
                                        )
                                    )
                                    showFeedback(context, "已自動暫存未保存草稿")
                                }
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                    Text(
                        text = if (noteId == null) "新增記事" else "編輯記事",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "取消釘選" else "釘選記事",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    if (loadedNote != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "刪除記事",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (hasUnsavedChanges) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    hasSavedOrDiscarded = true
                                    noteDraftPreferences.clearDraft()
                                    showFeedback(context, "已捨棄未保存內容")
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Text("捨棄")
                        }
                    }

                    Button(
                        colors = AppButtonThemeDefaults.filledColors(),
                        border = AppButtonThemeDefaults.filledBorder(),
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    hasSavedOrDiscarded = true
                                    val now = System.currentTimeMillis()
                                    val existing = loadedNote
                                    if (existing != null) {
                                        noteRepository.updateNote(
                                            existing.copy(
                                                title = title,
                                                content = content,
                                                isPinned = isPinned,
                                                categoryId = selectedCategoryId,
                                                color = selectedColor
                                            ),
                                            updatedAt = now
                                        )
                                    } else {
                                        noteRepository.insertNote(
                                            Note(
                                                title = title,
                                                content = content,
                                                isPinned = isPinned,
                                                categoryId = selectedCategoryId,
                                                color = selectedColor,
                                                createdAt = initialCreatedAtMillis ?: now,
                                                updatedAt = now
                                            )
                                        )
                                    }
                                    noteDraftPreferences.clearDraft()
                                    showFeedback(context, "記事儲存成功")
                                    onNavigateBack()
                                } catch (e: Exception) {
                                    hasSavedOrDiscarded = false
                                    showFeedback(context, "儲存記事失敗: ${e.localizedMessage ?: "發生錯誤"}")
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("儲存")
                    }
                }
            }

            if (restoredFromDraft) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📝 已自動還原您先前尚未保存的草稿內容",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // === 主內容編輯捲動區 ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                loadedNote?.let { existing ->
                    Text(
                        text = "最後修改時間：${DateFormatter.formatDateTime(existing.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // 所屬分類選擇
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "所屬分類：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text("未分類") }
                            )
                        }
                        items(categories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(cat.color))
                                    )
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }

                // 色彩標記
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

                // 標題輸入（允許為空白）
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("記事標題（可留白）") },
                    placeholder = { Text("輸入標題...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // === 核取方塊清單工具與選取文字一鍵轉換工具列 ===
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val (updatedText, newCursor) = NoteChecklistLogic.insertChecklistItem(
                                    content = contentFieldValue.text,
                                    selectionStart = contentFieldValue.selection.start,
                                    selectionEnd = contentFieldValue.selection.end
                                )
                                content = updatedText
                                contentFieldValue = TextFieldValue(updatedText, TextRange(newCursor))
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckBox,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("插入核取項目 (- [ ])", style = MaterialTheme.typography.labelMedium)
                        }

                        if (taskRepository != null) {
                            Box {
                                OutlinedButton(
                                    onClick = {
                                        if (cursorLineTitle.isBlank()) {
                                            showFeedback(context, "請先選取文字或將游標移至要轉換的文字行")
                                        } else {
                                            showSelectionMenu = true
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("轉待辦 / 排程", style = MaterialTheme.typography.labelMedium)
                                }
                                DropdownMenu(
                                    expanded = showSelectionMenu,
                                    onDismissRequest = { showSelectionMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("轉換為待辦事項 (ToDo)") },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null)
                                        },
                                        onClick = {
                                            showSelectionMenu = false
                                            pendingConvertToDoTitle = cursorLineTitle
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("排入今日時間規劃 (TimeBlock)") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Schedule, contentDescription = null)
                                        },
                                        onClick = {
                                            showSelectionMenu = false
                                            pendingConvertTimeBlockTitle = cursorLineTitle
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 選取文字時自動顯示高顯眼一鍵轉換快捷列
                AnimatedVisibility(visible = taskRepository != null && selectedTextTitle.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "已選取文字：「$selectedTextTitle」",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { pendingConvertToDoTitle = selectedTextTitle },
                                    colors = AppButtonThemeDefaults.filledColors(),
                                    border = AppButtonThemeDefaults.filledBorder(),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.FormatListBulleted,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("一鍵轉待辦", style = MaterialTheme.typography.labelMedium)
                                }
                                FilledTonalButton(
                                    onClick = { pendingConvertTimeBlockTitle = selectedTextTitle },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("排入今日規劃", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // 內文輸入（支援 Markdown `- [ ]` 換行自動延續與游標／選區追蹤）
                OutlinedTextField(
                    value = contentFieldValue,
                    onValueChange = { incoming ->
                        val autoContinued = NoteChecklistLogic.handleEnterAutoContinue(
                            oldContent = contentFieldValue.text,
                            newContent = incoming.text,
                            newCursor = incoming.selection.start
                        )
                        if (autoContinued != null) {
                            val (nextText, nextCursor) = autoContinued
                            content = nextText
                            contentFieldValue = TextFieldValue(nextText, TextRange(nextCursor))
                        } else {
                            content = incoming.text
                            contentFieldValue = incoming
                        }
                    },
                    label = { Text("記事內容（可留白，支援 - [ ] 核取清單）") },
                    placeholder = { Text("寫下您的筆記、想法，或輸入 - [ ] 建立核取清單...") },
                    minLines = 7,
                    modifier = Modifier.fillMaxWidth()
                )

                // === 互動式核取方塊清單卡片（可即時勾選並同步回 Markdown 內文）===
                if (checklistItems.isNotEmpty()) {
                    val completedCount = checklistItems.count { it.isChecked }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "☑ 記事內嵌待辦清單",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$completedCount / ${checklistItems.size} 已完成",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            checklistItems.forEach { item ->
                                var itemMenuExpanded by remember(item.lineIndex) { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val toggled = NoteChecklistLogic.toggleChecklistLine(
                                                content = content,
                                                lineIndex = item.lineIndex
                                            )
                                            content = toggled
                                            val clampedCursor = contentFieldValue.selection.start
                                                .coerceIn(0, toggled.length)
                                            contentFieldValue = TextFieldValue(toggled, TextRange(clampedCursor))
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = {
                                            val toggled = NoteChecklistLogic.toggleChecklistLine(
                                                content = content,
                                                lineIndex = item.lineIndex
                                            )
                                            content = toggled
                                            val clampedCursor = contentFieldValue.selection.start
                                                .coerceIn(0, toggled.length)
                                            contentFieldValue = TextFieldValue(toggled, TextRange(clampedCursor))
                                        }
                                    )
                                    Text(
                                        text = item.text.ifEmpty { "（空白待辦項目）" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (item.isChecked) {
                                            TextDecoration.LineThrough
                                        } else {
                                            TextDecoration.None
                                        },
                                        color = if (item.isChecked) {
                                            MaterialTheme.colorScheme.outline
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (taskRepository != null && item.text.isNotBlank()) {
                                        Box {
                                            IconButton(
                                                onClick = { itemMenuExpanded = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "轉換項目動作",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = itemMenuExpanded,
                                                onDismissRequest = { itemMenuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("轉為待辦事項 (ToDo)") },
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.FormatListBulleted,
                                                            contentDescription = null
                                                        )
                                                    },
                                                    onClick = {
                                                        itemMenuExpanded = false
                                                        pendingConvertToDoTitle = item.text
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("排入今日時間規劃") },
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Schedule, contentDescription = null)
                                                    },
                                                    onClick = {
                                                        itemMenuExpanded = false
                                                        pendingConvertTimeBlockTitle = item.text
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    pendingConvertToDoTitle?.let { todoTitle ->
        if (taskRepository != null) {
            AddToDoDialog(
                initialTitle = todoTitle,
                dialogTitle = "從記事轉換為待辦事項",
                onDismiss = { pendingConvertToDoTitle = null },
                onSave = { newToDo ->
                    coroutineScope.launch {
                        try {
                            taskRepository.insertToDo(newToDo)
                            showFeedback(context, "已將「${newToDo.title}」加入待辦事項")
                            pendingConvertToDoTitle = null
                        } catch (e: Exception) {
                            showFeedback(context, "轉換待辦失敗: ${e.localizedMessage ?: "發生錯誤"}")
                        }
                    }
                }
            )
        }
    }

    pendingConvertTimeBlockTitle?.let { blockTitle ->
        if (taskRepository != null) {
            val todayMillis = remember { RecurringTaskLogic.normalizeStartOfDay(System.currentTimeMillis()) }
            AddTimeBlockDialog(
                dateMillis = todayMillis,
                initialTitle = blockTitle,
                dialogTitle = "排入今日時間規劃",
                onDismiss = { pendingConvertTimeBlockTitle = null },
                onSave = { newBlock ->
                    coroutineScope.launch {
                        try {
                            taskRepository.insertTimeBlock(newBlock)
                            showFeedback(context, "已將「${newBlock.title}」排入今日時間規劃")
                            pendingConvertTimeBlockTitle = null
                        } catch (e: Exception) {
                            showFeedback(context, "排入規劃失敗: ${e.localizedMessage ?: "發生錯誤"}")
                        }
                    }
                }
            )
        }
    }

    if (showDeleteConfirm) {
        loadedNote?.let { targetNote ->
            ConfirmDeleteDialog(
                itemName = targetNote.title.ifBlank { "無標題記事" },
                onConfirm = {
                    coroutineScope.launch {
                        try {
                            hasSavedOrDiscarded = true
                            noteRepository.deleteNote(targetNote)
                            noteDraftPreferences.clearDraft()
                            showFeedback(context, "已刪除記事")
                            showDeleteConfirm = false
                            onNavigateBack()
                        } catch (e: Exception) {
                            hasSavedOrDiscarded = false
                            showFeedback(context, "刪除失敗: ${e.localizedMessage ?: "發生錯誤"}")
                        }
                    }
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}
