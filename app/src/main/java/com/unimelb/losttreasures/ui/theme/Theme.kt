package com.unimelb.losttreasures.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.unimelb.losttreasures.ui.model.RelicTone

val RelicGreen = Color(0xFF0A6A5A)
val RelicRed = Color(0xFFB84A52)
val RelicBlue = Color(0xFF3867D6)
val RelicGold = Color(0xFFD19A2A)
val Ink = Color(0xFF18211F)

private val AppColorScheme = lightColorScheme(
    primary = RelicGreen,
    onPrimary = Color.White,
    secondary = RelicRed,
    tertiary = RelicBlue,
    background = Color(0xFFF6F8F5),
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE6ECE8),
    onSurfaceVariant = Color(0xFF48524E),
    outline = Color(0xFF74807B)
)

@Composable
fun LostTreasuresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}

fun RelicTone.toColor(): Color = when (this) {
    RelicTone.Green -> RelicGreen
    RelicTone.Red -> RelicRed
    RelicTone.Blue -> RelicBlue
    RelicTone.Gold -> RelicGold
}
