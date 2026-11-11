package com.example.notepad.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.notepad.data.preferences.PersonalizationSettings

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * 實心按鈕客製化配色與自動高對比文字配置
 */
data class CustomButtonColors(
    val containerColor: Color = Purple40,
    val contentColor: Color = Color.White,
    val borderColor: Color = Color(0xFF79747E),
    val needsBorder: Boolean = false
)

val LocalPersonalizationSettings = staticCompositionLocalOf { PersonalizationSettings() }
val LocalCustomButtonColors = staticCompositionLocalOf { CustomButtonColors() }

/**
 * 依使用者個人化設定建構帶對比度保護之 [ColorScheme] 與 [CustomButtonColors]
 */
internal fun buildPersonalizedThemeColors(
    baseScheme: ColorScheme,
    buttonColorLong: Long?,
    darkTheme: Boolean
): Pair<ColorScheme, CustomButtonColors> {
    if (buttonColorLong == null) {
        val defaultBtnColors = CustomButtonColors(
            containerColor = baseScheme.primary,
            contentColor = ColorContrastUtils.ensureReadableTextColor(baseScheme.onPrimary, baseScheme.primary),
            borderColor = baseScheme.outline,
            needsBorder = ColorContrastUtils.needsSubtleBorder(baseScheme.primary, baseScheme.surface)
        )
        return baseScheme to defaultBtnColors
    }

    val chosenButtonColor = Color(buttonColorLong)
    val surfaceColor = baseScheme.surface

    // 用於文字、TextButton、OutlinedTextField 與圖示的 primary，強制維持與表面至少 4.5:1 對比度
    val safeSurfaceAccent = ColorContrastUtils.ensureReadableAccentOnSurface(
        accentColor = chosenButtonColor,
        surfaceColor = surfaceColor,
        minContrast = ColorContrastUtils.MIN_TEXT_CONTRAST
    )
    val safeOnSurfaceAccent = ColorContrastUtils.bestOnBackgroundColor(safeSurfaceAccent)

    // 淡化容器色同時兼顧疊加於 background 上時的清楚文字對比
    val softContainer = lerp(surfaceColor, chosenButtonColor, if (darkTheme) 0.28f else 0.20f)
    val effectiveBlendedContainer = lerp(baseScheme.background, softContainer, 0.5f)
    val safeOnContainer = ColorContrastUtils.bestOnBackgroundColor(effectiveBlendedContainer)

    val adjustedScheme = baseScheme.copy(
        primary = safeSurfaceAccent,
        onPrimary = safeOnSurfaceAccent,
        primaryContainer = softContainer,
        onPrimaryContainer = safeOnContainer,
        secondary = safeSurfaceAccent,
        onSecondary = safeOnSurfaceAccent
    )

    val customButtonColors = CustomButtonColors(
        containerColor = chosenButtonColor,
        contentColor = ColorContrastUtils.bestOnBackgroundColor(chosenButtonColor),
        borderColor = baseScheme.outline,
        needsBorder = ColorContrastUtils.needsSubtleBorder(chosenButtonColor, surfaceColor)
    )

    return adjustedScheme to customButtonColors
}

@Composable
fun NotepadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    personalization: PersonalizationSettings = PersonalizationSettings(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && personalization.buttonColorLong == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val (colorScheme, buttonColors) = buildPersonalizedThemeColors(
        baseScheme = baseScheme,
        buttonColorLong = personalization.buttonColorLong,
        darkTheme = darkTheme
    )

    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * personalization.fontScale.scaleFactor
    )

    CompositionLocalProvider(
        LocalPersonalizationSettings provides personalization,
        LocalCustomButtonColors provides buttonColors,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
