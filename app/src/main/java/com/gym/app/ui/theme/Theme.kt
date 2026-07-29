package com.gym.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = Gray50,
    primaryContainer = Blue200,
    secondary = Orange500,
    onSecondary = Gray50,
    background = Gray50,
    surface = Gray50,
    onBackground = Gray900,
    onSurface = Gray900,
    error = RedError,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = Gray900,
    primaryContainer = Blue700,
    secondary = Orange500,
    onSecondary = Gray900,
    background = Gray900,
    surface = Gray900,
    onBackground = Gray50,
    onSurface = Gray50,
    error = RedError,
)

@Composable
fun GymTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
