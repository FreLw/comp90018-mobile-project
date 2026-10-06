package com.comp90018.app.features.haptics

import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.*
import org.junit.Assert.*
import org.junit.Test

class TreasureHapticProximityTest {
    private val now = 20_000_000_000L
    private val fix = LocationOutput(currentLocation = GeoCoordinate(-37.8, 144.9),
        validity = SensorValidity.VALID, permission = LocationPermissionState.PRECISE,
        availability = LocationAvailabilityState.AVAILABLE, accuracyMeters = 5.0, timestampNanos = now)

    @Test fun boundaryIsInclusive() {
        assertFalse(TreasureHapticProximity.isNearby(20.1))
        assertTrue(TreasureHapticProximity.isNearby(20.0))
        assertTrue(TreasureHapticProximity.isNearby(19.9))
        listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -1.0).forEach {
            assertFalse(TreasureHapticProximity.isNearby(it))
        }
    }

    @Test fun onlyFreshReliableRealCoordinatesAreUsable() {
        assertEquals(fix.currentLocation, TreasureHapticProximity.usableCoordinate(fix, now))
        listOf(fix.copy(isMock = true), fix.copy(currentLocation = null), fix.copy(timestampNanos = null),
            fix.copy(timestampNanos = now - 10_000_000_001L), fix.copy(timestampNanos = now + 1),
            fix.copy(accuracyMeters = null), fix.copy(accuracyMeters = 50.1),
            fix.copy(accuracyMeters = Double.NaN), fix.copy(accuracyMeters = -1.0),
            fix.copy(validity = SensorValidity.UNRELIABLE), fix.copy(validity = SensorValidity.UNKNOWN),
            fix.copy(permission = LocationPermissionState.DENIED),
            fix.copy(permission = LocationPermissionState.APPROXIMATE),
            fix.copy(availability = LocationAvailabilityState.EXPIRED),
            fix.copy(currentLocation = GeoCoordinate(91.0, 0.0))
        ).forEach { assertNull(TreasureHapticProximity.usableCoordinate(it, now)) }
    }
}
