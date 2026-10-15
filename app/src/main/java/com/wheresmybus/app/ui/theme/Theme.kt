package com.wheresmybus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = HeaderGreen,
    onPrimary = Color.White,
    secondary = AccentYellow,
    onSecondary = Color.Black,
    background = Neutral95,
    surface = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = HeaderGreen,
    onPrimary = Color.White,
    secondary = AccentYellow,
    onSecondary = Color.Black,
    background = Neutral10,
    surface = SurfaceDark,
)

@Composable
fun WhereIsMyBusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
