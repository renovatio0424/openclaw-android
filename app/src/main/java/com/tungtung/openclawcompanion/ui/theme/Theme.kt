package com.tungtung.openclawcompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF78A6F9),
    secondary = Color(0xFF3949AB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF9FA8DA),
    tertiary = Color(0xFF03DAC5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color(0xFF0B2347),
    primaryContainer = Color(0xFF3C4C8F),
    secondary = Color(0xFF8C9EFF),
    onSecondary = Color(0xFF111632),
    secondaryContainer = Color(0xFF1D234D),
    tertiary = Color(0xFF26A69A)
)

@Composable
fun OpenClawCompanionTheme(useDarkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
