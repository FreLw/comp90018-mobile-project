package com.comp90018.app.features.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDiscoveryLogicTest {
    @Test
    fun radarDistanceBandsUseTheRequestedBoundaries() {
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
        assertTrue(evaluateHuntReadiness(10.0, 15.0, 12f).allReady)
        assertFalse(evaluateHuntReadiness(10.01, 0.0, 0f).allReady)
        assertFalse(evaluateHuntReadiness(5.0, 15.01, 0f).allReady)
        assertFalse(evaluateHuntReadiness(5.0, 0.0, 12.01f).allReady)
        assertFalse(evaluateHuntReadiness(null, null, 0f).allReady)
    }
    @Test
    fun recoveredMarkersRemainVisibleWhenTheNextHuntTargetChanges() {
        val here = com.comp90018.app.sensors.location.GeoCoordinate(-37.798, 144.96)
        val distant = com.comp90018.app.sensors.location.GeoCoordinate(-37.81, 144.98)
        val recovered = MapRelic("recovered", "Recovered relic", "Campus", coordinate = distant)
        val hidden = MapRelic("hidden", "Hidden relic", "Campus", coordinate = distant)
        val nearby = MapRelic("nearby", "Nearby relic", "Campus", coordinate = here)
        assertEquals(listOf(recovered, nearby), visibleMapRelics(
            listOf(recovered, hidden, nearby), here, 50.0, setOf(recovered.id)))
        assertEquals(listOf(hidden), visibleMapRelics(
            listOf(hidden), here, 50.0, returningRelicId = hidden.id))
    }
}
