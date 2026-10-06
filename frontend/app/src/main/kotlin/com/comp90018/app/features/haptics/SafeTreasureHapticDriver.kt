package com.comp90018.app.features.haptics

/** Small hardware boundary for testing unavailable/unsupported devices without Android mocks. */
interface TreasureVibrationDevice {
    fun hasVibrator(): Boolean
    fun hasAmplitudeControl(): Boolean
    fun vibrate(pattern: TreasureHapticPattern)
    fun cancel()
}

class SafeTreasureHapticDriver(private val device: TreasureVibrationDevice?) : TreasureHapticDriver {
    override fun play(event: TreasureHapticEvent) {
        runCatching {
            val hardware = device ?: return
            if (!hardware.hasVibrator()) return
            hardware.vibrate(TreasureHapticPattern.forEvent(event, hardware.hasAmplitudeControl()))
        }
    }
    fun cancel() { runCatching { device?.cancel() } }
}
