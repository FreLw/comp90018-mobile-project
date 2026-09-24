package com.comp90018.app.features.map

import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.sensors.location.GeoCoordinate

/** Treasure content loaded from the Firestore `treasures` collection. */
data class MapRelic(
    val id: String,
    val name: String,
    val locationName: String,
    val description: String = "",
    val buildingStory: String = "",
    val story: String = "",
    val clue: String = "",
    val coordinateSource: String = "",
    val prototypeDesign: String = "",
    val prototypeImageUrl: String = "",
    val historicalImageUrl: String = "",
    val historicalImageCredit: String = "",
    val sourceTitle: String = "",
    val sourceUrl: String = "",
    val treasureType: String = "",
    val coordinate: GeoCoordinate,
    val insideRadiusMeters: Double = 20.0,
    val nearbyRadiusMeters: Double = 120.0,
    val sortOrder: Int = Int.MAX_VALUE,
    val challengeConfig: RelicChallengeConfig? = null,
    val historicalImageResId: Int? = null,
) {
    val treasureTypeLabel: String
        get() = treasureType
            .split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
}

val sampleMapRelics: List<MapRelic> = NonFinalChallengeCatalog.relics
