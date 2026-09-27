package com.comp90018.app.features.map

import com.comp90018.app.sensors.HorizontalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDiscoveryLogicTest {
    @Test
    fun radarDistanceBandsUseTheRequestedBoundaries() {
        assertEquals(RadarSignalRange.HUNT_READY, radarSignalForDistance(0.0))
        assertEquals(RadarSignalRange.HUNT_READY, radarSignalForDistance(10.0))
        assertEquals(RadarSignalRange.REVEALED, radarSignalForDistance(10.01))
        assertEquals(RadarSignalRange.REVEALED, radarSignalForDistance(50.0))
        assertEquals(RadarSignalRange.NEARBY_HIDDEN, radarSignalForDistance(50.01))
        assertEquals(RadarSignalRange.NEARBY_HIDDEN, radarSignalForDistance(100.0))
        assertEquals(RadarSignalRange.OUT_OF_RANGE, radarSignalForDistance(100.01))
    }

    @Test
    fun bearingDifferenceChoosesTheShortestTurnAcrossNorth() {
        assertEquals(20.0, signedBearingDifference(10.0, 350.0), 0.0001)
        assertEquals(-20.0, signedBearingDifference(350.0, 10.0), 0.0001)
        assertEquals(0.0, signedBearingDifference(90.0, 90.0), 0.0001)
    }

    @Test
    fun bearingsAreRenderedAsFriendlyCompassDirections() {
        assertEquals("north", bearingToCompassDirection(0.0))
        assertEquals("north-east", bearingToCompassDirection(45.0))
        assertEquals("south", bearingToCompassDirection(180.0))
        assertEquals("north-west", bearingToCompassDirection(315.0))
        assertEquals("north", bearingToCompassDirection(359.9))
    }

    @Test
    fun treasureLockRequiresDistanceDirectionAndLevelPhone() {
        assertTrue(evaluateHuntReadiness(10.0, 15.0, HorizontalState.HORIZONTAL).allReady)
        assertFalse(evaluateHuntReadiness(10.01, 0.0, HorizontalState.HORIZONTAL).allReady)
        assertFalse(evaluateHuntReadiness(5.0, 15.01, HorizontalState.HORIZONTAL).allReady)
        assertFalse(evaluateHuntReadiness(5.0, 0.0, HorizontalState.NOT_HORIZONTAL).allReady)
        assertFalse(evaluateHuntReadiness(null, null, HorizontalState.UNKNOWN).allReady)
    }
}
