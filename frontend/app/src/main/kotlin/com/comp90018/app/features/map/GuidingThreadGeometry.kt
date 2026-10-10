package com.comp90018.app.features.map

/*
 * Calculates the geographic positions of the animated guiding-thread glints.
 * Visual geometry travels from treasure toward explorer; it does not determine arrival or task completion.
 */

import com.comp90018.app.sensors.location.GeoCoordinate
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal const val GUIDING_THREAD_GLINT_COUNT = 3

/** The relic calls to the explorer: glints travel from target toward user. */
internal fun guidingThreadGlintCentre(
    target: GeoCoordinate,
    user: GeoCoordinate,
    phase: Float,
    index: Int,
): GeoCoordinate {
    val safePhase = if (phase.isFinite()) phase.coerceIn(0f, 1f) else 0f
    val fraction = ((safePhase + index.toFloat() / GUIDING_THREAD_GLINT_COUNT) % 1f).toDouble()
    return GeoCoordinate(
        target.latitude + (user.latitude - target.latitude) * fraction,
        target.longitude + (user.longitude - target.longitude) * fraction,
    )
}

/** Four flat cardinal tips at a fixed ground distance; no marker hit targets or particle glow. */
internal fun guidingThreadGlintVertices(centre: GeoCoordinate, radiusMeters: Double): List<GeoCoordinate> {
    val latitude = Math.toRadians(centre.latitude)
    val longitude = Math.toRadians(centre.longitude)
    val angularRadius = radiusMeters / 6_371_000.0
    return List(4) { index ->
        val bearing = index * Math.PI / 2.0
        val destinationLatitude = asin(
            sin(latitude) * cos(angularRadius) + cos(latitude) * sin(angularRadius) * cos(bearing),
        )
        val destinationLongitude = longitude + atan2(
            sin(bearing) * sin(angularRadius) * cos(latitude),
            cos(angularRadius) - sin(latitude) * sin(destinationLatitude),
        )
        GeoCoordinate(Math.toDegrees(destinationLatitude),
            ((Math.toDegrees(destinationLongitude) + 540.0) % 360.0) - 180.0)
    }
}
