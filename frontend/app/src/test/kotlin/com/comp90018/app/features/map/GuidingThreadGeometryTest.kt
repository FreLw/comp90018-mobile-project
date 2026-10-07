package com.comp90018.app.features.map

import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidingThreadGeometryTest {
    private val target = GeoCoordinate(-37.7986, 144.9602)
    private val user = GeoCoordinate(-37.7996, 144.9602)

    @Test
    fun glintsTravelFromRelicTowardExplorerAndLoop() {
        assertEquals(target, guidingThreadGlintCentre(target, user, 0f, 0))
        val middle = guidingThreadGlintCentre(target, user, 0.5f, 0)
        assertEquals((target.latitude + user.latitude) / 2, middle.latitude, 0.000001)
        val later = guidingThreadGlintCentre(target, user, 0.9f, 0)
        assertTrue(LocationCalculator.distanceMeters(later, user) < LocationCalculator.distanceMeters(middle, user))
        assertEquals(target, guidingThreadGlintCentre(target, user, 1f, 0))
    }

    @Test
    fun exactlyThreeGlintsAreEvenlySpaced() {
        assertEquals(3, GUIDING_THREAD_GLINT_COUNT)
        val centres = List(GUIDING_THREAD_GLINT_COUNT) { guidingThreadGlintCentre(target, user, 0f, it) }
        assertEquals(3, centres.distinct().size)
        assertEquals(LocationCalculator.distanceMeters(centres[0], centres[1]),
            LocationCalculator.distanceMeters(centres[1], centres[2]), 0.01)
    }

    @Test
    fun diamondHasFourCardinalTipsAtRequestedDistance() {
        val vertices = guidingThreadGlintVertices(target, 1.2)
        assertEquals(4, vertices.size)
        vertices.forEach { assertEquals(1.2, LocationCalculator.distanceMeters(target, it), 0.001) }
        assertTrue(vertices[0].latitude > target.latitude)
        assertTrue(vertices[1].longitude > target.longitude)
        assertTrue(vertices[2].latitude < target.latitude)
        assertTrue(vertices[3].longitude < target.longitude)
    }

    @Test
    fun invalidPhaseUsesCalmInitialPosition() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f).forEach { phase ->
            assertEquals(target, guidingThreadGlintCentre(target, user, phase, 0))
        }
    }
}
