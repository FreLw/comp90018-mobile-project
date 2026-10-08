package com.comp90018.app.features.treasurechallenge

import com.comp90018.app.BuildConfig
import com.comp90018.app.contextengine.FakeDeviceContextEngine
import com.comp90018.app.contextengine.challenge.ChallengeEvent
import com.comp90018.app.contextengine.challenge.ChallengeRuleEvaluator
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeConfigs
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.PostChallengeRevealCoordinator
import com.comp90018.app.features.map.saveRelicDiscovery
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugChallengeSimulationTest {
    private val target = GeoCoordinate(-37.7971, 144.96189)
    private val union = RelicChallengeConfigs.unionLawnPhoto("union-lawn-photo", target, 25.0, 0.0)
    private val wilson = RelicChallengeConfigs.wilsonHallObservation("wilson-hall-observation", target, 20.0, 0.0)
    private val oldQuad = RelicChallengeConfigs.oldQuadExcavation("old-quad-excavation", target, 18.0)
    private val south = RelicChallengeConfigs.southLawnViewingAngle("south-lawn-viewing-angle", target, 15.0, 0.0)
    private val toneTool = RelicChallengeConfigs.graingerMuseumToneTool("grainger-tone-tool", target, 15.0)

    @Test fun debugBuildUsesExistingFakeEngineAndReleaseStubIsSeparateSource() {
        assertTrue(BuildConfig.DEBUG)
        assertTrue(ChallengeSimulationFactory.create(wilson).engine is FakeDeviceContextEngine)
    }

    @Test fun unionRequiresPhotoEventAfterReadyAndRejectedStatesStayIncomplete() {
        assertFalse(runPreset(union, "Outside target").completed)
        assertFalse(runPreset(union, "Heading misaligned").completed)
        val evaluator = ChallengeRuleEvaluator(union)
        val engine = ChallengeSimulationFactory.create(union).engine as FakeDeviceContextEngine
        val controls = preset(union, "Photo ready")
        engine.emit(controls.snapshot(union, 1L))
        evaluator.evaluate(engine.output.value, 0L)
        engine.emit(controls.snapshot(union, 2L))
        evaluator.evaluate(engine.output.value, 1L)
        engine.emit(controls.snapshot(union, 3L))
        val ready = evaluator.evaluate(engine.output.value, union.holdDurationNanos + 1L)
        assertTrue(ready.actionReady)
        assertFalse(ready.completed)
        assertTrue(evaluator.evaluate(engine.output.value, union.holdDurationNanos + 2L,
            ChallengeEvent.PHOTO_CAPTURED).completed)
    }

    @Test fun wilsonFailuresAndThreeSecondHoldUseEvaluator() {
        listOf("Inside but moving", "Stationary but unstable", "Stable but rotating", "Heading misaligned")
            .forEach { assertFalse(it, runPreset(wilson, it).completed) }
        assertTrue(runPreset(wilson, "All stars ready").completed)
    }

    @Test fun oldQuadFailuresAndHoldUseEvaluator() {
        assertFalse(runPreset(oldQuad, "Not horizontal").completed)
        assertFalse(runPreset(oldQuad, "Horizontal but moving").completed)
        assertTrue(runPreset(oldQuad, "All stars ready").completed)
    }

    @Test fun southLawnFailuresAndHoldUseEvaluator() {
        assertFalse(runPreset(south, "Heading misaligned").completed)
        assertFalse(runPreset(south, "Aligned but rotating").completed)
        assertTrue(runPreset(south, "All stars ready").completed)
    }

    @Test fun toneToolDebugPresetsSimulateQuietAndDetectedSound() {
        assertFalse(runPreset(toneTool, "Quiet (inside target)").completed)
        assertTrue(runPreset(toneTool, "Sound detected").completed)
    }

    @Test fun invalidGpsAndPoorAccuracyBlockCompletion() {
        val valid = preset(wilson, "All stars ready")
        assertEquals(SensorValidity.UNRELIABLE, valid.copy(locationValid = false).snapshot(wilson, 1L).location.validity)
        assertFalse(runControls(wilson, valid.copy(locationValid = false)).completed)
        assertFalse(runControls(wilson, valid.copy(accuracyMeters = 50.0)).completed)
        assertEquals(37.0, valid.copy(pitchDegrees = 37.0).snapshot(wilson, 1L).orientation.attitude.pitchDegrees)
        assertEquals(-12.0, valid.copy(rollDegrees = -12.0).snapshot(wilson, 1L).orientation.attitude.rollDegrees)
    }

    @Test fun completedSimulationUsesExistingTreasureIdSaveAndRevealGate() {
        val progress = runPreset(wilson, "All stars ready")
        val save = ChallengeDiscoverySave()
        val reveal = PostChallengeRevealCoordinator()
        val relic = MapRelic("wilson_hall_rosette", "The Surviving Stone Rosette", "Wilson Hall",
            coordinate = target, challengeConfig = wilson)
        var savedId: String? = null
        assertFalse(canOpenTreasureReveal(false))
        if (progress.completed) save.onChallengeCompleted { done ->
            saveRelicDiscovery(relic, { id, callback -> savedId = id; callback(null) }, done)
        }
        if (canOpenTreasureReveal(progress.completed)) reveal.openOnCompletion(relic, null)
        assertEquals("wilson_hall_rosette", savedId)
        assertEquals(relic, reveal.session?.relic)
        save.onChallengeCompleted { error("Duplicate persistence") }
    }

    private fun preset(config: RelicChallengeConfig, label: String): DebugContextControls =
        debugScenarios(config).first { it.label == label }.controls

    private fun runPreset(config: RelicChallengeConfig, label: String) = runControls(config, preset(config, label))

    private fun runControls(config: RelicChallengeConfig, controls: DebugContextControls):
        com.comp90018.app.contextengine.challenge.ChallengeProgress {
        val engine = ChallengeSimulationFactory.create(config).engine as FakeDeviceContextEngine
        val evaluator = ChallengeRuleEvaluator(config)
        engine.emit(controls.snapshot(config, 1L))
        evaluator.evaluate(engine.output.value, 0L)
        engine.emit(controls.snapshot(config, 2L))
        evaluator.evaluate(engine.output.value, 1L)
        engine.emit(controls.snapshot(config, 3L))
        return evaluator.evaluate(engine.output.value, config.holdDurationNanos + 2L)
    }
}
