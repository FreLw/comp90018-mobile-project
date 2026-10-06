package com.comp90018.app.features.haptics

import org.junit.Assert.*
import org.junit.Test

class TreasureHapticPriorityTest {
    private var now = 0L
    private val patterns = mutableListOf<TreasureHapticPattern>()
    private val device = object : TreasureVibrationDevice {
        override fun hasVibrator() = true
        override fun hasAmplitudeControl() = true
        override fun vibrate(pattern: TreasureHapticPattern) { patterns.add(pattern) }
        override fun cancel() {}
    }
    private val controller = TreasureHapticController(SafeTreasureHapticDriver(device)) { now }.apply {
        enabled = true; foreground = true
    }

    @Test fun actualDriverReceivesNoProximityPatternDuringSuccessWindow() {
        controller.discoverySaved("a", null)
        controller.nearby("b", 19.9)
        now = TreasureHapticPattern.SUCCESS_DURATION_NANOS - 1
        controller.nearby("b", 20.0)
        assertEquals(1, patterns.size)
        assertArrayEquals(longArrayOf(150, 100, 200), patterns.single().timings)
        assertEquals(TreasureHapticPattern.SUCCESS_DURATION_NANOS, patterns.single().timings.sum() * 1_000_000L)
        now++
        controller.nearby("b", 20.0) // ID was not consumed by the suppressed GPS updates
        controller.nearby("b", 20.0)
        assertEquals(2, patterns.size)
        assertArrayEquals(longArrayOf(100), patterns.last().timings)
    }

    @Test fun laggingCollectionSnapshotCannotEmitProximityForAnUnlockedTreasure() {
        controller.discoverySaved("a", null)
        now = 1_000_000_000L
        controller.nearby("a", 1.0)
        assertEquals(1, patterns.size)
    }

    @Test fun failureAndBackgroundSuccessDoNotReservePlaybackAndCancellationClearsProtection() {
        controller.discoverySaved("failed", "denied")
        controller.foreground = false
        controller.discoverySaved("background", null)
        controller.foreground = true
        controller.nearby("b", 20.0)
        assertArrayEquals(longArrayOf(100), patterns.single().timings)
        controller.discoverySaved("a", null)
        controller.cancelPlaybackProtection()
        controller.nearby("c", 20.0)
        assertEquals(3, patterns.size)
    }
}
