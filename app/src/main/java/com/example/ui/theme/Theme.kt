package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanBright,
    onPrimary = Midnight900,
    primaryContainer = Midnight700,
    onPrimaryContainer = CyanGlow,
    secondary = VioletLight,
    onSecondary = Color.White,
    secondaryContainer = Midnight600,
    onSecondaryContainer = VioletSoft,
    tertiary = EmeraldGreen,
    onTertiary = Color.White,
    background = Midnight900,
    onBackground = PureWhite,
    surface = Midnight800,
    onSurface = PureWhite,
    surfaceVariant = Midnight700,
    onSurfaceVariant = Slate200,
    outline = Slate700
)

private val LightColorScheme = lightColorScheme(
    primary = VioletPrimary,
    onPrimary = Color.White,
    primaryContainer = VioletSoft,
    onPrimaryContainer = Midnight900,
    secondary = ElectricCyan,
    onSecondary = Color.White,
    secondaryContainer = CyanGlow,
    onSecondaryContainer = Midnight900,
    tertiary = EmeraldDark,
    onTertiary = Color.White,
    background = IvoryWhite,
    onBackground = Slate900,
    surface = PureWhite,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
