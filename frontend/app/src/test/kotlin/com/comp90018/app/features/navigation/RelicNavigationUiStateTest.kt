package com.comp90018.app.features.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RelicNavigationUiStateTest {
    @Test
    fun noActionableLocationCannotOfferGuidanceOrBeginHunt() {
        val sample = fix(300).copy(hasActionableLocation = false)
        val state = uiState(sample, arrived())

        assertEquals(RelicResonanceStage.ACQUIRING, state.resonanceStage)
        assertNull(state.distanceMeters)
        assertNull(state.targetBearingDegrees)
        assertEquals(0f, state.resonanceProgress, 0f)
        assertFalse(state.arrivalConfirmed)
        assertFalse(state.canBeginHunt)
    }

    @Test
    fun ordinaryDistancesUseTheExistingClassifierAndProgressFormula() {
        listOf(
            300.0 to RelicResonanceStage.DORMANT,
            250.0 to RelicResonanceStage.FAINT,
            200.0 to RelicResonanceStage.FAINT,
            100.0 to RelicResonanceStage.DRAWN,
            50.0 to RelicResonanceStage.DRAWN,
            30.0 to RelicResonanceStage.STRONG,
            15.0 to RelicResonanceStage.STRONG,
        ).forEach { (distance, expected) ->
            val state = uiState(fix(100, distance))
            assertEquals(expected, state.resonanceStage)
            assertEquals(distance, requireNotNull(state.distanceMeters), 0.0)
            assertEquals(navigationEnergyProgress(distance), state.resonanceProgress, 0f)
            assertFalse(state.arrivalConfirmed)
            assertFalse(state.canBeginHunt)
        }
    }

    @Test
    fun firstInsideFixCanFillBatteryWithoutPermittingHunt() {
        val state = uiState(fix(100))

        assertEquals(RelicResonanceStage.CONFIRMING, state.resonanceStage)
        assertEquals(1f, state.resonanceProgress, 0f)
        assertFalse(state.arrivalConfirmed)
        assertFalse(state.canBeginHunt)
    }

    @Test
    fun secondNewerInsideFixConfirmsArrivalAndPermitsHunt() {
        val first = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), fix(100))
        val state = uiState(fix(200), first)

        assertEquals(RelicResonanceStage.ARRIVED, state.resonanceStage)
        assertTrue(state.arrivalConfirmed)
        assertTrue(state.canBeginHunt)
    }

    @Test
    fun repeatedFixCannotConfirmArrivalEvenIfBearingChanges() {
        val sample = fix(100)
        var confirmation = RelicArrivalConfirmationState()
        repeat(10) { index ->
            confirmation = updateRelicArrivalConfirmation(confirmation, sample)
            val state = deriveRelicNavigationUiState(sample, confirmation, index.toDouble())
            assertEquals(RelicResonanceStage.CONFIRMING, state.resonanceStage)
            assertFalse(state.canBeginHunt)
        }
    }

    @Test
    fun hysteresisRetainsArrivalAndFullBatteryUntilExitRadiusIsExceeded() {
        var confirmation = arrived()
        listOf(12.0, 15.0).forEachIndexed { index, distance ->
            val sample = fix(300L + index, distance)
            confirmation = updateRelicArrivalConfirmation(confirmation, sample)
            val state = deriveRelicNavigationUiState(sample, confirmation)
            assertEquals(RelicResonanceStage.ARRIVED, state.resonanceStage)
            assertTrue(state.canBeginHunt)
            assertEquals(1f, state.resonanceProgress, 0f)
        }
        val outside = uiState(fix(400, 15.1), confirmation)
        assertEquals(RelicResonanceStage.STRONG, outside.resonanceStage)
        assertFalse(outside.arrivalConfirmed)
        assertFalse(outside.canBeginHunt)
        assertEquals(navigationEnergyProgress(15.1), outside.resonanceProgress, 0f)
    }

    @Test
    fun invalidOrInaccurateEvidenceImmediatelyRemovesHuntReadiness() {
        val samples = listOf(
            fix(300, null),
            fix(300, Double.NaN),
            fix(300, Double.POSITIVE_INFINITY),
            fix(300, -1.0),
            fix(300).copy(accuracyMeters = 20.0),
            fix(300).copy(timestampNanos = null),
        )
        samples.forEach { sample ->
            val state = uiState(sample, arrived())
            assertFalse(state.arrivalConfirmed)
            assertFalse(state.canBeginHunt)
            assertFalse(state.resonanceStage == RelicResonanceStage.ARRIVED)
        }
    }

    @Test
    fun bearingIsPreservedWhileDirectionIntegrationRemainsUnavailable() {
        val state = uiState(fix(100, 50.0))
        assertEquals(90.0, requireNotNull(state.targetBearingDegrees), 0.0)
        assertEquals(NavigationDirectionHint.UNAVAILABLE, state.directionHint)
        assertNull(state.headingErrorDegrees)
        listOf(Double.NaN, Double.POSITIVE_INFINITY).forEach { bearing ->
            assertNull(deriveRelicNavigationUiState(fix(100), RelicArrivalConfirmationState(), bearing).targetBearingDegrees)
        }
    }

    @Test
    fun changedDebugDistancesProduceDistinctFixesWithoutRealGpsMetadata() {
        val first = RelicNavigationSimulation().withDistance(8.0)
        assertEquals(1L, first.fixSequence)
        assertTrue(first.arrivalSample().hasActionableLocation)
        assertNull(first.arrivalSample().accuracyMeters)
        val pending = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), first.arrivalSample())
        assertFalse(pending.arrivalConfirmed)
        val second = first.withDistance(7.0)
        assertEquals(2L, second.fixSequence)
        assertTrue(uiState(second.arrivalSample(), pending).canBeginHunt)
    }

    @Test
    fun unchangedDebugDistanceCannotGenerateASecondFix() {
        val first = RelicNavigationSimulation().withDistance(8.0)
        val pending = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), first.arrivalSample())
        repeat(10) {
            assertEquals(first, first.withDistance(8.0))
            assertFalse(uiState(first.withDistance(8.0).arrivalSample(), pending).canBeginHunt)
        }
    }

    @Test
    fun newEvidenceStreamRequiresFreshConfirmationAndRestartsDebugSequence() {
        val realArrival = arrived()
        assertTrue(realArrival.arrivalConfirmed)
        val simulation = RelicNavigationSimulation().withDistance(8.0)
        // Matches remember(relic.id, isSimulating): mode switches start with empty evidence.
        assertFalse(uiState(simulation.arrivalSample(), RelicArrivalConfirmationState()).canBeginHunt)
        val disabled = simulation.withDistance(null)
        assertEquals(RelicNavigationSimulation(), disabled)
        assertEquals(1L, disabled.withDistance(8.0).fixSequence)
        assertFalse(uiState(fix(500), RelicArrivalConfirmationState()).canBeginHunt)
    }

    private fun uiState(
        sample: RelicArrivalSample,
        previous: RelicArrivalConfirmationState = RelicArrivalConfirmationState(),
    ): RelicNavigationUiState = deriveRelicNavigationUiState(
        sample,
        updateRelicArrivalConfirmation(previous, sample),
        targetBearingDegrees = 90.0,
    )

    private fun arrived(): RelicArrivalConfirmationState {
        val first = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), fix(100))
        return updateRelicArrivalConfirmation(first, fix(200))
    }

    private fun fix(timestamp: Long, distance: Double? = 8.0) =
        RelicArrivalSample(true, distance, 5.0, timestamp)
}
