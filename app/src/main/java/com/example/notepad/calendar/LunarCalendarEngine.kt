package com.example.notepad.calendar

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class LunarCalendarEngine {

    companion object {
        // Lunar calendar data from 1900 to 2100
        private val lunarInfo = intArrayOf(
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2, // 1900-1909
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977, // 1910-1919
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, // 1920-1929
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950, // 1930-1939
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557, // 1940-1949
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0, // 1950-1959
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0, // 1960-1969
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6, // 1970-1979
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570, // 1980-1989
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0, // 1990-1999
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, // 2000-2009
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930, // 2010-2019
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530, // 2020-2029
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, // 2030-2039
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0, // 2040-2049
            0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0, // 2050-2059
            0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4, // 2060-2069
            0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0, // 2070-2079
            0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160, // 2080-2089
            0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252, // 2090-2099
            0x0d520 // 2100
        )

        private val solarTermNames = arrayOf(
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
            "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
        )

        private val solarTermInfo = intArrayOf(
            0, 21208, 42467, 63836, 85337, 107014, 128867, 150921, 173149, 195551, 218072, 240693,
            263343, 285989, 308563, 331033, 353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
        )

        // Maps MMDD of solar date to festival name
        private val solarFestivals = mapOf(
            "0101" to "New Year's Day",
            "0214" to "Valentine's Day",
            "0501" to "Labor Day",
            "1225" to "Christmas"
        )

        // Maps MMDD of lunar date to festival name
        private val lunarFestivals = mapOf(
            "0101" to "Spring Festival",
            "0115" to "Lantern Festival",
            "0505" to "Dragon Boat Festival",
            "0707" to "Qixi Festival",
            "0815" to "Mid-Autumn Festival",
            "0909" to "Double Ninth Festival"
        )

        // Calculate total days in a lunar year
        fun getYearDays(year: Int): Int {
            var sum = 348
            var i = 0x8000
            while (i > 0x8) {
                if ((lunarInfo[year - 1900] and i) != 0) {
                    sum += 1
                }
                i = i shr 1
            }
            return sum + getLeapMonthDays(year)
        }

        // Calculate days in leap month of a year
        fun getLeapMonthDays(year: Int): Int {
            if (getLeapMonth(year) != 0) {
                return if ((lunarInfo[year - 1900] and 0x10000) != 0) 30 else 29
            }
            return 0
        }

        // Return leap month of a year (1-12). 0 if no leap month.
        fun getLeapMonth(year: Int): Int {
            return lunarInfo[year - 1900] and 0xf
        }

        // Calculate days in a regular lunar month
        fun getMonthDays(year: Int, month: Int): Int {
            return if ((lunarInfo[year - 1900] and (0x10000 shr month)) != 0) 30 else 29
        }

        // Get lunar date from a standard java.util.Date
        fun getLunarDate(date: Date): LunarDate {
            val dateCal = Calendar.getInstance().apply { 
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val baseCal = Calendar.getInstance().apply {
                set(1900, 0, 31, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val offset = ((dateCal.timeInMillis - baseCal.timeInMillis) / 86400000L).toInt()

            var daysToConsume = offset
            var iYear = 1900
            var daysOfYear = 0
            while (iYear <= 2100 && daysToConsume > 0) {
                daysOfYear = getYearDays(iYear)
                daysToConsume -= daysOfYear
                iYear++
            }

            if (daysToConsume < 0) {
                daysToConsume += daysOfYear
                iYear--
            }

            var leapMonth = getLeapMonth(iYear)
            var isLeap = false
            var iMonth = 1
            var daysOfMonth = 0

            while (iMonth <= 12 && daysToConsume > 0) {
                if (leapMonth > 0 && iMonth == leapMonth + 1 && !isLeap) {
                    --iMonth
                    isLeap = true
                    daysOfMonth = getLeapMonthDays(iYear)
                } else {
                    daysOfMonth = getMonthDays(iYear, iMonth)
                }

                if (isLeap && iMonth == leapMonth + 1) {
                    isLeap = false
                }

                daysToConsume -= daysOfMonth
                if (!isLeap) {
                    iMonth++
                }
            }

            if (daysToConsume == 0 && leapMonth > 0 && iMonth == leapMonth + 1) {
                if (isLeap) {
                    isLeap = false
                } else {
                    isLeap = true
                    --iMonth
                }
            }

            if (daysToConsume < 0) {
                daysToConsume += daysOfMonth
                if (!isLeap) {
                    --iMonth
                } else {
                    isLeap = false
                }
            }

            return LunarDate(iYear, iMonth, daysToConsume + 1, isLeap)
        }

        fun getSolarTerm(year: Int, month: Int, day: Int): String? {
            val baseDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(1900, 0, 6, 2, 5, 0)
                set(Calendar.MILLISECOND, 0)
            }.time.time

            for (i in 0 until 24) {
                val ms = (31556925974.7 * (year - 1900) + solarTermInfo[i] * 60000L).toLong()
                val termDate = Date(baseDate + ms)
                val cal = Calendar.getInstance().apply { time = termDate }
                if (cal.get(Calendar.MONTH) == month - 1 && cal.get(Calendar.DAY_OF_MONTH) == day) {
                    return solarTermNames[i]
                }
            }
            return null
        }

        fun getFestival(solarDate: Calendar, lunarDate: LunarDate): String? {
            val sMonth = (solarDate.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            val sDay = solarDate.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
            val sKey = sMonth + sDay
            if (solarFestivals.containsKey(sKey)) {
                return solarFestivals[sKey]
            }

            val lMonth = lunarDate.month.toString().padStart(2, '0')
            val lDay = lunarDate.day.toString().padStart(2, '0')
            val lKey = lMonth + lDay
            if (!lunarDate.isLeapMonth && lunarFestivals.containsKey(lKey)) {
                return lunarFestivals[lKey]
            }
            return null
        }
    }
}
