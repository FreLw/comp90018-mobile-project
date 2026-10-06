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
        val evaluator = ChallengeRuleEvaluator(unionConfig())
        evaluator.evaluate(snapshot(headingDegrees = 340.0), 0L)
        val result = evaluator.evaluate(snapshot(headingDegrees = 340.0), 0L)

        assertEquals(ChallengeInstruction.TURN_RIGHT, result.instruction)
        assertFalse(result.condition(ChallengeCondition.HEADING_ALIGNED))
        assertEquals(0.0, result.holdProgress, 0.0)
    }

    @Test fun unionAlignedFor800MillisBecomesActionReadyButNotComplete() {
        val evaluator = ChallengeRuleEvaluator(unionConfig())
        evaluator.evaluate(snapshot(), 0L)
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
        evaluator.evaluate(snapshot(), 0L)
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
        evaluator.evaluate(snapshot(), 0L)
        val completed = evaluator.evaluate(snapshot(), 3_000_000_000L)

        assertTrue(completed.completed)
        assertEquals(ChallengeInstruction.COMPLETED, completed.instruction)
    }

    @Test fun wilsonMovementResetsTimer() {
        val evaluator = ChallengeRuleEvaluator(wilsonConfig())
        evaluator.evaluate(snapshot(), 0L)
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
        evaluator.evaluate(snapshot(), 0L)
        evaluator.evaluate(snapshot(), 2_000_000_000L)

        val rotating = evaluator.evaluate(snapshot(rotationStill = false), 2_100_000_000L)
        assertEquals(ChallengeInstruction.HOLD_STILL, rotating.instruction)
        assertEquals(0.0, rotating.holdProgress, 0.0)

        evaluator.evaluate(snapshot(), 3_000_000_000L)
        assertTrue(evaluator.evaluate(snapshot(), 6_000_000_000L).completed)
    }

    @Test fun oldQuadNonHorizontalCannotProgress() {
        val evaluator = ChallengeRuleEvaluator(oldQuadConfig())
        evaluator.evaluate(snapshot(horizontal = false), 0L)
        val result = evaluator.evaluate(snapshot(horizontal = false), 0L)

        assertEquals(ChallengeInstruction.KEEP_PHONE_LEVEL, result.instruction)
        assertEquals(0.0, result.holdProgress, 0.0)
        assertFalse(result.condition(ChallengeCondition.PHONE_HORIZONTAL))
    }

    @Test fun oldQuadHorizontalButMovingCannotProgress() {
        val evaluator = ChallengeRuleEvaluator(oldQuadConfig())
        evaluator.evaluate(snapshot(stationary = false), 0L)
        val result = evaluator.evaluate(snapshot(stationary = false), 0L)

        assertEquals(ChallengeInstruction.STOP_MOVING, result.instruction)
        assertEquals(0.0, result.holdProgress, 0.0)
    }

    @Test fun oldQuadAllConditionsHeldForThreeSecondsCompletes() {
        val evaluator = ChallengeRuleEvaluator(oldQuadConfig())
        evaluator.evaluate(snapshot(), 0L)
        evaluator.evaluate(snapshot(), 0L)

        val completed = evaluator.evaluate(snapshot(), 3_000_000_000L)
        assertTrue(completed.completed)
        assertEquals(1.0, completed.holdProgress, 0.0)
    }

    @Test fun southLawnAngularErrorProducesLeftAndRightInstructions() {
        val leftEvaluator = ChallengeRuleEvaluator(southConfig())
        leftEvaluator.evaluate(snapshot(headingDegrees = 20.0), 0L)
        val left = leftEvaluator.evaluate(snapshot(headingDegrees = 20.0), 0L)

        val rightEvaluator = ChallengeRuleEvaluator(southConfig())
        rightEvaluator.evaluate(snapshot(headingDegrees = 340.0), 0L)
        val right = rightEvaluator.evaluate(snapshot(headingDegrees = 340.0), 0L)

        assertEquals(ChallengeInstruction.TURN_LEFT, left.instruction)
        assertEquals(ChallengeInstruction.TURN_RIGHT, right.instruction)
    }

    @Test fun southLawnAlignedButRotatingDoesNotComplete() {
        val evaluator = ChallengeRuleEvaluator(southConfig())
        evaluator.evaluate(snapshot(rotationStill = false), 0L)
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
        evaluator.evaluate(snapshot(), 0L)
        val completed = evaluator.evaluate(snapshot(), 1_200_000_000L)

        assertTrue(completed.completed)
        assertEquals(ChallengeInstruction.COMPLETED, completed.instruction)
    }

    @Test fun systemGardenRequiresPhotoWithoutMotionOrHeading() {
        val config = systemGardenConfig()
        assertEquals(null, config.requiredHeadingDegrees)
        assertFalse(config.requiresHorizontal || config.requiresStationary || config.requiresStability || config.requiresRotationStill || config.requiresSound)
        assertTrue(config.photoActionRequired)
        assertEquals(0L, config.holdDurationNanos)
        val evaluator = ChallengeRuleEvaluator(config)
        evaluator.evaluate(snapshot(horizontal = false, stationary = false, stable = false), 0L)
        val ready = evaluator.evaluate(snapshot(horizontal = false, stationary = false, stable = false), 1L)
        assertTrue(ready.actionReady)
        assertFalse(ready.completed)
        assertEquals(listOf(ChallengeCondition.LOCATION_INSIDE), ready.requiredConditions.map { it.condition })
        assertFalse(evaluator.evaluate(snapshot(), 3_000_000_000L).completed)
        assertTrue(evaluator.evaluate(snapshot(), 3_000_000_001L, ChallengeEvent.PHOTO_CAPTURED).completed)
    }

    @Test fun systemGardenPhotoOutsideCannotComplete() {
        val evaluator = ChallengeRuleEvaluator(systemGardenConfig())
        assertFalse(evaluator.evaluate(snapshot(inside = false), 0L, ChallengeEvent.PHOTO_CAPTURED).completed)
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
        val evaluator = ChallengeRuleEvaluator(graingerConfig())
        evaluator.evaluate(snapshot(soundDecibels = -60.0), 0L)
        val result = evaluator.evaluate(snapshot(soundDecibels = -60.0), 0L)

        assertEquals(ChallengeInstruction.MAKE_SOUND, result.instruction)
        assertFalse(result.condition(ChallengeCondition.SOUND_DETECTED))
        assertFalse(result.completed)
    }

    @Test fun graingerBriefLoudSoundDoesNotCompleteYet() {
        val evaluator = ChallengeRuleEvaluator(graingerConfig())
        evaluator.evaluate(snapshot(soundDecibels = -10.0), 0L)
        val result = evaluator.evaluate(snapshot(soundDecibels = -10.0), 0L)

        assertTrue(result.condition(ChallengeCondition.SOUND_DETECTED))
        assertFalse(result.completed)
        assertEquals(0.0, result.holdProgress, 0.0)
        assertEquals(ChallengeInstruction.HOLD_TONE, result.instruction)
    }

    @Test fun graingerLoudSoundHeldForOneSecondCompletes() {
        val evaluator = ChallengeRuleEvaluator(graingerConfig())
        evaluator.evaluate(snapshot(soundDecibels = -10.0), 0L)
        evaluator.evaluate(snapshot(soundDecibels = -10.0), 0L)

        val completed = evaluator.evaluate(snapshot(soundDecibels = -10.0), 1_000_000_000L)
        assertTrue(completed.completed)
        assertEquals(ChallengeInstruction.COMPLETED, completed.instruction)
    }

    @Test fun graingerSoundDroppingBelowThresholdResetsTheHoldTimer() {
        val evaluator = ChallengeRuleEvaluator(graingerConfig())
        evaluator.evaluate(snapshot(soundDecibels = -10.0), 0L)
        evaluator.evaluate(snapshot(soundDecibels = -10.0), 0L)
        assertTrue(evaluator.evaluate(snapshot(soundDecibels = -10.0), 500_000_000L).holdProgress > 0.0)

        val silence = evaluator.evaluate(snapshot(soundDecibels = -60.0), 600_000_000L)
        assertEquals(ChallengeInstruction.MAKE_SOUND, silence.instruction)
        assertEquals(0.0, silence.holdProgress, 0.0)

        evaluator.evaluate(snapshot(soundDecibels = -10.0), 700_000_000L)
        assertFalse(evaluator.evaluate(snapshot(soundDecibels = -10.0), 1_699_999_999L).completed)
        assertTrue(evaluator.evaluate(snapshot(soundDecibels = -10.0), 1_700_000_000L).completed)
    }

    @Test fun locationInsideRequiresTwoDistinctReadingsBeforeItIsTrusted() {
        val evaluator = ChallengeRuleEvaluator(unionConfig())

        val first = evaluator.evaluate(snapshot(locationTimestampNanos = 1_000L), 0L)
        assertFalse(first.condition(ChallengeCondition.LOCATION_INSIDE))
        assertEquals(ChallengeInstruction.MOVE_CLOSER, first.instruction)

        val second = evaluator.evaluate(snapshot(locationTimestampNanos = 2_000L), 0L)
        assertTrue(second.condition(ChallengeCondition.LOCATION_INSIDE))
    }

    @Test fun locationInsideIgnoresRepeatedFixWithTheSameTimestamp() {
        val evaluator = ChallengeRuleEvaluator(unionConfig())
        evaluator.evaluate(snapshot(locationTimestampNanos = 1_000L), 0L)

        val repeated = evaluator.evaluate(snapshot(locationTimestampNanos = 1_000L), 0L)

        assertFalse(repeated.condition(ChallengeCondition.LOCATION_INSIDE))
    }

    @Test fun locationInsideRejectsAFixWhoseAccuracyIsWorseThanTheGeofenceRadius() {
        val evaluator = ChallengeRuleEvaluator(unionConfig())
        evaluator.evaluate(snapshot(locationAccuracyMeters = 200.0, locationTimestampNanos = 1_000L), 0L)

        val result = evaluator.evaluate(snapshot(locationAccuracyMeters = 200.0, locationTimestampNanos = 2_000L), 0L)

        assertFalse(result.condition(ChallengeCondition.LOCATION_INSIDE))
        assertEquals(ChallengeInstruction.MOVE_CLOSER, result.instruction)
    }

    @Test fun locationInsideAcceptsAFixAsAccurateAsTheGeofenceRadius() {
        val evaluator = ChallengeRuleEvaluator(unionConfig())
        evaluator.evaluate(snapshot(locationAccuracyMeters = radiusMeters, locationTimestampNanos = 1_000L), 0L)

        val result = evaluator.evaluate(snapshot(locationAccuracyMeters = radiusMeters, locationTimestampNanos = 2_000L), 0L)

        assertTrue(result.condition(ChallengeCondition.LOCATION_INSIDE))
    }

    @Test fun locationInsideOneNoisyReadingBreaksTheStreak() {
        val evaluator = ChallengeRuleEvaluator(unionConfig())
        evaluator.evaluate(snapshot(locationTimestampNanos = 1_000L), 0L)
        evaluator.evaluate(snapshot(locationAccuracyMeters = 200.0, locationTimestampNanos = 2_000L), 0L)

        val result = evaluator.evaluate(snapshot(locationTimestampNanos = 3_000L), 0L)

        assertFalse(result.condition(ChallengeCondition.LOCATION_INSIDE))
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
        locationAccuracyMeters: Double? = null,
        locationTimestampNanos: Long? = null,
    ) = DeviceContextSnapshot(
        location = LocationOutput(
            currentLocation = if (inside) target else GeoCoordinate(0.0, 0.001),
            validity = SensorValidity.VALID,
            accuracyMeters = locationAccuracyMeters,
            timestampNanos = locationTimestampNanos,
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
