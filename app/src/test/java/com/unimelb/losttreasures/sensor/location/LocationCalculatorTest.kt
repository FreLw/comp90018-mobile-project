package com.unimelb.losttreasures.sensor.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCalculatorTest {
    private val oldQuad = GeoCoordinate(
        latitude = -37.79721,
        longitude = 144.96144
    )

    private val nearbyOldQuad = GeoCoordinate(
        latitude = -37.79725,
        longitude = 144.96148
    )

    private val southLawn = GeoCoordinate(
        latitude = -37.79831,
        longitude = 144.96099
    )

    private val melbourneCentral = GeoCoordinate(
        latitude = -37.81000,
        longitude = 144.96290
    )

    @Test
    fun distanceMetersReturnsZeroForSameCoordinate() {
        val distance = LocationCalculator.distanceMeters(oldQuad, oldQuad)

        assertEquals(0f, distance, 0.1f)
    }

    @Test
    fun buildLocationStateReturnsInsideWhenCurrentLocationIsWithinTargetRadius() {
        val state = LocationCalculator.buildLocationState(
            currentLocation = nearbyOldQuad,
            targetLocation = oldQuad,
            insideRadiusMeters = 15f,
            nearbyRadiusMeters = 80f
        )

        assertEquals(ProximityState.Inside, state.proximityState)
        assertTrue(state.isLocationAvailable)
        assertNotNull(state.distanceToTargetMeters)
        assertNotNull(state.targetBearingDegrees)
    }

    @Test
    fun buildLocationStateReturnsNearbyWhenCurrentLocationIsNearTarget() {
        val state = LocationCalculator.buildLocationState(
            currentLocation = southLawn,
            targetLocation = oldQuad,
            insideRadiusMeters = 15f,
            nearbyRadiusMeters = 150f
        )

        assertEquals(ProximityState.Nearby, state.proximityState)
    }

    @Test
    fun buildLocationStateReturnsOutsideWhenCurrentLocationIsFarFromTarget() {
        val state = LocationCalculator.buildLocationState(
            currentLocation = melbourneCentral,
            targetLocation = oldQuad,
            insideRadiusMeters = 15f,
            nearbyRadiusMeters = 150f
        )

        assertEquals(ProximityState.Outside, state.proximityState)
    }

    @Test
    fun bearingDegreesAlwaysReturnsNormalisedDegrees() {
        val bearing = LocationCalculator.bearingDegrees(southLawn, oldQuad)

        assertTrue(bearing >= 0f)
        assertTrue(bearing < 360f)
    }

    @Test
    fun buildLocationStateReturnsUnknownWhenCurrentOrTargetLocationIsMissing() {
        val state = LocationCalculator.buildLocationState(
            currentLocation = null,
            targetLocation = oldQuad,
            insideRadiusMeters = 15f,
            nearbyRadiusMeters = 150f
        )

        assertEquals(ProximityState.Unknown, state.proximityState)
        assertNull(state.distanceToTargetMeters)
        assertNull(state.targetBearingDegrees)
        assertTrue(!state.isLocationAvailable)
    }
}
