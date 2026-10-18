package com.example.notepad.calendar

data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean
) {
    fun getFormattedString(): String {
        return "${year}年${if (isLeapMonth) "閏" else ""}${month}月${day}日"
    }
}
