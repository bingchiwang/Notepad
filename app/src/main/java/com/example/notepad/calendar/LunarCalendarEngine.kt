package com.example.notepad.calendar

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * 農曆萬年曆核心演算法引擎（支援西元 1900 年至 2100 年）
 * - 透過 16 進位位元遮罩表 (lunarInfo) 解算農曆大月 (30天)、小月 (29天) 與閏月月份
 * - 結合太陽黃經分鐘偏移量計算二十四節氣與傳統／國曆節慶
 */
class LunarCalendarEngine {

    companion object {
        // 西元 1900 年至 2100 年農曆大小月與閏月位元編碼表（共 201 年）
        // Bit 16: 閏月是否為大月 (1=30天, 0=29天)
        // Bit 15..4: 農曆 1~12 月大小月標記 (1=30天, 0=29天)
        // Bit 3..0: 當年閏月月份 (0 表示無閏月，1~12 表示閏幾月)
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
            "小寒", "大寒", "立春", "雨水", "驚蟄", "春分",
            "清明", "穀雨", "立夏", "小滿", "芒種", "夏至",
            "小暑", "大暑", "立秋", "處暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
        )

        private val solarTermInfo = intArrayOf(
            0, 21208, 42467, 63836, 85337, 107014, 128867, 150921, 173149, 195551, 218072, 240693,
            263343, 285989, 308563, 331033, 353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
        )

        // 台灣國曆主要節慶與紀念日對照表 (MMDD -> 繁體中文節慶名稱)
        private val solarFestivals = mapOf(
            "0101" to "元旦",
            "0214" to "西洋情人節",
            "0228" to "和平紀念日",
            "0308" to "婦女節",
            "0329" to "青年節",
            "0404" to "兒童節",
            "0501" to "勞動節",
            "0808" to "父親節",
            "0903" to "軍人節",
            "0928" to "教師節",
            "1010" to "國慶日",
            "1025" to "台灣光復節",
            "1225" to "行憲紀念日"
        )

        // 台灣農曆傳統節慶對照表 (MMDD -> 繁體中文節慶名稱)
        private val lunarFestivals = mapOf(
            "0101" to "春節",
            "0109" to "天公生",
            "0115" to "元宵節",
            "0202" to "頭牙",
            "0323" to "媽祖生",
            "0505" to "端午節",
            "0707" to "七夕",
            "0715" to "中元節",
            "0815" to "中秋節",
            "0909" to "重陽節",
            "1216" to "尾牙",
            "1224" to "送神"
        )

        /**
         * 計算指定農曆年份之總天數（基礎 348 天加上各月大小月與閏月天數）
         */
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

        /**
         * 計算指定農曆年份之閏月天數（無閏月則回傳 0）
         */
        fun getLeapMonthDays(year: Int): Int {
            if (getLeapMonth(year) != 0) {
                return if ((lunarInfo[year - 1900] and 0x10000) != 0) 30 else 29
            }
            return 0
        }

        /**
         * 取得指定農曆年份之閏月月份（1~12，若當年無閏月則回傳 0）
         */
        fun getLeapMonth(year: Int): Int {
            return lunarInfo[year - 1900] and 0xf
        }

        /**
         * 計算指定農曆年份與月份之當月天數（30 天或 29 天）
         */
        fun getMonthDays(year: Int, month: Int): Int {
            return if ((lunarInfo[year - 1900] and (0x10000 shr month)) != 0) 30 else 29
        }

        /**
         * 將西曆日期 (Date) 轉換為農曆日期 (LunarDate)
         * 以西元 1900 年 1 月 31 日（農曆正月初一）為基準日計算相差天數並逐年逐月推算
         */
        fun getLunarDate(date: Date): LunarDate {
            // 先擷取本地年月日，再統一以 UTC 計算日差，避免跨日／夏令時間（DST）誤差
            val localCal = Calendar.getInstance().apply { time = date }
            val utcZone = TimeZone.getTimeZone("UTC")
            val dateCal = Calendar.getInstance(utcZone).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                    0,
                    0,
                    0
                )
            }
            val baseCal = Calendar.getInstance(utcZone).apply {
                clear()
                set(1900, 0, 31, 0, 0, 0)
            }

            val offset = ((dateCal.timeInMillis - baseCal.timeInMillis) / 86400000L).toInt()

            var daysToConsume = offset
            var iYear = 1900
            while (iYear <= 2100) {
                val daysOfYear = getYearDays(iYear)
                if (daysToConsume < daysOfYear) break
                daysToConsume -= daysOfYear
                iYear++
            }

            val leapMonth = getLeapMonth(iYear)
            var isLeap = false
            var iMonth = 1

            while (iMonth <= 12 && daysToConsume >= 0) {
                val daysOfMonth = if (isLeap) {
                    getLeapMonthDays(iYear)
                } else {
                    getMonthDays(iYear, iMonth)
                }
                if (daysToConsume < daysOfMonth) {
                    break
                }
                daysToConsume -= daysOfMonth
                if (leapMonth > 0 && iMonth == leapMonth && !isLeap) {
                    isLeap = true
                } else {
                    isLeap = false
                    iMonth++
                }
            }

            return LunarDate(iYear, iMonth, daysToConsume + 1, isLeap)
        }

        fun getSolarTerm(year: Int, month: Int, day: Int): String? {
            val baseDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(1900, 0, 6, 2, 5, 0)
                set(Calendar.MILLISECOND, 0)
            }.time.time

            // 傳統二十四節氣依東亞標準時間（GMT+8）判定所屬曆日，確保在 UTC 或全球各時區（含 CI）結果完全一致
            val eastAsiaZone = TimeZone.getTimeZone("GMT+8")
            for (i in 0 until 24) {
                val ms = (31556925974.7 * (year - 1900) + solarTermInfo[i] * 60000L).toLong()
                val termDate = Date(baseDate + ms)
                val cal = Calendar.getInstance(eastAsiaZone).apply { time = termDate }
                if (cal.get(Calendar.MONTH) == month - 1 && cal.get(Calendar.DAY_OF_MONTH) == day) {
                    return solarTermNames[i]
                }
            }
            return null
        }

        fun getFestival(solarDate: Calendar, lunarDate: LunarDate): String? {
            // 1. 台灣農曆重要傳統節慶（含農曆臘月最後一日「除夕」）
            if (!lunarDate.isLeapMonth) {
                if (lunarDate.month == 12 && lunarDate.day == getMonthDays(lunarDate.year, 12)) {
                    return "除夕"
                }
                val lMonth = lunarDate.month.toString().padStart(2, '0')
                val lDay = lunarDate.day.toString().padStart(2, '0')
                val lKey = lMonth + lDay
                lunarFestivals[lKey]?.let { return it }
            }

            // 2. 台灣國曆固定節慶與紀念日
            val monthNum = solarDate.get(Calendar.MONTH) + 1
            val dayNum = solarDate.get(Calendar.DAY_OF_MONTH)
            val sMonth = monthNum.toString().padStart(2, '0')
            val sDay = dayNum.toString().padStart(2, '0')
            val sKey = sMonth + sDay
            solarFestivals[sKey]?.let { return it }

            // 3. 台灣母親節（每年 5 月第 2 個星期日）
            if (monthNum == 5 &&
                solarDate.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY &&
                dayNum in 8..14
            ) {
                return "母親節"
            }

            return null
        }
    }
}
