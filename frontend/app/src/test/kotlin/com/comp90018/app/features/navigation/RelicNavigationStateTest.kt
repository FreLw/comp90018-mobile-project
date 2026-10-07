package com.comp90018.app.features.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RelicNavigationStateTest {
    @Test
    fun nonActionableLocationCannotProvideGuidanceEvenWithNearDistance() {
        listOf(null, 0.0, 10.0, 50.0, 200.0, 300.0).forEach { distance ->
            assertEquals(RelicResonanceStage.ACQUIRING, relicResonanceStage(distance, false))
        }
    }

    @Test
    fun boundariesBelongToTheStrongerStage() {
        val cases = listOf(
            300.0 to RelicResonanceStage.DORMANT,
            250.001 to RelicResonanceStage.DORMANT,
            250.0 to RelicResonanceStage.FAINT,
            200.0 to RelicResonanceStage.FAINT,
            100.001 to RelicResonanceStage.FAINT,
            100.0 to RelicResonanceStage.DRAWN,
            50.0 to RelicResonanceStage.DRAWN,
            30.001 to RelicResonanceStage.DRAWN,
            30.0 to RelicResonanceStage.STRONG,
            15.0 to RelicResonanceStage.STRONG,
            10.001 to RelicResonanceStage.STRONG,
        )
        cases.forEach { (distance, expected) ->
            assertEquals("Distance: $distance", expected, relicResonanceStage(distance, true))
        }
    }

    @Test
    fun insideEntryRadiusDoesNotConfirmArrival() {
        listOf(10.0, 9.0, 0.0).forEach { distance ->
            assertEquals(RelicResonanceStage.STRONG, relicResonanceStage(distance, true))
        }
    }

    @Test
    fun invalidDistancesRequireAcquiringEvenWithActionableLocation() {
        listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0)
            .forEach { distance ->
                assertEquals(RelicResonanceStage.ACQUIRING, relicResonanceStage(distance, true))
            }
    }

    @Test
    fun defaultStateProvidesNoGuidanceOrHuntReadiness() {
        val state = RelicNavigationUiState()

        assertEquals(RelicResonanceStage.ACQUIRING, state.resonanceStage)
        assertNull(state.distanceMeters)
        assertEquals(0f, state.resonanceProgress, 0f)
        assertNull(state.targetBearingDegrees)
        assertEquals(NavigationDirectionHint.UNAVAILABLE, state.directionHint)
        assertNull(state.headingErrorDegrees)
        assertFalse(state.arrivalConfirmed)
        assertFalse(state.canBeginHunt)
    }

    @Test
    fun legacyEnergyAndArrivalUseSharedNavigationThresholds() {
        assertEquals(250.0, RelicNavigationConfig.resonanceRangeMeters, 0.0)
        assertEquals(100.0, RelicNavigationConfig.faintDrawnBoundaryMeters, 0.0)
        assertEquals(30.0, RelicNavigationConfig.drawnStrongBoundaryMeters, 0.0)
        assertEquals(10.0, RelicNavigationConfig.huntArrivalEntryRadiusMeters, 0.0)
        assertEquals(15.0, RelicNavigationConfig.huntArrivalExitRadiusMeters, 0.0)
        assertEquals(RelicNavigationConfig.resonanceRangeMeters, NAVIGATION_ENERGY_RANGE_METERS, 0.0)
        assertEquals(RelicNavigationConfig.huntArrivalEntryRadiusMeters, NAVIGATION_ARRIVAL_METERS, 0.0)
        assertEquals(RelicNavigationConfig.huntArrivalExitRadiusMeters, NAVIGATION_ARRIVAL_RESET_METERS, 0.0)
    }
}
