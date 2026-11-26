package com.example.notepad.calendar

import java.util.Calendar
import java.util.Date

/**
 * 農曆萬年曆外觀類別（Facade Pattern）
 * 封裝底層農曆演算、二十四節氣推算、中西節慶判定與午夜時戳正規化邏輯
 */
class LunarCalendarFacade {

    /**
     * 取得指定西曆日期對應之農曆日期資訊
     */
    fun getLunarDate(date: Date): LunarDate {
        return LunarCalendarEngine.getLunarDate(date)
    }

    /**
     * 取得指定西曆日期對應之二十四節氣名稱（若當日非節氣則回傳 null）
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
     * 取得指定西曆日期對應之國曆或農曆節慶識別名稱（若無則回傳 null）
     */
    fun getFestival(date: Date): String? {
        val cal = Calendar.getInstance()
        cal.time = date
        val lunarDate = getLunarDate(date)
        return LunarCalendarEngine.getFestival(cal, lunarDate)
    }

    /**
     * 彙整指定日期之完整曆法資訊（包含農曆年月日、節氣與節慶）
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
     * 將任意日期正規化為當日午夜零時整點毫秒數 (00:00:00.000)
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

    /**
     * 判斷目標時間戳是否落在「明天」區間（用於觸發明日到期呼吸燈邊框警示）
     */
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
            "Valentine's Day" -> "西洋情人節"
            "Labor Day" -> "勞動節"
            "Christmas" -> "行憲紀念日"
            else -> festival
        }

    val displayLabel: String
        get() {
            if (solarTerm == "清明") return "清明"
            val f = when (val fest = displayFestival) {
                "和平紀念日" -> "二二八"
                "西洋情人節" -> "情人節"
                "台灣光復節" -> "光復節"
                "行憲紀念日" -> "行憲紀念"
                else -> fest
            }
            if (!f.isNullOrEmpty()) return f
            if (!solarTerm.isNullOrEmpty()) return solarTerm
            return if (lunarDate.day == 1) lunarDate.getLunarMonthName() else lunarDate.getLunarDayName()
        }
}
