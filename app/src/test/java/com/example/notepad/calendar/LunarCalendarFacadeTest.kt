package com.example.notepad.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals("春節", info.festival)
        assertEquals("春節", info.displayFestival)
        assertEquals("春節", info.displayLabel)
        assertEquals("初一", info.lunarDate.getLunarDayName())
        assertEquals("甲辰龍年", info.lunarDate.getYearCyclic())
    }

    @Test
    fun `test solar term calculation in Traditional Chinese`() {
        // 2024-04-04 is Qingming
        val date = createDate(2024, 4, 4)
        val info = facade.getCalendarInfo(date)
        
        assertEquals("清明", info.solarTerm)
        assertEquals("清明", info.displayLabel)

        // 2024-03-05 為驚蟄（確認為台灣繁體中文「驚蟄」而非簡體）
        val jingzhe = facade.getCalendarInfo(createDate(2024, 3, 5))
        assertEquals("驚蟄", jingzhe.solarTerm)
    }

    @Test
    fun `test Taiwan solar and lunar festivals in Traditional Chinese`() {
        // 台灣二二八和平紀念日 (02-28)
        val peaceDay = facade.getCalendarInfo(createDate(2026, 2, 28))
        assertEquals("和平紀念日", peaceDay.festival)
        assertEquals("二二八", peaceDay.displayLabel)

        // 台灣母親節 (2026-05-10 為 5 月第 2 個星期日)
        val mothersDay = facade.getCalendarInfo(createDate(2026, 5, 10))
        assertEquals("母親節", mothersDay.festival)
        assertEquals("母親節", mothersDay.displayLabel)

        // 台灣父親節 (08-08)
        val fathersDay = facade.getCalendarInfo(createDate(2026, 8, 8))
        assertEquals("父親節", fathersDay.festival)
        assertEquals("父親節", fathersDay.displayLabel)

        // 台灣教師節 (09-28)
        val teachersDay = facade.getCalendarInfo(createDate(2026, 9, 28))
        assertEquals("教師節", teachersDay.festival)
        assertEquals("教師節", teachersDay.displayLabel)

        // 雙十國慶日 (10-10)
        val nationalDay = facade.getCalendarInfo(createDate(2026, 10, 10))
        assertEquals("國慶日", nationalDay.festival)
        assertEquals("國慶日", nationalDay.displayLabel)

        // 行憲紀念日 (12-25)
        val constitutionDay = facade.getCalendarInfo(createDate(2024, 12, 25))
        assertEquals("行憲紀念日", constitutionDay.festival)
        assertEquals("行憲紀念", constitutionDay.displayLabel)

        // 農曆除夕 (2026-02-16 為農曆乙巳年臘月廿九最後一日)
        val lunarNewYearEve = facade.getCalendarInfo(createDate(2026, 2, 16))
        assertEquals("除夕", lunarNewYearEve.festival)
        assertEquals("除夕", lunarNewYearEve.displayLabel)
    }

    @Test
    fun `test isTomorrow helper`() {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowMillis = facade.getMidnightMillis(tomorrow.time)
        assertTrue(facade.isTomorrow(tomorrowMillis))
    }
}
