package com.comp90018.app.features.haptics

/*
 * Selects the active or nearest eligible treasure for proximity feedback.
 * Uses raw location evidence without changing map visibility or hunt eligibility.
 */

import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationOutput

/** Select only an active target, or the nearest undiscovered candidate using raw GPS. */
object TreasureHapticTarget {
    fun nearest(
        treasures: List<MapRelic>, discoveredIds: Set<String>, activeTargetId: String?,
        location: LocationOutput, nowNanos: Long,
    ): Pair<String, Double>? {
        val coordinate = TreasureHapticProximity.usableCoordinate(location, nowNanos) ?: return null
        return treasures.asSequence()
            .filter { it.id.isNotBlank() && it.id !in discoveredIds }
            .filter { activeTargetId == null || it.id == activeTargetId }
            .filter { TreasureHapticProximity.validCoordinate(it.coordinate) }
            .map { it.id to LocationCalculator.distanceMeters(coordinate, it.coordinate) }
            .minByOrNull { it.second }
    }
}
