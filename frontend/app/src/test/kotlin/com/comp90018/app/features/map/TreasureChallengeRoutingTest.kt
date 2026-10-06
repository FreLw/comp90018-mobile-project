package com.comp90018.app.features.map

import com.comp90018.app.contextengine.challenge.*
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.*
import org.junit.Test

class TreasureChallengeRoutingTest {
    private val target = GeoCoordinate(0.0, 0.0)
    private val configs = listOf(
        "union_lawn_lost_lake" to RelicChallengeConfigs.unionLawnPhoto("union", target, 25.0, 288.0),
        "wilson_hall_rosette" to RelicChallengeConfigs.wilsonHallObservation("wilson", target, 20.0, 197.0),
        "old_quad_fossil" to RelicChallengeConfigs.oldQuadExcavation("quad", target, 18.0),
        "south_lawn_atlas" to RelicChallengeConfigs.southLawnViewingAngle("atlas", target, 15.0, 185.0),
        "system_garden_glasshouse" to RelicChallengeConfigs.systemGardenGlasshouse("garden", target, 22.0),
        "grainger_tone_tool" to RelicChallengeConfigs.graingerMuseumToneTool("tone", target, 18.0),
    )

    @Test fun allSixIdentitiesResolveTheirDistinctConfiguredTaskAndRadius() {
        assertEquals(RelicChallengeType.entries.toSet(), configs.map { it.second.type }.toSet())
        configs.forEach { (id, config) ->
            assertEquals(config, relic(id, config).validatedChallengeConfig())
            assertTrue(config.hasRequiredTaskRules())
            assertNull(relic(id, null).validatedChallengeConfig())
            val other = configs.first { it.second.type != config.type }.second
            assertNull(relic(id, other).validatedChallengeConfig())
        }
        assertEquals(listOf(25.0, 20.0, 18.0, 15.0, 22.0, 18.0), configs.map { it.second.insideRadiusMeters })
    }

    @Test fun weakenedPhotoSoundAndHoldRulesAreRejected() {
        assertNull(relic(configs[0].first, configs[0].second.copy(requiredHeadingDegrees = null)).validatedChallengeConfig())
        assertNull(relic(configs[1].first, configs[1].second.copy(holdDurationNanos = 0L)).validatedChallengeConfig())
        assertNull(relic(configs[2].first, configs[2].second.copy(requiresHorizontal = false)).validatedChallengeConfig())
        assertNull(relic(configs[3].first, configs[3].second.copy(requiresStability = false)).validatedChallengeConfig())
        assertNull(relic(configs[4].first, configs[4].second.copy(photoActionRequired = false)).validatedChallengeConfig())
        assertNull(relic(configs[5].first, configs[5].second.copy(requiresSound = false)).validatedChallengeConfig())
    }

    @Test fun multiplayerOwnerUsesChallengeAndPendingMemberRetainsQuiz() {
        assertEquals(HuntEntry.CHALLENGE, huntEntry(false, false))
        assertEquals(HuntEntry.CHALLENGE, huntEntry(true, true))
        assertEquals(HuntEntry.MEMBER_QUIZ, huntEntry(true, false))
    }

    @Test fun roomOwnerKeepsAllSixChallengeRoutesUntilTheirTaskIsComplete() {
        configs.forEach { (id, config) ->
            assertEquals(HuntEntry.CHALLENGE, huntEntry(true, true, true, false))
            assertEquals(config, relic(id, config).validatedChallengeConfig())
        }
        assertEquals(HuntEntry.MEMBER_QUIZ, huntEntry(true, true, false, false))
        assertEquals(HuntEntry.CHALLENGE, huntEntry(false, false, false, false))
    }

    @Test fun completedOwnerWaitsThenClaimsAndNeverReopensTheTaskAfterClaiming() {
        val members = listOf("owner", "member")
        fun entry(completed: List<String>, claimed: List<String>, user: String) = huntEntry(
            true,
            TeamHuntTaskEligibility.isPendingFor(true, members, completed, user),
            user == "owner",
            TeamHuntTaskEligibility.canClaim(true, members, completed, claimed, user),
        )
        assertEquals(HuntEntry.WAITING, entry(listOf("owner"), emptyList(), "owner"))
        assertEquals(HuntEntry.MEMBER_QUIZ, entry(listOf("owner"), emptyList(), "member"))
        assertEquals(HuntEntry.CLAIM, entry(members, emptyList(), "owner"))
        assertEquals(HuntEntry.WAITING, entry(members, listOf("owner"), "owner"))
        assertEquals(HuntEntry.CLAIM, entry(members, listOf("owner"), "member"))
        assertEquals(HuntEntry.WAITING, entry(listOf("member"), emptyList(), "member"))
        assertEquals(HuntEntry.CHALLENGE, entry(listOf("member"), emptyList(), "owner"))
    }

    private fun relic(id: String, config: RelicChallengeConfig?) =
        MapRelic(id, id, "Campus", coordinate = target, challengeConfig = config)
}
