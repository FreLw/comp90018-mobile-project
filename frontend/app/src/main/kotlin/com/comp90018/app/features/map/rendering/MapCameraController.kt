package com.comp90018.app.features.map.rendering

import androidx.compose.foundation.layout.size
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val MAX_INITIAL_TREASURE_AREA_SPAN_METERS = 3_000.0

/** Fits the map viewport to catalogue coordinates and an optional explorer position. */
internal fun moveCameraToCampus(map: GoogleMap, coordinates: List<GeoCoordinate>, currentLocation: GeoCoordinate?) {
    val points = buildList {
        addAll(coordinates)
        currentLocation?.let(::add)
    }
    if (points.isEmpty()) return
    val latitudeSpan = (points.maxOfOrNull { it.latitude } ?: 0.0) - (points.minOfOrNull { it.latitude } ?: 0.0)
    val longitudeSpan = (points.maxOfOrNull { it.longitude } ?: 0.0) - (points.minOfOrNull { it.longitude } ?: 0.0)
    if (points.isNotEmpty() && latitudeSpan < 0.0001 && longitudeSpan < 0.0001) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first().toLatLng(), 17f))
        return
    }
    val bounds = LatLngBounds.builder().apply {
        coordinates.forEach { include(it.toLatLng()) }
        currentLocation?.let { include(it.toLatLng()) }
    }.build()
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
}

/** Frames the treasure area without forcing a full-campus overview. */
internal fun moveCameraToTreasureArea(
    map: GoogleMap,
    relics: List<MapRelic>,
    activeHuntTreasureId: String?,
) {
    relics.firstOrNull { it.id == activeHuntTreasureId }?.let { relic ->
        moveCameraToRelic(map, relic)
        return
    }

    if (!isCompactTreasureArea(relics)) {
        recommendedInitialRelic(relics)?.let { relic ->
            moveCameraToRelic(map, relic)
            return
        }
    }

    val points = relics.map { it.coordinate }
    if (points.isEmpty()) return
    val latitudeSpan = (points.maxOfOrNull { it.latitude } ?: 0.0) - (points.minOfOrNull { it.latitude } ?: 0.0)
    val longitudeSpan = (points.maxOfOrNull { it.longitude } ?: 0.0) - (points.minOfOrNull { it.longitude } ?: 0.0)
    if (points.isNotEmpty() && latitudeSpan < 0.0001 && longitudeSpan < 0.0001) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first().toLatLng(), 17f))
        return
    }
    val bounds = LatLngBounds.builder().apply {
        relics.forEach { include(it.coordinate.toLatLng()) }
    }.build()
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
}

internal fun recommendedInitialRelic(relics: List<MapRelic>): MapRelic? =
    relics.minWithOrNull(compareBy<MapRelic> { it.sortOrder }.thenBy { it.name })

internal fun isCompactTreasureArea(
    relics: List<MapRelic>,
    maxSpanMeters: Double = MAX_INITIAL_TREASURE_AREA_SPAN_METERS,
): Boolean {
    if (relics.size < 2) return true
    return relics.indices.all { firstIndex ->
        ((firstIndex + 1) until relics.size).all { secondIndex ->
            runCatching {
                LocationCalculator.distanceMeters(
                    relics[firstIndex].coordinate,
                    relics[secondIndex].coordinate,
                ) <= maxSpanMeters
            }.getOrDefault(false)
        }
    }
}

internal fun moveCameraToRelic(map: GoogleMap, selectedRelic: MapRelic) {
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedRelic.coordinate.toLatLng(), 17f))
}

internal fun moveCameraToHuntView(map: GoogleMap, currentLocation: GeoCoordinate, headingDegrees: Float) {
    val cameraTarget = coordinateAhead(currentLocation, HUNT_CAMERA_LEAD_METERS, headingDegrees.toDouble())
    map.moveCamera(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                .target(cameraTarget.toLatLng())
                .zoom(HUNT_CAMERA_ZOOM)
                .bearing(headingDegrees)
                .tilt(HUNT_CAMERA_TILT)
                .build(),
        ),
    )
}

private fun coordinateAhead(origin: GeoCoordinate, distanceMeters: Double, bearingDegrees: Double): GeoCoordinate {
    val angularDistance = distanceMeters / EARTH_RADIUS_METERS
    val bearing = Math.toRadians(bearingDegrees)
    val latitude = Math.toRadians(origin.latitude)
    val longitude = Math.toRadians(origin.longitude)
    val destinationLatitude = asin(
        sin(latitude) * cos(angularDistance) + cos(latitude) * sin(angularDistance) * cos(bearing),
    )
    val destinationLongitude = longitude + atan2(
        sin(bearing) * sin(angularDistance) * cos(latitude),
        cos(angularDistance) - sin(latitude) * sin(destinationLatitude),
    )
    return GeoCoordinate(Math.toDegrees(destinationLatitude), Math.toDegrees(destinationLongitude))
}

internal fun coordinateAtDistance(target: GeoCoordinate, distanceMeters: Double): GeoCoordinate =
    coordinateAhead(target, distanceMeters, 180.0)

internal fun GeoCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)

private const val EARTH_RADIUS_METERS = 6_371_000.0

private const val HUNT_CAMERA_LEAD_METERS = 80.0

private const val HUNT_CAMERA_ZOOM = 18.5f

private const val HUNT_CAMERA_TILT = 45f
