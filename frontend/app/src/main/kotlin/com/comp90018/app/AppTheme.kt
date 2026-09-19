package com.comp90018.app

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

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

/** Prefer Android's friendly rounded system face and fall back gracefully on other devices. */
private val RoundedFontFamily = FontFamily(Font(DeviceFontFamilyName("sans-serif-rounded")))
private val DefaultTypography = Typography()

val RelicTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = RoundedFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = RoundedFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = RoundedFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = RoundedFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = RoundedFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = RoundedFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = RoundedFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = RoundedFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = RoundedFontFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = RoundedFontFamily),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = RoundedFontFamily),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = RoundedFontFamily),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = RoundedFontFamily),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = RoundedFontFamily),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = RoundedFontFamily),
)
