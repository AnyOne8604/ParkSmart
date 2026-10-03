package com.parksmart.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ParkSmartColors = lightColorScheme(
    primary = ParkGreen,
    onPrimary = Color.White,
    primaryContainer = ParkGreenLight,
    onPrimaryContainer = ParkGreenDark,
    secondary = Color(0xFF237B68),
    background = Canvas,
    surface = Color.White,
    onSurface = Ink,
    onSurfaceVariant = MutedInk,
    outline = Line,
    error = Danger,
)

@Composable
fun ParkSmartTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ParkSmartColors,
        content = content,
    )
}
