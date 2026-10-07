package com.comp90018.app.features.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResonanceVisualStyleTest {
    @Test
    fun representativeDistancesIncreaseArcPetalsAndThreadEmphasis() {
        val distances = listOf(120.0, 80.0, 32.0, 17.0)
        val stages = listOf(RelicResonanceStage.FAINT, RelicResonanceStage.DRAWN,
            RelicResonanceStage.DRAWN, RelicResonanceStage.STRONG)
        val progress = distances.map(::navigationEnergyProgress)
        distances.forEachIndexed { index, distance ->
            assertEquals(stages[index], relicResonanceStage(distance, true))
        }
        progress.zipWithNext().forEach { (fainter, stronger) ->
            val a = guidingThreadStyle(fainter)
            val b = guidingThreadStyle(stronger)
            assertTrue(a.lineAlpha < b.lineAlpha)
            assertTrue(a.lineWidthPixels < b.lineWidthPixels)
            assertTrue(a.glintAlpha < b.glintAlpha)
            assertTrue(a.glintRadiusMeters < b.glintRadiusMeters)
        }
        val petals = progress.mapIndexed { index, value -> rosetteActivation(value, stages[index]).activePetals }
        assertEquals(listOf(2, 5, 6, 7), petals)
    }

    @Test
    fun boundaryVisualsAreQuietAtRangeAndFullAtEntry() {
        listOf(150.0 to 0f, 80.0 to 0.5f, 30.0 to (120f / 140f), 10.0 to 1f)
            .forEach { (distance, expected) ->
                val progress = navigationEnergyProgress(distance)
                assertEquals(expected, progress, 0.00001f)
                assertEquals(expected, guidingThreadStyle(progress).lineAlpha, 0.00001f)
            }
        assertEquals(0, rosetteActivation(0f, RelicResonanceStage.DORMANT).activePetals)
        assertEquals(8, rosetteActivation(1f, RelicResonanceStage.CONFIRMING).activePetals)
        assertEquals(8, rosetteActivation(1f, RelicResonanceStage.ARRIVED).activePetals)
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f).forEach {
            assertEquals(0f, guidingThreadStyle(it).lineAlpha, 0f)
        }
    }

    @Test
    fun directionChangesSpeedWithoutOverwritingDistanceStrengthOrHuntReadiness() {
        listOf(120.0, 17.0).forEach { distance ->
            val sample = RelicArrivalSample(true, distance, 5.0, 100)
            val evidence = updateRelicArrivalConfirmation(RelicArrivalConfirmationState(), sample)
            val aligned = deriveRelicNavigationUiState(sample, evidence, 0.0, 0.0)
            val opposite = deriveRelicNavigationUiState(sample, evidence, 0.0, 180.0)
            assertEquals(aligned.resonanceProgress, opposite.resonanceProgress, 0f)
            assertEquals(guidingThreadStyle(aligned.resonanceProgress), guidingThreadStyle(opposite.resonanceProgress))
            assertTrue(navigationTrailDurationMillis(0.0, 0.0) < navigationTrailDurationMillis(0.0, 180.0))
            assertFalse(aligned.canBeginHunt)
            assertFalse(opposite.canBeginHunt)
        }
    }
}
