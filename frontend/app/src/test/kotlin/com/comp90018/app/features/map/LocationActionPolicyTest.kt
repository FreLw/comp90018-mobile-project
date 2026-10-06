package com.comp90018.app.features.map

import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.location.LocationPermissionState
import com.comp90018.app.sensors.location.ProximityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationActionPolicyTest {
    private val target = GeoCoordinate(-37.798156, 144.960481)
    private val nearby = GeoCoordinate(-37.798170, 144.960490)

    @Test
    fun validLiveLocationCanDriveTreasureActions() {
        val location = liveLocation()

        assertEquals(nearby, LocationActionPolicy.actionableCoordinate(location))
        assertTrue(LocationActionPolicy.isWithinRadius(location, target, radiusMeters = 10.0))
    }

    @Test
    fun staleLastKnownLocationCannotDriveTreasureActions() {
        val location = liveLocation().copy(
            currentLocation = null,
            lastKnownLocation = nearby,
            validity = SensorValidity.UNRELIABLE,
        )

        assertNull(LocationActionPolicy.actionableCoordinate(location))
        assertFalse(LocationActionPolicy.isWithinRadius(location, target, radiusMeters = 10.0))

        val output = targetOutput(location)
        assertNull(output.currentLocation)
        assertNull(output.distanceToTargetMeters)
        assertEquals(ProximityState.UNKNOWN, output.proximity)
        assertEquals(SensorValidity.UNKNOWN, output.validity)
    }

    @Test
    fun deniedOrUnknownLocationCannotDriveTreasureActions() {
        val denied = liveLocation().copy(permission = LocationPermissionState.DENIED)
        val unknown = liveLocation().copy(validity = SensorValidity.UNKNOWN)

        assertNull(LocationActionPolicy.actionableCoordinate(denied))
        assertNull(LocationActionPolicy.actionableCoordinate(unknown))
        assertFalse(LocationActionPolicy.isWithinRadius(denied, target, radiusMeters = 10.0))
        assertFalse(LocationActionPolicy.isWithinRadius(unknown, target, radiusMeters = 10.0))
        assertNull(targetOutput(denied).distanceToTargetMeters)
        assertNull(targetOutput(unknown).distanceToTargetMeters)
    }

    @Test
    fun validLocationOutsideRadiusCannotCompleteAction() {
        val farAway = liveLocation().copy(
            currentLocation = GeoCoordinate(-37.8100, 144.9629),
        )

        assertFalse(LocationActionPolicy.isWithinRadius(farAway, target, radiusMeters = 10.0))
    }

    @Test
    fun malformedRadiusCannotCompleteAction() {
        val location = liveLocation()

        assertFalse(LocationActionPolicy.isWithinRadius(location, target, radiusMeters = -1.0))
        assertFalse(LocationActionPolicy.isWithinRadius(location, target, radiusMeters = Double.NaN))
    }

    @Test
    fun explicitDebugCoordinateCanDriveSimulationWithoutPromotingFallbackLocation() {
        val unavailable = LocationOutput(
            lastKnownLocation = target,
            validity = SensorValidity.UNRELIABLE,
            permission = LocationPermissionState.DENIED,
        )

        val normalOutput = targetOutput(unavailable)
        val simulatedOutput = LocationActionPolicy.targetOutput(
            location = unavailable,
            target = target,
            config = config,
            simulatedCoordinate = nearby,
        )

        assertNull(normalOutput.distanceToTargetMeters)
        assertEquals(SensorValidity.VALID, simulatedOutput.validity)
        assertEquals(ProximityState.INSIDE, simulatedOutput.proximity)
    }

    private fun liveLocation() = LocationOutput(
        currentLocation = nearby,
        lastKnownLocation = nearby,
        validity = SensorValidity.VALID,
        permission = LocationPermissionState.PRECISE,
    )

    private fun targetOutput(location: LocationOutput) = LocationActionPolicy.targetOutput(
        location = location,
        target = target,
        config = config,
    )

    private companion object {
        val config = LocationConfig(insideRadiusMeters = 10.0, nearbyRadiusMeters = 100.0)
    }
}
