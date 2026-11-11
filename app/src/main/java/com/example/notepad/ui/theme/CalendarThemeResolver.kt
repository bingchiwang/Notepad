package com.example.notepad.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.example.notepad.data.preferences.CalendarStyle

/**
 * 解析後之月曆視覺調色盤與幾何樣式（所有文字色均經過對比度強制保證）
 */
data class CalendarPalette(
    val headerBgColor: Color,
    val headerContentColor: Color,
    val headerSubContentColor: Color,
    val cardBgColor: Color,
    val cardBorderColor: Color?,
    val detailCardBgColor: Color,
    val cellShape: Shape,
    val showCellGridBorder: Boolean,
    val gridBorderColor: Color,
    val selectedBgColor: Color,
    val selectedTextColor: Color,
    val todayBgColor: Color,
    val todayBorderColor: Color,
    val sundayHeaderColor: Color,
    val saturdayHeaderColor: Color,
    val weekdayHeaderColor: Color,
    val detailPrimaryColor: Color,
    val detailTextColor: Color,
    val detailFestivalColor: Color,
    val detailSolarTermColor: Color
) {
    /**
     * 計算月曆單一日期格之數字文字顏色（確保不與選日或卡片底色相同）
     */
    fun resolveDayNumberColor(isSelected: Boolean, dayOfWeekIndex: Int): Color {
        val activeBg = if (isSelected) selectedBgColor else cardBgColor
        val preferred = when {
            isSelected -> selectedTextColor
            dayOfWeekIndex == 0 -> Color(0xFFE53935)
            dayOfWeekIndex == 6 -> Color(0xFF1E88E5)
            else -> ColorContrastUtils.bestOnBackgroundColor(cardBgColor)
        }
        return ColorContrastUtils.ensureReadableTextColor(preferred, activeBg)
    }

    /**
     * 計算月曆格內農曆／節氣／節慶小字顏色（若文字色與選日底色或卡片底色接近，自動轉為清晰高對比色）
     */
    fun resolveLunarLabelColor(
        isSelected: Boolean,
        hasFestival: Boolean,
        hasSolarTerm: Boolean
    ): Color {
        val activeBg = if (isSelected) selectedBgColor else cardBgColor
        val preferred = when {
            hasFestival -> Color(0xFFD32F2F)
            hasSolarTerm -> Color(0xFF388E3C)
            isSelected -> selectedTextColor.copy(alpha = 0.9f)
            else -> ColorContrastUtils.bestOnBackgroundColor(cardBgColor).copy(alpha = 0.68f)
        }
        return ColorContrastUtils.ensureReadableTextColor(preferred, activeBg)
    }
}

/**
 * 依據使用者自訂的 [CalendarStyle]、可選自訂主色與深淺主題解析月曆視覺樣式
 */
object CalendarThemeResolver {

    fun resolvePalette(
        style: CalendarStyle,
        customAccentColorLong: Long?,
        darkTheme: Boolean,
        themeColorScheme: ColorScheme
    ): CalendarPalette {
        val customAccent = customAccentColorLong?.let { Color(it) }

        val rawBase = when (style) {
            CalendarStyle.CLASSIC -> RawCalendarStyleColors(
                headerBg = customAccent ?: themeColorScheme.primaryContainer,
                cardBg = themeColorScheme.surface,
                detailBg = themeColorScheme.surfaceVariant,
                selectedBg = customAccent ?: themeColorScheme.primaryContainer,
                accentForToday = customAccent ?: themeColorScheme.primary,
                cellShape = RoundedCornerShape(8.dp),
                showCellGridBorder = false,
                cardBorderColor = null
            )

            CalendarStyle.INK_WASH -> RawCalendarStyleColors(
                headerBg = customAccent ?: if (darkTheme) Color(0xFF4E342E) else Color(0xFF3E2723),
                cardBg = if (darkTheme) Color(0xFF23201B) else Color(0xFFFBF7EE),
                detailBg = if (darkTheme) Color(0xFF2E2922) else Color(0xFFF3ECDD),
                selectedBg = customAccent ?: Color(0xFFC62828),
                accentForToday = customAccent ?: Color(0xFFD84315),
                cellShape = CircleShape,
                showCellGridBorder = true,
                cardBorderColor = if (darkTheme) Color(0xFF5D4037) else Color(0xFFD7CCC8)
            )

            CalendarStyle.FOREST_ZEN -> RawCalendarStyleColors(
                headerBg = customAccent ?: if (darkTheme) Color(0xFF1B5E20) else Color(0xFF2E7D32),
                cardBg = if (darkTheme) Color(0xFF172219) else Color(0xFFEEF6EF),
                detailBg = if (darkTheme) Color(0xFF213124) else Color(0xFFDCECDC),
                selectedBg = customAccent ?: if (darkTheme) Color(0xFF43A047) else Color(0xFF2E7D32),
                accentForToday = customAccent ?: Color(0xFF4CAF50),
                cellShape = RoundedCornerShape(14.dp),
                showCellGridBorder = false,
                cardBorderColor = if (darkTheme) Color(0xFF2E7D32) else Color(0xFFA5D6A7)
            )

            CalendarStyle.OCEAN_NIGHT -> RawCalendarStyleColors(
                headerBg = customAccent ?: if (darkTheme) Color(0xFF0D47A1) else Color(0xFF1565C0),
                cardBg = if (darkTheme) Color(0xFF141D29) else Color(0xFFECF4FC),
                detailBg = if (darkTheme) Color(0xFF1C293A) else Color(0xFFD9EAFA),
                selectedBg = customAccent ?: if (darkTheme) Color(0xFF1E88E5) else Color(0xFF1565C0),
                accentForToday = customAccent ?: Color(0xFF29B6F6),
                cellShape = CircleShape,
                showCellGridBorder = false,
                cardBorderColor = if (darkTheme) Color(0xFF1E88E5) else Color(0xFF90CAF9)
            )

            CalendarStyle.SUNSET_WARM -> RawCalendarStyleColors(
                headerBg = customAccent ?: if (darkTheme) Color(0xFFBF360C) else Color(0xFFE65100),
                cardBg = if (darkTheme) Color(0xFF281D16) else Color(0xFFFFF5EA),
                detailBg = if (darkTheme) Color(0xFF36271E) else Color(0xFFFFE5CC),
                selectedBg = customAccent ?: Color(0xFFEF6C00),
                accentForToday = customAccent ?: Color(0xFFFF9800),
                cellShape = RoundedCornerShape(10.dp),
                showCellGridBorder = true,
                cardBorderColor = if (darkTheme) Color(0xFFE65100) else Color(0xFFFFCC80)
            )

            CalendarStyle.HIGH_CONTRAST -> RawCalendarStyleColors(
                headerBg = customAccent ?: if (darkTheme) Color(0xFFF5F5F5) else Color(0xFF111111),
                cardBg = if (darkTheme) Color(0xFF101010) else Color.White,
                detailBg = if (darkTheme) Color(0xFF1E1E1E) else Color(0xFFF2F2F2),
                selectedBg = customAccent ?: if (darkTheme) Color(0xFFF5F5F5) else Color(0xFF111111),
                accentForToday = customAccent ?: if (darkTheme) Color.White else Color.Black,
                cellShape = RoundedCornerShape(4.dp),
                showCellGridBorder = true,
                cardBorderColor = if (darkTheme) Color.White else Color.Black
            )
        }

        val headerText = ColorContrastUtils.bestOnBackgroundColor(rawBase.headerBg)
        val selectedText = ColorContrastUtils.bestOnBackgroundColor(rawBase.selectedBg)
        val onCardInk = ColorContrastUtils.bestOnBackgroundColor(rawBase.cardBg)
        val onDetailInk = ColorContrastUtils.bestOnBackgroundColor(rawBase.detailBg)
        val gridLineColor = onCardInk.copy(alpha = if (style == CalendarStyle.HIGH_CONTRAST) 0.35f else 0.14f)

        val todayBg = lerp(rawBase.cardBg, rawBase.accentForToday, 0.20f)
        val todayBorder = ColorContrastUtils.ensureReadableAccentOnSurface(
            rawBase.accentForToday,
            rawBase.cardBg,
            minContrast = ColorContrastUtils.MIN_ACCENT_CONTRAST
        )

        return CalendarPalette(
            headerBgColor = rawBase.headerBg,
            headerContentColor = headerText,
            headerSubContentColor = headerText.copy(alpha = 0.85f),
            cardBgColor = rawBase.cardBg,
            cardBorderColor = rawBase.cardBorderColor,
            detailCardBgColor = rawBase.detailBg,
            cellShape = rawBase.cellShape,
            showCellGridBorder = rawBase.showCellGridBorder,
            gridBorderColor = gridLineColor,
            selectedBgColor = rawBase.selectedBg,
            selectedTextColor = selectedText,
            todayBgColor = todayBg,
            todayBorderColor = todayBorder,
            sundayHeaderColor = ColorContrastUtils.ensureReadableTextColor(Color(0xFFE53935), rawBase.cardBg),
            saturdayHeaderColor = ColorContrastUtils.ensureReadableTextColor(Color(0xFF1E88E5), rawBase.cardBg),
            weekdayHeaderColor = onCardInk.copy(alpha = 0.78f),
            detailPrimaryColor = ColorContrastUtils.ensureReadableAccentOnSurface(rawBase.selectedBg, rawBase.detailBg),
            detailTextColor = onDetailInk,
            detailFestivalColor = ColorContrastUtils.ensureReadableTextColor(Color(0xFFD32F2F), rawBase.detailBg),
            detailSolarTermColor = ColorContrastUtils.ensureReadableTextColor(Color(0xFF388E3C), rawBase.detailBg)
        )
    }

    private data class RawCalendarStyleColors(
        val headerBg: Color,
        val cardBg: Color,
        val detailBg: Color,
        val selectedBg: Color,
        val accentForToday: Color,
        val cellShape: Shape,
        val showCellGridBorder: Boolean,
        val cardBorderColor: Color?
    )
}
