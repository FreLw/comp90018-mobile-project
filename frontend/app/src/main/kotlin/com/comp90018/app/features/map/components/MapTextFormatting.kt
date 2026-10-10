package com.comp90018.app.features.map.components

import com.comp90018.app.sensors.location.ProximityState
import java.util.Locale

internal fun Double.formatGateDistance(): String =
    "${java.math.BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()} m"

internal fun Double?.formatDistance(): String = when {
    this == null -> "unknown"
    this >= 1000.0 -> String.format(Locale.US, "%.2f km", this / 1000.0)
    else -> "${kotlin.math.round(this / 10.0).toInt() * 10} m"
}

internal fun Double?.formatDegrees(): String =
    this?.let { String.format(Locale.US, "%.0f°", it) } ?: "unknown"

internal fun ProximityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }
