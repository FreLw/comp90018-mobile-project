package com.comp90018.app.sensors

import org.junit.Assert.*
import org.junit.Test

class SoundLevelCalculatorTest {
    @Test fun silenceHitsTheFloor() {
        val silence = ShortArray(100) { 0 }
        assertEquals(
            SoundLevelCalculator.SILENCE_FLOOR_DECIBELS,
            SoundLevelCalculator.decibelsFullScale(silence),
            0.0,
        )
    }

    @Test fun fullScaleIsZeroDecibels() {
        val clipping = ShortArray(100) { Short.MAX_VALUE }
        assertEquals(0.0, SoundLevelCalculator.decibelsFullScale(clipping), 0.5)
    }

    @Test fun louderSamplesProduceHigherDecibels() {
        val quiet = ShortArray(100) { 100 }
        val loud = ShortArray(100) { 10_000 }
        assertTrue(SoundLevelCalculator.decibelsFullScale(loud) > SoundLevelCalculator.decibelsFullScale(quiet))
    }

    @Test fun countLimitsHowManySamplesAreRead() {
        val samples = shortArrayOf(20_000, 20_000, 0, 0, 0)
        val firstTwo = SoundLevelCalculator.decibelsFullScale(samples, count = 2)
        val all = SoundLevelCalculator.decibelsFullScale(samples, count = samples.size)
        assertTrue(firstTwo > all)
    }

    @Test fun invalidCountIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { SoundLevelCalculator.decibelsFullScale(ShortArray(4), 0) }
        assertThrows(IllegalArgumentException::class.java) { SoundLevelCalculator.decibelsFullScale(ShortArray(4), 5) }
    }
}
