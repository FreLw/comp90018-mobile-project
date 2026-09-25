package com.comp90018.app.contextengine.challenge

import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.sensors.AttitudeOutput
import com.comp90018.app.sensors.DirectionOutput
import com.comp90018.app.sensors.HorizontalState
import com.comp90018.app.sensors.MotionOutput
import com.comp90018.app.sensors.MotionStabilityOutput
import com.comp90018.app.sensors.MotionState
import com.comp90018.app.sensors.RotationOutput
import com.comp90018.app.sensors.RotationState
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.StabilityOutput
import com.comp90018.app.sensors.StabilityState
import com.comp90018.app.sensors.audio.SoundLevelOutput
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.orientation.OrientationOutput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengeRuleEvaluatorTest {
    private val target = GeoCoordinate(0.0, 0.0)
    private val radiusMeters = 20.0

    @Test fun initialConfigsContainTheFourRequiredRuleSets() {
        val union = unionConfig()
        assertEquals(12.0, union.headingToleranceDegrees, 0.0)
        assertEquals(800_000_000L, union.holdDurationNanos)
        assertTrue(union.photoActionRequired)

        val wilson = wilsonConfig()
        assertEquals(10.0, wilson.headingToleranceDegrees, 0.0)
        assertEquals(3_000_000_000L, wilson.holdDurationNanos)
        assertTrue(wilson.requiresStationary && wilson.requiresStability && wilson.requiresRotationStill)

        val oldQuad = oldQuadConfig()
        assertEquals(null, oldQuad.requiredHeadingDegrees)
        assertTrue(oldQuad.requiresHorizontal)

        val south = southConfig()
        assertEquals(8.0, south.headingToleranceDegrees, 0.0)
        assertEquals(1_200_000_000L, south.holdDurationNanos)
    }

    @Test fun unionOutsideCannotActivate() {
        val result = ChallengeRuleEvaluator(unionConfig()).evaluate(snapshot(inside = false), 0L)

        assertEquals(ChallengeInstruction.MOVE_CLOSER, result.instruction)
        assertFalse(result.condition(ChallengeCondition.LOCATION_INSIDE))
        assertEquals(0.0, result.holdProgress, 0.0)
        assertFalse(result.actionReady)
        assertFalse(result.completed)
    }

    @Test fun unionMisalignmentGivesTurnDirection() {
        val result = ChallengeRuleEvaluator(unionConfig()).evaluate(snapshot(headingDegrees = 340.0), 0L)

        assertEquals(ChallengeInstruction.TURN_RIGHT, result.instruction)
        assertFalse(result.condition(ChallengeCondition.HEADING_ALIGNED))
        assertEquals(0.0, result.holdProgress, 0.0)
    }

    @Test fun unionAlignedFor800MillisBecomesActionReadyButNotComplete() {
        val evaluator = ChallengeRuleEvaluator(unionConfig())
        evaluator.evaluate(snapshot(), 0L)
        val almostReady = evaluator.evaluate(snapshot(), 799_000_000L)
        val ready = evaluator.evaluate(snapshot(), 800_000_000L)

        assertFalse(almostReady.actionReady)
        assertTrue(ready.actionReady)
        assertFalse(ready.completed)
        assertEquals(1.0, ready.holdProgress, 0.0)
        assertEquals(ChallengeInstruction.TAKE_PHOTO, ready.instruction)
    }

    @Test fun unionPhotoEventCompletesOnlyAfterActionIsReady() {
        val evaluator = ChallengeRuleEvaluator(unionConfig())
        val earlyCapture = evaluator.evaluate(snapshot(), 0L, ChallengeEvent.PHOTO_CAPTURED)
        assertFalse(earlyCapture.completed)

        val ready = evaluator.evaluate(snapshot(), 800_000_000L)
        assertTrue(ready.actionReady)
        assertFalse(ready.completed)

        val captured = evaluator.evaluate(snapshot(), 800_000_000L, ChallengeEvent.PHOTO_CAPTURED)
        assertTrue(captured.completed)
        assertFalse(captured.actionReady)
        assertEquals(ChallengeInstruction.COMPLETED, captured.instruction)
    }

    @Test fun wilsonAllConditionsHeldForThreeSecondsCompletes() {
        val evaluator = ChallengeRuleEvaluator(wilsonConfig())
        evaluator.evaluate(snapshot(), 0L)
        val completed = evaluator.evaluate(snapshot(), 3_000_000_000L)

        assertTrue(completed.completed)
        assertEquals(ChallengeInstruction.COMPLETED, completed.instruction)
    }

    @Test fun wilsonMovementResetsTimer() {
        val evaluator = ChallengeRuleEvaluator(wilsonConfig())
        evaluator.evaluate(snapshot(), 0L)
        assertTrue(evaluator.evaluate(snapshot(), 2_000_000_000L).holdProgress > 0.0)

        val moving = evaluator.evaluate(snapshot(stationary = false), 2_500_000_000L)
        assertEquals(ChallengeInstruction.STOP_MOVING, moving.instruction)
        assertEquals(0.0, moving.holdProgress, 0.0)

        assertEquals(0.0, evaluator.evaluate(snapshot(), 3_000_000_000L).holdProgress, 0.0)
        assertTrue(evaluator.evaluate(snapshot(), 6_000_000_000L).completed)
    }

    @Test fun wilsonDirectionLossResetsTimer() {
        val evaluator = ChallengeRuleEvaluator(wilsonConfig())
        evaluator.evaluate(snapshot(), 0L)
        evaluator.evaluate(snapshot(), 1_000_000_000L)

        val directionLost = evaluator.evaluate(snapshot(headingDegrees = 30.0), 1_500_000_000L)
        assertEquals(ChallengeInstruction.TURN_LEFT, directionLost.instruction)
        assertEquals(0.0, directionLost.holdProgress, 0.0)

        evaluator.evaluate(snapshot(), 2_000_000_000L)
        assertFalse(evaluator.evaluate(snapshot(), 4_999_999_999L).completed)
        assertTrue(evaluator.evaluate(snapshot(), 5_000_000_000L).completed)
    }

    @Test fun wilsonGyroscopeRotationResetsTimer() {
        val evaluator = ChallengeRuleEvaluator(wilsonConfig())
        evaluator.evaluate(snapshot(), 0L)
        evaluator.evaluate(snapshot(), 2_000_000_000L)

        val rotating = evaluator.evaluate(snapshot(rotationStill = false), 2_100_000_000L)
        assertEquals(ChallengeInstruction.HOLD_STILL, rotating.instruction)
        assertEquals(0.0, rotating.holdProgress, 0.0)

        evaluator.evaluate(snapshot(), 3_000_000_000L)
        assertTrue(evaluator.evaluate(snapshot(), 6_000_000_000L).completed)
    }

    @Test fun oldQuadNonHorizontalCannotProgress() {
        val result = ChallengeRuleEvaluator(oldQuadConfig()).evaluate(snapshot(horizontal = false), 0L)

        assertEquals(ChallengeInstruction.KEEP_PHONE_LEVEL, result.instruction)
        assertEquals(0.0, result.holdProgress, 0.0)
        assertFalse(result.condition(ChallengeCondition.PHONE_HORIZONTAL))
    }

    @Test fun oldQuadHorizontalButMovingCannotProgress() {
        val result = ChallengeRuleEvaluator(oldQuadConfig()).evaluate(snapshot(stationary = false), 0L)

        assertEquals(ChallengeInstruction.STOP_MOVING, result.instruction)
        assertEquals(0.0, result.holdProgress, 0.0)
    }

    @Test fun oldQuadAllConditionsHeldForThreeSecondsCompletes() {
        val evaluator = ChallengeRuleEvaluator(oldQuadConfig())
        evaluator.evaluate(snapshot(), 0L)

        val completed = evaluator.evaluate(snapshot(), 3_000_000_000L)
        assertTrue(completed.completed)
        assertEquals(1.0, completed.holdProgress, 0.0)
    }

    @Test fun southLawnAngularErrorProducesLeftAndRightInstructions() {
        val left = ChallengeRuleEvaluator(southConfig()).evaluate(snapshot(headingDegrees = 20.0), 0L)
        val right = ChallengeRuleEvaluator(southConfig()).evaluate(snapshot(headingDegrees = 340.0), 0L)

        assertEquals(ChallengeInstruction.TURN_LEFT, left.instruction)
        assertEquals(ChallengeInstruction.TURN_RIGHT, right.instruction)
    }

    @Test fun southLawnAlignedButRotatingDoesNotComplete() {
        val evaluator = ChallengeRuleEvaluator(southConfig())
        val rotating = evaluator.evaluate(snapshot(rotationStill = false), 0L)
        val stillStartsTimer = evaluator.evaluate(snapshot(), 2_000_000_000L)

        assertEquals(ChallengeInstruction.HOLD_STILL, rotating.instruction)
        assertEquals(0.0, rotating.holdProgress, 0.0)
        assertFalse(stillStartsTimer.completed)
        assertEquals(0.0, stillStartsTimer.holdProgress, 0.0)
    }

    @Test fun southLawnAlignedStableAndStillFor1200MillisCompletes() {
        val evaluator = ChallengeRuleEvaluator(southConfig())
        evaluator.evaluate(snapshot(), 0L)
        val completed = evaluator.evaluate(snapshot(), 1_200_000_000L)

        assertTrue(completed.completed)
        assertEquals(ChallengeInstruction.COMPLETED, completed.instruction)
    }

    @Test fun systemGardenUsesTheSameRuleSetAsOldQuad() {
        val systemGarden = systemGardenConfig()
        assertEquals(null, systemGarden.requiredHeadingDegrees)
        assertTrue(
            systemGarden.requiresStationary && systemGarden.requiresStability &&
                systemGarden.requiresRotationStill && systemGarden.requiresHorizontal,
        )
        assertEquals(3_000_000_000L, systemGarden.holdDurationNanos)
        assertFalse(systemGarden.requiresSound)
    }

    @Test fun systemGardenNonHorizontalCannotProgress() {
        val result = ChallengeRuleEvaluator(systemGardenConfig()).evaluate(snapshot(horizontal = false), 0L)

        assertEquals(ChallengeInstruction.KEEP_PHONE_LEVEL, result.instruction)
        assertEquals(0.0, result.holdProgress, 0.0)
    }

    @Test fun systemGardenAllConditionsHeldForThreeSecondsCompletes() {
        val evaluator = ChallengeRuleEvaluator(systemGardenConfig())
        evaluator.evaluate(snapshot(), 0L)

        val completed = evaluator.evaluate(snapshot(), 3_000_000_000L)
        assertTrue(completed.completed)
        assertEquals(1.0, completed.holdProgress, 0.0)
    }

    @Test fun graingerConfigRequiresASoundThreshold() {
        val grainger = graingerConfig()
        assertTrue(grainger.requiresSound)
        assertEquals(-30.0, grainger.soundThresholdDecibels!!, 0.0)
        assertEquals(1_000_000_000L, grainger.holdDurationNanos)
    }

    @Test fun graingerOutsideCannotActivate() {
        val result = ChallengeRuleEvaluator(graingerConfig()).evaluate(snapshot(inside = false), 0L)

        assertEquals(ChallengeInstruction.MOVE_CLOSER, result.instruction)
        assertFalse(result.condition(ChallengeCondition.LOCATION_INSIDE))
        assertFalse(result.completed)
    }

    @Test fun graingerSilenceAsksForSound() {
        val result = ChallengeRuleEvaluator(graingerConfig()).evaluate(snapshot(soundDecibels = -60.0), 0L)

        assertEquals(ChallengeInstruction.MAKE_SOUND, result.instruction)
        assertFalse(result.condition(ChallengeCondition.SOUND_DETECTED))
        assertFalse(result.completed)
    }

    @Test fun graingerBriefLoudSoundDoesNotCompleteYet() {
        val result = ChallengeRuleEvaluator(graingerConfig()).evaluate(snapshot(soundDecibels = -10.0), 0L)

        assertTrue(result.condition(ChallengeCondition.SOUND_DETECTED))
        assertFalse(result.completed)
        assertEquals(0.0, result.holdProgress, 0.0)
        assertEquals(ChallengeInstruction.HOLD_TONE, result.instruction)
    }

    @Test fun graingerLoudSoundHeldForOneSecondCompletes() {
        val evaluator = ChallengeRuleEvaluator(graingerConfig())
        evaluator.evaluate(snapshot(soundDecibels = -10.0), 0L)

        val completed = evaluator.evaluate(snapshot(soundDecibels = -10.0), 1_000_000_000L)
        assertTrue(completed.completed)
        assertEquals(ChallengeInstruction.COMPLETED, completed.instruction)
    }

    @Test fun graingerSoundDroppingBelowThresholdResetsTheHoldTimer() {
        val evaluator = ChallengeRuleEvaluator(graingerConfig())
        evaluator.evaluate(snapshot(soundDecibels = -10.0), 0L)
        assertTrue(evaluator.evaluate(snapshot(soundDecibels = -10.0), 500_000_000L).holdProgress > 0.0)

        val silence = evaluator.evaluate(snapshot(soundDecibels = -60.0), 600_000_000L)
        assertEquals(ChallengeInstruction.MAKE_SOUND, silence.instruction)
        assertEquals(0.0, silence.holdProgress, 0.0)

        evaluator.evaluate(snapshot(soundDecibels = -10.0), 700_000_000L)
        assertFalse(evaluator.evaluate(snapshot(soundDecibels = -10.0), 1_699_999_999L).completed)
        assertTrue(evaluator.evaluate(snapshot(soundDecibels = -10.0), 1_700_000_000L).completed)
    }

    private fun unionConfig() = RelicChallengeConfigs.unionLawnPhoto(
        challengeId = "union-test",
        targetLocation = target,
        insideRadiusMeters = radiusMeters,
        requiredHeadingDegrees = 0.0,
    )

    private fun wilsonConfig() = RelicChallengeConfigs.wilsonHallObservation(
        challengeId = "wilson-test",
        targetLocation = target,
        insideRadiusMeters = radiusMeters,
        requiredHeadingDegrees = 0.0,
    )

    private fun oldQuadConfig() = RelicChallengeConfigs.oldQuadExcavation(
        challengeId = "old-quad-test",
        targetLocation = target,
        insideRadiusMeters = radiusMeters,
    )

    private fun southConfig() = RelicChallengeConfigs.southLawnViewingAngle(
        challengeId = "south-test",
        targetLocation = target,
        insideRadiusMeters = radiusMeters,
        requiredHeadingDegrees = 0.0,
    )

    private fun systemGardenConfig() = RelicChallengeConfigs.systemGardenGlasshouse(
        challengeId = "system-garden-test",
        targetLocation = target,
        insideRadiusMeters = radiusMeters,
    )

    private fun graingerConfig() = RelicChallengeConfigs.graingerMuseumToneTool(
        challengeId = "grainger-test",
        targetLocation = target,
        insideRadiusMeters = radiusMeters,
    )

    private fun snapshot(
        inside: Boolean = true,
        headingDegrees: Double = 0.0,
        horizontal: Boolean = true,
        stationary: Boolean = true,
        stable: Boolean = true,
        rotationStill: Boolean = true,
        soundDecibels: Double? = null,
    ) = DeviceContextSnapshot(
        location = LocationOutput(
            currentLocation = if (inside) target else GeoCoordinate(0.0, 0.001),
            validity = SensorValidity.VALID,
        ),
        orientation = OrientationOutput(
            direction = DirectionOutput(
                headingDegrees = headingDegrees,
                headingValidity = SensorValidity.VALID,
            ),
            rotation = RotationOutput(
                classification = if (rotationStill) RotationState.STILL else RotationState.ROTATING,
                validity = SensorValidity.VALID,
            ),
            attitude = AttitudeOutput(
                horizontalState = if (horizontal) HorizontalState.HORIZONTAL else HorizontalState.NOT_HORIZONTAL,
                validity = SensorValidity.VALID,
            ),
        ),
        motionStability = MotionStabilityOutput(
            motion = MotionOutput(
                classification = if (stationary) MotionState.STATIONARY else MotionState.MOVING,
                validity = SensorValidity.VALID,
            ),
            stability = StabilityOutput(
                classification = if (stable) StabilityState.STABLE else StabilityState.UNSTABLE,
                validity = SensorValidity.VALID,
            ),
        ),
        sound = if (soundDecibels == null) {
            SoundLevelOutput()
        } else {
            SoundLevelOutput(decibels = soundDecibels, validity = SensorValidity.VALID)
        },
    )

    private fun ChallengeProgress.condition(condition: ChallengeCondition): Boolean =
        requiredConditions.single { it.condition == condition }.satisfied
}
