package com.comp90018.app.sensors

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Decibels relative to 16-bit PCM full scale (dBFS): 0 is clipping, more negative is quieter.
 * This is a relative loudness measure, not a calibrated absolute SPL reading.
 */
object SoundLevelCalculator {
    private const val FULL_SCALE_AMPLITUDE = 32768.0
    const val SILENCE_FLOOR_DECIBELS = -96.0

    fun decibelsFullScale(samples: ShortArray, count: Int = samples.size): Double {
        require(count in 1..samples.size)
        var sumOfSquares = 0.0
        for (i in 0 until count) sumOfSquares += samples[i].toDouble() * samples[i].toDouble()
        val rms = sqrt(sumOfSquares / count)
        if (rms <= 0.0) return SILENCE_FLOOR_DECIBELS
        return (20.0 * log10(rms / FULL_SCALE_AMPLITUDE)).coerceAtLeast(SILENCE_FLOOR_DECIBELS)
    }
}
