package com.example.notepad.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.notepad.data.preferences.CalendarStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 個人化主題按鍵反色、月曆樣式調色盤與文字防重色保護單元測試
 */
class ColorContrastUtilsTest {

    @Test
    fun `bestOnBackgroundColor selects dark ink for white or bright yellow and white for black or navy`() {
        assertEquals(ColorContrastUtils.DARK_INK, ColorContrastUtils.bestOnBackgroundColor(Color.White))
        assertEquals(ColorContrastUtils.DARK_INK, ColorContrastUtils.bestOnBackgroundColor(Color(0xFFFFEB3B)))
        assertEquals(ColorContrastUtils.DARK_INK, ColorContrastUtils.bestOnBackgroundColor(Color(0xFFF8BBD0)))
        assertEquals(ColorContrastUtils.DARK_INK, ColorContrastUtils.bestOnBackgroundColor(Color(0xFFF44336)))

        assertEquals(ColorContrastUtils.LIGHT_INK, ColorContrastUtils.bestOnBackgroundColor(Color(0xFF212121)))
        assertEquals(ColorContrastUtils.LIGHT_INK, ColorContrastUtils.bestOnBackgroundColor(Color(0xFF1A237E)))
        assertEquals(ColorContrastUtils.LIGHT_INK, ColorContrastUtils.bestOnBackgroundColor(Color(0xFFC62828)))
    }

    @Test
    fun `ensureReadableTextColor flips text when background matches text color`() {
        // 白字放白底 -> 自動翻轉為深墨色且對比度達 WCAG AA 4.5+
        val whiteOnWhite = ColorContrastUtils.ensureReadableTextColor(Color.White, Color.White)
        assertEquals(ColorContrastUtils.DARK_INK, whiteOnWhite)
        assertTrue(ColorContrastUtils.contrastRatio(whiteOnWhite, Color.White) >= 4.5f)

        // 紅節慶字放珊瑚紅／硃砂深紅底 -> 自動切換高對比墨色或白色且達 WCAG AA 4.5+
        val redOnCoral = ColorContrastUtils.ensureReadableTextColor(Color(0xFFD32F2F), Color(0xFFF44336))
        assertEquals(ColorContrastUtils.DARK_INK, redOnCoral)
        assertTrue(ColorContrastUtils.contrastRatio(redOnCoral, Color(0xFFF44336)) >= 4.5f)

        val redOnCrimson = ColorContrastUtils.ensureReadableTextColor(Color(0xFFD32F2F), Color(0xFFC62828))
        assertEquals(ColorContrastUtils.LIGHT_INK, redOnCrimson)
        assertTrue(ColorContrastUtils.contrastRatio(redOnCrimson, Color(0xFFC62828)) >= 4.5f)

        // 深墨字在白底已達高對比 -> 保留原本文字色不亂改
        val preserved = ColorContrastUtils.ensureReadableTextColor(Color(0xFF1E88E5), Color.White, minContrast = 3.0f)
        assertEquals(Color(0xFF1E88E5), preserved)
    }

    @Test
    fun `buildPersonalizedThemeColors keeps custom bright or dark button fill and maintains surface legibility`() {
        val lightBase = lightColorScheme()
        val (adjustedLightScheme, whiteButtonColors) = buildPersonalizedThemeColors(
            baseScheme = lightBase,
            buttonColorLong = 0xFFFFFFFFL,
            darkTheme = false
        )
        // 實心按鍵維持純白底，按鍵文字自動翻轉為深墨色並加上外框線
        assertEquals(Color.White, whiteButtonColors.containerColor)
        assertEquals(ColorContrastUtils.DARK_INK, whiteButtonColors.contentColor)
        assertTrue(whiteButtonColors.needsBorder)
        // 用於 TextButton 與表面字體之 primary 亦強制保有 >= 4.5 對比度
        assertTrue(ColorContrastUtils.contrastRatio(adjustedLightScheme.primary, adjustedLightScheme.surface) >= 4.5f)

        val darkBase = darkColorScheme()
        val (adjustedDarkScheme, blackButtonColors) = buildPersonalizedThemeColors(
            baseScheme = darkBase,
            buttonColorLong = 0xFF212121L,
            darkTheme = true
        )
        assertEquals(Color(0xFF212121), blackButtonColors.containerColor)
        assertEquals(ColorContrastUtils.LIGHT_INK, blackButtonColors.contentColor)
        assertFalse(ColorContrastUtils.contrastRatio(Color(0xFF212121), adjustedDarkScheme.surface) >= 4.5f)
        assertTrue(ColorContrastUtils.contrastRatio(adjustedDarkScheme.primary, adjustedDarkScheme.surface) >= 4.5f)
    }

    @Test
    fun `calendar theme resolver preserves readable day numbers and festival labels on same-hue selected backgrounds`() {
        val lightScheme = lightColorScheme()
        // 測試：紅色選中格遇到紅色節慶文字與週日紅字
        val redCellPalette = CalendarThemeResolver.resolvePalette(
            style = CalendarStyle.INK_WASH,
            customAccentColorLong = 0xFFD32F2FL,
            darkTheme = false,
            themeColorScheme = lightScheme
        )
        val selectedSundayInk = redCellPalette.resolveDayNumberColor(isSelected = true, dayOfWeekIndex = 0)
        val selectedFestivalInk = redCellPalette.resolveLunarLabelColor(
            isSelected = true,
            hasFestival = true,
            hasSolarTerm = false
        )
        assertTrue(ColorContrastUtils.contrastRatio(selectedSundayInk, redCellPalette.selectedBgColor) >= 4.5f)
        assertTrue(ColorContrastUtils.contrastRatio(selectedFestivalInk, redCellPalette.selectedBgColor) >= 4.5f)

        // 測試：高對比黃底選中格遇到淺色/亮字
        val yellowCellPalette = CalendarThemeResolver.resolvePalette(
            style = CalendarStyle.HIGH_CONTRAST,
            customAccentColorLong = 0xFFFFEB3BL,
            darkTheme = true,
            themeColorScheme = darkColorScheme()
        )
        val selectedSolarTermInk = yellowCellPalette.resolveLunarLabelColor(
            isSelected = true,
            hasFestival = false,
            hasSolarTerm = true
        )
        assertTrue(ColorContrastUtils.contrastRatio(selectedSolarTermInk, yellowCellPalette.selectedBgColor) >= 4.5f)
    }

    @Test
    fun `hex color parser validates 6-digit and 8-digit inputs`() {
        assertEquals(0xFFFFEB3BL, ColorContrastUtils.parseHexColorOrNull("#ffeb3b"))
        assertEquals(0xFF2196F3L, ColorContrastUtils.parseHexColorOrNull("2196F3"))
        assertEquals(0x80FFFFFFL, ColorContrastUtils.parseHexColorOrNull("#80FFFFFF"))
        assertNull(ColorContrastUtils.parseHexColorOrNull("#12345"))
        assertNull(ColorContrastUtils.parseHexColorOrNull("GGGGGG"))
        assertEquals("#2196F3", ColorContrastUtils.formatHexColor(0xFF2196F3L))
    }
}
