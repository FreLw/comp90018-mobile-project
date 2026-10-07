package com.comp90018.app.features.map

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
