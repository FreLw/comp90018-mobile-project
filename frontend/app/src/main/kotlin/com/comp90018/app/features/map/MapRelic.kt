package com.comp90018.app.features.map

import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.sensors.location.GeoCoordinate

data class MapRelic(
    val id: String,
    val name: String,
    val locationName: String,
    val coordinate: GeoCoordinate,
    val insideRadiusMeters: Double = 20.0,
    val nearbyRadiusMeters: Double = 120.0,
    val challengeConfig: RelicChallengeConfig? = null,
    val historicalImageResId: Int? = null,
)

val sampleMapRelics: List<MapRelic> = NonFinalChallengeCatalog.relics
