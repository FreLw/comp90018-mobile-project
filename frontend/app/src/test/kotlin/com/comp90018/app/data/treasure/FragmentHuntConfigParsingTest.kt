package com.comp90018.app.data.treasure

import com.comp90018.app.features.map.SouthLawnFragmentIds
import org.junit.Assert.*
import org.junit.Test

class FragmentHuntConfigParsingTest {
    private fun fields(radius: Any = 8.0, latitude: Any = -37.79849): Map<String, Any> = mapOf(
        "collectionRadiusMeters" to radius,
        "fragments" to SouthLawnFragmentIds.map { id -> mapOf(
            "id" to id, "title" to "Custom title", "latitude" to latitude, "longitude" to 144.95994,
        ) },
    )

    @Test fun readsRemoteCoordinatesNamesAndRadius() {
        val config = requireNotNull(parseFragmentHuntConfig(fields(12L, -37.8)))
        assertEquals(12.0, config.collectionRadiusMeters, 0.0)
        assertEquals(4, config.fragments.size)
        assertEquals(-37.8, config.fragments.first().coordinate.latitude, 0.0)
        assertEquals("Custom title", config.fragments.first().title)
    }

    @Test fun rejectsMissingAndMalformedConfiguration() {
        assertNull(parseFragmentHuntConfig(null))
        assertNull(parseFragmentHuntConfig(emptyMap<String, Any>()))
        for (radius in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, "8")) {
            assertNull(parseFragmentHuntConfig(fields(radius)))
        }
        for (latitude in listOf(91.0, Double.NaN, "-37.8")) {
            assertNull(parseFragmentHuntConfig(fields(latitude = latitude)))
        }
    }

    @Test fun rejectsPartialDuplicateAndUnknownFragments() {
        val valid = fields()
        val fragments = valid["fragments"] as List<*>
        assertNull(parseFragmentHuntConfig(valid + ("fragments" to fragments.dropLast(1))))
        assertNull(parseFragmentHuntConfig(valid + ("fragments" to List(4) { fragments.first() })))
        assertNull(parseFragmentHuntConfig(valid + ("fragments" to fragments.dropLast(1) + mapOf(
            "id" to "unknown", "title" to "Unknown", "latitude" to 0, "longitude" to 0,
        ))))
    }
}
