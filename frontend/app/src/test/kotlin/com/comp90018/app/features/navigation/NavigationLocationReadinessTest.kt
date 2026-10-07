package com.comp90018.app.features.navigation

import com.comp90018.app.features.map.LocationActionPolicy
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationAvailabilityState
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.location.LocationPermissionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationLocationReadinessTest {
    private val live = LocationOutput(currentLocation = GeoCoordinate(-37.7986, 144.9602),
        validity = SensorValidity.VALID, permission = LocationPermissionState.PRECISE,
        availability = LocationAvailabilityState.AVAILABLE)

    @Test
    fun unknownLocationIsAcquiringWithoutValidResonanceOrThread() {
        val location = LocationOutput()
        val sample = sample(location, 100)
        val state = state(location, sample, RelicArrivalConfirmationState())
        assertEquals(NavigationLocationReadiness.ACQUIRING, state.locationReadiness)
        assertEquals(RelicResonanceStage.ACQUIRING, state.resonanceStage)
        assertNull(state.distanceMeters)
        assertEquals(NavigationDirectionHint.UNAVAILABLE, state.directionHint)
        assertFalse(navigationGuidingThreadAvailable(state))
        assertEquals("Seeking your position…", relicArrivalPresentation(state, true).stateDescription)
    }

    @Test
    fun readinessUsesKnownPermissionProviderAndValidityReasons() {
        assertEquals(NavigationLocationReadiness.ACCESS_REQUIRED,
            navigationLocationReadiness(live.copy(permission = LocationPermissionState.DENIED)))
        assertEquals(NavigationLocationReadiness.UNAVAILABLE,
            navigationLocationReadiness(live.copy(currentLocation = null, availability = LocationAvailabilityState.EXPIRED)))
        assertEquals(NavigationLocationReadiness.UNAVAILABLE,
            navigationLocationReadiness(live.copy(currentLocation = null, availability = LocationAvailabilityState.UNAVAILABLE)))
        assertEquals(NavigationLocationReadiness.IMPROVING_SIGNAL,
            navigationLocationReadiness(live.copy(currentLocation = null, validity = SensorValidity.UNRELIABLE)))
        assertEquals(NavigationLocationReadiness.IMPROVING_SIGNAL,
            navigationLocationReadiness(live.copy(currentLocation = null, availability = LocationAvailabilityState.RECOVERING)))
        assertEquals(NavigationLocationReadiness.ACQUIRING,
            navigationLocationReadiness(live.copy(currentLocation = null, validity = SensorValidity.WARMING_UP)))
    }

    @Test
    fun liveLocationResumesGuidanceAndThread() {
        val state = state(live, sample(live, 100), RelicArrivalConfirmationState())
        assertEquals(NavigationLocationReadiness.READY, state.locationReadiness)
        assertEquals(RelicResonanceStage.CONFIRMING, state.resonanceStage)
        assertTrue(navigationGuidingThreadAvailable(state))
        assertFalse(state.canBeginHunt)
    }

    @Test
    fun staleFallbackCannotRetainArrivalAndRecoveryRequiresFreshFixes() {
        val first = sample(live, 100)
        val second = sample(live, 200)
        val confirmed = updateRelicArrivalConfirmation(updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), first), second)
        assertTrue(confirmed.arrivalConfirmed)
        val stale = live.copy(currentLocation = null, validity = SensorValidity.UNRELIABLE)
        val rejected = sample(stale, 200)
        val reset = updateRelicArrivalConfirmation(confirmed, rejected)
        val unavailable = state(stale, rejected, reset)
        assertFalse(unavailable.canBeginHunt)
        assertFalse(navigationGuidingThreadAvailable(unavailable))
        assertNull(unavailable.distanceMeters)
        assertEquals("Improving location signal…", relicArrivalPresentation(unavailable, true).stateDescription)
        val recovering = sample(live, 300)
        val pending = updateRelicArrivalConfirmation(reset, recovering)
        assertFalse(state(live, recovering, pending).canBeginHunt)
        val recovered = sample(live, 400)
        val arrived = updateRelicArrivalConfirmation(pending, recovered)
        assertTrue(state(live, recovered, arrived).canBeginHunt)
    }

    @Test
    fun debugSimulationIntentionallyOverridesDeniedGpsAndReturningRestoresReadiness() {
        val denied = live.copy(permission = LocationPermissionState.DENIED)
        val simulation = RelicNavigationSimulation().withDistance(8.0)
        val sample = simulation.arrivalSample()
        val state = deriveRelicNavigationUiState(sample,
            updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), sample),
            locationReadiness = navigationLocationReadiness(denied, true))
        assertEquals(NavigationLocationReadiness.READY, state.locationReadiness)
        assertTrue(navigationGuidingThreadAvailable(state))
        assertEquals(NavigationLocationReadiness.ACCESS_REQUIRED, navigationLocationReadiness(denied, false))
    }

    private fun sample(location: LocationOutput, timestamp: Long) = RelicArrivalSample(
        hasActionableLocation = LocationActionPolicy.actionableCoordinate(location) != null,
        distanceMeters = 8.0, accuracyMeters = 5.0, timestampNanos = timestamp,
    )

    private fun state(location: LocationOutput, sample: RelicArrivalSample, evidence: RelicArrivalConfirmationState) =
        deriveRelicNavigationUiState(sample, updateRelicArrivalConfirmation(evidence, sample),
            targetBearingDegrees = 90.0, deviceHeadingDegrees = 90.0,
            locationReadiness = navigationLocationReadiness(location))
}
