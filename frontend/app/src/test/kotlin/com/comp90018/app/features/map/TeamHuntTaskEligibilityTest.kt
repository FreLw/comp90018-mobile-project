package com.comp90018.app.features.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamHuntTaskEligibilityTest {
    private val members = listOf("owner", "member")

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
