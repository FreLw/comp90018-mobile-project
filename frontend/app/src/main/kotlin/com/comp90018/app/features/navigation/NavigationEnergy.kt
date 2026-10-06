package com.comp90018.app.features.navigation

import com.comp90018.app.sensors.location.GeoCoordinate
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal const val NAVIGATION_ENERGY_RANGE_METERS = 250.0
internal const val NAVIGATION_ARRIVAL_METERS = 10.0

internal fun navigationEnergyProgress(distanceMeters: Double?): Float = when {
    distanceMeters == null || !distanceMeters.isFinite() || distanceMeters < 0.0 -> 0f
    distanceMeters <= NAVIGATION_ARRIVAL_METERS -> 1f
    distanceMeters >= NAVIGATION_ENERGY_RANGE_METERS -> 0f
    else -> ((NAVIGATION_ENERGY_RANGE_METERS - distanceMeters) /
        (NAVIGATION_ENERGY_RANGE_METERS - NAVIGATION_ARRIVAL_METERS)).toFloat()
}

internal fun navigationEnergyPercent(distanceMeters: Double?): Int =
    (navigationEnergyProgress(distanceMeters) * 100f).roundToInt()

internal fun navigationDistanceLabel(distanceMeters: Double?): String = when {
    distanceMeters == null || !distanceMeters.isFinite() || distanceMeters < 0.0 -> "-- m"
    distanceMeters >= 1000.0 -> String.format(java.util.Locale.US, "%.2f km", distanceMeters / 1000.0)
    else -> "${distanceMeters.roundToInt()} m"
}

/** Deterministic debug coordinate south of the target, kept out of release navigation state. */
internal fun navigationTestCoordinate(target: GeoCoordinate, distanceMeters: Double): GeoCoordinate {
    val angularDistance = distanceMeters.coerceAtLeast(0.0) / EARTH_RADIUS_METERS
    val bearing = Math.PI
    val latitude = Math.toRadians(target.latitude)
    val longitude = Math.toRadians(target.longitude)
    val destinationLatitude = asin(
        sin(latitude) * cos(angularDistance) + cos(latitude) * sin(angularDistance) * cos(bearing),
    )
    val destinationLongitude = longitude + atan2(
        sin(bearing) * sin(angularDistance) * cos(latitude),
        cos(angularDistance) - sin(latitude) * sin(destinationLatitude),
    )
    return GeoCoordinate(Math.toDegrees(destinationLatitude), Math.toDegrees(destinationLongitude))
}

private const val EARTH_RADIUS_METERS = 6_371_000.0
