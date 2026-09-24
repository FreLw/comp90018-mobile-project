package com.comp90018.app.features.map

import com.comp90018.app.contextengine.challenge.RelicChallengeConfigs
import com.comp90018.app.sensors.location.GeoCoordinate

/**
 * Development-only calibration data kept in one location. These coordinates and headings are
 * NOT final game data; every supported relic must be surveyed and calibrated on site.
 */
object NonFinalChallengeCatalog {
    private const val INSIDE_RADIUS_METERS = 20.0
    private const val NON_FINAL_UNION_HEADING_DEGREES = 0.0
    private const val NON_FINAL_WILSON_HEADING_DEGREES = 0.0
    private const val NON_FINAL_SOUTH_LAWN_HEADING_DEGREES = 0.0

    private val unionLawn = GeoCoordinate(-37.7967, 144.9606)
    private val wilsonHall = GeoCoordinate(-37.7981, 144.9612)
    private val oldQuad = GeoCoordinate(-37.798156, 144.960481)
    private val southLawn = GeoCoordinate(-37.79856, 144.96050)

    val relics = listOf(
        MapRelic(
            id = "union-lawn",
            name = "Union Lawn Lost Lake",
            locationName = "Union Lawn",
            coordinate = unionLawn,
            challengeConfig = RelicChallengeConfigs.unionLawnPhoto(
                challengeId = "union-lawn-photo",
                targetLocation = unionLawn,
                insideRadiusMeters = INSIDE_RADIUS_METERS,
                requiredHeadingDegrees = NON_FINAL_UNION_HEADING_DEGREES,
            ),
        ),
        MapRelic(
            id = "wilson",
            name = "Wilson Hall Observation",
            locationName = "Wilson Hall",
            coordinate = wilsonHall,
            challengeConfig = RelicChallengeConfigs.wilsonHallObservation(
                challengeId = "wilson-hall-observation",
                targetLocation = wilsonHall,
                insideRadiusMeters = INSIDE_RADIUS_METERS,
                requiredHeadingDegrees = NON_FINAL_WILSON_HEADING_DEGREES,
            ),
        ),
        MapRelic(
            id = "old-quad",
            name = "Old Quad Excavation",
            locationName = "Old Quadrangle",
            coordinate = oldQuad,
            challengeConfig = RelicChallengeConfigs.oldQuadExcavation(
                challengeId = "old-quad-excavation",
                targetLocation = oldQuad,
                insideRadiusMeters = INSIDE_RADIUS_METERS,
            ),
        ),
        MapRelic(
            id = "south-lawn",
            name = "South Lawn Atlas",
            locationName = "South Lawn",
            coordinate = southLawn,
            challengeConfig = RelicChallengeConfigs.southLawnViewingAngle(
                challengeId = "south-lawn-viewing-angle",
                targetLocation = southLawn,
                insideRadiusMeters = INSIDE_RADIUS_METERS,
                requiredHeadingDegrees = NON_FINAL_SOUTH_LAWN_HEADING_DEGREES,
            ),
        ),
        MapRelic(
            id = "baillieu",
            name = "Baillieu Lantern",
            locationName = "Baillieu Library",
            coordinate = GeoCoordinate(-37.7986, 144.9602),
        ),
    )
}
