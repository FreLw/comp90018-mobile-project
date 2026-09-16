package com.comp90018.app.features.map

import com.comp90018.app.sensors.location.GeoCoordinate

data class MapRelic(
    val id: String,
    val name: String,
    val locationName: String,
    val coordinate: GeoCoordinate,
    val insideRadiusMeters: Double = 20.0,
    val nearbyRadiusMeters: Double = 120.0,
)

val sampleMapRelics = listOf(
    MapRelic(
        id = "old-quad",
        name = "Old Quad Compass",
        locationName = "Old Quadrangle",
        coordinate = GeoCoordinate(-37.798156, 144.960481),
    ),
    MapRelic(
        id = "baillieu",
        name = "Baillieu Lantern",
        locationName = "Baillieu Library",
        coordinate = GeoCoordinate(-37.7986, 144.9602),
    ),
    MapRelic(
        id = "south-lawn",
        name = "South Lawn Sundial",
        locationName = "South Lawn",
        coordinate = GeoCoordinate(-37.79856, 144.96050),
    ),
    MapRelic(
        id = "wilson",
        name = "Wilson Hall Bell",
        locationName = "Wilson Hall",
        coordinate = GeoCoordinate(-37.7981, 144.9612),
    ),
)
