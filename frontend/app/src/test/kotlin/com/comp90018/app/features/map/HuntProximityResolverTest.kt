package com.comp90018.app.features.map

import com.comp90018.app.sensors.SensorValidity
import org.junit.Assert.assertEquals
import org.junit.Test

class HuntProximityResolverTest {
    private fun stage(distance: Double?, inside: Double = 20.0, radar: Double = 100.0,
        validity: SensorValidity = SensorValidity.VALID) =
        HuntProximityResolver.resolve(distance, validity, radar, inside)

    @Test fun unavailableLocationIsUnknown() {
        assertEquals(HuntProximityStage.UNKNOWN, stage(null))
        assertEquals(HuntProximityStage.UNKNOWN, stage(0.0, validity = SensorValidity.UNKNOWN))
        assertEquals(HuntProximityStage.UNKNOWN, stage(0.0, validity = SensorValidity.UNRELIABLE))
    }

    @Test fun radarBoundaryAndFar() {
        assertEquals(HuntProximityStage.FAR, stage(100.01))
        assertEquals(HuntProximityStage.NEARBY, stage(100.0))
        assertEquals(HuntProximityStage.NEARBY, stage(50.0))
    }

    @Test fun insideBoundaryAndTreasureSpecificRadii() {
        assertEquals(HuntProximityStage.HUNT_READY, stage(20.0))
        assertEquals(HuntProximityStage.HUNT_READY, stage(19.99))
        assertEquals(HuntProximityStage.HUNT_READY, stage(24.0, inside = 25.0))
        assertEquals(HuntProximityStage.NEARBY, stage(24.0, inside = 20.0))
        assertEquals(HuntProximityStage.NEARBY, stage(19.0, inside = 18.0))
        assertEquals(HuntProximityStage.NEARBY, stage(16.0, inside = 15.0))
    }

    @Test fun malformedValuesAreUnknown() {
        assertEquals(HuntProximityStage.UNKNOWN, stage(Double.NaN))
        assertEquals(HuntProximityStage.UNKNOWN, stage(-1.0))
        assertEquals(HuntProximityStage.UNKNOWN, stage(10.0, radar = Double.POSITIVE_INFINITY))
        assertEquals(HuntProximityStage.UNKNOWN, stage(10.0, inside = 101.0))
    }
}
