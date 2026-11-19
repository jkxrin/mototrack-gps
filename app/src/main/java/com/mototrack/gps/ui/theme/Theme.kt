package com.mototrack.gps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Motorsport Racing Palette
val CarbonBlack = Color(0xFF0D0F12)
val DarkGunmetal = Color(0xFF14181E)
val CardBackground = Color(0xFF1B2129)
val BorderHighlight = Color(0xFF2A3441)

val NeonCyan = Color(0xFF00E5FF)
val NeonGreen = Color(0xFF00E676)
val NeonAmber = Color(0xFFFFAB00)
val NeonRed = Color(0xFFFF1744)
val NeonRedGlow = Color(0x66FF1744)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF90A4AE)
val TextDisabled = Color(0xFF546E7A)

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CarbonBlack,
    primaryContainer = DarkGunmetal,
    onPrimaryContainer = NeonCyan,
    secondary = NeonRed,
    onSecondary = Color.White,
    background = CarbonBlack,
    onBackground = TextPrimary,
    surface = DarkGunmetal,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    outline = BorderHighlight
)

@Composable
fun MotoTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
