package com.example.notepad.widget

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.notepad.R
import com.example.notepad.data.local.entity.Priority

/**
 * 待辦事項小工具單列項目介面繫結共用元件（同時供 RemoteViewsService 與 API 31+ RemoteCollectionItems 使用，落實 DRY）
 */
object ToDoWidgetRowBinder {

    fun bindRow(context: Context, item: WidgetToDoItem): RemoteViews {
        val (barColorRes, badgeBgRes, badgeTextColorRes) = when (item.priority) {
            Priority.HIGH -> Triple(
                R.color.widget_priority_high_bar,
                R.drawable.widget_priority_high_bg,
                R.color.widget_priority_high_text
            )
            Priority.MEDIUM -> Triple(
                R.color.widget_priority_medium_bar,
                R.drawable.widget_priority_medium_bg,
                R.color.widget_priority_medium_text
            )
            Priority.LOW -> Triple(
                R.color.widget_priority_low_bar,
                R.drawable.widget_priority_low_bg,
                R.color.widget_priority_low_text
            )
        }

        val dueTextColorRes = when (item.dueDateUrgency) {
            DueDateUrgency.OVERDUE -> R.color.widget_due_overdue_text
            DueDateUrgency.TODAY, DueDateUrgency.TOMORROW -> R.color.widget_due_soon_text
            DueDateUrgency.UPCOMING, DueDateUrgency.NONE -> R.color.widget_text_secondary
        }

        return RemoteViews(context.packageName, R.layout.widget_todo_item).apply {
            setInt(
                R.id.widget_todo_priority_bar,
                "setBackgroundColor",
                ContextCompat.getColor(context, barColorRes)
            )
            setTextViewText(R.id.widget_todo_title, item.title)

            setTextViewText(R.id.widget_todo_priority_badge, item.priorityLabel)
            setInt(R.id.widget_todo_priority_badge, "setBackgroundResource", badgeBgRes)
            setTextColor(
                R.id.widget_todo_priority_badge,
                ContextCompat.getColor(context, badgeTextColorRes)
            )

            setTextViewText(R.id.widget_todo_due_date, item.dueDateText)
            setTextColor(
                R.id.widget_todo_due_date,
                ContextCompat.getColor(context, dueTextColorRes)
            )

            if (item.tagText.isNotEmpty()) {
                setViewVisibility(R.id.widget_todo_tag, View.VISIBLE)
                setTextViewText(R.id.widget_todo_tag, item.tagText)
            } else {
                setViewVisibility(R.id.widget_todo_tag, View.GONE)
            }
        }
    }
}
