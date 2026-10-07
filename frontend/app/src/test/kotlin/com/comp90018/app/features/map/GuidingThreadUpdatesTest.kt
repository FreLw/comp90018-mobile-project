package com.comp90018.app.features.map

import com.comp90018.app.features.navigation.NavigationMapUpdateGate
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidingThreadUpdatesTest {
    private val frame = GuidingThreadFrame(GeoCoordinate(-37.7996, 144.9602),
        GeoCoordinate(-37.7986, 144.9602), 0.5f, 0f)

    @Test
    fun phaseOnlyMovesGlintsAndStrengthRestylesWithoutCameraUpdate() {
        val gate = NavigationMapUpdateGate()
        val scene = listOf(frame.location, frame.target, 0)
        assertTrue(gate.shouldUpdateScene(scene))
        val animation = guidingThreadUpdates(frame, frame.copy(phase = 0.1f))
        assertTrue(animation.animation)
        assertFalse(animation.geometry)
        assertFalse(animation.style)
        assertFalse(gate.shouldUpdateScene(scene))
        val style = guidingThreadUpdates(frame, frame.copy(strength = 0.9f))
        assertTrue(style.style)
        assertTrue(style.animation) // Changed radius must refresh diamond vertices too.
        assertFalse(style.geometry)
        assertFalse(gate.shouldUpdateScene(scene))
    }

    @Test
    fun newGeometryRequiresThreadAndStructuralSceneUpdate() {
        listOf(frame.copy(location = GeoCoordinate(-37.7997, 144.9602)),
            frame.copy(target = GeoCoordinate(-37.7987, 144.9602)), frame.copy(target = null))
            .forEach { next ->
                val gate = NavigationMapUpdateGate()
                gate.shouldUpdateScene(listOf(frame.location, frame.target))
                assertTrue(guidingThreadUpdates(frame, next).geometry)
                assertTrue(gate.shouldUpdateScene(listOf(next.location, next.target)))
            }
    }

    @Test
    fun firstFrameAfterOverlayRecreationUpdatesEverythingAndUnchangedFrameDoesNothing() {
        val initial = guidingThreadUpdates(null, frame)
        assertTrue(initial.geometry)
        assertTrue(initial.style)
        assertTrue(initial.animation)
        val same = guidingThreadUpdates(frame, frame)
        assertFalse(same.geometry)
        assertFalse(same.style)
        assertFalse(same.animation)
    }
}
