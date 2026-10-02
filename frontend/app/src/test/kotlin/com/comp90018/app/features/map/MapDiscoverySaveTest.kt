package com.comp90018.app.features.map

import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Test

class MapDiscoverySaveTest {
    @Test fun persistsRelicIdRatherThanChallengeId() {
        val relic = MapRelic(
            id = "union_lawn_lost_lake",
            name = "Union Lawn",
            locationName = "Union Lawn",
            coordinate = GeoCoordinate(-37.8, 144.96),
            challengeConfig = RelicChallengeConfig(
                challengeId = "union-lawn-photo",
                type = RelicChallengeType.UNION_LAWN_PHOTO,
                targetLocation = GeoCoordinate(-37.8, 144.96),
                insideRadiusMeters = 25.0,
                requiredHeadingDegrees = null,
                headingToleranceDegrees = 12.0,
                requiresStationary = false,
                requiresStability = false,
                requiresRotationStill = false,
                requiresHorizontal = false,
                holdDurationNanos = 800_000_000L,
                photoActionRequired = true,
            ),
        )
        var savedId: String? = null
        saveRelicDiscovery(relic, { id, complete ->
            savedId = id
            complete(null)
        }, {})
        assertEquals("union_lawn_lost_lake", savedId)
        assertEquals("union-lawn-photo", relic.challengeConfig?.challengeId)
    }
}
