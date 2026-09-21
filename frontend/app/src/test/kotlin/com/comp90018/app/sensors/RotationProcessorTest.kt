package com.comp90018.app.sensors

import org.junit.Assert.*
import org.junit.Test

class RotationProcessorTest {
    private val step = 20_000_000L

    @Test fun stillSamplesStayStill() {
        val processor = RotationProcessor()
        var result = RotationOutput()
        repeat(30) { result = processor.process(it * step, 0.0, 0.0, 0.0) }
        assertEquals(RotationState.STILL, result.classification)
        assertEquals(SensorValidity.VALID, result.validity)
        assertEquals(0.0, result.angularVelocityMagnitude!!, 1e-9)
    }

    @Test fun fastSamplesBecomeRotating() {
        val processor = RotationProcessor()
        var result = RotationOutput()
        repeat(30) { result = processor.process(it * step, 0.0, 0.0, 2.0) }
        assertEquals(RotationState.ROTATING, result.classification)
        assertTrue(result.angularVelocityMagnitude!! > 0.35)
    }

    @Test fun warmUpRequiresTimeAndSamples() {
        val processor = RotationProcessor()
        repeat(2) {
            val result = processor.process(it * step, 0.0, 0.0, 2.0)
            assertEquals(RotationState.UNKNOWN, result.classification)
            assertEquals(SensorValidity.WARMING_UP, result.validity)
        }
    }

    @Test fun hysteresisRetainsStateBetweenThresholds() {
        val processor = RotationProcessor(
            SensorConfig(rotationTimeConstantNanos = 1L, rotationWarmUpNanos = 1L, rotationMinSamples = 2),
        )
        processor.process(0, 0.0, 0.0, 0.0)
        assertEquals(RotationState.STILL, processor.process(step, 0.0, 0.0, 0.2).classification)
        assertEquals(RotationState.ROTATING, processor.process(2 * step, 0.0, 0.0, 0.5).classification)
        assertEquals(RotationState.ROTATING, processor.process(3 * step, 0.0, 0.0, 0.2).classification)
        assertEquals(RotationState.STILL, processor.process(4 * step, 0.0, 0.0, 0.1).classification)
    }

    @Test fun invalidAndNonIncreasingSamplesAreRejected() {
        val processor = RotationProcessor()
        processor.process(step, 0.0, 0.0, 0.0)
        for (time in listOf(-1L, 0L, step)) {
            assertEquals(SensorValidity.UNRELIABLE, processor.process(time, 0.0, 0.0, 0.0).validity)
        }
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(SensorValidity.UNRELIABLE, processor.process(2 * step, value, 0.0, 0.0).validity)
        }
    }

    @Test fun resetRequiresFreshWarmUp() {
        val processor = RotationProcessor()
        repeat(30) { processor.process(it * step, 0.0, 0.0, 0.0) }
        processor.reset()
        val result = processor.process(0, 0.0, 0.0, 2.0)
        assertEquals(RotationState.UNKNOWN, result.classification)
        assertEquals(SensorValidity.WARMING_UP, result.validity)
    }

    @Test fun invalidConfigurationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { SensorConfig(rotationExitThreshold = 1.0) }
        assertThrows(IllegalArgumentException::class.java) { SensorConfig(rotationMinSamples = 1) }
    }
}
