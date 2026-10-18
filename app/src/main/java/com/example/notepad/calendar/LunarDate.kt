package com.example.notepad.calendar

data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean
) {
    fun getFormattedString(): String {
        return "${getYearCyclic()}${if (isLeapMonth) "閏" else ""}${getLunarMonthName()}${getLunarDayName()}"
    }

    fun getLunarDayName(): String {
        val dayNames = arrayOf(
            "", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
            "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
        )
        return if (day in 1..30) dayNames[day] else "${day}日"
    }

    fun getLunarMonthName(): String {
        val monthNames = arrayOf(
            "", "正月", "二月", "三月", "四月", "五月", "六月",
            "七月", "八月", "九月", "十月", "冬月", "臘月"
        )
        return if (month in 1..12) monthNames[month] else "${month}月"
    }

    fun getYearCyclic(): String {
        val stems = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
        val branches = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
        val animals = arrayOf("鼠", "牛", "虎", "兔", "龍", "蛇", "馬", "羊", "猴", "雞", "狗", "豬")

        val offset = year - 4
        val stem = stems[(offset % 10 + 10) % 10]
        val branchIndex = (offset % 12 + 12) % 12
        val branch = branches[branchIndex]
        val animal = animals[branchIndex]

        return "${stem}${branch}${animal}年"
    }
}
