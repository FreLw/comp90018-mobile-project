package com.comp90018.app.features.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationLifecyclePolicyTest {
    @Test
    fun loadingTargetCanResolveButSettledMissingTargetExits() {
        assertFalse(shouldCancelRelicNavigation("relic", false, true))
        assertFalse(shouldCancelRelicNavigation("relic", true, false))
        assertTrue(shouldCancelRelicNavigation("relic", false, false))
        assertFalse(shouldCancelRelicNavigation(null, false, false))
    }

    @Test
    fun unchangedSceneSkipsRenderingWhileLocationTargetHeadingAndRecenterChangesRender() {
        val gate = NavigationMapUpdateGate()
        var scene = listOf<Any?>("target-a", 50.0, 0f, 0)
        assertTrue(gate.shouldUpdateScene(scene))
        // Any number of thread animation ticks reuse the scene; phase is deliberately excluded.
        repeat(24) { assertFalse(gate.shouldUpdateScene(scene.toList())) }
        listOf(
            listOf<Any?>("target-b", 50.0, 0f, 0),
            listOf<Any?>("target-b", 30.0, 0f, 0),
            listOf<Any?>("target-b", 30.0, 90f, 0),
            listOf<Any?>("target-b", 30.0, 90f, 1),
        ).forEach {
            scene = it
            assertTrue(gate.shouldUpdateScene(scene))
            assertFalse(gate.shouldUpdateScene(scene))
        }
    }

    @Test
    fun disposedMapRejectsLateUpdatesAndNewSessionRendersInitially() {
        val scene = listOf<Any?>("target", 0)
        val old = NavigationMapUpdateGate()
        assertTrue(old.shouldUpdateScene(scene))
        old.dispose()
        assertTrue(old.disposed)
        assertFalse(old.shouldUpdateScene(scene))
        assertFalse(old.shouldUpdateScene(listOf("another-target", 1)))
        assertTrue(NavigationMapUpdateGate().shouldUpdateScene(scene))
    }
}
