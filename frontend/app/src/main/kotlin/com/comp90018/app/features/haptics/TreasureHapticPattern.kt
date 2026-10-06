package com.comp90018.app.features.haptics

/** -1 requests the device default amplitude when amplitude control is unavailable. */
data class TreasureHapticPattern(val timings: LongArray, val amplitudes: IntArray) {
    companion object {
        fun forEvent(event: TreasureHapticEvent, amplitudeControl: Boolean): TreasureHapticPattern =
            when (event) {
                is TreasureHapticEvent.Nearby -> TreasureHapticPattern(
                    longArrayOf(100), intArrayOf(if (amplitudeControl) 60 else -1))
                is TreasureHapticEvent.Unlocked -> TreasureHapticPattern(
                    longArrayOf(150, 100, 200), intArrayOf(
                        if (amplitudeControl) 220 else -1, 0, if (amplitudeControl) 255 else -1))
            }
    }
}
