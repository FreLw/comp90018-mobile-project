package com.unimelb.losttreasures.sensor.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLocationSensorTest {
    private val target = GeoCoordinate(
        latitude = -37.798156,
        longitude = 144.960481
    )

    private val nearbyUserLocation = GeoCoordinate(
        latitude = -37.79820,
        longitude = 144.96052
    )

    @Test
    fun setTargetLocationStoresTargetInState() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(target)

        assertEquals(target, sensor.state.value.targetLocation)
    }

    @Test
    fun updateCurrentLocationCalculatesStateWhenSensorIsStarted() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(target)
        sensor.start()
        sensor.updateCurrentLocation(nearbyUserLocation)

        val state = sensor.state.value
        assertEquals(ProximityState.Inside, state.proximityState)
        assertEquals(nearbyUserLocation, state.currentLocation)
        assertTrue(state.isLocationAvailable)
        assertNotNull(state.distanceToTargetMeters)
        assertNotNull(state.targetBearingDegrees)
    }

    @Test
    fun stopKeepsLocationsButMarksLocationUnavailable() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(target)
        sensor.start()
        sensor.updateCurrentLocation(nearbyUserLocation)
        sensor.stop()

        val state = sensor.state.value
        assertEquals(target, state.targetLocation)
        assertEquals(nearbyUserLocation, state.currentLocation)
        assertFalse(state.isLocationAvailable)
        assertEquals(ProximityState.Unknown, state.proximityState)
    }

    @Test
    fun clearingTargetLocationReturnsUnknownProximity() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(target)
        sensor.start()
        sensor.updateCurrentLocation(nearbyUserLocation)
        sensor.setTargetLocation(null)

        val state = sensor.state.value
        assertEquals(null, state.targetLocation)
        assertEquals(ProximityState.Unknown, state.proximityState)
    }
}
