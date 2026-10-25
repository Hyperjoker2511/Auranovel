package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ArcaneDarkColorScheme = darkColorScheme(
    primary = ArcanePrimary,
    onPrimary = Color(0xFF1E1145),
    primaryContainer = Color(0xFF3B2B6D),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = ArcaneSecondary,
    onSecondary = Color(0xFF191E47),
    background = ArcaneBackground,
    onBackground = ArcaneOnSurface,
    surface = ArcaneSurface,
    onSurface = ArcaneOnSurface,
    surfaceVariant = ArcaneSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBC4DC),
    tertiary = ArcaneGold,
    outline = Color(0xFF4C4463)
)

private val ArcaneLightColorScheme = lightColorScheme(
    primary = Color(0xFF6B46C1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9D8FD),
    onPrimaryContainer = Color(0xFF321360),
    secondary = Color(0xFF4338CA),
    onSecondary = Color.White,
    background = Color(0xFFF9F7FD),
    onBackground = Color(0xFF1B1824),
    surface = Color.White,
    onSurface = Color(0xFF1B1824),
    surfaceVariant = Color(0xFFEDE9F6),
    onSurfaceVariant = Color(0xFF4D465C),
    tertiary = Color(0xFFD97706),
    outline = Color(0xFFD1C7E0)
)

private val EInkColorScheme = lightColorScheme(
    primary = EInkBlack,
    onPrimary = EInkWhite,
    primaryContainer = EInkLightGray,
    onPrimaryContainer = EInkBlack,
    secondary = EInkBlack,
    onSecondary = EInkWhite,
    background = EInkWhite,
    onBackground = EInkBlack,
    surface = EInkWhite,
    onSurface = EInkBlack,
    surfaceVariant = EInkLightGray,
    onSurfaceVariant = EInkBlack,
    tertiary = EInkBlack,
    outline = EInkBlack
)

@Composable
fun AuraNovelTheme(
    darkTheme: Boolean = true,
    isEInkMode: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isEInkMode -> EInkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ArcaneDarkColorScheme
        else -> ArcaneLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
