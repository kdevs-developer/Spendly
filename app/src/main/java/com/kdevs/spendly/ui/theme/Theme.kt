package com.kdevs.spendly.ui.theme

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
import com.kdevs.spendly.data.Prefs

/**
 * Light and dark values live side by side. Every colour below is read at the moment a screen
 * draws, from Prefs.isDark (Compose state), so switching the theme in Settings repaints the
 * whole app straight away.
 */
private fun pick(light: Long, dark: Long): Color = Color(if (Prefs.isDark) dark else light)

/** The brand blue. Never changes with the theme (splash, buttons, chat bubbles). */
val Brand = Color(0xFF1D4ED8)

val Primary: Color get() = pick(0xFF1D4ED8, 0xFF5B8CFF)
val PrimaryContainer: Color get() = pick(0xFFDBE6FD, 0xFF1E2B52)
val Bg: Color get() = pick(0xFFF5F7FB, 0xFF0B1220)
val Surface: Color get() = pick(0xFFFFFFFF, 0xFF151D30)
val Ink: Color get() = pick(0xFF0F172A, 0xFFF1F5F9)
val Muted: Color get() = pick(0xFF475569, 0xFFA3B1C6)
val Faint: Color get() = pick(0xFF94A3B8, 0xFF6B7A93)
val Line: Color get() = pick(0xFFEDF1F7, 0xFF252F47)
val Track: Color get() = pick(0xFFE5EAF3, 0xFF222C45)         // toggle track, empty progress bars
val FieldBorder: Color get() = pick(0xFFD8DFEC, 0xFF36435F)   // text field outline
val Danger: Color get() = pick(0xFFC62828, 0xFFE5534B)
val DangerBg: Color get() = pick(0xFFFDE7E7, 0xFF3A1D22)
val Success: Color get() = pick(0xFF15803D, 0xFF22A559)
val Warn: Color get() = pick(0xFFB45309, 0xFFD97706)

val HeroBrush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF1E3A8A)))
val ButtonBrush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))

private val lightScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8), onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE6FD), onPrimaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFFF5F7FB), onBackground = Color(0xFF0F172A),
    surface = Color.White, onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE5EAF3), onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFC62828), outline = Color(0xFF94A3B8),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFF5B8CFF), onPrimary = Color.White,
    primaryContainer = Color(0xFF1E2B52), onPrimaryContainer = Color(0xFFDBE6FD),
    background = Color(0xFF0B1220), onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF151D30), onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF222C45), onSurfaceVariant = Color(0xFFA3B1C6),
    error = Color(0xFFE5534B), outline = Color(0xFF6B7A93),
)

private val type = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
)

private val shapes = Shapes(
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
