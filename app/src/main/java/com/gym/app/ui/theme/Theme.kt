package com.gym.app.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import com.gym.app.ui.components.AnimationConfig
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ── Typography ──
private val GymTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

// ── Shapes ──
private val GymShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

// ── Color Schemes ──

private val LightColorScheme = lightColorScheme(
    primary = CatOrangeDark,
    onPrimary = Gray50,
    primaryContainer = CatOrangeLight,
    secondary = AccentOrangeRed,
    onSecondary = Gray50,
    background = Gray50,
    surface = Gray50,
    onBackground = Gray900,
    onSurface = Gray900,
    error = RedError,
)

private val DarkColorScheme = darkColorScheme(
    primary = CatOrangeLight,
    onPrimary = Gray900,
    primaryContainer = CatOrangeDark,
    secondary = AccentOrangeRed,
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
        val statusBarColor by animateColorAsState(
            targetValue = colorScheme.primary,
            animationSpec = tween(AnimationConfig.StatusBar.colorMs),
            label = "statusBarColor",
        )
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = statusBarColor.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GymTypography,
        shapes = GymShapes,
        content = content,
    )
}
