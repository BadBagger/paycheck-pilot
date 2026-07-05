package com.paycheckpilot.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF1E6F5C),
    secondary = Color(0xFF52645E),
    tertiary = Color(0xFF6E5D2E),
    error = Color(0xFFB3261E),
    surface = Color(0xFFF8FAF9),
    surfaceVariant = Color(0xFFE3E8E5),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8AD6C0),
    secondary = Color(0xFFB8CCC4),
    tertiary = Color(0xFFDCC78C),
    surface = Color(0xFF111413),
    surfaceVariant = Color(0xFF3F4945),
)

@Composable
fun PaycheckPilotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content,
    )
}
