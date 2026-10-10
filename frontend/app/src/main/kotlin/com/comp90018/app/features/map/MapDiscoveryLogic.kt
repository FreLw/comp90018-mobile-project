package com.comp90018.app.features.map

/*
 * Contains the map distance bands, direction captions, compass readiness, and visibility policies.
 * Pure helpers keep presentation decisions reusable and testable without rendering a map.
 */

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

/** The distance and direction conditions required before the task challenge opens. */
internal data class HuntReadiness(
    val nearTreasure: Boolean,
    val facingTreasure: Boolean,
) {
    val allReady: Boolean = nearTreasure && facingTreasure
}

internal fun evaluateHuntReadiness(
    distanceMeters: Double?,
    signedTurnDegrees: Double?,
    config: CompassGateConfig = CompassGateConfig(),
): HuntReadiness = HuntReadiness(
    nearTreasure = distanceMeters != null && distanceMeters <= config.huntReadyRadiusMeters,
    facingTreasure = signedTurnDegrees != null &&
        abs(signedTurnDegrees) <= config.compassAlignmentToleranceDegrees,
)

internal val DEFAULT_CAMPUS_CENTRE = GeoCoordinate(-37.7986, 144.9602)
internal const val RADAR_SCAN_RADIUS_METERS = 100.0
internal const val REVEAL_RADIUS_METERS = 50.0
internal const val HUNT_READY_RADIUS_METERS = 10.0

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

/** Replays use a fresh, precise fix and the same proximity radius as the compass gate. */
internal fun canReplayTreasure(
    distanceMeters: Double?,
    validity: com.comp90018.app.sensors.SensorValidity,
    preciseLocationEnabled: Boolean,
    radiusMeters: Double = HUNT_READY_RADIUS_METERS,
): Boolean = preciseLocationEnabled && validity == com.comp90018.app.sensors.SensorValidity.VALID &&
    distanceMeters != null && distanceMeters.isFinite() && distanceMeters >= 0 &&
    distanceMeters <= radiusMeters
