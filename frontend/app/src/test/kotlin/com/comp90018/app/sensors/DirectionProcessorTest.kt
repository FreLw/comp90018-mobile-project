package com.comp90018.app.sensors

import org.junit.Assert.*
import org.junit.Test

class DirectionProcessorTest {
    @Test fun normalization() {
        assertEquals(350.0, DirectionProcessor.normalize(-10.0), 0.0)
        assertEquals(10.0, DirectionProcessor.normalize(370.0), 0.0)
        assertEquals(0.0, DirectionProcessor.normalize(360.0), 0.0)
        assertThrows(IllegalArgumentException::class.java) { DirectionProcessor.normalize(Double.NaN) }
    }

    @Test fun circularSmoothingCrossesNorthInBothDirections() {
        for ((start, end) in listOf(359.0 to 1.0, 1.0 to 359.0)) {
            val processor = DirectionProcessor()
            processor.process(0, start)
            val heading = processor.process(200_000_000L, end).headingDegrees!!
            assertTrue(heading < 2.0 || heading > 358.0)
            assertTrue(kotlin.math.abs(DirectionProcessor.angularDifference(heading, end)) < 2.0)
        }
    }

    @Test fun signedErrorsGiveExpectedTurns() {
        val right = DirectionProcessor.evaluate(350.0, 10.0)
        assertEquals(20.0, right.angularErrorDegrees!!, 0.0)
        assertEquals(TurnDirection.TURN_RIGHT, right.turn)
        val left = DirectionProcessor.evaluate(10.0, 350.0)
        assertEquals(-20.0, left.angularErrorDegrees!!, 0.0)
        assertEquals(TurnDirection.TURN_LEFT, left.turn)
    }

    @Test fun toleranceIsInclusiveAndCallerControlled() {
        for (sign in listOf(-1, 1)) {
            for (error in listOf(4.99, 5.0)) {
                val result = DirectionProcessor.evaluate(0.0, sign * error, 5.0)
                assertEquals(DirectionAlignment.ALIGNED, result.alignment)
                assertEquals(TurnDirection.NONE, result.turn)
            }
            assertEquals(DirectionAlignment.MISALIGNED, DirectionProcessor.evaluate(0.0, sign * 5.01, 5.0).alignment)
        }
        assertEquals(DirectionAlignment.ALIGNED, DirectionProcessor.evaluate(0.0, 0.0, 0.0).alignment)
    }

    @Test fun oppositeBearingAlwaysUsesNegative180() {
        for ((heading, target) in listOf(0.0 to 180.0, 180.0 to 0.0, 10.0 to 190.0)) {
            val result = DirectionProcessor.evaluate(heading, target)
            assertEquals(-180.0, result.angularErrorDegrees!!, 0.0)
            assertEquals(TurnDirection.TURN_LEFT, result.turn)
        }
        assertEquals(DirectionAlignment.ALIGNED, DirectionProcessor.evaluate(0.0, 180.0, 180.0).alignment)
    }

    @Test fun missingAndInvalidInputsHaveSeparateValidity() {
        assertEquals(SensorValidity.UNKNOWN, DirectionProcessor.evaluate(null, 10.0).headingValidity)
        val missingTarget = DirectionProcessor.evaluate(10.0, null)
        assertEquals(SensorValidity.VALID, missingTarget.headingValidity)
        assertEquals(DirectionAlignment.UNKNOWN, missingTarget.alignment)
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(SensorValidity.UNRELIABLE, DirectionProcessor.evaluate(value, 10.0).headingValidity)
            assertEquals(SensorValidity.UNRELIABLE, DirectionProcessor.evaluate(10.0, value).comparisonValidity)
        }
        for (value in listOf(-1.0, 181.0, Double.NaN)) {
            assertEquals(SensorValidity.UNRELIABLE, DirectionProcessor.evaluate(0.0, 10.0, value).comparisonValidity)
        }
    }

    @Test fun timestampsResetAndGapHandling() {
        val processor = DirectionProcessor()
        processor.process(10, 90.0)
        assertEquals(SensorValidity.UNRELIABLE, processor.process(10, 180.0).headingValidity)
        assertEquals(SensorValidity.UNRELIABLE, processor.process(-1, 180.0).headingValidity)
        assertNull(processor.process(20, null).headingDegrees)
        assertEquals(180.0, processor.process(3_000_000_000L, 180.0).headingDegrees!!, 0.0)
        processor.reset()
        assertEquals(45.0, processor.process(0, 45.0).headingDegrees!!, 0.0)
    }
}
