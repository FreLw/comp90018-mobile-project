package com.comp90018.app.features.haptics

import org.junit.Assert.*
import org.junit.Test

class TreasureHapticLifecycleTest {
    @Test fun disabledAndBackgroundEventsAreSuppressedWithoutReplayingSuccess() {
        val events = mutableListOf<TreasureHapticEvent>()
        val controller = TreasureHapticController(events::add)
        controller.nearby("a", 20.0)
        controller.enabled = true
        controller.nearby("a", 20.0)
        assertTrue(events.isEmpty())
        controller.foreground = true
        controller.nearby("a", 20.0)
        controller.enabled = false
        controller.discoverySaved("disabled", null)
        controller.enabled = true
        controller.foreground = false
        controller.discoverySaved("background", null)
        controller.foreground = true
        controller.discoverySaved("disabled", null)
        controller.discoverySaved("background", null)
        controller.nearby("a", 20.0) // navigation/recomposition uses the same session
        controller.discoverySaved("fresh", null)
        assertEquals(listOf(TreasureHapticEvent.Nearby("a"), TreasureHapticEvent.Unlocked("fresh")), events)
    }
    @Test fun aNewSessionCanNotifyTheSameTreasureAgain() {
        val events = mutableListOf<TreasureHapticEvent>()
        repeat(2) {
            TreasureHapticController(events::add).apply {
                enabled = true; foreground = true; nearby("a", 20.0)
            }
        }
        assertEquals(2, events.size)
    }
    @Test fun patternsHaveDistinctDurationsAndHardwareFallbackAmplitudes() {
        val nearby = TreasureHapticPattern.forEvent(TreasureHapticEvent.Nearby("a"), true)
        val unlocked = TreasureHapticPattern.forEvent(TreasureHapticEvent.Unlocked("a"), true)
        assertArrayEquals(longArrayOf(100), nearby.timings)
        assertArrayEquals(intArrayOf(60), nearby.amplitudes)
        assertArrayEquals(longArrayOf(150, 100, 200), unlocked.timings)
        assertArrayEquals(intArrayOf(220, 0, 255), unlocked.amplitudes)
        assertArrayEquals(intArrayOf(-1, 0, -1),
            TreasureHapticPattern.forEvent(TreasureHapticEvent.Unlocked("a"), false).amplitudes)
    }
}
