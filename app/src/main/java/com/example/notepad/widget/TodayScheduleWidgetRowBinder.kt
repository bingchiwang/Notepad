package com.example.notepad.widget

import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.example.notepad.R

/**
 * 今日行程小工具多型態列表列共用繫結器（同時服務 RemoteViewsService 與 API 31+ RemoteCollectionItems）
 */
object TodayScheduleWidgetRowBinder {

    const val VIEW_TYPE_SECTION_HEADER = 0
    const val VIEW_TYPE_DUE_TODO = 1
    const val VIEW_TYPE_UPCOMING_REMINDER = 2
    const val VIEW_TYPE_EMPTY_NOTICE = 3
    const val VIEW_TYPE_COUNT = 4

    fun viewTypeOf(row: TodayScheduleRow): Int = when (row) {
        is TodayScheduleRow.SectionHeader -> VIEW_TYPE_SECTION_HEADER
        is TodayScheduleRow.DueTodayToDoRow -> VIEW_TYPE_DUE_TODO
        is TodayScheduleRow.UpcomingReminderRow -> VIEW_TYPE_UPCOMING_REMINDER
        is TodayScheduleRow.EmptyNoticeRow -> VIEW_TYPE_EMPTY_NOTICE
    }

    fun bindRow(context: Context, row: TodayScheduleRow): RemoteViews {
        return when (row) {
            is TodayScheduleRow.SectionHeader -> {
                RemoteViews(context.packageName, R.layout.widget_today_section_header).apply {
                    setTextViewText(R.id.widget_today_section_title, row.title)
                    setTextViewText(R.id.widget_today_section_count, row.countText)
                }
            }
            is TodayScheduleRow.DueTodayToDoRow -> {
                // 直接重用 ToDoWidgetRowBinder 保持優先級與截止日呈現 100% 一致
                ToDoWidgetRowBinder.bindRow(context, row.item)
            }
            is TodayScheduleRow.UpcomingReminderRow -> {
                val reminder = row.reminder
                RemoteViews(context.packageName, R.layout.widget_today_reminder_item).apply {
                    setInt(R.id.widget_reminder_color_bar, "setBackgroundColor", reminder.accentColor.toInt())
                    setTextViewText(R.id.widget_reminder_title, reminder.title)
                    setTextViewText(R.id.widget_reminder_time, reminder.timeText)

                    if (reminder.isToday) {
                        setInt(R.id.widget_reminder_time, "setBackgroundResource", R.drawable.widget_chip_bg)
                        setTextColor(
                            R.id.widget_reminder_time,
                            ContextCompat.getColor(context, R.color.widget_reminder_today_text)
                        )
                    } else {
                        setInt(R.id.widget_reminder_time, "setBackgroundResource", R.drawable.widget_chip_bg)
                        setTextColor(
                            R.id.widget_reminder_time,
                            ContextCompat.getColor(context, R.color.widget_chip_text)
                        )
                    }

                    if (reminder.messagePreview.isNotEmpty()) {
                        setViewVisibility(R.id.widget_reminder_message, View.VISIBLE)
                        setTextViewText(R.id.widget_reminder_message, reminder.messagePreview)
                    } else {
                        setViewVisibility(R.id.widget_reminder_message, View.GONE)
                    }
                }
            }
            is TodayScheduleRow.EmptyNoticeRow -> {
                RemoteViews(context.packageName, R.layout.widget_today_empty_row).apply {
                    setTextViewText(R.id.widget_today_empty_row_text, row.message)
                }
            }
        }
    }
}
