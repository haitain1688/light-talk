package com.lighttool.dengyu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7A7FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7856FF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF66E6DD),
    background = Color(0xFF0F1222),
    surface = Color(0xFF171B31),
    surfaceVariant = Color(0xFF212744),
    onSurface = Color(0xFFF2F3FA),
    outline = Color(0xFF7C86B2)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7856FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E0FF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF008984),
    background = Color(0xFFF6F7FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E8F6),
    onSurface = Color(0xFF171B31),
    outline = Color(0xFF66719B)
)

@Composable
fun LightSignalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
