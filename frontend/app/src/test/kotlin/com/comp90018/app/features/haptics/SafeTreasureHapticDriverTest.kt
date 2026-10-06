package com.comp90018.app.features.haptics

import org.junit.Assert.*
import org.junit.Test

class SafeTreasureHapticDriverTest {
    @Test fun absentHardwareAndPlatformFailuresAreSafe() {
        SafeTreasureHapticDriver(null).apply { play(TreasureHapticEvent.Nearby("a")); cancel() }
        val device = Device().apply { supported = false }
        SafeTreasureHapticDriver(device).play(TreasureHapticEvent.Unlocked("a"))
        assertNull(device.pattern)
        device.throwOnAccess = true
        SafeTreasureHapticDriver(device).apply { play(TreasureHapticEvent.Unlocked("a")); cancel() }
    }
    @Test fun devicesWithoutAmplitudeControlUseDefaultAmplitudeAndCanCancel() {
        val device = Device()
        val driver = SafeTreasureHapticDriver(device)
        driver.play(TreasureHapticEvent.Unlocked("a"))
        assertArrayEquals(intArrayOf(-1, 0, -1), device.pattern!!.amplitudes)
        driver.cancel()
        assertTrue(device.cancelled)
    }
    private class Device : TreasureVibrationDevice {
        var supported = true
        var throwOnAccess = false
        var cancelled = false
        var pattern: TreasureHapticPattern? = null
        override fun hasVibrator(): Boolean {
            if (throwOnAccess) error("Hardware unavailable")
            return supported
        }
        override fun hasAmplitudeControl() = false
        override fun vibrate(pattern: TreasureHapticPattern) { this.pattern = pattern }
        override fun cancel() { if (throwOnAccess) error("Hardware unavailable"); cancelled = true }
    }
}
