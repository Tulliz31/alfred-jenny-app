package com.alfredJenny.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun AlfredJennyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
