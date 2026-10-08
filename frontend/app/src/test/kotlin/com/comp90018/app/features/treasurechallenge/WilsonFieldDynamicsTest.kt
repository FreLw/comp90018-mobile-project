package com.comp90018.app.features.treasurechallenge

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class WilsonFieldDynamicsTest {
    @Test fun eachFlowerHasAnIndependentBoundedMotionSpinAndFlash() {
        val samples = (0..7).map { rosetteDrift(it * 331 + 149, 3.7125f) }
        assertEquals(8, samples.distinct().size)
        assertTrue(samples.map { it.x }.distinct().size > 1)
        assertTrue(samples.map { it.y }.distinct().size > 1)
        assertTrue(samples.map { it.spin }.distinct().size > 1)
        assertTrue(samples.map { it.pulse }.distinct().size > 1)
        samples.forEach { sample ->
            assertTrue(listOf(sample.x, sample.y, sample.spin, sample.pulse).all { it.isFinite() && it in -1f..1f })
        }
    }

    @Test fun randomPathsAreContinuousForFractionalTimeAndAtTheLoopBoundary() {
        val first = rosetteDrift(419, 1.2345f)
        val next = rosetteDrift(419, 1.2355f)
        assertTrue(abs(first.x - next.x) < .02f)
        assertTrue(abs(first.y - next.y) < .02f)
        assertNotEquals(first, next)
        assertEquals(first, rosetteDrift(419, 1.2345f))
        assertEquals(rosetteDrift(419, 0f), rosetteDrift(419, 120f))
        val edge = rosetteDrift(419, 119.999f)
        assertEquals(rosetteDrift(419, 0f).x, edge.x, .001f)
        assertEquals(rosetteDrift(419, 0f).y, edge.y, .001f)
    }

    @Test fun floatingStarsGildOnlyWhenTheirPartOfThePoolIsIlluminated() {
        fun count(fill: Float) = RosetteWaterStars.count { waterStarIlluminated(fill, it) }
        assertEquals(0, count(questApproachFill(100.0, 20.0)))
        assertEquals(6, count(questApproachFill(60.0, 20.0)))
        assertEquals(10, count(questApproachFill(20.0, 20.0)))
        assertEquals(0, count(Float.NaN))
        assertEquals(0, count(questApproachFill(null, 20.0)))
        RosetteWaterStars.forEach { star ->
            assertFalse(waterStarIlluminated(star.illuminationThreshold - .001f, star))
            assertTrue(waterStarIlluminated(star.illuminationThreshold + .001f, star))
        }
    }
}
