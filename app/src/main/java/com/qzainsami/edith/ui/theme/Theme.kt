package com.qzainsami.edith.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DraculaColorScheme = darkColorScheme(
    primary = DraculaPurple,
    onPrimary = DraculaBackground,
    primaryContainer = DraculaCurrentLine,
    onPrimaryContainer = DraculaForeground,
    secondary = DraculaPink,
    onSecondary = DraculaBackground,
    secondaryContainer = DraculaComment,
    onSecondaryContainer = DraculaForeground,
    tertiary = DraculaCyan,
    background = DraculaBackground,
    onBackground = DraculaForeground,
    surface = DraculaCurrentLine,
    onSurface = DraculaForeground,
    error = DraculaRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun EDITHTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DraculaColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
