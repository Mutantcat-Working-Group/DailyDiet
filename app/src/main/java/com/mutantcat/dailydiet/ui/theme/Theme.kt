package com.mutantcat.dailydiet.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF00696D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F5),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF6E5C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE07A),
    onSecondaryContainer = Color(0xFF221B00),
    tertiary = Color(0xFF9A4522),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCC),
    onTertiaryContainer = Color(0xFF3A0B00),
    background = Color(0xFFF6FAF8),
    onBackground = Color(0xFF161D1D),
    surface = Color(0xFFF6FAF8),
    onSurface = Color(0xFF161D1D),
    surfaceVariant = Color(0xFFDAE5E4),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7979),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4D9),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F52),
    onPrimaryContainer = Color(0xFF9CF1F5),
    secondary = Color(0xFFE0C44A),
    onSecondary = Color(0xFF3A2F00),
    secondaryContainer = Color(0xFF534600),
    onSecondaryContainer = Color(0xFFFFE07A),
    tertiary = Color(0xFFFFB596),
    onTertiary = Color(0xFF571F09),
    tertiaryContainer = Color(0xFF7A3413),
    onTertiaryContainer = Color(0xFFFFDBCC),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C8),
    outline = Color(0xFF889392),
)

@Composable
fun DailyDietTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

