package com.example.taskera.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF6200EE),
    onPrimary          = Color.White,
    secondary          = Color(0xFF03DAC6),
    onSecondary        = Color.Black,
    surface            = Color.White,
    onSurface          = Color.Black,
    background         = Color.White,
    onBackground       = Color.Black,
    error              = Color(0xFFB00020),
    onError            = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary            = Color(0xFFBB86FC),
    onPrimary          = Color.Black,
    secondary          = Color(0xFF03DAC6),
    onSecondary        = Color.Black,
    surface            = Color(0xFF121212),
    onSurface          = Color.White,
    background         = Color(0xFF121212),
    onBackground       = Color.White,
    error              = Color(0xFFCF6679),
    onError            = Color.Black
)

@Composable
fun TaskeraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography  = Typography(),
        content     = content
    )
}
