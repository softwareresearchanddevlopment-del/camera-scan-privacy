package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IosBlueDark,
    onPrimary = Color.White,
    primaryContainer = IosBlueDarkContainer,
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = IosIndigo,
    onSecondary = Color.White,
    tertiary = IosGreen,
    onTertiary = Color.White,
    background = IosBackgroundDark,
    onBackground = IosTextPrimaryDark,
    surface = IosSurfaceDark,
    onSurface = IosTextPrimaryDark,
    surfaceVariant = IosSurfaceVariantDark,
    onSurfaceVariant = IosTextSecondaryDark,
    outline = IosBorderDark,
    error = IosRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = IosBlueLight,
    onPrimaryContainer = IosBlue,
    secondary = IosIndigo,
    onSecondary = Color.White,
    tertiary = IosGreen,
    onTertiary = Color.White,
    background = IosBackgroundLight,
    onBackground = IosTextPrimaryLight,
    surface = IosSurfaceLight,
    onSurface = IosTextPrimaryLight,
    surfaceVariant = IosSurfaceVariantLight,
    onSurfaceVariant = IosTextSecondaryLight,
    outline = IosBorderLight,
    error = IosRed,
    onError = Color.White
)

@Composable
fun DocuFlowTheme(
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
