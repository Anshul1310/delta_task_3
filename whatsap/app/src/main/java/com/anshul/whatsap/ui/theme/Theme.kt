package com.anshul.whatsap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppGreen,
    onPrimary = AppBlack,
    secondary = AppGreenDark,
    onSecondary = AppBlack,
    tertiary = AppGreenDark,
    background = AppBlack,
    onBackground = AppWhite,
    surface = AppDarkSurface,
    onSurface = AppWhite,
    surfaceVariant = AppDarkGray,
    onSurfaceVariant = AppGray
)

@Composable
fun WhatsapTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}