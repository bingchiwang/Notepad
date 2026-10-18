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
}

data class CalendarInfo(
    val lunarDate: LunarDate,
    val solarTerm: String?,
    val festival: String?
)
