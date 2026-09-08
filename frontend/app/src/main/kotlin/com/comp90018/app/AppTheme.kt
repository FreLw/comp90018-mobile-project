package com.comp90018.app

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Shared visual tokens for every feature screen. */
val Brand = Color(0xFF0A6A5A)
val BrandSoft = Color(0xFFE6ECE8)
val Background = Color(0xFFF6F8F5)
val Ink = Color(0xFF18211F)
val Muted = Color(0xFF48524E)
val RelicGold = Color(0xFFD19A2A)
val RelicRed = Color(0xFFB84A52)
val RelicBlue = Color(0xFF3867D6)

val RelicColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    secondary = RelicRed,
    tertiary = RelicBlue,
    background = Background,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = BrandSoft,
    onSurfaceVariant = Muted,
    outline = Color(0xFF74807B),
)

val RelicTypography = Typography()
