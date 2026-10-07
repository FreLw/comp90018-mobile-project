package com.comp90018.app.features.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelicArrivalConfirmationTest {
    @Test
    fun defaultsCannotConfirmArrival() {
        assertFalse(RelicArrivalConfirmationState().arrivalConfirmed)
        assertEquals(0, updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), RelicArrivalSample()).consecutiveInsideFixes)
    }

    @Test
    fun firstInsideFixConfirmsOnlyAfterSecondDistinctFix() {
        val first = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), fix(100))
        assertEquals(1, first.consecutiveInsideFixes)
        assertFalse(first.arrivalConfirmed)
        assertEquals(RelicResonanceStage.CONFIRMING, confirmedRelicResonanceStage(fix(100), first))
        repeat(5) { assertEquals(first, updateRelicArrivalConfirmation(first, fix(100))) }
        val second = updateRelicArrivalConfirmation(first, fix(200))
        assertTrue(second.arrivalConfirmed)
        assertEquals(RelicResonanceStage.ARRIVED, confirmedRelicResonanceStage(fix(200), second))
        assertEquals(second, updateRelicArrivalConfirmation(second, fix(200)))
    }

    @Test
    fun outsideFixBreaksPendingConfirmationAndRepeatedRejectionIsStable() {
        val first = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), fix(100))
        val outside = fix(200, distance = 14.0)
        val reset = updateRelicArrivalConfirmation(first, outside)
        assertEquals(0, reset.consecutiveInsideFixes)
        assertEquals(reset, updateRelicArrivalConfirmation(reset, outside))
        val restarted = updateRelicArrivalConfirmation(reset, fix(300))
        assertEquals(1, restarted.consecutiveInsideFixes)
        assertFalse(restarted.arrivalConfirmed)
    }

    @Test
    fun entryAndAccuracyBoundariesAreInclusiveAndNullAccuracyIsAccepted() {
        listOf(10.0, 0.0, null).forEach { accuracy ->
            val sample = fix(100, distance = 10.0, accuracy = accuracy)
            val first = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), sample)
            assertEquals(1, first.consecutiveInsideFixes)
            assertTrue(updateRelicArrivalConfirmation(first, sample.copy(timestampNanos = 200)).arrivalConfirmed)
        }
        assertEquals(0, updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), fix(100, distance = 10.001)).consecutiveInsideFixes)
    }

    @Test
    fun invalidEvidenceNeverConfirmsAndResetsPendingOrConfirmedArrival() {
        val pending = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), fix(100))
        val invalid = listOf(
            fix(300, distance = 7.0, accuracy = 20.0),
            fix(300, accuracy = 10.001),
            fix(300, accuracy = -1.0),
            fix(300, accuracy = Double.NaN),
            fix(300, accuracy = Double.POSITIVE_INFINITY),
            fix(300, accuracy = Double.NEGATIVE_INFINITY),
            fix(300, distance = null),
            fix(300, distance = Double.NaN),
            fix(300, distance = Double.POSITIVE_INFINITY),
            fix(300, distance = Double.NEGATIVE_INFINITY),
            fix(300, distance = -1.0),
            fix(300).copy(hasActionableLocation = false),
            fix(null),
        )
        invalid.forEach { sample ->
            listOf(RelicArrivalConfirmationState(), pending, arrived()).forEach { previous ->
                val reset = updateRelicArrivalConfirmation(previous, sample)
                assertEquals("Sample: $sample", 0, reset.consecutiveInsideFixes)
                assertFalse(reset.arrivalConfirmed)
                assertEquals(reset, updateRelicArrivalConfirmation(reset, sample))
            }
        }
    }

    @Test
    fun confirmedArrivalUsesExitRadiusAndRequiresTwoFreshFixesAfterExit() {
        val at12 = updateRelicArrivalConfirmation(arrived(), fix(300, distance = 12.0))
        assertTrue(at12.arrivalConfirmed)
        assertEquals(RelicResonanceStage.ARRIVED, confirmedRelicResonanceStage(fix(300, distance = 12.0), at12))
        val at15 = updateRelicArrivalConfirmation(at12, fix(400, distance = 15.0))
        assertTrue(at15.arrivalConfirmed)
        val outside = updateRelicArrivalConfirmation(at15, fix(500, distance = 15.1))
        assertEquals(0, outside.consecutiveInsideFixes)
        assertFalse(outside.arrivalConfirmed)
        val first = updateRelicArrivalConfirmation(outside, fix(600))
        assertFalse(first.arrivalConfirmed)
        assertEquals(1, first.consecutiveInsideFixes)
        assertTrue(updateRelicArrivalConfirmation(first, fix(700)).arrivalConfirmed)
    }

    @Test
    fun olderOrReplayedFixCannotCountAfterNewerFixOrReset() {
        val first = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), fix(200))
        assertEquals(first, updateRelicArrivalConfirmation(first, fix(100)))
        val reset = updateRelicArrivalConfirmation(first, fix(300, distance = 14.0))
        assertEquals(reset, updateRelicArrivalConfirmation(reset, fix(200)))
        assertEquals(reset, updateRelicArrivalConfirmation(reset, fix(300)))
    }

    @Test
    fun lossOfActionabilityOnSameFixClearsArrivalWithoutReusingItsTimestamp() {
        val confirmed = arrived()
        val reset = updateRelicArrivalConfirmation(confirmed, fix(200).copy(hasActionableLocation = false))
        assertFalse(reset.arrivalConfirmed)
        assertEquals(200L, reset.lastProcessedTimestampNanos)
        assertEquals(reset, updateRelicArrivalConfirmation(reset, fix(200)))
        assertEquals(1, updateRelicArrivalConfirmation(reset, fix(300)).consecutiveInsideFixes)
    }

    @Test
    fun stageMappingPreservesOrdinaryDistanceClassificationAndAcquiring() {
        listOf(
            300.0 to RelicResonanceStage.DORMANT,
            150.0 to RelicResonanceStage.FAINT,
            80.0 to RelicResonanceStage.DRAWN,
            30.0 to RelicResonanceStage.STRONG,
            10.0 to RelicResonanceStage.STRONG,
        ).forEach { (distance, expected) ->
            assertEquals(expected, confirmedRelicResonanceStage(fix(100, distance), RelicArrivalConfirmationState()))
        }
        assertEquals(RelicResonanceStage.ACQUIRING,
            confirmedRelicResonanceStage(fix(300).copy(hasActionableLocation = false), arrived()))
        assertEquals(RelicResonanceStage.ACQUIRING,
            confirmedRelicResonanceStage(fix(300, distance = null), arrived()))
    }

    private fun arrived(): RelicArrivalConfirmationState {
        val first = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), fix(100))
        return updateRelicArrivalConfirmation(first, fix(200))
    }

    private fun fix(timestamp: Long?, distance: Double? = 8.0, accuracy: Double? = 5.0) =
        RelicArrivalSample(true, distance, accuracy, timestamp)
}
