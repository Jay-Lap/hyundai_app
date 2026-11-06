package com.example.hyundiaboysranger.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Explicit foreground/background pairs, independent of wallpaper dynamic colors. */
fun accessibleColors(dark: Boolean) = if (dark) darkColorScheme(
    primary = Color.White, onPrimary = Color.Black,
    primaryContainer = Color(0xFF222222), onPrimaryContainer = Color.White,
    secondary = Color.White, onSecondary = Color.Black,
    secondaryContainer = Color(0xFF222222), onSecondaryContainer = Color.White,
    background = Color.Black, onBackground = Color.White,
    surface = Color.Black, onSurface = Color.White,
    surfaceVariant = Color(0xFF222222), onSurfaceVariant = Color.White,
    surfaceContainerHighest = Color(0xFF222222),
    outline = Color.White, outlineVariant = Color.White,
) else lightColorScheme(
    primary = Color.Black, onPrimary = Color.White,
    primaryContainer = Color(0xFFEEEEEE), onPrimaryContainer = Color.Black,
    secondary = Color.Black, onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEEEEE), onSecondaryContainer = Color.Black,
    background = Color.White, onBackground = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceVariant = Color(0xFFEEEEEE), onSurfaceVariant = Color.Black,
    surfaceContainerHighest = Color(0xFFEEEEEE),
    outline = Color.Black, outlineVariant = Color.Black,
)
