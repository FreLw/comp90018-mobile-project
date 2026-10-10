package com.comp90018.app.features.map

import com.comp90018.app.features.map.compass.oracleStarCount

import org.junit.Assert.assertEquals
import org.junit.Test

class OracleStarProgressTest {
    @Test fun starsLightAtEachTwoMetreMilestone() {
        assertEquals(0, oracleStarCount(30.0))
        assertEquals(0, oracleStarCount(28.01))
        assertEquals(1, oracleStarCount(28.0))
        assertEquals(9, oracleStarCount(10.01))
        assertEquals(10, oracleStarCount(10.0))
    }

    @Test fun starsReachFullBrightnessAtConfiguredDistance() {
        assertEquals(0, oracleStarCount(45.0, 25.0))
        assertEquals(5, oracleStarCount(35.0, 25.0))
        assertEquals(10, oracleStarCount(25.0, 25.0))
    }

    @Test fun retreatExtinguishesStarsAndMissingSignalsStayDark() {
        assertEquals(10, oracleStarCount(0.0))
        assertEquals(5, oracleStarCount(20.0))
        assertEquals(0, oracleStarCount(100.0))
        assertEquals(0, oracleStarCount(null))
        assertEquals(0, oracleStarCount(Double.NaN))
    }
}
