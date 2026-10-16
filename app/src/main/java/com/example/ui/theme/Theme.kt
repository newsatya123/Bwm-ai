package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BwmColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F25),
    primaryContainer = Color(0xFF004D5B),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = ElectricViolet,
    onSecondary = Color(0xFF28004B),
    secondaryContainer = Color(0xFF581C87),
    onSecondaryContainer = Color(0xFFF3E8FF),
    tertiary = RadiantEmerald,
    onTertiary = Color(0xFF00201A),
    background = DeepNavyDark,
    onBackground = TextPrimary,
    surface = SurfaceNavy,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantNavy,
    onSurfaceVariant = TextSecondary,
    outline = CardBorderNavy,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep bespoke premium Navy theme for BWM AI
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BwmColorScheme,
        typography = Typography,
        content = content
    )
}
