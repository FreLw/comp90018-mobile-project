package com.comp90018.app.features.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationDirectionGuidanceTest {
    @Test
    fun signedErrorsCrossNorthAndHandleOppositeBearing() {
        assertDirection(350.0, 10.0, 20.0, NavigationDirectionHint.TURN_RIGHT)
        assertDirection(10.0, 350.0, -20.0, NavigationDirectionHint.TURN_LEFT)
        assertDirection(0.0, 180.0, -180.0, NavigationDirectionHint.TURN_LEFT)
    }

    @Test
    fun alignmentIncludesNorthAndBothToleranceBoundaries() {
        assertDirection(0.0, 0.0, 0.0, NavigationDirectionHint.ALIGNED)
        assertDirection(0.0, 12.0, 12.0, NavigationDirectionHint.ALIGNED)
        assertDirection(0.0, 348.0, -12.0, NavigationDirectionHint.ALIGNED)
        assertDirection(0.0, 12.01, 12.01, NavigationDirectionHint.TURN_RIGHT)
        assertDirection(0.0, 347.99, -12.01, NavigationDirectionHint.TURN_LEFT)
    }

    @Test
    fun missingGpsBearingOrHeadingCannotProvideDirection() {
        val sample = RelicArrivalSample(true, 50.0, 5.0, 100)
        listOf(
            deriveRelicNavigationUiState(sample.copy(hasActionableLocation = false), RelicArrivalConfirmationState(), 90.0, 0.0),
            deriveRelicNavigationUiState(sample, RelicArrivalConfirmationState(), null, 0.0),
            deriveRelicNavigationUiState(sample, RelicArrivalConfirmationState(), 90.0, null),
            deriveRelicNavigationUiState(sample, RelicArrivalConfirmationState(), Double.NaN, 0.0),
            deriveRelicNavigationUiState(sample, RelicArrivalConfirmationState(), 90.0, Double.POSITIVE_INFINITY),
        ).forEach { state ->
            assertEquals(NavigationDirectionHint.UNAVAILABLE, state.directionHint)
            assertNull(state.headingErrorDegrees)
        }
    }

    @Test
    fun declinationIsAddedAndNormalizedBeforeComparison() {
        assertEquals(10.0, requireNotNull(navigationTrueHeading(350.0, 20.0)), 0.0)
        assertEquals(350.0, requireNotNull(navigationTrueHeading(10.0, -20.0)), 0.0)
        assertEquals(0.0, requireNotNull(navigationTrueHeading(350.0, 10.0)), 0.0)
        assertDirection(requireNotNull(navigationTrueHeading(350.0, 20.0)), 10.0, 0.0, NavigationDirectionHint.ALIGNED)
        assertNull(navigationTrueHeading(null, 20.0))
        assertNull(navigationTrueHeading(20.0, null))
        assertNull(navigationTrueHeading(Double.NaN, 20.0))
        assertNull(navigationTrueHeading(20.0, Double.NaN))
    }

    @Test
    fun simulatedHeadingIsAlreadyTrueNorthAndOverridesDeclinationAndRealHeading() {
        val heading = navigationTrueHeading(90.0, 20.0, 350.0)
        assertDirection(requireNotNull(heading), 10.0, 20.0, NavigationDirectionHint.TURN_RIGHT)
        assertEquals(0.0, requireNotNull(navigationTrueHeading(null, null, 360.0)), 0.0)
        assertEquals(110.0, requireNotNull(navigationTrueHeading(90.0, 20.0, null)), 0.0)
    }

    @Test
    fun directionCannotGatePendingOrConfirmedArrival() {
        val firstSample = RelicArrivalSample(true, 8.0, 5.0, 100)
        val first = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), firstSample)
        val secondSample = firstSample.copy(timestampNanos = 200)
        val second = updateRelicArrivalConfirmation(first, secondSample)
        listOf<Double?>(0.0, 90.0, 270.0, null).forEach { heading ->
            val pending = deriveRelicNavigationUiState(firstSample, first, 0.0, heading)
            assertEquals(RelicResonanceStage.CONFIRMING, pending.resonanceStage)
            assertFalse(pending.arrivalConfirmed)
            assertFalse(pending.canBeginHunt)
            val arrived = deriveRelicNavigationUiState(secondSample, second, 0.0, heading)
            assertEquals(RelicResonanceStage.ARRIVED, arrived.resonanceStage)
            assertTrue(arrived.arrivalConfirmed)
            assertTrue(arrived.canBeginHunt)
        }
    }

    private fun assertDirection(heading: Double, target: Double, error: Double, hint: NavigationDirectionHint) {
        val state = deriveRelicNavigationUiState(
            RelicArrivalSample(true, 50.0, 5.0, 100), RelicArrivalConfirmationState(), target, heading,
        )
        assertEquals(error, requireNotNull(state.headingErrorDegrees), 0.000001)
        assertEquals(hint, state.directionHint)
    }
}
