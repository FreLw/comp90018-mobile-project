package com.comp90018.app.features.map

import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapInitialCameraStrategyTest {
    @Test
    fun campusTreasuresAreCompactEnoughToFitTogether() {
        val treasures = listOf(
            relic("old_quad", -37.79729, 144.96146),
            relic("south_lawn", -37.79868, 144.95972),
            relic("wilson_hall", -37.79921, 144.96208),
        )

        assertTrue(isCompactTreasureArea(treasures))
    }

    @Test
    fun distantTreasuresShouldNotBeFitIntoOneInitialCampusView() {
        val treasures = listOf(
            relic("south_lawn", -37.79868, 144.95972),
            relic("distant_site", -37.81363, 144.96306),
            relic("another_city", -33.86882, 151.20929),
        )

        assertFalse(isCompactTreasureArea(treasures))
    }

    @Test
    fun recommendedRelicUsesSortOrderBeforeName() {
        val treasures = listOf(
            relic("third", -37.79868, 144.95972, sortOrder = 3, name = "Third"),
            relic("first", -37.79729, 144.96146, sortOrder = 1, name = "First"),
            relic("second", -37.79921, 144.96208, sortOrder = 2, name = "Second"),
        )

        assertEquals("first", recommendedInitialRelic(treasures)?.id)
    }

    private fun relic(
        id: String,
        latitude: Double,
        longitude: Double,
        sortOrder: Int = Int.MAX_VALUE,
        name: String = id,
    ) = MapRelic(
        id = id,
        name = name,
        locationName = name,
        coordinate = GeoCoordinate(latitude, longitude),
        sortOrder = sortOrder,
    )
}
