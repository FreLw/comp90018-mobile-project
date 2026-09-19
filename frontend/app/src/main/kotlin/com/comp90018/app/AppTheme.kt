package com.comp90018.app

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/** Shared visual tokens for every feature screen. */
val Brand = Color(0xFF7A4B2A)
val BrandSoft = Color(0xFFF0E1CC)
val Background = Color(0xFFF8F0E4)
val Ink = Color(0xFF342319)
val Muted = Color(0xFF705B4C)
val RelicGold = Color(0xFFB7791F)
val RelicRed = Color(0xFFA64B3F)
val RelicBlue = Color(0xFF6D7892)

val RelicColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    secondary = RelicRed,
    tertiary = RelicBlue,
    background = Background,
    onBackground = Ink,
    surface = Color(0xFFFFFBF5),
    onSurface = Ink,
    surfaceVariant = BrandSoft,
    onSurfaceVariant = Muted,
    outline = Color(0xFF9A7D67),
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
