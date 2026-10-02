package com.comp90018.app.sensors.location

data class LocationConfig(
    val insideRadiusMeters: Double = 20.0,
    val nearbyRadiusMeters: Double = 120.0,
    val proximityHysteresisMeters: Double = 5.0,
    val staleTimeoutNanos: Long = 10_000_000_000L,
    val maxAcceptedAccuracyMeters: Double = 50.0,
) {
    init {
        require(insideRadiusMeters >= 0.0)
        require(nearbyRadiusMeters >= insideRadiusMeters)
        require(proximityHysteresisMeters >= 0.0)
        require(staleTimeoutNanos > 0)
        require(maxAcceptedAccuracyMeters > 0.0)
    }
}
