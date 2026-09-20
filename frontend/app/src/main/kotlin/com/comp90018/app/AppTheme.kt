package com.comp90018.app

import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

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

/** Bundled fonts keep the friendly visual identity consistent on every Android device. */
val RoundedBodyFontFamily = FontFamily(
    Font(R.font.nunito_bold, weight = FontWeight.Bold),
)

val RoundedTitleFontFamily = FontFamily(
    Font(R.font.fredoka_variable, weight = FontWeight.Medium),
    Font(R.font.fredoka_variable, weight = FontWeight.SemiBold),
    Font(R.font.fredoka_variable, weight = FontWeight.Bold),
)

val GothicTreasureFontFamily = FontFamily(
    Font(R.font.unifraktur_cook_bold, weight = FontWeight.Bold),
)

private val DefaultTypography = Typography()

val RelicTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = RoundedTitleFontFamily),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = RoundedTitleFontFamily),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = RoundedTitleFontFamily),
    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = RoundedTitleFontFamily),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = RoundedTitleFontFamily),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = RoundedTitleFontFamily),
    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = RoundedTitleFontFamily),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = RoundedTitleFontFamily),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = RoundedTitleFontFamily),
    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = RoundedBodyFontFamily, fontWeight = FontWeight.Bold),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = RoundedBodyFontFamily, fontWeight = FontWeight.Bold),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = RoundedBodyFontFamily, fontWeight = FontWeight.Bold),
    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = RoundedBodyFontFamily, fontWeight = FontWeight.Bold),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = RoundedBodyFontFamily, fontWeight = FontWeight.Bold),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = RoundedBodyFontFamily, fontWeight = FontWeight.Bold),
)
