package com.comp90018.app.features.map

import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import kotlin.math.abs

/** Distance bands used by the map before the full sensor hunt begins. */
internal enum class RadarSignalRange {
    OUT_OF_RANGE,
    NEARBY_HIDDEN,
    REVEALED,
    HUNT_READY,
}

internal fun radarSignalForDistance(distanceMeters: Double): RadarSignalRange = when {
    distanceMeters <= HUNT_READY_RADIUS_METERS -> RadarSignalRange.HUNT_READY
    distanceMeters <= REVEAL_RADIUS_METERS -> RadarSignalRange.REVEALED
    distanceMeters <= RADAR_SCAN_RADIUS_METERS -> RadarSignalRange.NEARBY_HIDDEN
    else -> RadarSignalRange.OUT_OF_RANGE
}

internal fun treasureProximityMessage(
    distanceMeters: Double,
    currentLocation: GeoCoordinate,
    treasureLocation: GeoCoordinate,
): String = when (radarSignalForDistance(distanceMeters)) {
    RadarSignalRange.HUNT_READY ->
        "The treasure is right before your eyes — steady your compass and begin the hunt!"
    RadarSignalRange.REVEALED -> {
        val direction = bearingToCompassDirection(
            LocationCalculator.bearingDegrees(currentLocation, treasureLocation),
        )
        "Hot trail! A treasure is within 50 m, lurking to the $direction."
    }
    RadarSignalRange.NEARBY_HIDDEN ->
        "Your relic-sense is tingling… a treasure is hiding nearby!"
    RadarSignalRange.OUT_OF_RANGE ->
        "The trail has gone quiet — no treasure within 100 m. Follow the hint and venture closer!"
}

internal fun bearingToCompassDirection(bearingDegrees: Double): String {
    val directions = arrayOf(
        "north", "north-east", "east", "south-east",
        "south", "south-west", "west", "north-west",
    )
    val normalized = LocationCalculator.normalizeDegrees(bearingDegrees)
    return directions[((normalized + 22.5) / 45.0).toInt() % directions.size]
}

internal fun signedBearingDifference(
    targetBearingDegrees: Double,
    deviceHeadingDegrees: Double,
): Double = ((targetBearingDegrees - deviceHeadingDegrees + 540.0) % 360.0) - 180.0

/** The three independent sensor conditions required before digging is enabled. */
internal data class HuntReadiness(
    val nearTreasure: Boolean,
    val facingTreasure: Boolean,
    val phoneHorizontal: Boolean,
) {
    val allReady: Boolean = nearTreasure && facingTreasure && phoneHorizontal
}

internal fun evaluateHuntReadiness(
    distanceMeters: Double?,
    signedTurnDegrees: Double?,
    tiltDegrees: Float,
): HuntReadiness = HuntReadiness(
    nearTreasure = distanceMeters != null && distanceMeters <= HUNT_READY_RADIUS_METERS,
    facingTreasure = signedTurnDegrees != null &&
        abs(signedTurnDegrees) <= COMPASS_ALIGNMENT_TOLERANCE_DEGREES,
    phoneHorizontal = tiltDegrees <= HORIZONTAL_TOLERANCE_DEGREES,
)

internal val DEFAULT_CAMPUS_CENTRE = GeoCoordinate(-37.7986, 144.9602)
internal const val RADAR_SCAN_RADIUS_METERS = 100.0
internal const val REVEAL_RADIUS_METERS = 50.0
internal const val HUNT_READY_RADIUS_METERS = 10.0
internal const val COMPASS_ALIGNMENT_TOLERANCE_DEGREES = 15.0
internal const val HORIZONTAL_TOLERANCE_DEGREES = 12f

/** Overview shows the catalogue; proximity mode keeps recovered and returning relics visible. */
internal fun visibleMapRelics(
    relics: List<MapRelic>,
    coordinate: GeoCoordinate,
    radiusMeters: Double,
    discoveredIds: Set<String> = emptySet(),
    returningRelicId: String? = null,
    showAllRelics: Boolean = false,
): List<MapRelic> = relics.filter {
    showAllRelics || it.id in discoveredIds || it.id == returningRelicId ||
        LocationCalculator.distanceMeters(coordinate, it.coordinate) <= radiusMeters
}
