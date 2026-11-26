package com.example.notepad.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `test all 24 solar terms of 2026 match Taiwan CWA official dates`() {
        val official2026 = listOf(
            Triple(1, 5, "小寒"),
            Triple(1, 20, "大寒"),
            Triple(2, 4, "立春"),
            Triple(2, 18, "雨水"),
            Triple(3, 5, "驚蟄"),
            Triple(3, 20, "春分"),
            Triple(4, 5, "清明"),
            Triple(4, 20, "穀雨"),
            Triple(5, 5, "立夏"),
            Triple(5, 21, "小滿"),
            Triple(6, 5, "芒種"),
            Triple(6, 21, "夏至"),
            Triple(7, 7, "小暑"),
            Triple(7, 23, "大暑"),
            Triple(8, 7, "立秋"),
            Triple(8, 23, "處暑"),
            Triple(9, 7, "白露"),
            Triple(9, 23, "秋分"),
            Triple(10, 8, "寒露"),
            Triple(10, 23, "霜降"),
            Triple(11, 7, "立冬"),
            Triple(11, 22, "小雪"),
            Triple(12, 7, "大雪"),
            Triple(12, 22, "冬至")
        )
        assertEquals(24, SolarTermAstronomy.getSolarTermsForYear(2026).size)
        for ((month, day, expectedName) in official2026) {
            assertEquals(
                "2026-$month-$day 應為 $expectedName",
                expectedName,
                facade.getSolarTerm(createDate(2026, month, day))
            )
        }
        // 確認 2026-11-08 不再誤判為立冬
        assertNull(facade.getSolarTerm(createDate(2026, 11, 8)))
    }

    @Test
    fun `test all 24 solar terms across 2024 2025 2027 2028 match official UTC+8 dates`() {
        val officialByYear = mapOf(
            2024 to listOf(
                106 to "小寒", 120 to "大寒", 204 to "立春", 219 to "雨水", 305 to "驚蟄", 320 to "春分",
                404 to "清明", 419 to "穀雨", 505 to "立夏", 520 to "小滿", 605 to "芒種", 621 to "夏至",
                706 to "小暑", 722 to "大暑", 807 to "立秋", 822 to "處暑", 907 to "白露", 922 to "秋分",
                1008 to "寒露", 1023 to "霜降", 1107 to "立冬", 1122 to "小雪", 1206 to "大雪", 1221 to "冬至"
            ),
            2025 to listOf(
                105 to "小寒", 120 to "大寒", 203 to "立春", 218 to "雨水", 305 to "驚蟄", 320 to "春分",
                404 to "清明", 420 to "穀雨", 505 to "立夏", 521 to "小滿", 605 to "芒種", 621 to "夏至",
                707 to "小暑", 722 to "大暑", 807 to "立秋", 823 to "處暑", 907 to "白露", 923 to "秋分",
                1008 to "寒露", 1023 to "霜降", 1107 to "立冬", 1122 to "小雪", 1207 to "大雪", 1221 to "冬至"
            ),
            2027 to listOf(
                105 to "小寒", 120 to "大寒", 204 to "立春", 219 to "雨水", 306 to "驚蟄", 321 to "春分",
                405 to "清明", 420 to "穀雨", 506 to "立夏", 521 to "小滿", 606 to "芒種", 621 to "夏至",
                707 to "小暑", 723 to "大暑", 808 to "立秋", 823 to "處暑", 908 to "白露", 923 to "秋分",
                1008 to "寒露", 1023 to "霜降", 1107 to "立冬", 1122 to "小雪", 1207 to "大雪", 1222 to "冬至"
            ),
            2028 to listOf(
                106 to "小寒", 120 to "大寒", 204 to "立春", 219 to "雨水", 305 to "驚蟄", 320 to "春分",
                404 to "清明", 419 to "穀雨", 505 to "立夏", 520 to "小滿", 605 to "芒種", 621 to "夏至",
                706 to "小暑", 722 to "大暑", 807 to "立秋", 822 to "處暑", 907 to "白露", 922 to "秋分",
                1008 to "寒露", 1023 to "霜降", 1107 to "立冬", 1122 to "小雪", 1206 to "大雪", 1221 to "冬至"
            )
        )
        for ((year, expectedList) in officialByYear) {
            val actualMap = SolarTermAstronomy.getSolarTermsForYear(year)
            assertEquals("$year 年應有 24 個節氣", 24, actualMap.size)
            for ((monthDay, expectedName) in expectedList) {
                assertEquals(
                    "$year-$monthDay 應為 $expectedName",
                    expectedName,
                    actualMap[monthDay]
                )
            }
        }
    }

    @Test
    fun `test 2026 lunar first days leap month label 2060 fix and bounds safety`() {
        // 驗證 2026 年農曆全部 12 個月初一（丙午年無閏月）
        val firstDays2026 = listOf(
            Triple(2026, 2, 17) to 1,
            Triple(2026, 3, 19) to 2,
            Triple(2026, 4, 17) to 3,
            Triple(2026, 5, 17) to 4,
            Triple(2026, 6, 15) to 5,
            Triple(2026, 7, 14) to 6,
            Triple(2026, 8, 13) to 7,
            Triple(2026, 9, 11) to 8,
            Triple(2026, 10, 10) to 9,
            Triple(2026, 11, 9) to 10,
            Triple(2026, 12, 9) to 11,
            Triple(2027, 1, 8) to 12
        )
        for ((ymd, lunarMonth) in firstDays2026) {
            val lunar = facade.getLunarDate(createDate(ymd.first, ymd.second, ymd.third))
            assertEquals(2026, lunar.year)
            assertEquals(lunarMonth, lunar.month)
            assertEquals(1, lunar.day)
        }

        // 驗證 2025 閏六月初一（2025-07-25）顯示「閏六月」而非漏掉「閏」字
        val leapMonthInfo = facade.getCalendarInfo(createDate(2025, 7, 25))
        assertTrue(leapMonthInfo.lunarDate.isLeapMonth)
        assertEquals(6, leapMonthInfo.lunarDate.month)
        assertEquals(1, leapMonthInfo.lunarDate.day)
        assertEquals("閏六月", leapMonthInfo.displayLabel)

        // 驗證 2060 年農曆四月初一已由 2060-05-01 修正為正確天文朔日 2060-04-30
        val lunar2060 = facade.getLunarDate(createDate(2060, 4, 30))
        assertEquals(2060, lunar2060.year)
        assertEquals(4, lunar2060.month)
        assertEquals(1, lunar2060.day)

        // 驗證超出 1900-2100 範圍之日期不會拋出索引越界例外
        assertEquals(1900, facade.getLunarDate(createDate(1899, 1, 1)).year)
        assertEquals(2100, facade.getLunarDate(createDate(2105, 1, 1)).year)
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
