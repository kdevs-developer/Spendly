package com.kdevs.spendly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

val Primary = Color(0xFF1D4ED8)
val PrimaryContainer = Color(0xFFDBE6FD)
val Bg = Color(0xFFF5F7FB)
val Surface = Color(0xFFFFFFFF)
val Ink = Color(0xFF0F172A)
val Muted = Color(0xFF475569)
val Faint = Color(0xFF94A3B8)
val Line = Color(0xFFEDF1F7)
val Danger = Color(0xFFC62828)
val DangerBg = Color(0xFFFDE7E7)
val Success = Color(0xFF15803D)
val Warn = Color(0xFFB45309)

val HeroBrush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF1E3A8A)))
val ButtonBrush = Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))

private val scheme = lightColorScheme(
    primary = Primary, onPrimary = Color.White,
    primaryContainer = PrimaryContainer, onPrimaryContainer = Color(0xFF1E3A8A),
    background = Bg, onBackground = Ink,
    surface = Surface, onSurface = Ink,
    surfaceVariant = Color(0xFFE5EAF3), onSurfaceVariant = Muted,
    error = Danger, outline = Faint,
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
    // The design is light only (no dark mode in this version).
    MaterialTheme(colorScheme = scheme, typography = type, shapes = shapes, content = content)
}
