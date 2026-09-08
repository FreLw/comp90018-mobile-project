package com.unimelb.losttreasures.sensor.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationCalculator {
    private const val EarthRadiusMeters = 6_371_000.0

    fun buildLocationState(
        currentLocation: GeoCoordinate?,
        targetLocation: GeoCoordinate?,
        insideRadiusMeters: Float,
        nearbyRadiusMeters: Float
    ): LocationState {
        if (currentLocation == null || targetLocation == null) {
            return LocationState(
                currentLocation = currentLocation,
                targetLocation = targetLocation,
                isLocationAvailable = currentLocation != null
            )
        }

        val distance = distanceMeters(currentLocation, targetLocation)
        val bearing = bearingDegrees(currentLocation, targetLocation)

        return LocationState(
            currentLocation = currentLocation,
            targetLocation = targetLocation,
            distanceToTargetMeters = distance,
            targetBearingDegrees = bearing,
            proximityState = proximityState(distance, insideRadiusMeters, nearbyRadiusMeters),
            isLocationAvailable = true
        )
    }

    fun distanceMeters(from: GeoCoordinate, to: GeoCoordinate): Float {
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
            cos(fromLat) * cos(toLat) *
            sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (EarthRadiusMeters * c).toFloat()
    }

    fun bearingDegrees(from: GeoCoordinate, to: GeoCoordinate): Float {
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val y = sin(deltaLon) * cos(toLat)
        val x = cos(fromLat) * sin(toLat) -
            sin(fromLat) * cos(toLat) * cos(deltaLon)

        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    fun proximityState(
        distanceMeters: Float,
        insideRadiusMeters: Float,
        nearbyRadiusMeters: Float
    ): ProximityState {
        return when {
            distanceMeters <= insideRadiusMeters -> ProximityState.Inside
            distanceMeters <= nearbyRadiusMeters -> ProximityState.Nearby
            else -> ProximityState.Outside
        }
    }
}
