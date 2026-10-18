package com.example.notepad.calendar

import java.util.Calendar
import java.util.Date

/**
 * Facade pattern to abstract the complexity of Lunar Calendar calculations.
 */
class LunarCalendarFacade {

    /**
     * Get the Lunar date corresponding to the given standard Date.
     */
    fun getLunarDate(date: Date): LunarDate {
        return LunarCalendarEngine.getLunarDate(date)
    }

    /**
     * Get the Solar Term for the given standard Date, if any.
     */
    fun getSolarTerm(date: Date): String? {
        val cal = Calendar.getInstance()
        cal.time = date
        return LunarCalendarEngine.getSolarTerm(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Get the Festival name for the given standard Date, if any.
     * This checks both Solar and Lunar festivals.
     */
    fun getFestival(date: Date): String? {
        val cal = Calendar.getInstance()
        cal.time = date
        val lunarDate = getLunarDate(date)
        return LunarCalendarEngine.getFestival(cal, lunarDate)
    }

    /**
     * Convenience method to get a complete description (Lunar Date, Solar Term, Festival)
     * for a given standard Date.
     */
    fun getCalendarInfo(date: Date): CalendarInfo {
        val lunarDate = getLunarDate(date)
        val solarTerm = getSolarTerm(date)
        val festival = getFestival(date)
        
        return CalendarInfo(
            lunarDate = lunarDate,
            solarTerm = solarTerm,
            festival = festival
        )
    }

    /**
     * Normalize a date to midnight millis (00:00:00.000).
     */
    fun getMidnightMillis(date: Date): Long {
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun isTomorrow(targetMillis: Long): Boolean {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowEnd = cal.timeInMillis
        return targetMillis in tomorrowStart until tomorrowEnd
    }
}

data class CalendarInfo(
    val lunarDate: LunarDate,
    val solarTerm: String?,
    val festival: String?
) {
    val displayFestival: String?
        get() = when (festival) {
            "Spring Festival" -> "春節"
            "Lantern Festival" -> "元宵節"
            "Dragon Boat Festival" -> "端午節"
            "Qixi Festival" -> "七夕"
            "Mid-Autumn Festival" -> "中秋節"
            "Double Ninth Festival" -> "重陽節"
            "New Year's Day" -> "元旦"
            "Valentine's Day" -> "情人節"
            "Labor Day" -> "勞動節"
            "Christmas" -> "聖誕節"
            else -> festival
        }

    val displayLabel: String
        get() {
            val f = when (festival) {
                "Spring Festival" -> "春節"
                "Lantern Festival" -> "元宵"
                "Dragon Boat Festival" -> "端午"
                "Qixi Festival" -> "七夕"
                "Mid-Autumn Festival" -> "中秋"
                "Double Ninth Festival" -> "重陽"
                "New Year's Day" -> "元旦"
                "Christmas" -> "聖誕"
                else -> festival
            }
            if (!f.isNullOrEmpty()) return f
            if (!solarTerm.isNullOrEmpty()) return solarTerm
            return if (lunarDate.day == 1) lunarDate.getLunarMonthName() else lunarDate.getLunarDayName()
        }
}
