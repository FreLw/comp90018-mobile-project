package com.comp90018.app.features.map

import com.comp90018.app.contextengine.challenge.RelicChallengeConfigs
import com.comp90018.app.sensors.location.GeoCoordinate

/** Local UI fixtures: all six destinations remain available without the remote catalogue. */
internal fun debugChallengeRelics(): List<MapRelic> {
    val union = GeoCoordinate(-37.7971, 144.96189)
    val wilson = GeoCoordinate(-37.79804, 144.9613)
    val quad = GeoCoordinate(-37.79759, 144.96097)
    val south = GeoCoordinate(-37.7988889, 144.9597222)
    val garden = GeoCoordinate(-37.796765, 144.959095)
    val grainger = GeoCoordinate(-37.7973, 144.95831)
    return listOf(
        MapRelic(
            id = "union_lawn_lost_lake", name = "The Lost Lake Photograph", locationName = "Union Lawn",
            coordinate = union, insideRadiusMeters = 25.0,
            challengeConfig = RelicChallengeConfigs.unionLawnPhoto("debug-union-lawn-photo", union, 25.0, 288.0),
        ),
        MapRelic(
            id = "wilson_hall_rosette", name = "Stone Rosette", locationName = "Wilson Hall",
            coordinate = wilson, insideRadiusMeters = 20.0,
            challengeConfig = RelicChallengeConfigs.wilsonHallObservation("debug-wilson-hall-observation", wilson, 20.0, 197.0),
        ),
        MapRelic(
            id = "old_quad_fossil", name = "Ancient Fern Fossil", locationName = "Old Quad",
            coordinate = quad, insideRadiusMeters = 18.0,
            challengeConfig = RelicChallengeConfigs.oldQuadExcavation("debug-old-quad-excavation", quad, 18.0),
        ),
        MapRelic(
            id = "south_lawn_atlas", name = "Atlas", locationName = "South Lawn",
            coordinate = south, insideRadiusMeters = 15.0,
            challengeConfig = RelicChallengeConfigs.southLawnViewingAngle("debug-south-lawn-viewing-angle", south, 15.0, 88.0),
        ),
        MapRelic(
            id = "system_garden_glasshouse", name = "The Lost Glasshouse", locationName = "System Garden",
            coordinate = garden, insideRadiusMeters = 22.0,
            challengeConfig = RelicChallengeConfigs.systemGardenGlasshouse("debug-system-garden-glasshouse", garden, 22.0),
        ),
        MapRelic(
            id = "grainger_tone_tool", name = "Engraved Tone Tool", locationName = "Grainger Museum",
            coordinate = grainger, insideRadiusMeters = 18.0,
            challengeConfig = RelicChallengeConfigs.graingerMuseumToneTool("debug-grainger-museum-tone-tool", grainger, 18.0),
        ),
    )
}
