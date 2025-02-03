package com.example.widgetsy

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders


object MyGlanceTheme {
    // dynamically changing color doesnt work

    private val lightColorScheme = ColorScheme(
        primary = Color.Black,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFBB86FC),
        onPrimaryContainer = Color.Black,
        secondary = Color(0xFF03DAC6),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF018786),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFF03DAC6),
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF018786),
        onTertiaryContainer = Color.White,
        background = Color.White,
        onBackground = Color.Black,
        surface = Color(0xFFFFFFFF),
        onSurface = Color.Black,
        surfaceVariant = Color(0xFF121212),
        onSurfaceVariant = Color.White,
        error = Color(0xFFB00020),
        onError = Color.White,
        errorContainer = Color(0xFFCF6679),
        onErrorContainer = Color.Black,
        outline = Color(0xFF000000),
        outlineVariant = Color(0xFF000000),
        inverseOnSurface = Color(0xFF000000),
        inverseSurface = Color(0xFFFFFFFF),
        inversePrimary = Color(0xFFBB86FC),
        surfaceTint = Color(0xFFBB86FC),
        scrim = Color(0xFF000000)
    )

    private val darkColorScheme = ColorScheme(
        primary = Color.White,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFBB86FC),
        onPrimaryContainer = Color.Black,
        secondary = Color(0xFF03DAC6),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF018786),
        onSecondaryContainer = Color.White,
        tertiary = Color(0xFF03DAC6),
        onTertiary = Color.Black,
        tertiaryContainer = Color(0xFF018786),
        onTertiaryContainer = Color.White,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color(0xFF121212),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF121212),
        onSurfaceVariant = Color.White,
        error = Color(0xFFCF6679),
        onError = Color.Black,
        errorContainer = Color(0xFFB00020),
        onErrorContainer = Color.White,
        outline = Color(0xFFFFFFFF),
        outlineVariant = Color(0xFFFFFFFF),
        inverseOnSurface = Color(0xFFFFFFFF),
        inverseSurface = Color(0xFF121212),
        inversePrimary = Color(0xFFBB86FC),
        surfaceTint = Color(0xFFBB86FC),
        scrim = Color(0xFFFFFFFF)
    )

    val colors = ColorProviders(
        light = lightColorScheme,
        dark = darkColorScheme
    )
}