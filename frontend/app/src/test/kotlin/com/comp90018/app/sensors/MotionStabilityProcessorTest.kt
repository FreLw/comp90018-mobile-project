package com.comp90018.app.sensors

import org.junit.Assert.*
import org.junit.Test

class MotionStabilityProcessorTest {
    private val step = 20_000_000L

    @Test fun stationarySamplesRemoveGravityAndBecomeStable() {
        for (gravity in listOf(Triple(0.0, 0.0, 9.81), Triple(9.81, 0.0, 0.0), Triple(3.0, 4.0, 8.0))) {
            val processor = MotionStabilityProcessor()
            var result = MotionStabilityOutput()
            repeat(60) { result = processor.process(it * step, gravity.first, gravity.second, gravity.third) }
            assertEquals(MotionState.STATIONARY, result.motion.classification)
            assertEquals(StabilityState.STABLE, result.stability.classification)
            assertEquals(SensorValidity.VALID, result.motion.validity)
            assertEquals(SensorValidity.VALID, result.stability.validity)
            assertEquals(0.0, result.motion.linearAccelerationMagnitude!!, 1e-9)
            assertEquals(0.0, result.stability.variation!!, 1e-9)
        }
    }

    @Test fun warmUpRequiresTimeAndSamples() {
        val processor = MotionStabilityProcessor()
        repeat(5) {
            val result = processor.process(it * step, 0.0, 0.0, 9.81)
            assertEquals(MotionState.UNKNOWN, result.motion.classification)
            assertEquals(StabilityState.UNKNOWN, result.stability.classification)
            assertEquals(SensorValidity.WARMING_UP, result.motion.validity)
        }
        val sparse = MotionStabilityProcessor()
        sparse.process(0, 0.0, 0.0, 9.81)
        val result = sparse.process(700_000_000L, 0.0, 0.0, 9.81)
        assertEquals(MotionState.UNKNOWN, result.motion.classification)
        assertEquals(StabilityState.UNKNOWN, result.stability.classification)
    }

    @Test fun movingSamplesProduceUnstableWindow() {
        val processor = MotionStabilityProcessor()
        var result = MotionStabilityOutput()
        repeat(100) { result = processor.process(it * step, if (it % 2 == 0) 0.0 else 6.0, 0.0, 9.81) }
        assertEquals(MotionState.MOVING, result.motion.classification)
        assertEquals(StabilityState.UNSTABLE, result.stability.classification)
        assertTrue(result.motion.smoothedMagnitude!! > 0.8)
    }

    @Test fun motionHysteresisRetainsStateBetweenThresholds() {
        val processor = MotionStabilityProcessor(SensorConfig(
            gravityTimeConstantNanos = 1_000_000_000_000L,
            motionTimeConstantNanos = 1L, motionWarmUpNanos = 1L, motionMinSamples = 2,
        ))
        processor.process(0, 0.0, 0.0, 9.81)
        assertEquals(MotionState.STATIONARY, processor.process(step, 0.1, 0.0, 9.81).motion.classification)
        assertEquals(MotionState.STATIONARY, processor.process(2 * step, 0.5, 0.0, 9.81).motion.classification)
        assertEquals(MotionState.MOVING, processor.process(3 * step, 1.2, 0.0, 9.81).motion.classification)
        assertEquals(MotionState.MOVING, processor.process(4 * step, 0.5, 0.0, 9.81).motion.classification)
        assertEquals(MotionState.STATIONARY, processor.process(5 * step, 0.1, 0.0, 9.81).motion.classification)
    }

    @Test fun stabilityHysteresisRetainsStateBetweenThresholds() {
        val processor = MotionStabilityProcessor(SensorConfig(
            gravityTimeConstantNanos = 1_000_000_000_000L,
            stabilityWindowNanos = step, stabilityMinSamples = 2,
        ))
        processor.process(0, 0.0, 0.0, 9.81)
        assertEquals(StabilityState.STABLE, processor.process(step, 0.0, 0.0, 9.81).stability.classification)
        assertEquals(StabilityState.STABLE, processor.process(2 * step, 0.36, 0.0, 9.81).stability.classification)
        assertEquals(StabilityState.UNSTABLE, processor.process(3 * step, 1.0, 0.0, 9.81).stability.classification)
        assertEquals(StabilityState.UNSTABLE, processor.process(4 * step, 0.64, 0.0, 9.81).stability.classification)
        assertEquals(StabilityState.STABLE, processor.process(5 * step, 0.64, 0.0, 9.81).stability.classification)
    }

    @Test fun resetAndLongGapRequireFreshWarmUp() {
        val processor = MotionStabilityProcessor()
        repeat(60) { processor.process(it * step, 0.0, 0.0, 9.81) }
        assertEquals(SensorValidity.WARMING_UP, processor.process(4_000_000_000L, 0.0, 0.0, 9.81).motion.validity)
        processor.reset()
        val result = processor.process(0, 9.81, 0.0, 0.0)
        assertEquals(MotionState.UNKNOWN, result.motion.classification)
        assertEquals(StabilityState.UNKNOWN, result.stability.classification)
        assertEquals(0.0, result.motion.linearAccelerationMagnitude!!, 0.0)
    }

    @Test fun invalidAndNonIncreasingSamplesDoNotPoisonHistory() {
        val processor = MotionStabilityProcessor()
        processor.process(step, 0.0, 0.0, 9.81)
        for (time in listOf(-1L, 0L, step)) {
            assertEquals(SensorValidity.UNRELIABLE, processor.process(time, 0.0, 0.0, 9.81).motion.validity)
        }
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(SensorValidity.UNRELIABLE, processor.process(2 * step, value, 0.0, 9.81).motion.validity)
        }
        assertEquals(0.0, processor.process(2 * step, 0.0, 0.0, 9.81).motion.linearAccelerationMagnitude!!, 1e-9)
    }

    @Test fun oldMovementLeavesRollingWindow() {
        val processor = MotionStabilityProcessor()
        repeat(60) { processor.process(it * step, if (it % 3 == 0) 6.0 else 0.0, 0.0, 9.81) }
        var result = MotionStabilityOutput()
        for (i in 60..400) result = processor.process(i * step, 0.0, 0.0, 9.81)
        assertEquals(MotionState.STATIONARY, result.motion.classification)
        assertEquals(StabilityState.STABLE, result.stability.classification)
    }

    @Test fun insufficientWindowCoverageCannotKeepValidClassification() {
        val processor = MotionStabilityProcessor(SensorConfig(
            stabilityWindowNanos = step, stabilityMinSamples = 2, stabilityMaxSamples = 2,
        ))
        processor.process(0, 0.0, 0.0, 9.81)
        assertEquals(StabilityState.STABLE, processor.process(step, 0.0, 0.0, 9.81).stability.classification)
        val result = processor.process(step + 1, 0.0, 0.0, 9.81)
        assertEquals(StabilityState.UNKNOWN, result.stability.classification)
        assertEquals(SensorValidity.WARMING_UP, result.stability.validity)
    }

    @Test fun invalidConfigurationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { SensorConfig(motionExitThreshold = 1.0) }
        assertThrows(IllegalArgumentException::class.java) { SensorConfig(stabilityMinSamples = 1) }
        assertThrows(IllegalArgumentException::class.java) { SensorConfig(headingTimeConstantNanos = 0) }
    }
}
