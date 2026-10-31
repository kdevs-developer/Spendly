package com.kdevs.spendly.ui.theme

import androidx.annotation.ColorRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdevs.spendly.R
import com.kdevs.spendly.data.Prefs
import com.kdevs.spendly.data.clr

/**
 * Light and dark values live side by side. Every colour below is read at the moment a screen
 * draws, from Prefs.isDark (Compose state), so switching the theme in Settings repaints the
 * whole app straight away.
 */
private fun pick(
    @ColorRes light: Int,
    @ColorRes dark: Int,
): Color = clr(if (Prefs.isDark) dark else light)

/** The brand blue. Never changes with the theme (splash, buttons, chat bubbles). */
val Brand = clr(R.color.brand_primary)

val Primary: Color get() = pick(R.color.primary_light, R.color.primary_dark)
val PrimaryContainer: Color get() = pick(R.color.primary_container_light, R.color.primary_container_dark)
val Bg: Color get() = pick(R.color.bg_light, R.color.bg_dark)
val Surface: Color get() = pick(R.color.surface_light, R.color.surface_dark)
val Ink: Color get() = pick(R.color.ink_light, R.color.ink_dark)
val Muted: Color get() = pick(R.color.muted_light, R.color.muted_dark)
val Faint: Color get() = pick(R.color.faint_light, R.color.faint_dark)
val Line: Color get() = pick(R.color.line_light, R.color.line_dark)
val Track: Color get() = pick(R.color.track_light, R.color.track_dark) // toggle track, empty progress bars
val FieldBorder: Color get() = pick(R.color.field_border_light, R.color.field_border_dark) // text field outline
val Danger: Color get() = pick(R.color.danger_light, R.color.danger_dark)
val DangerBg: Color get() = pick(R.color.danger_bg_light, R.color.danger_bg_dark)
val Success: Color get() = pick(R.color.success_light, R.color.success_dark)
val Warn: Color get() = pick(R.color.warn_light, R.color.warn_dark)

val HeroBrush = Brush.linearGradient(listOf(clr(R.color.brand_light), clr(R.color.brand_primary), clr(R.color.brand_dark)))
val ButtonBrush = Brush.linearGradient(listOf(clr(R.color.brand_light), clr(R.color.brand_primary)))

private val lightScheme =
    lightColorScheme(
        primary = clr(R.color.primary_light),
        onPrimary = Color.White,
        primaryContainer = clr(R.color.primary_container_light),
        onPrimaryContainer = clr(R.color.brand_dark),
        background = clr(R.color.bg_light),
        onBackground = clr(R.color.ink_light),
        surface = clr(R.color.surface_light),
        onSurface = clr(R.color.ink_light),
        surfaceVariant = clr(R.color.track_light),
        onSurfaceVariant = clr(R.color.muted_light),
        error = clr(R.color.danger_light),
        outline = clr(R.color.faint_light),
    )

private val darkScheme =
    darkColorScheme(
        primary = clr(R.color.primary_dark),
        onPrimary = Color.White,
        primaryContainer = clr(R.color.primary_container_dark),
        onPrimaryContainer = clr(R.color.primary_container_light),
        background = clr(R.color.bg_dark),
        onBackground = clr(R.color.ink_dark),
        surface = clr(R.color.surface_dark),
        onSurface = clr(R.color.ink_dark),
        surfaceVariant = clr(R.color.track_dark),
        onSurfaceVariant = clr(R.color.muted_dark),
        error = clr(R.color.danger_dark),
        outline = clr(R.color.faint_dark),
    )

private val type =
    Typography(
        headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
        titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
        titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
    )

private val shapes =
    Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

@Composable
fun SpendlyTheme(content: @Composable () -> Unit) {
    val dark = Prefs.isDark
    MaterialTheme(colorScheme = if (dark) darkScheme else lightScheme, typography = type, shapes = shapes) {
        // Text and icons with no colour of their own follow the theme instead of defaulting to black.
        CompositionLocalProvider(LocalContentColor provides Ink, content = content)
    }
}
