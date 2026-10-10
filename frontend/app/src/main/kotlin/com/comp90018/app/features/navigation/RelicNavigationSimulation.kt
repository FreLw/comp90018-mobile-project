package com.comp90018.app.features.navigation

/*
 * Stores target-local debug distance/heading inputs and synthetic GPS fix identity.
 * Only changed slider values create new evidence, so recomposition cannot falsely confirm arrival.
 */

/** Local debug evidence; only changed slider values create fixes, never recompositions. */
internal data class RelicNavigationSimulation(
    val distanceMeters: Double? = null,
    val fixSequence: Long = 0L,
) {
    fun withDistance(distance: Double?): RelicNavigationSimulation = when {
        distance == distanceMeters -> this
        distance == null -> RelicNavigationSimulation()
        else -> RelicNavigationSimulation(distance, fixSequence + 1L)
    }

    fun arrivalSample(): RelicArrivalSample = RelicArrivalSample(
        hasActionableLocation = distanceMeters != null,
        distanceMeters = distanceMeters,
        accuracyMeters = null,
        timestampNanos = fixSequence.takeIf { distanceMeters != null },
    )
}
