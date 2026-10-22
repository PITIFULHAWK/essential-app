package com.nothing.essential.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Nothing OS AMOLED Palette
val AmoledBlack = Color(0xFF000000)
val DarkSurface = Color(0xFF0D0D0D)
val CardBackground = Color(0xFF141414)
val CardBorder = Color(0xFF222222)
val PillBackground = Color(0xFF1A1A1A)
val PillBorder = Color(0xFF2E2E2E)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF888888)
val TextTertiary = Color(0xFF555555)
val DotInactive = Color(0xFF1E1E1E)

// Accent Colors
val NothingRed = Color(0xFFD71921)
val StatusGreen = Color(0xFF22C55E)
val LeetCodeOrange = Color(0xFFFFA116)
val CodeforcesBlue = Color(0xFF318CE7)
val TufViolet = Color(0xFFE23E57)

private val DarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = AmoledBlack,
    secondary = NothingRed,
    onSecondary = TextPrimary,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun NothingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
