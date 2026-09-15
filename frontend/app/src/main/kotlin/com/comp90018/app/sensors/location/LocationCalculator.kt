package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationCalculator {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun buildOutput(
        currentLocation: GeoCoordinate?,
        targetLocation: GeoCoordinate?,
        timestampNanos: Long? = null,
        config: LocationConfig = LocationConfig(),
    ): LocationOutput {
        if (currentLocation == null || targetLocation == null) {
            return LocationOutput(
                currentLocation = currentLocation,
                targetLocation = targetLocation,
                validity = SensorValidity.UNKNOWN,
                timestampNanos = timestampNanos,
            )
        }
        if (!currentLocation.isValid() || !targetLocation.isValid()) {
            return LocationOutput(
                currentLocation = currentLocation,
                targetLocation = targetLocation,
                validity = SensorValidity.UNRELIABLE,
                timestampNanos = timestampNanos,
            )
        }

        val distance = distanceMeters(currentLocation, targetLocation)
        val bearing = bearingDegrees(currentLocation, targetLocation)

        return LocationOutput(
            currentLocation = currentLocation,
            targetLocation = targetLocation,
            distanceToTargetMeters = distance,
            targetBearingDegrees = bearing,
            proximity = proximityState(distance, config.insideRadiusMeters, config.nearbyRadiusMeters),
            validity = SensorValidity.VALID,
            timestampNanos = timestampNanos,
        )
    }

    fun distanceMeters(from: GeoCoordinate, to: GeoCoordinate): Double {
        require(from.isValid() && to.isValid())
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(fromLat) * cos(toLat) *
            sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    fun bearingDegrees(from: GeoCoordinate, to: GeoCoordinate): Double {
        require(from.isValid() && to.isValid())
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val y = sin(deltaLon) * cos(toLat)
        val x = cos(fromLat) * sin(toLat) -
            sin(fromLat) * cos(toLat) * cos(deltaLon)

        return normalizeDegrees(Math.toDegrees(atan2(y, x)))
    }

    fun proximityState(
        distanceMeters: Double,
        insideRadiusMeters: Double,
        nearbyRadiusMeters: Double,
    ): ProximityState {
        require(distanceMeters.isFinite() && distanceMeters >= 0.0)
        require(insideRadiusMeters >= 0.0 && nearbyRadiusMeters >= insideRadiusMeters)
        return when {
            distanceMeters <= insideRadiusMeters -> ProximityState.INSIDE
            distanceMeters <= nearbyRadiusMeters -> ProximityState.NEARBY
            else -> ProximityState.OUTSIDE
        }
    }

    fun normalizeDegrees(degrees: Double): Double {
        require(degrees.isFinite())
        val normalized = ((degrees % 360.0) + 360.0) % 360.0
        return if (normalized == 0.0) 0.0 else normalized
    }

    private fun GeoCoordinate.isValid(): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
}
