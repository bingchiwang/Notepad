package com.example.notepad.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/**
 * 色彩對比度檢測與自動調色工具
 *
 * 依據 WCAG 相對亮度公式計算文字與背景對比度；若文字或圖示色彩過於接近背景色，
 * 自動切換或調整為高對比色彩，確保在任何自訂按鍵顏色與月曆樣式下均清晰可辨。
 */
object ColorContrastUtils {

    val DARK_INK = Color(0xFF121316)
    val LIGHT_INK = Color.White

    /** WCAG AA 一般文字建議最小對比度 */
    const val MIN_TEXT_CONTRAST = 4.5f

    /** 大文字、邊框與圖示最小對比度 */
    const val MIN_ACCENT_CONTRAST = 3.0f

    /**
     * 計算前景色疊加於背景色後的 WCAG 對比度 (1.0 ~ 21.0)
     */
    fun contrastRatio(foreground: Color, background: Color): Float {
        val opaqueBg = if (background.alpha < 1f) background.copy(alpha = 1f) else background
        val effectiveFg = if (foreground.alpha < 1f) {
            foreground.compositeOver(opaqueBg)
        } else {
            foreground
        }
        val l1 = effectiveFg.luminance()
        val l2 = opaqueBg.luminance()
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * 針對給定實心背景色，回傳對比度最高之清晰前景色（深墨黑或純白）
     */
    fun bestOnBackgroundColor(background: Color): Color {
        val opaqueBg = if (background.alpha < 1f) background.copy(alpha = 1f) else background
        val whiteContrast = contrastRatio(LIGHT_INK, opaqueBg)
        val darkContrast = contrastRatio(DARK_INK, opaqueBg)
        return if (whiteContrast >= darkContrast) LIGHT_INK else DARK_INK
    }

    /**
     * 若預期文字顏色與背景色過於接近，自動改用與背景對比度最高之深／淺清晰文字色
     */
    fun ensureReadableTextColor(
        preferredColor: Color,
        backgroundColor: Color,
        minContrast: Float = MIN_TEXT_CONTRAST
    ): Color {
        val opaqueBg = if (backgroundColor.alpha < 1f) backgroundColor.copy(alpha = 1f) else backgroundColor
        return if (contrastRatio(preferredColor, opaqueBg) >= minContrast) {
            preferredColor
        } else {
            bestOnBackgroundColor(opaqueBg)
        }
    }

    /**
     * 針對置於表面（如 Dialog 卡片底色）上之強調文字、圖示或邊框調整明度，
     * 優先保留原色相；若仍不足則逐步往高對比方向深化／提亮或回退至最高對比墨色。
     */
    fun ensureReadableAccentOnSurface(
        accentColor: Color,
        surfaceColor: Color,
        minContrast: Float = MIN_TEXT_CONTRAST
    ): Color {
        val opaqueSurface = if (surfaceColor.alpha < 1f) surfaceColor.copy(alpha = 1f) else surfaceColor
        if (contrastRatio(accentColor, opaqueSurface) >= minContrast) {
            return accentColor
        }
        val surfaceIsLight = opaqueSurface.luminance() > 0.5f
        val targetTarget = if (surfaceIsLight) DARK_INK else LIGHT_INK
        for (step in 1..10) {
            val fraction = step * 0.1f
            val candidate = lerp(accentColor, targetTarget, fraction)
            if (contrastRatio(candidate, opaqueSurface) >= minContrast) {
                return candidate
            }
        }
        return bestOnBackgroundColor(opaqueSurface)
    }

    /**
     * 判斷實心按鈕是否因顏色極接近所在背景而需要加上明顯外框線
     */
    fun needsSubtleBorder(containerColor: Color, surfaceColor: Color): Boolean {
        return contrastRatio(containerColor, surfaceColor) < 1.35f
    }

    /**
     * 解析 6 碼或 8 碼十六進位色碼（支援可選 '#' 前綴），無效格式回傳 null
     */
    fun parseHexColorOrNull(rawInput: String): Long? {
        val cleaned = rawInput.trim().removePrefix("#")
        if (cleaned.length != 6 && cleaned.length != 8) return null
        val parsed = cleaned.toLongOrNull(16) ?: return null
        return if (cleaned.length == 6) {
            0xFF000000L or parsed
        } else {
            parsed
        }
    }

    /**
     * 格式化 ARGB Color Long 為 `#RRGGBB` 顯示字串
     */
    fun formatHexColor(colorLong: Long): String {
        val rgb = colorLong and 0x00FFFFFFL
        return "#" + rgb.toString(16).uppercase().padStart(6, '0')
    }
}
