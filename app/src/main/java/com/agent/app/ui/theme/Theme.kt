package com.agent.app.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    // 品牌
    primary = Teal400,
    onPrimary = Slate950,
    primaryContainer = Teal800,
    onPrimaryContainer = Teal200,
    // 二级
    secondary = Slate400,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = Slate200,
    // 三级
    tertiary = Green400,
    onTertiary = Slate950,
    tertiaryContainer = Color(0xFF1A3A2A),
    onTertiaryContainer = Green400,
    // 错误
    error = Red400,
    onError = Slate950,
    errorContainer = Color(0xFF3B1A1A),
    onErrorContainer = Red400,
    // 表面
    background = Slate950,
    onBackground = Slate200,
    surface = Slate950,
    onSurface = Slate200,
    surfaceVariant = Slate900,
    onSurfaceVariant = Slate400,
    // 轮廓
    outline = Slate800,
    outlineVariant = Color(0xFF2D3A4F),
    // 反色
    inverseSurface = Slate200,
    inverseOnSurface = Slate950,
    inversePrimary = Teal800,
)

@Composable
fun AndroidAgentAppTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Slate950.toArgb()
            window.navigationBarColor = Slate950.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgentTypography,
        shapes = AgentShapes,
        content = content
    )
}
