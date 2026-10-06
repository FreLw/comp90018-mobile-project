package com.comp90018.app.features.navigation

import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationEnergyTest {
    @Test
    fun energyUsesFixedRangeAndReachesFullAtHuntDistance() {
        assertEquals(0, navigationEnergyPercent(null))
        assertEquals(0, navigationEnergyPercent(300.0))
        assertEquals(0, navigationEnergyPercent(250.0))
        assertEquals(63, navigationEnergyPercent(100.0))
        assertEquals(83, navigationEnergyPercent(50.0))
        assertEquals(100, navigationEnergyPercent(10.0))
        assertEquals(100, navigationEnergyPercent(0.0))
    }

    @Test
    fun invalidDistancesNeverChargeTheEnergyBar() {
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
}
