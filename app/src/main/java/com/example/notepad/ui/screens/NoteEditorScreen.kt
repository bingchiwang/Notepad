package com.example.notepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.preferences.NoteDraft
import com.example.notepad.data.preferences.NoteDraftPreferences
import com.example.notepad.data.repository.NoteRepository
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
 */
@Composable
fun NoteEditorScreen(
    noteId: Int?,
    noteRepository: NoteRepository,
    noteDraftPreferences: NoteDraftPreferences,
    onNavigateBack: () -> Unit
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
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedColor by remember { mutableLongStateOf(PRESET_COLORS.first().colorLong) }
    var isPinned by remember { mutableStateOf(false) }

    var baselineTitle by remember { mutableStateOf("") }
    var baselineContent by remember { mutableStateOf("") }
    var baselineCategoryId by remember { mutableStateOf<Int?>(null) }
    var baselineColor by remember { mutableLongStateOf(PRESET_COLORS.first().colorLong) }
    var baselinePinned by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

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
            selectedCategoryId = draft.categoryId
            selectedColor = draft.color
            isPinned = draft.isPinned
            restoredFromDraft = true
        } else if (existingNote != null) {
            title = existingNote.title
            content = existingNote.content
            selectedCategoryId = existingNote.categoryId
            selectedColor = existingNote.color
            isPinned = existingNote.isPinned
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
                                                createdAt = now,
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

                // 內文輸入（允許為空白）
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("記事內容（可留白）") },
                    placeholder = { Text("寫下您的筆記、想法或重點...") },
                    minLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
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
