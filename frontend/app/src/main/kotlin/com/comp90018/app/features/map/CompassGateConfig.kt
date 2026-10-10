package com.comp90018.app.features.map

/*
 * Defines configurable distance and heading tolerances for the pre-task compass interface.
 * These values determine when the compass may offer entry to the physical challenge.
 */

/** Editable fields on each Firestore treasures/{id} document. */
data class CompassGateConfig(
    val huntReadyRadiusMeters: Double = 10.0,
    val compassAlignmentToleranceDegrees: Double = 15.0,
) {
    init {
        require(huntReadyRadiusMeters.isFinite() && huntReadyRadiusMeters > 0.0)
        require(compassAlignmentToleranceDegrees.isFinite() && compassAlignmentToleranceDegrees in 0.0..180.0)
    }
}
