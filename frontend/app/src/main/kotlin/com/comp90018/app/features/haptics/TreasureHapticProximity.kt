package com.comp90018.app.features.haptics

import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.*

/** Feedback policy only: never used for Start Hunt or challenge eligibility. */
object TreasureHapticProximity {
    const val RADIUS_METERS = 20.0

    fun isNearby(distanceMeters: Double?): Boolean = distanceMeters != null &&
        distanceMeters.isFinite() && distanceMeters >= 0.0 && distanceMeters <= RADIUS_METERS

    fun usableCoordinate(location: LocationOutput, nowNanos: Long): GeoCoordinate? {
        val config = LocationConfig()
        val timestamp = location.timestampNanos ?: return null
        val accuracy = location.accuracyMeters ?: return null
        if (location.validity != SensorValidity.VALID || !location.permission.canUnlockTreasure ||
            location.availability != LocationAvailabilityState.AVAILABLE ||
            timestamp < 0 || nowNanos < timestamp || nowNanos - timestamp > config.staleTimeoutNanos ||
            !accuracy.isFinite() || accuracy < 0 || accuracy > config.maxAcceptedAccuracyMeters
        ) return null
        return location.currentLocation?.takeIf(::validCoordinate)
    }

    fun validCoordinate(coordinate: GeoCoordinate): Boolean =
        coordinate.latitude.isFinite() && coordinate.longitude.isFinite() &&
            coordinate.latitude in -90.0..90.0 && coordinate.longitude in -180.0..180.0
}
