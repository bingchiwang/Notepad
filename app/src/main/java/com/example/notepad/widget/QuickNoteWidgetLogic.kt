package com.example.notepad.widget

import com.example.notepad.data.local.entity.Category
import com.example.notepad.data.local.entity.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 快速記事桌面小工具之分類顯示篩選模式
 */
sealed interface WidgetCategoryFilter {
    /** 顯示所有分類與未分類之全部記事 */
    data object All : WidgetCategoryFilter

    /** 僅顯示未歸屬分類（categoryId = null）之記事 */
    data object Uncategorized : WidgetCategoryFilter

    /** 僅顯示指定分類 ID 下之記事 */
    data class CategoryFilter(val categoryId: Int) : WidgetCategoryFilter
}

/**
 * 桌面小工具單筆記事展示模型
 */
data class WidgetNoteItem(
    val id: Int,
    val title: String,
    val contentPreview: String,
    val isPinned: Boolean,
    val categoryName: String?,
    val accentColor: Long,
    val formattedTime: String
)

/**
 * 經篩選、排序與防錯處理後的桌面小工具完整呈現狀態
 */
data class ResolvedWidgetState(
    val filterLabel: String,
    val effectiveFilter: WidgetCategoryFilter,
    val prefillCategoryId: Int?,
    val notes: List<WidgetNoteItem>,
    val isCategoryMissingFallback: Boolean
)

/**
 * 快速記事桌面小工具核心業務邏輯與格式化工具（純 Kotlin 實作，支援完整單元測試）
 */
object QuickNoteWidgetLogic {

    const val DEFAULT_MAX_ITEMS = 12
    private const val MODE_ALL = "ALL"
    private const val MODE_UNCATEGORIZED = "UNCATEGORIZED"
    private const val MODE_CATEGORY_PREFIX = "CAT:"

    /**
     * 將篩選設定編碼為偏好儲存字串
     */
    fun encodeFilter(filter: WidgetCategoryFilter): String {
        return when (filter) {
            WidgetCategoryFilter.All -> MODE_ALL
            WidgetCategoryFilter.Uncategorized -> MODE_UNCATEGORIZED
            is WidgetCategoryFilter.CategoryFilter -> "$MODE_CATEGORY_PREFIX${filter.categoryId}"
        }
    }

    /**
     * 從偏好儲存字串還原小工具分類篩選設定，格式異常時安全退回 All
     */
    fun decodeFilter(raw: String?): WidgetCategoryFilter {
        if (raw.isNullOrBlank() || raw == MODE_ALL) return WidgetCategoryFilter.All
        if (raw == MODE_UNCATEGORIZED) return WidgetCategoryFilter.Uncategorized
        if (raw.startsWith(MODE_CATEGORY_PREFIX)) {
            val id = raw.removePrefix(MODE_CATEGORY_PREFIX).toIntOrNull()
            if (id != null && id > 0) {
                return WidgetCategoryFilter.CategoryFilter(id)
            }
        }
        return WidgetCategoryFilter.All
    }

    /**
     * 依據請求之分類篩選條件、既有分類與記事清單，產生小工具顯示狀態：
     * 1. 若原本指定的分類已被刪除，自動平滑退回顯示「全部記事」並標示退回狀態
     * 2. 依「釘選置頂優先，其次依最新修改時間降冪」排列並擷取最近筆數
     * 3. 保留空白標題／空白內文規範，轉換為親切可讀預覽文字
     */
    fun resolveWidgetState(
        notes: List<Note>,
        categories: List<Category>,
        requestedFilter: WidgetCategoryFilter,
        maxItems: Int = DEFAULT_MAX_ITEMS,
        timeFormatter: (Long) -> String = ::formatWidgetTimestamp
    ): ResolvedWidgetState {
        val categoryMap = categories.associateBy { it.id }

        var fallbackTriggered = false
        val effectiveFilter = when (requestedFilter) {
            is WidgetCategoryFilter.CategoryFilter -> {
                if (categoryMap.containsKey(requestedFilter.categoryId)) {
                    requestedFilter
                } else {
                    fallbackTriggered = true
                    WidgetCategoryFilter.All
                }
            }
            else -> requestedFilter
        }

        val filteredNotes = when (effectiveFilter) {
            WidgetCategoryFilter.All -> notes
            WidgetCategoryFilter.Uncategorized -> notes.filter { it.categoryId == null }
            is WidgetCategoryFilter.CategoryFilter -> notes.filter { it.categoryId == effectiveFilter.categoryId }
        }

        val sortedNotes = filteredNotes
            .sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { it.updatedAt }
                    .thenByDescending { it.id }
            )
            .take(maxItems.coerceAtLeast(1))

        val items = sortedNotes.map { note ->
            val categoryName = note.categoryId?.let { categoryMap[it]?.name }
            WidgetNoteItem(
                id = note.id,
                title = note.title.trim().ifEmpty { "（無標題記事）" },
                contentPreview = formatContentPreview(note.content),
                isPinned = note.isPinned,
                categoryName = categoryName,
                accentColor = note.color,
                formattedTime = timeFormatter(note.updatedAt)
            )
        }

        val filterLabel = when (effectiveFilter) {
            WidgetCategoryFilter.All -> "全部記事"
            WidgetCategoryFilter.Uncategorized -> "未分類"
            is WidgetCategoryFilter.CategoryFilter -> categoryMap[effectiveFilter.categoryId]?.name ?: "全部記事"
        }

        val prefillCategoryId = (effectiveFilter as? WidgetCategoryFilter.CategoryFilter)?.categoryId

        return ResolvedWidgetState(
            filterLabel = filterLabel,
            effectiveFilter = effectiveFilter,
            prefillCategoryId = prefillCategoryId,
            notes = items,
            isCategoryMissingFallback = fallbackTriggered
        )
    }

    /**
     * 將記事內文摺疊整理為適合桌面小工具卡片顯示的簡潔摘要
     */
    fun formatContentPreview(content: String, maxLength: Int = 90): String {
        val normalized = content
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        if (normalized.isEmpty()) return "（尚無內文）"
        return if (normalized.length > maxLength) {
            normalized.take(maxLength).trimEnd() + "…"
        } else {
            normalized
        }
    }

    /**
     * 格式化小工具卡片之最近修改時間
     */
    fun formatWidgetTimestamp(timestamp: Long): String {
        return SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
