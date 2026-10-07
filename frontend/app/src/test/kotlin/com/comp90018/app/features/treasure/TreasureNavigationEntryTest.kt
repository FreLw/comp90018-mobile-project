package com.comp90018.app.features.treasure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreasureNavigationEntryTest {
    @Test
    fun undiscoveredRelicOffersResonanceWithActionableDistanceAndSecondaryMap() {
        val entry = treasureNavigationEntry(false, 142.0, true)
        assertEquals("Follow the Resonance · 142 m", entry.primaryLabel)
        assertEquals("View on map", entry.secondaryMapLabel)
    }

    @Test
    fun missingOrInvalidDistanceStillOffersNavigationWithoutMisleadingDistance() {
        listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -1.0).forEach { distance ->
            assertEquals("Follow the Resonance", treasureNavigationEntry(false, distance, true).primaryLabel)
        }
    }

    @Test
    fun nearbyDistancesNeverOfferDirectHuntEntryOrCompetingMapPrimary() {
        listOf(0.0, 8.0, 10.0, 49.0, 50.0, 51.0, 200.0).forEach { distance ->
            val entry = treasureNavigationEntry(false, distance, true)
            assertEquals("Follow the Resonance · ${distance.toInt()} m", entry.primaryLabel)
            assertEquals("View on map", entry.secondaryMapLabel)
        }
    }

    @Test
    fun discoveredRelicPreservesLoreOnlyActions() {
        val entry = treasureNavigationEntry(true, 8.0, true)
        assertNull(entry.primaryLabel)
        assertNull(entry.secondaryMapLabel)
    }

    @Test
    fun missingNavigationCallbackKeepsMapInspectionSecondary() {
        val entry = treasureNavigationEntry(false, 8.0, false)
        assertNull(entry.primaryLabel)
        assertEquals("View on map", entry.secondaryMapLabel)
    }
}
