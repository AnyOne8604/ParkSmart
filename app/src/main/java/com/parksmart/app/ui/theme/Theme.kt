package com.parksmart.app.ui.theme

// Tema Material 3 claro de ParkSmart; centraliza colores para que todas las pantallas compartan estilo (v0.1.0).

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Traduce la paleta de Color.kt a los roles semánticos que esperan los componentes Material.
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
// Proveedor de tema que envuelve la aplicación y propaga el esquema de color a sus componentes.
fun ParkSmartTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ParkSmartColors,
        content = content,
    )
}
