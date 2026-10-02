package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
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
    fun buildOutputPreservesPermissionAvailabilityAccuracyAndTimestampMetadata() {
        val output = LocationCalculator.buildOutput(
            currentLocation = nearbyOldQuad,
            targetLocation = oldQuad,
            timestampNanos = 42_000L,
            config = LocationConfig(insideRadiusMeters = 15.0, nearbyRadiusMeters = 80.0),
            permission = LocationPermissionState.GRANTED,
            availability = LocationAvailabilityState.AVAILABLE,
            accuracyMeters = 7.5,
        )

        assertEquals(LocationPermissionState.GRANTED, output.permission)
        assertEquals(LocationAvailabilityState.AVAILABLE, output.availability)
        assertEquals(7.5, output.accuracyMeters)
        assertEquals(42_000L, output.timestampNanos)
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

    @Test
    fun buildOutputReturnsUnreliableForInvalidCurrentCoordinate() {
        val output = LocationCalculator.buildOutput(
            currentLocation = GeoCoordinate(latitude = -91.0, longitude = 144.960481),
            targetLocation = oldQuad,
        )

        assertEquals(ProximityState.UNKNOWN, output.proximity)
        assertEquals(SensorValidity.UNRELIABLE, output.validity)
        assertEquals(null, output.distanceToTargetMeters)
        assertEquals(null, output.targetBearingDegrees)
    }

    @Test
    fun proximityStateTreatsRadiusBoundariesAsInsideAndNearby() {
        assertEquals(
            ProximityState.INSIDE,
            LocationCalculator.proximityState(
                distanceMeters = 20.0,
                insideRadiusMeters = 20.0,
                nearbyRadiusMeters = 120.0,
            ),
        )
        assertEquals(
            ProximityState.NEARBY,
            LocationCalculator.proximityState(
                distanceMeters = 120.0,
                insideRadiusMeters = 20.0,
                nearbyRadiusMeters = 120.0,
            ),
        )
    }

    @Test
    fun proximityStateRejectsMisconfiguredRadii() {
        assertThrows(IllegalArgumentException::class.java) {
            LocationCalculator.proximityState(
                distanceMeters = 10.0,
                insideRadiusMeters = 80.0,
                nearbyRadiusMeters = 20.0,
            )
        }
    }

    @Test
    fun stabilizedProximityKeepsInsideAcrossSmallGpsDriftPastInsideRadius() {
        val config = LocationConfig(
            insideRadiusMeters = 20.0,
            nearbyRadiusMeters = 120.0,
            proximityHysteresisMeters = 5.0,
        )

        val proximity = LocationCalculator.stabilizedProximityState(
            distanceMeters = 23.0,
            config = config,
            previousProximity = ProximityState.INSIDE,
        )

        assertEquals(ProximityState.INSIDE, proximity)
    }

    @Test
    fun stabilizedProximityKeepsOutsideAcrossSmallGpsDriftInsideNearbyRadius() {
        val config = LocationConfig(
            insideRadiusMeters = 20.0,
            nearbyRadiusMeters = 120.0,
            proximityHysteresisMeters = 5.0,
        )

        val proximity = LocationCalculator.stabilizedProximityState(
            distanceMeters = 118.0,
            config = config,
            previousProximity = ProximityState.OUTSIDE,
        )

        assertEquals(ProximityState.OUTSIDE, proximity)
    }

    @Test
    fun stabilizedProximityTransitionsAfterHysteresisBandIsCrossed() {
        val config = LocationConfig(
            insideRadiusMeters = 20.0,
            nearbyRadiusMeters = 120.0,
            proximityHysteresisMeters = 5.0,
        )

        val proximity = LocationCalculator.stabilizedProximityState(
            distanceMeters = 114.0,
            config = config,
            previousProximity = ProximityState.OUTSIDE,
        )

        assertEquals(ProximityState.NEARBY, proximity)
    }

    @Test
    fun normalizeDegreesWrapsNegativeAndLargeBearings() {
        assertEquals(350.0, LocationCalculator.normalizeDegrees(-10.0), 0.0)
        assertEquals(5.0, LocationCalculator.normalizeDegrees(725.0), 0.0)
        assertEquals(0.0, LocationCalculator.normalizeDegrees(360.0), 0.0)
    }
}
