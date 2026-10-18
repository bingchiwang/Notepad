package com.example.notepad.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class LunarCalendarFacadeTest {

    private val facade = LunarCalendarFacade()

    private fun createDate(year: Int, month: Int, day: Int): java.util.Date {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    @Test
    fun `test lunar date calculation for Spring Festival`() {
        // 2024-02-10 is Lunar Year 2024, Month 1, Day 1
        val date = createDate(2024, 2, 10)
        val info = facade.getCalendarInfo(date)
        
        assertEquals(2024, info.lunarDate.year)
        assertEquals(1, info.lunarDate.month)
        assertEquals(1, info.lunarDate.day)
        assertEquals("Spring Festival", info.festival)
    }

    @Test
    fun `test solar term calculation`() {
        // 2024-04-04 is Qingming
        val date = createDate(2024, 4, 4)
        val info = facade.getCalendarInfo(date)
        
        assertEquals("清明", info.solarTerm)
    }

    @Test
    fun `test solar festival calculation`() {
        // 2024-12-25 is Christmas
        val date = createDate(2024, 12, 25)
        val info = facade.getCalendarInfo(date)
        
        assertEquals("Christmas", info.festival)
    }
}
