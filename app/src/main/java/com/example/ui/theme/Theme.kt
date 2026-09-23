package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = LabTealPrimaryDark,
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF70F7E5),
    secondary = LabCyanSecondaryDark,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = LabAmberAccentDark,
    onTertiary = Color(0xFF422C00),
    tertiaryContainer = Color(0xFF604100),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = LabDarkBackground,
    onBackground = LabDarkOnSurface,
    surface = LabDarkSurface,
    onSurface = LabDarkOnSurface,
    surfaceVariant = LabDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBEC9C7)
)

private val LightColorScheme = lightColorScheme(
    primary = LabTealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF70F7E5),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = LabCyanSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5EBFF),
    onSecondaryContainer = Color(0xFF001F25),
    tertiary = LabAmberAccent,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFDF9E),
    onTertiaryContainer = Color(0xFF261900),
    background = LabLightBackground,
    onBackground = LabLightOnSurface,
    surface = LabLightSurface,
    onSurface = LabLightOnSurface,
    surfaceVariant = LabLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF3F4947)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep laboratory brand colors consistent
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
