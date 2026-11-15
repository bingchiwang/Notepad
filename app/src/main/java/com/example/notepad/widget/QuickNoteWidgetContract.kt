package com.example.notepad.widget

import android.content.Context
import android.content.Intent
import com.example.notepad.MainActivity

/**
 * 桌面快速記事小工具深度連結動作與參數契約
 */
sealed interface WidgetDeepLink {
    /** 點擊小工具「新增」按鈕後直接開啟空白記事編輯器（可帶入預選分類） */
    data class CreateNote(val categoryId: Int? = null) : WidgetDeepLink

    /** 點擊小工具內任一記事項目後直接進入該篇記事編輯頁面 */
    data class EditNote(val noteId: Int) : WidgetDeepLink
}

object QuickNoteWidgetContract {
    const val EXTRA_WIDGET_ACTION = "com.example.notepad.EXTRA_WIDGET_ACTION"
    const val EXTRA_WIDGET_NOTE_ID = "com.example.notepad.EXTRA_WIDGET_NOTE_ID"
    const val EXTRA_WIDGET_CATEGORY_ID = "com.example.notepad.EXTRA_WIDGET_CATEGORY_ID"

    const val ACTION_CREATE_NOTE = "CREATE_NOTE"
    const val ACTION_EDIT_NOTE = "EDIT_NOTE"

    const val INVALID_ID = -1

    /**
     * 建立開啟主程式「新增記事編輯器」的明確 Intent
     */
    fun buildCreateNoteIntent(context: Context, categoryId: Int?): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ACTION, ACTION_CREATE_NOTE)
            if (categoryId != null && categoryId > 0) {
                putExtra(EXTRA_WIDGET_CATEGORY_ID, categoryId)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    /**
     * 建立供 RemoteViews ListView 使用之「編輯特定記事」Fill-In Intent
     */
    fun buildEditNoteFillInIntent(noteId: Int): Intent {
        return Intent().apply {
            putExtra(EXTRA_WIDGET_ACTION, ACTION_EDIT_NOTE)
            putExtra(EXTRA_WIDGET_NOTE_ID, noteId)
        }
    }

    /**
     * 由啟動 MainActivity 之 Intent 解析來自桌面小工具的導航請求
     */
    fun parseWidgetDeepLink(intent: Intent?): WidgetDeepLink? {
        if (intent == null) return null
        val action = intent.getStringExtra(EXTRA_WIDGET_ACTION)
        val noteId = intent.getIntExtra(EXTRA_WIDGET_NOTE_ID, INVALID_ID)
        val categoryId = intent.getIntExtra(EXTRA_WIDGET_CATEGORY_ID, INVALID_ID)
        return parseWidgetDeepLink(action = action, noteId = noteId, categoryId = categoryId)
    }

    /**
     * 純邏輯解析桌面小工具導航動作與參數（方便單元測試驗證）
     */
    fun parseWidgetDeepLink(
        action: String?,
        noteId: Int = INVALID_ID,
        categoryId: Int = INVALID_ID
    ): WidgetDeepLink? {
        return when (action) {
            ACTION_CREATE_NOTE -> WidgetDeepLink.CreateNote(
                categoryId = categoryId.takeIf { it > 0 }
            )
            ACTION_EDIT_NOTE -> noteId.takeIf { it > 0 }?.let { WidgetDeepLink.EditNote(it) }
            else -> null
        }
    }
}
