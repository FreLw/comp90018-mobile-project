package com.comp90018.app.features.haptics

import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.*
import org.junit.Assert.*
import org.junit.Test

class TreasureHapticTargetTest {
    private val origin = GeoCoordinate(-37.8, 144.9)
    private val fix = LocationOutput(currentLocation = origin, validity = SensorValidity.VALID,
        permission = LocationPermissionState.PRECISE, availability = LocationAvailabilityState.AVAILABLE,
        accuracyMeters = 5.0, timestampNanos = 100L)
    private val a = MapRelic("a", "A", "Campus", coordinate = origin)
    private val b = MapRelic("b", "B", "Campus", coordinate = GeoCoordinate(-37.8001, 144.9))

    @Test fun selectsNearestUndiscoveredOrExplicitActiveTarget() {
        assertEquals("a", target()?.first)
        assertEquals("b", target(discovered = setOf("a"))?.first)
        assertEquals("b", target(active = "b")?.first)
        assertNull(target(active = "missing"))
        assertNull(target(active = "a", discovered = setOf("a")))
    }
    @Test fun neverUsesLastKnownDisplayFallbackOrMockLocation() {
        assertNull(target(location = fix.copy(currentLocation = null, lastKnownLocation = origin)))
        assertNull(target(location = fix.copy(isMock = true)))
        assertNull(target(location = fix.copy(timestampNanos = 0), now = 10_000_000_001L))
    }
    private fun target(discovered: Set<String> = emptySet(), active: String? = null,
        location: LocationOutput = fix, now: Long = 100L) =
        TreasureHapticTarget.nearest(listOf(b, a), discovered, active, location, now)
}
