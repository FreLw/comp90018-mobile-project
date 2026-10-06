package com.comp90018.app.features.map

import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput

/** Keeps display-only fallback coordinates out of treasure completion decisions. */
internal object LocationActionPolicy {
    /** Display fallbacks reveal nearby markers without granting arrival or hunt readiness. */
    fun mapDisplayCoordinate(location: LocationOutput, runningInEmulator: Boolean): GeoCoordinate =
        if (runningInEmulator) DEFAULT_CAMPUS_CENTRE
        else location.currentLocation ?: location.lastKnownLocation ?: DEFAULT_CAMPUS_CENTRE

    fun actionableCoordinate(location: LocationOutput): GeoCoordinate? =
        location.currentLocation?.takeIf {
            location.permission.isGranted && location.validity == SensorValidity.VALID
        }

    fun isWithinRadius(
        location: LocationOutput,
        target: GeoCoordinate,
        radiusMeters: Double,
    ): Boolean {
        if (!radiusMeters.isFinite() || radiusMeters < 0.0) return false
        val current = actionableCoordinate(location) ?: return false
        return LocationCalculator.distanceMeters(current, target) <= radiusMeters
    }

    fun targetOutput(
        location: LocationOutput,
        target: GeoCoordinate?,
        config: LocationConfig,
        simulatedCoordinate: GeoCoordinate? = null,
    ): LocationOutput {
        val current = simulatedCoordinate ?: actionableCoordinate(location)
        val display = simulatedCoordinate ?: location.currentLocation ?: location.lastKnownLocation
        return LocationCalculator.buildOutput(
            currentLocation = current,
            targetLocation = target,
            timestampNanos = location.timestampNanos,
            config = config,
            permission = location.permission,
            availability = location.availability,
            accuracyMeters = location.accuracyMeters,
            lastKnownLocation = display,
        )
    }
}
