package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FakeLocationSensorTest {
    private val target = GeoCoordinate(-37.798156, 144.960481)
    private val nearbyUserLocation = GeoCoordinate(-37.79820, 144.96052)

    @Test
    fun setTargetLocationStoresTargetInOutput() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(target)

        assertEquals(target, sensor.output.value.targetLocation)
    }

    @Test
    fun updateCurrentLocationCalculatesOutputWhenSensorIsStarted() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(target)
        sensor.start()
        sensor.updateCurrentLocation(nearbyUserLocation, timestampNanos = 1_000L)

        val output = sensor.output.value
        assertEquals(ProximityState.INSIDE, output.proximity)
        assertEquals(SensorValidity.VALID, output.validity)
        assertEquals(nearbyUserLocation, output.currentLocation)
        assertNotNull(output.distanceToTargetMeters)
        assertNotNull(output.targetBearingDegrees)
        assertEquals(1_000L, output.timestampNanos)
    }

    @Test
    fun stopKeepsLocationsButReturnsUnknownValidity() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(target)
        sensor.start()
        sensor.updateCurrentLocation(nearbyUserLocation)
        sensor.stop()

        val output = sensor.output.value
        assertEquals(target, output.targetLocation)
        assertEquals(nearbyUserLocation, output.currentLocation)
        assertEquals(ProximityState.UNKNOWN, output.proximity)
        assertEquals(SensorValidity.UNKNOWN, output.validity)
    }

    @Test
    fun clearingTargetLocationReturnsUnknownProximity() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(target)
        sensor.start()
        sensor.updateCurrentLocation(nearbyUserLocation)
        sensor.setTargetLocation(null)

        val output = sensor.output.value
        assertEquals(null, output.targetLocation)
        assertEquals(ProximityState.UNKNOWN, output.proximity)
        assertEquals(SensorValidity.UNKNOWN, output.validity)
    }

    @Test
    fun relicSpecificRadiiControlProximityClassification() {
        val sensor = FakeLocationSensor()

        sensor.setTargetLocation(
            targetLocation = target,
            insideRadiusMeters = 2.0,
            nearbyRadiusMeters = 3.0,
        )
        sensor.start()
        sensor.updateCurrentLocation(nearbyUserLocation)

        assertEquals(ProximityState.OUTSIDE, sensor.output.value.proximity)
    }
}
