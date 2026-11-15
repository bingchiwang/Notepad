package com.example.notepad.widget

import android.content.Context
import android.content.Intent
import com.example.notepad.MainActivity

/**
 * 桌面小工具深度連結動作與參數契約（支援快速記事與待辦事項桌面小工具）
 */
sealed interface WidgetDeepLink {
    /** 點擊記事小工具「新增」按鈕後直接開啟空白記事編輯器（可帶入預選分類） */
    data class CreateNote(val categoryId: Int? = null) : WidgetDeepLink

    /** 點擊記事小工具內任一記事項目後直接進入該篇記事編輯頁面 */
    data class EditNote(val noteId: Int) : WidgetDeepLink

    /** 點擊待辦小工具標題或「新增」按鈕後切換至待辦分頁（可選自動彈出新增待辦視窗） */
    data class OpenToDoTab(val openAddDialog: Boolean = false) : WidgetDeepLink

    /** 點擊待辦小工具內任一任務列後直接進入該任務詳情頁 */
    data class OpenToDoDetail(val taskId: Int) : WidgetDeepLink

    /** 點擊今日行程小工具標題或進度卡後直接切換至每日時間規劃頁面 */
    data object OpenScheduleTab : WidgetDeepLink

    /** 點擊今日行程小工具內即將到來的提醒後切換至提醒事項分頁 */
    data object OpenRemindersTrack : WidgetDeepLink
}

object QuickNoteWidgetContract {
    const val EXTRA_WIDGET_ACTION = "com.example.notepad.EXTRA_WIDGET_ACTION"
    const val EXTRA_WIDGET_NOTE_ID = "com.example.notepad.EXTRA_WIDGET_NOTE_ID"
    const val EXTRA_WIDGET_CATEGORY_ID = "com.example.notepad.EXTRA_WIDGET_CATEGORY_ID"
    const val EXTRA_WIDGET_TODO_ID = "com.example.notepad.EXTRA_WIDGET_TODO_ID"
    const val EXTRA_WIDGET_OPEN_ADD_TODO = "com.example.notepad.EXTRA_WIDGET_OPEN_ADD_TODO"

    const val ACTION_CREATE_NOTE = "CREATE_NOTE"
    const val ACTION_EDIT_NOTE = "EDIT_NOTE"
    const val ACTION_OPEN_TODO_TAB = "OPEN_TODO_TAB"
    const val ACTION_OPEN_TODO_DETAIL = "OPEN_TODO_DETAIL"
    const val ACTION_OPEN_SCHEDULE_TAB = "OPEN_SCHEDULE_TAB"
    const val ACTION_OPEN_REMINDERS_TRACK = "OPEN_REMINDERS_TRACK"

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
     * 建立開啟主程式「待辦分頁（可選擇直接彈出新增待辦對話框）」的明確 Intent
     */
    fun buildOpenToDoTabIntent(context: Context, openAddDialog: Boolean): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ACTION, ACTION_OPEN_TODO_TAB)
            putExtra(EXTRA_WIDGET_OPEN_ADD_TODO, openAddDialog)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    /**
     * 建立開啟主程式「指定待辦任務詳情頁」的明確 Intent
     */
    fun buildOpenToDoDetailIntent(context: Context, taskId: Int): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ACTION, ACTION_OPEN_TODO_DETAIL)
            putExtra(EXTRA_WIDGET_TODO_ID, taskId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    /**
     * 建立開啟主程式「每日排程」分頁的明確 Intent
     */
    fun buildOpenScheduleTabIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ACTION, ACTION_OPEN_SCHEDULE_TAB)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    /**
     * 建立開啟主程式「待辦＞提醒事項」分軌的明確 Intent
     */
    fun buildOpenRemindersTrackIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ACTION, ACTION_OPEN_REMINDERS_TRACK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
        val todoId = intent.getIntExtra(EXTRA_WIDGET_TODO_ID, INVALID_ID)
        val openAddToDo = intent.getBooleanExtra(EXTRA_WIDGET_OPEN_ADD_TODO, false)
        return parseWidgetDeepLink(
            action = action,
            noteId = noteId,
            categoryId = categoryId,
            todoId = todoId,
            openAddToDo = openAddToDo
        )
    }

    /**
     * 純邏輯解析桌面小工具導航動作與參數（方便單元測試驗證）
     */
    fun parseWidgetDeepLink(
        action: String?,
        noteId: Int = INVALID_ID,
        categoryId: Int = INVALID_ID,
        todoId: Int = INVALID_ID,
        openAddToDo: Boolean = false
    ): WidgetDeepLink? {
        return when (action) {
            ACTION_CREATE_NOTE -> WidgetDeepLink.CreateNote(
                categoryId = categoryId.takeIf { it > 0 }
            )
            ACTION_EDIT_NOTE -> noteId.takeIf { it > 0 }?.let { WidgetDeepLink.EditNote(it) }
            ACTION_OPEN_TODO_TAB -> WidgetDeepLink.OpenToDoTab(openAddDialog = openAddToDo)
            ACTION_OPEN_TODO_DETAIL -> todoId.takeIf { it > 0 }?.let { WidgetDeepLink.OpenToDoDetail(it) }
            ACTION_OPEN_SCHEDULE_TAB -> WidgetDeepLink.OpenScheduleTab
            ACTION_OPEN_REMINDERS_TRACK -> WidgetDeepLink.OpenRemindersTrack
            else -> null
        }
    }
}
