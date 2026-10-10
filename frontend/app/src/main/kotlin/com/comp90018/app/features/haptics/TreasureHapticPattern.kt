package com.comp90018.app.features.haptics

/*
 * Defines waveform timings and amplitudes for treasure feedback.
 * Edit these patterns to change the feel of a feedback event while preserving controller policy.
 */

/** -1 requests the device default amplitude when amplitude control is unavailable. */
data class TreasureHapticPattern(val timings: LongArray, val amplitudes: IntArray) {
    companion object {
        private const val SUCCESS_FIRST_MS = 150L
        private const val SUCCESS_PAUSE_MS = 100L
        private const val SUCCESS_SECOND_MS = 200L
        const val SUCCESS_DURATION_NANOS = (SUCCESS_FIRST_MS + SUCCESS_PAUSE_MS + SUCCESS_SECOND_MS) * 1_000_000L

        fun forEvent(event: TreasureHapticEvent, amplitudeControl: Boolean): TreasureHapticPattern =
            when (event) {
                is TreasureHapticEvent.Nearby -> TreasureHapticPattern(
                    longArrayOf(100), intArrayOf(if (amplitudeControl) 60 else -1))
                is TreasureHapticEvent.Unlocked -> TreasureHapticPattern(
                    longArrayOf(SUCCESS_FIRST_MS, SUCCESS_PAUSE_MS, SUCCESS_SECOND_MS), intArrayOf(
                        if (amplitudeControl) 220 else -1, 0, if (amplitudeControl) 255 else -1))
            }
    }
}
