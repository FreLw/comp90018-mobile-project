package com.comp90018.app.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class AttitudeProcessorTest {
    @Test fun levelPhoneIsHorizontal() {
        val result = AttitudeProcessor().process(10L, 0.0, 0.0)

        assertEquals(HorizontalState.HORIZONTAL, result.horizontalState)
        assertEquals(SensorValidity.VALID, result.validity)
        assertEquals(0.0, result.pitchDegrees!!, 0.0)
        assertEquals(0.0, result.rollDegrees!!, 0.0)
        assertEquals(10L, result.timestampNanos)
    }

    @Test fun pitchOutsideToleranceIsNotHorizontal() {
        val result = AttitudeProcessor().process(0L, 12.01, 0.0)

        assertEquals(HorizontalState.NOT_HORIZONTAL, result.horizontalState)
        assertEquals(SensorValidity.VALID, result.validity)
    }

    @Test fun rollOutsideToleranceIsNotHorizontal() {
        val result = AttitudeProcessor().process(0L, 0.0, -12.01)

        assertEquals(HorizontalState.NOT_HORIZONTAL, result.horizontalState)
        assertEquals(SensorValidity.VALID, result.validity)
    }

    @Test fun exactConfiguredToleranceIsHorizontal() {
        val processor = AttitudeProcessor(SensorConfig(horizontalToleranceDegrees = 7.5))

        assertEquals(HorizontalState.HORIZONTAL, processor.process(0L, 7.5, -7.5).horizontalState)
    }

    @Test fun invalidInputsAreUnreliableAndDoNotAffectLaterResults() {
        val processor = AttitudeProcessor()
        for ((pitch, roll) in listOf(
            Double.NaN to 0.0,
            Double.POSITIVE_INFINITY to 0.0,
            0.0 to Double.NEGATIVE_INFINITY,
        )) {
            val result = processor.process(0L, pitch, roll)
            assertEquals(HorizontalState.UNKNOWN, result.horizontalState)
            assertEquals(SensorValidity.UNRELIABLE, result.validity)
            assertNull(result.pitchDegrees)
            assertNull(result.rollDegrees)
            assertNull(result.timestampNanos)
        }
        assertEquals(SensorValidity.UNRELIABLE, processor.process(-1L, 0.0, 0.0).validity)

        val valid = processor.process(1L, 0.0, 0.0)
        assertEquals(HorizontalState.HORIZONTAL, valid.horizontalState)
        assertEquals(SensorValidity.VALID, valid.validity)
    }

    @Test fun invalidToleranceIsRejected() {
        for (tolerance in listOf(-0.01, 90.01, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) {
                SensorConfig(horizontalToleranceDegrees = tolerance)
            }
        }
    }
}
