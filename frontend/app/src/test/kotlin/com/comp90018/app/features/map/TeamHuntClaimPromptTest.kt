package com.comp90018.app.features.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamHuntClaimPromptTest {
    @Test fun reopeningTheMapDoesNotRepeatAPromptButANewRunOrAnotherMemberCanSeeIt() {
        val seen = mutableSetOf<String>()
        fun prompt(user: String, run: String): TeamHuntClaimPrompt {
            val key = TeamHuntTaskEligibility.promptKey(user, run)
            return TeamHuntClaimPrompt({ key in seen }, { seen.add(key); Unit })
        }
        val owner = prompt("owner", "run-one")
        assertFalse(owner.shouldShow(false))
        assertTrue(owner.shouldShow(true))
        // An effect behind the task screen must not acknowledge a prompt that was never shown.
        assertTrue(prompt("owner", "run-one").shouldShow(true))
        owner.onShown()
        assertFalse(prompt("owner", "run-one").shouldShow(true))
        assertTrue(prompt("member", "run-one").shouldShow(true))
        assertTrue(prompt("owner", "run-two").shouldShow(true))
    }
}
