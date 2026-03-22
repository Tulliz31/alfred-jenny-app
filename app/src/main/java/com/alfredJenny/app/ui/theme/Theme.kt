package com.alfredJenny.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AlfredBlue,
    onPrimary = OnBackground,
    primaryContainer = AlfredBlueDark,
    onPrimaryContainer = AlfredBlueLight,
    secondary = JennyPurple,
    onSecondary = OnBackground,
    secondaryContainer = JennyPurpleDark,
    onSecondaryContainer = JennyPurpleLight,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorRed,
)

private val LightColorScheme = lightColorScheme(
    primary = AlfredBlueLight,
    onPrimary = LightBackground,
    primaryContainer = AlfredBlue,
    onPrimaryContainer = LightBackground,
    secondary = JennyPurpleLight,
    onSecondary = LightBackground,
    secondaryContainer = JennyPurple,
    onSecondaryContainer = LightBackground,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = ErrorRed,
)

@Composable
fun AlfredJennyTheme(useLightTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (useLightTheme) LightColorScheme else DarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
