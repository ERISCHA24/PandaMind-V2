package com.example.animepopular.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Color Palette ────────────────────────────────────────────────────────────

val BackgroundDark     = Color(0xFF0F0F1A)
val CardBackground     = Color(0xFF1E1E30)
val ColorPrimary       = Color(0xFF1A1A2E)
val ColorPrimaryVar    = Color(0xFF16213E)
val AccentColor        = Color(0xFFE94560)
val AccentColorLight   = Color(0xFFFF6B81)
val RatingColor        = Color(0xFFFFD700)
val ImdbYellow         = Color(0xFFF5C518)
val TextPrimary        = Color(0xFFFFFFFF)
val TextSecondary      = Color(0xFFAAAAACC)
val SurfaceColor       = Color(0xFF252538)
val DividerColor       = Color(0xFF3A3A55)

val darkColorScheme = darkColorScheme(
    primary        = AccentColor,
    onPrimary      = Color.White,
    primaryContainer = ColorPrimary,
    onPrimaryContainer = TextPrimary,
    secondary      = AccentColorLight,
    onSecondary    = Color.White,
    background     = BackgroundDark,
    onBackground   = TextPrimary,
    surface        = CardBackground,
    onSurface      = TextPrimary,
    surfaceVariant = SurfaceColor,
    onSurfaceVariant = TextSecondary,
    error          = Color(0xFFCF6679),
    outline        = DividerColor
)

val lightColorScheme = lightColorScheme(
    primary        = AccentColor,
    onPrimary      = Color.White,
    primaryContainer = Color(0xFFFFE0E6),
    onPrimaryContainer = Color(0xFF3D0012),
    secondary      = Color(0xFF8B0000),
    onSecondary    = Color.White,
    background     = Color(0xFFF5F5F5),
    onBackground   = Color(0xFF1A1A2E),
    surface        = Color.White,
    onSurface      = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF444455),
    error          = Color(0xFFB00020),
    outline        = Color(0xFFCCCCDD)
)

@Composable
fun AnimePopularTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}