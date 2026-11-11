package com.example.notepad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import com.example.notepad.data.preferences.NoteDraft
import com.example.notepad.data.preferences.NoteDraftPreferences
import com.example.notepad.data.repository.NoteRepository
import com.example.notepad.ui.components.AppButtonThemeDefaults
import com.example.notepad.ui.components.ColorPickerRow
import com.example.notepad.ui.components.ColorTag
import com.example.notepad.ui.components.ConfirmDeleteDialog
import com.example.notepad.ui.components.DateFormatter
import com.example.notepad.ui.components.PRESET_COLORS
import com.example.notepad.ui.components.showFeedback
import kotlinx.coroutines.launch

/**
 * 記事管理主畫面（遵循規範 4.1 記事管理 & 4.3 分類搜尋）
 * - 支援標題與內文全文即時搜尋（依最新修改時間排序，查無結果顯示清晰提示）
 * - 支援分類篩選、分類新增（防重複檢核）與分類刪除連動處理
 * - 釘選記事自動置頂，並支援一鍵切換釘選
 * - 偵測未保存草稿並提供一鍵恢復編輯，避免切換頁面遺失內容
 */
@Composable
fun NotesScreen(
    noteRepository: NoteRepository,
    noteDraftPreferences: NoteDraftPreferences,
    onAddNoteClick: () -> Unit,
    onNoteClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    // null = 全部, -1 = 未分類, >0 = 特定分類 ID
    var selectedCategoryFilter by remember { mutableStateOf<Int?>(null) }

    val categories by noteRepository.allCategories.collectAsState(initial = emptyList())
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    val activeDraft by noteDraftPreferences.draftFlow.collectAsState(initial = NoteDraft())

    val notesFlow = remember(searchQuery, selectedCategoryFilter) {
        if (searchQuery.isNotBlank()) {
            val targetCategoryId = selectedCategoryFilter?.takeIf { it > 0 }
            noteRepository.searchNotes(searchQuery, targetCategoryId)
        } else {
            when {
                selectedCategoryFilter == null -> noteRepository.allNotes
                selectedCategoryFilter == -1 -> noteRepository.getUncategorizedNotes()
                else -> noteRepository.getNotesByCategory(selectedCategoryFilter!!)
            }
        }
    }
    val rawNotes by notesFlow.collectAsState(initial = emptyList())

    // 若使用者在「未分類」篩選條件下進行搜尋，於端側精確保留未分類結果
    val displayedNotes = remember(rawNotes, searchQuery, selectedCategoryFilter) {
        if (searchQuery.isNotBlank() && selectedCategoryFilter == -1) {
            rawNotes.filter { it.categoryId == null }
        } else {
            rawNotes
        }
    }

    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var deletingNote by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNoteClick,
                containerColor = AppButtonThemeDefaults.fabContainerColor(),
                contentColor = AppButtonThemeDefaults.fabContentColor(),
                icon = { Icon(Icons.Default.Add, contentDescription = "新增記事") },
                text = { Text("新增記事") }
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
            // === 4.3 全文搜尋列（搜尋標題與內文） ===
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜尋記事標題或內文...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除搜尋")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // === 4.3 分類篩選晶片與分類管理入口 ===
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("全部") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == -1,
                        onClick = { selectedCategoryFilter = -1 },
                        label = { Text("未分類") }
                    )
                }
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategoryFilter == category.id,
                        onClick = { selectedCategoryFilter = category.id },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(category.color))
                            )
                        },
                        label = { Text(category.name) }
                    )
                }
                item {
                    AssistChip(
                        onClick = { showCategoryManagerDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "管理分類",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("管理分類") }
                    )
                }
            }

            // === 4.1 未保存草稿恢復提示橫幅 ===
            if (activeDraft.hasActiveDraft) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📝 有尚未保存的記事草稿",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = activeDraft.title.ifBlank {
                                    activeDraft.content.take(24).ifBlank { "（空白草稿）" }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        noteDraftPreferences.clearDraft()
                                        showFeedback(context, "已捨棄草稿")
                                    }
                                }
                            ) {
                                Text("捨棄")
                            }
                            Button(
                                onClick = {
                                    val draftNoteId = activeDraft.editingNoteId
                                    if (draftNoteId != null) {
                                        onNoteClick(draftNoteId)
                                    } else {
                                        onAddNoteClick()
                                    }
                                },
                                colors = AppButtonThemeDefaults.filledColors(),
                                border = AppButtonThemeDefaults.filledBorder(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("繼續編輯")
                            }
                        }
                    }
                }
            }

            // === 記事列表或搜尋／空狀態提示 ===
            if (displayedNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        if (searchQuery.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "找不到符合「$searchQuery」的記事",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "系統已比對所有記事標題與內文，請嘗試其他關鍵字或重設分類篩選。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            OutlinedButton(onClick = {
                                searchQuery = ""
                                selectedCategoryFilter = null
                            }) {
                                Text("清除搜尋條件")
                            }
                        } else {
                            Text(
                                text = "目前此區尚無記事",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "點擊右下角「+ 新增記事」即可隨時記錄想法或筆記",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = "🔍 找到 ${displayedNotes.size} 筆相關記事（依最新修改時間排序）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedNotes, key = { it.id }) { note ->
                        val category = note.categoryId?.let { categoryMap[it] }
                        NoteCard(
                            note = note,
                            categoryName = category?.name,
                            categoryColor = category?.color?.let { Color(it) },
                            onClick = { onNoteClick(note.id) },
                            onTogglePin = {
                                coroutineScope.launch {
                                    try {
                                        noteRepository.toggleNotePinned(note)
                                        showFeedback(
                                            context,
                                            if (!note.isPinned) "已釘選至列表前方" else "已取消釘選"
                                        )
                                    } catch (e: Exception) {
                                        showFeedback(context, "操作失敗: ${e.localizedMessage ?: "發生錯誤"}")
                                    }
                                }
                            },
                            onDelete = { deletingNote = note }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(76.dp))
                    }
                }
            }
        }
    }

    // === 刪除記事二次確認對話框（操作規範 3） ===
    deletingNote?.let { noteToDelete ->
        ConfirmDeleteDialog(
            itemName = noteToDelete.title.ifBlank { "無標題記事" },
            onConfirm = {
                coroutineScope.launch {
                    try {
                        noteRepository.deleteNote(noteToDelete)
                        if (activeDraft.hasActiveDraft && activeDraft.editingNoteId == noteToDelete.id) {
                            noteDraftPreferences.clearDraft()
                        }
                        showFeedback(context, "已刪除記事")
                        deletingNote = null
                    } catch (e: Exception) {
                        showFeedback(context, "刪除失敗: ${e.localizedMessage ?: "發生錯誤"}")
                    }
                }
            },
            onDismiss = { deletingNote = null }
        )
    }

    // === 分類管理對話框（規範 4.3） ===
    if (showCategoryManagerDialog) {
        CategoryManagerDialog(
            categories = categories,
            noteRepository = noteRepository,
            onCategoryDeleted = { deletedCategoryId ->
                if (selectedCategoryFilter == deletedCategoryId) {
                    selectedCategoryFilter = null
                }
            },
            onDismiss = { showCategoryManagerDialog = false }
        )
    }
}

/**
 * 單筆記事卡片元件（顯示釘選狀態、分類、修改時間與快速操作）
 */
@Composable
fun NoteCard(
    note: Note,
    categoryName: String?,
    categoryColor: Color?,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 記事左側色條
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(note.color))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "已釘選",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = note.title.ifBlank { "（無標題記事）" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (note.title.isBlank()) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (note.isPinned) "取消釘選" else "釘選記事",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "刪除記事",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Text(
                    text = note.content.ifBlank { "（尚無內文）" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (note.content.isBlank()) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (categoryName != null && categoryColor != null) {
                        ColorTag(text = categoryName, tagColor = categoryColor)
                    } else {
                        Text(
                            text = "未分類",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = "修改於 ${DateFormatter.formatDateTime(note.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * 分類管理對話框（規範 4.3：名稱不得重複、刪除時處理所屬記事）
 */
@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    noteRepository: NoteRepository,
    onCategoryDeleted: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(PRESET_COLORS.first().colorLong) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var deletingCategory by remember { mutableStateOf<Category?>(null) }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏷️ 管理記事分類",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = {
                        newCategoryName = it
                        errorMessage = null
                    },
                    label = { Text("新增分類名稱 *") },
                    placeholder = { Text("例如: 工作、靈感、旅遊") },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg ->
                        { Text(msg, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ColorPickerRow(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                noteRepository.insertCategory(newCategoryName, selectedColor)
                                showFeedback(context, "已新增分類「${newCategoryName.trim()}」")
                                newCategoryName = ""
                                errorMessage = null
                            } catch (e: Exception) {
                                val msg = e.localizedMessage ?: "新增分類失敗"
                                errorMessage = msg
                                showFeedback(context, msg)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = AppButtonThemeDefaults.filledColors(),
                    border = AppButtonThemeDefaults.filledBorder()
                ) {
                    Text("新增分類")
                }

                HorizontalDivider()

                Text(
                    text = "現有分類 (${categories.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (categories.isEmpty()) {
                    Text(
                        text = "尚未建立任何自訂分類",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { category ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(category.color))
                                        )
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { editingCategory = category },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "編輯分類",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { deletingCategory = category },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "刪除分類",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("關閉")
                    }
                }
            }
        }
    }

    // 編輯分類名稱／色彩對話框
    editingCategory?.let { cat ->
        var editName by remember(cat.id) { mutableStateOf(cat.name) }
        var editColor by remember(cat.id) { mutableLongStateOf(cat.color) }
        var editError by remember(cat.id) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("編輯分類", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            editError = null
                        },
                        label = { Text("分類名稱 *") },
                        isError = editError != null,
                        supportingText = editError?.let { msg ->
                            { Text(msg, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ColorPickerRow(
                        selectedColor = editColor,
                        onColorSelected = { editColor = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = AppButtonThemeDefaults.filledColors(),
                    border = AppButtonThemeDefaults.filledBorder(),
                    onClick = {
                        coroutineScope.launch {
                        try {
                            noteRepository.updateCategory(
                                cat.copy(name = editName, color = editColor)
                            )
                            showFeedback(context, "分類已更新")
                            editingCategory = null
                        } catch (e: Exception) {
                            val msg = e.localizedMessage ?: "更新分類失敗"
                            editError = msg
                            showFeedback(context, msg)
                        }
                    }
                }) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 刪除分類與記事歸屬處理確認對話框（規範 4.3）
    deletingCategory?.let { cat ->
        val otherCategories = remember(categories, cat.id) {
            categories.filter { it.id != cat.id }
        }
        var targetCategoryId by remember(cat.id) { mutableStateOf<Int?>(null) }

        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("確認刪除分類「${cat.name}」", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("刪除此分類前，請選擇此分類下原有記事的處理方式：")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { targetCategoryId = null }
                    ) {
                        RadioButton(
                            selected = targetCategoryId == null,
                            onClick = { targetCategoryId = null }
                        )
                        Text("移至「未分類」（預設保留記事）", style = MaterialTheme.typography.bodyMedium)
                    }
                    otherCategories.forEach { other ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { targetCategoryId = other.id }
                        ) {
                            RadioButton(
                                selected = targetCategoryId == other.id,
                                onClick = { targetCategoryId = other.id }
                            )
                            Text("轉移至「${other.name}」", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                noteRepository.deleteCategory(cat, targetCategoryId = targetCategoryId)
                                onCategoryDeleted(cat.id)
                                showFeedback(context, "已刪除分類，該分類下的記事已妥善重新歸類")
                                deletingCategory = null
                            } catch (e: Exception) {
                                showFeedback(context, "刪除分類失敗: ${e.localizedMessage ?: "發生錯誤"}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("確認刪除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCategory = null }) {
                    Text("取消")
                }
            }
        )
    }
}
