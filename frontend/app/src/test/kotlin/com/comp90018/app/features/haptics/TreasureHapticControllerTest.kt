package com.comp90018.app.features.haptics

import org.junit.Assert.*
import org.junit.Test

class TreasureHapticControllerTest {
    private val events = mutableListOf<TreasureHapticEvent>()
    private val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }

    @Test fun gpsOscillationAndNavigationDoNotResetTreasureState() {
        listOf(20.1, 20.0, 19.9, 19.0, 21.0, 19.0).forEach { controller.nearby("a", it) }
        controller.nearby("b", 19.0)
        controller.nearby("a", 19.0)
        assertEquals(listOf(TreasureHapticEvent.Nearby("a"), TreasureHapticEvent.Nearby("b")), events)
    }

    @Test fun successRequiresSaveConfirmationAndDeduplicatesCallbacks() {
        controller.discoverySaved("a", "failed")
        assertTrue(events.isEmpty())
        controller.discoverySaved("a", null)
        controller.discoverySaved("a", null)
        controller.discoverySaved("b", null, alreadyDiscovered = true)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
    }

    @Test fun cancellationAndReadinessAloneNeverEmitSuccess() {
        controller.nearby("a", 1.0)
        assertEquals(listOf(TreasureHapticEvent.Nearby("a")), events)
    }
}
