package com.comp90018.app.features.treasure

import java.util.Locale
import kotlin.math.round

/** Entry presentation only: even a nearby relic must go through reliable Navigation arrival. */
internal data class TreasureNavigationEntry(
    val primaryLabel: String? = null,
    val secondaryMapLabel: String? = null,
)

internal fun treasureNavigationEntry(
    discovered: Boolean,
    actionableDistanceMeters: Double?,
    navigationAvailable: Boolean,
): TreasureNavigationEntry {
    if (discovered) return TreasureNavigationEntry()
    val distance = actionableDistanceMeters?.takeIf { it.isFinite() && it >= 0.0 }
    return TreasureNavigationEntry(
        primaryLabel = if (navigationAvailable) distance?.let {
            "Follow the Resonance · ${it.formatTreasureDistance()}"
        } ?: "Follow the Resonance" else null,
        secondaryMapLabel = "View on map",
    )
}

internal fun Double.formatTreasureDistance(): String = when {
    this >= 1000.0 -> String.format(Locale.US, "%.2f km", this / 1000.0)
    else -> "${round(this).toInt()} m"
}
