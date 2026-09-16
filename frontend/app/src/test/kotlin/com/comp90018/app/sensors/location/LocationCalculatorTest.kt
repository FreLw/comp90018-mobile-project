package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCalculatorTest {
    private val oldQuad = GeoCoordinate(-37.798156, 144.960481)
    private val nearbyOldQuad = GeoCoordinate(-37.79820, 144.96052)
    private val southLawn = GeoCoordinate(-37.79856, 144.96050)
    private val melbourneCentral = GeoCoordinate(-37.81000, 144.96290)

    @Test
    fun distanceMetersReturnsZeroForSameCoordinate() {
        val distance = LocationCalculator.distanceMeters(oldQuad, oldQuad)

        assertEquals(0.0, distance, 0.1)
    }

    @Test
    fun buildOutputReturnsInsideWhenCurrentLocationIsWithinTargetRadius() {
        val output = LocationCalculator.buildOutput(
            currentLocation = nearbyOldQuad,
            targetLocation = oldQuad,
            config = LocationConfig(insideRadiusMeters = 15.0, nearbyRadiusMeters = 80.0),
        )

        assertEquals(ProximityState.INSIDE, output.proximity)
        assertEquals(SensorValidity.VALID, output.validity)
        assertNotNull(output.distanceToTargetMeters)
        assertNotNull(output.targetBearingDegrees)
    }

    @Test
    fun buildOutputReturnsNearbyWhenCurrentLocationIsNearTarget() {
        val output = LocationCalculator.buildOutput(
            currentLocation = southLawn,
            targetLocation = oldQuad,
            config = LocationConfig(insideRadiusMeters = 15.0, nearbyRadiusMeters = 150.0),
        )

        assertEquals(ProximityState.NEARBY, output.proximity)
    }

    @Test
    fun buildOutputReturnsOutsideWhenCurrentLocationIsFarFromTarget() {
        val output = LocationCalculator.buildOutput(
            currentLocation = melbourneCentral,
            targetLocation = oldQuad,
            config = LocationConfig(insideRadiusMeters = 15.0, nearbyRadiusMeters = 150.0),
        )

        assertEquals(ProximityState.OUTSIDE, output.proximity)
    }

    @Test
    fun bearingDegreesAlwaysReturnsNormalisedDegrees() {
        val bearing = LocationCalculator.bearingDegrees(southLawn, oldQuad)

        assertTrue(bearing >= 0.0)
        assertTrue(bearing < 360.0)
    }

    @Test
    fun buildOutputReturnsUnknownWhenCurrentOrTargetLocationIsMissing() {
        val output = LocationCalculator.buildOutput(
            currentLocation = null,
            targetLocation = oldQuad,
        )

        assertEquals(ProximityState.UNKNOWN, output.proximity)
        assertEquals(SensorValidity.UNKNOWN, output.validity)
        assertEquals(null, output.distanceToTargetMeters)
        assertEquals(null, output.targetBearingDegrees)
    }
}
