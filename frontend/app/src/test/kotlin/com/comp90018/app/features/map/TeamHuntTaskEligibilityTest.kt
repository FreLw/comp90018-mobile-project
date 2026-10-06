package com.comp90018.app.features.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TeamHuntTaskEligibilityTest {
    private val members = listOf("owner", "member")

    @Test fun claimRequiresEveryMemberAndIsTrackedSeparatelyForEachExplorer() {
        assertFalse(TeamHuntTaskEligibility.canClaim(true, members, listOf("owner"), emptyList(), "owner"))
        assertTrue(TeamHuntTaskEligibility.canClaim(true, members, members, emptyList(), "owner"))
        assertFalse(TeamHuntTaskEligibility.canClaim(true, members, members, listOf("owner"), "owner"))
        assertTrue(TeamHuntTaskEligibility.canClaim(true, members, members, listOf("owner"), "member"))
        assertFalse(TeamHuntTaskEligibility.canClaim(false, members, members, emptyList(), "owner"))
        assertFalse(TeamHuntTaskEligibility.canClaim(true, members, members, emptyList(), "outsider"))
        assertFalse(TeamHuntTaskEligibility.canClaim(true, listOf("owner"), listOf("owner"), emptyList(), "owner"))
    }

    @Test fun sameTreasureInANewSessionAndDifferentUsersHaveIndependentPrompts() {
        val first = TeamHuntTaskEligibility.promptKey("owner", "run-one")
        assertNotEquals(first, TeamHuntTaskEligibility.promptKey("owner", "run-two"))
        assertNotEquals(first, TeamHuntTaskEligibility.promptKey("member", "run-one"))
    }

    @Test fun incompleteMemberMustCompleteTheirTask() {
        assertTrue(TeamHuntTaskEligibility.isPendingFor(true, members, listOf("owner"), "member"))
    }

    @Test fun completedMemberIsNotPromptedAgain() {
        assertFalse(TeamHuntTaskEligibility.isPendingFor(true, members, listOf("owner"), "owner"))
    }

    @Test fun requiresAnActiveTeamHuntAndAMemberIdentity() {
        assertFalse(TeamHuntTaskEligibility.isPendingFor(false, members, emptyList(), "owner"))
        assertFalse(TeamHuntTaskEligibility.isPendingFor(true, listOf("owner"), emptyList(), "owner"))
        assertFalse(TeamHuntTaskEligibility.isPendingFor(true, members, emptyList(), ""))
    }
    @Test fun largerRoomsAllowEachIncompleteExplorerToParticipate() {
        for (size in 3..4) {
            val team = (1..size).map { "explorer$it" }
            assertTrue(TeamHuntTaskEligibility.isPendingFor(true, team, team.dropLast(1), team.last()))
            assertFalse(TeamHuntTaskEligibility.isPendingFor(true, team, team, team.last()))
        }
    }
}
