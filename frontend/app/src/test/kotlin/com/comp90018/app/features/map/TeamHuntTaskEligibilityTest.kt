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

    @Test fun requiresAnActiveTwoPersonHuntAndAMemberIdentity() {
        assertFalse(TeamHuntTaskEligibility.isPendingFor(false, members, emptyList(), "owner"))
        assertFalse(TeamHuntTaskEligibility.isPendingFor(true, listOf("owner"), emptyList(), "owner"))
        assertFalse(TeamHuntTaskEligibility.isPendingFor(true, members, emptyList(), ""))
    }
}
