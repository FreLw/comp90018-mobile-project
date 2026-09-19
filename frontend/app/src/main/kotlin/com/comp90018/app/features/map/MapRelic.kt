package com.comp90018.app.features.map

import com.comp90018.app.sensors.location.GeoCoordinate

data class MapRelic(
    val id: String,
    val name: String,
    val locationName: String,
    val description: String,
    val coordinate: GeoCoordinate,
    val insideRadiusMeters: Double = 20.0,
    val nearbyRadiusMeters: Double = 120.0,
)

val sampleMapRelics = listOf(
    MapRelic(
        id = "old-quad",
        name = "Old Quad Compass",
        locationName = "Old Quadrangle",
        description = "Follow the old stone paths to uncover a compass hidden in the heart of campus.",
        coordinate = GeoCoordinate(-37.798156, 144.960481),
    ),
    MapRelic(
        id = "baillieu",
        name = "Baillieu Lantern",
        locationName = "Baillieu Library",
        description = "A library light that rewards explorers who follow knowledge through the stacks.",
        coordinate = GeoCoordinate(-37.7986, 144.9602),
    ),
    MapRelic(
        id = "south-lawn",
        name = "South Lawn Sundial",
        locationName = "South Lawn",
        description = "Trace the sun across South Lawn and reveal a fragment of campus history.",
        coordinate = GeoCoordinate(-37.79856, 144.96050),
    ),
    MapRelic(
        id = "wilson",
        name = "Wilson Hall Bell",
        locationName = "Wilson Hall",
        description = "Listen for the echoes of Wilson Hall to find this historic campus relic.",
        coordinate = GeoCoordinate(-37.7981, 144.9612),
    ),
)
