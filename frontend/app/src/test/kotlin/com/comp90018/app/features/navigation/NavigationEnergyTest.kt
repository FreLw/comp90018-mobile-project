package com.comp90018.app.features.navigation

import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationEnergyTest {
    @Test
    fun resonanceUsesFixedRangeAndReachesFullAtEntryDistance() {
        assertEquals(0, navigationEnergyPercent(null))
        assertEquals(0, navigationEnergyPercent(300.0))
        assertEquals(0, navigationEnergyPercent(150.0))
        assertEquals(36, navigationEnergyPercent(100.0))
        assertEquals(71, navigationEnergyPercent(50.0))
        assertEquals(100, navigationEnergyPercent(10.0))
        assertEquals(100, navigationEnergyPercent(0.0))
        assertEquals(0f, navigationEnergyProgress(150.0), 0f)
        assertEquals(0.5f, navigationEnergyProgress(80.0), 0f)
        assertEquals(1f, navigationEnergyProgress(10.0), 0f)
    }

    @Test
    fun invalidDistancesNeverProvideResonanceProgress() {
        assertEquals(0, navigationEnergyPercent(Double.NaN))
        assertEquals(0, navigationEnergyPercent(Double.POSITIVE_INFINITY))
        assertEquals(0, navigationEnergyPercent(-1.0))
    }

    @Test
    fun debugCoordinateProducesTheRequestedDistance() {
        val target = GeoCoordinate(-37.7986, 144.9602)
        val simulated = navigationTestCoordinate(target, 175.0)

        assertEquals(175.0, LocationCalculator.distanceMeters(simulated, target), 0.05)
    }

    @Test
    fun trailRunsFastestWhenHeadingMatchesTargetBearing() {
        assertEquals(1_500, navigationTrailDurationMillis(10.0, 10.0))
        assertEquals(1_500, navigationTrailDurationMillis(5.0, 365.0))
        assertEquals(2_850, navigationTrailDurationMillis(90.0, 0.0))
        assertEquals(4_200, navigationTrailDurationMillis(180.0, 0.0))
        assertEquals(4_200, navigationTrailDurationMillis(null, 0.0))
    }

    @Test
    fun trailUsesSlowDefaultForMissingOrNonFiniteHeadingsOrBearings() {
        listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { value ->
            assertEquals(4_200, navigationTrailDurationMillis(90.0, value))
            assertEquals(4_200, navigationTrailDurationMillis(value, 90.0))
        }
        assertEquals(4_200, navigationTrailDurationMillis(null, null))
        assertEquals(1_500, navigationTrailDurationMillis(0.0, 0.0))
    }

}
