package com.comp90018.app.features.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelicArrivalPresentationTest {
    @Test
    fun fullResonanceDuringConfirmationCannotRevealBeginHunt() {
        val sample = RelicArrivalSample(true, 8.0, 5.0, 100)
        val evidence = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), sample)
        val state = deriveRelicNavigationUiState(sample, evidence)
        assertEquals(1f, state.resonanceProgress, 0f)
        val presentation = relicArrivalPresentation(state, animationComplete = true)
        assertEquals("Confirming the resonance…", presentation.stateDescription)
        assertFalse(presentation.showBeginHunt)
    }

    @Test
    fun confirmedArrivalRevealsBeginHuntAfterTheSingleTransition() {
        val (state, _) = arrived()
        assertEquals("Resonance complete", relicArrivalPresentation(state, false).stateDescription)
        assertFalse(relicArrivalPresentation(state, false).showBeginHunt)
        assertTrue(relicArrivalPresentation(state, true).showBeginHunt)
    }

    @Test
    fun lostArrivalRemovesCompleteMessageAndReadinessEvenIfAnimationWasFinished() {
        val (_, evidence) = arrived()
        listOf(
            RelicArrivalSample(true, 15.1, 5.0, 300),
            RelicArrivalSample(false, null, null, 300),
        ).forEach { sample ->
            val updated = updateRelicArrivalConfirmation(evidence, sample)
            val state = deriveRelicNavigationUiState(sample, updated)
            val presentation = relicArrivalPresentation(state, true)
            assertFalse(presentation.showBeginHunt)
            assertFalse(presentation.stateDescription == "Resonance complete")
        }
    }

    private fun arrived(): Pair<RelicNavigationUiState, RelicArrivalConfirmationState> {
        val first = RelicArrivalSample(true, 8.0, 5.0, 100)
        val pending = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), first)
        val second = first.copy(timestampNanos = 200)
        val confirmed = updateRelicArrivalConfirmation(pending, second)
        return deriveRelicNavigationUiState(second, confirmed) to confirmed
    }
}
