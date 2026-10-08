package com.comp90018.app.features.treasurechallenge

import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.contextengine.challenge.*
import com.comp90018.app.sensors.*
import com.comp90018.app.sensors.audio.SoundLevelOutput
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.orientation.OrientationOutput
import org.junit.Assert.*
import org.junit.Test

class QuestVisualSignalsTest {
    private val state = TreasureChallengeUiState("trial", RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE, "Trial")

    @Test fun atlasVinesFillContinuouslyFromApproachUntilArrivalAndRejectUnknownDistance() {
        assertEquals(0f, questApproachFill(100.0, 20.0), 0f)
        assertEquals(.5f, questApproachFill(60.0, 20.0), .00001f)
        assertEquals(.500125f, questApproachFill(59.99, 20.0), .00001f)
        assertEquals(1f, questApproachFill(20.0, 20.0), 0f)
        assertEquals(1f, questApproachFill(0.0, 20.0), 0f)
        listOf<Double?>(null, Double.NaN, Double.POSITIVE_INFINITY, -1.0).forEach {
            assertEquals(0f, questApproachFill(it, 20.0), 0f)
        }
    }

    @Test fun headingUsesTheShortAngleAcrossNorth() {
        val signals = QuestVisualSignals.from(state.copy(
            requiredHeadingDegrees = 350.0,
            latestSnapshot = DeviceContextSnapshot(orientation = OrientationOutput(
                direction = DirectionOutput(headingDegrees = 5.0, headingValidity = SensorValidity.VALID),
            )),
        ))
        assertEquals(-15f, signals.headingError, .001f)
    }

    @Test fun artworkMovesContinuouslyWithFractionalSensorReadings() {
        val signals = QuestVisualSignals.from(state.copy(latestSnapshot = DeviceContextSnapshot(
            orientation = OrientationOutput(
                attitude = AttitudeOutput(rollDegrees = 12.75, pitchDegrees = -5.25, validity = SensorValidity.VALID),
                rotation = RotationOutput(angularVelocityMagnitude = .375, validity = SensorValidity.VALID),
            ),
            motionStability = MotionStabilityOutput(
                MotionOutput(smoothedMagnitude = .675, validity = SensorValidity.VALID),
                StabilityOutput(variation = .1375, validity = SensorValidity.VALID),
            ),
            sound = SoundLevelOutput(decibels = -35.5, validity = SensorValidity.VALID),
        )))
        assertEquals(12.75f, signals.roll, .0001f)
        assertEquals(-5.25f, signals.pitch, .0001f)
        assertEquals(.225f, signals.motion, .0001f)
        assertEquals(.1375f, signals.instability, .0001f)
        assertEquals(.125f, signals.rotation, .0001f)
        assertEquals(.55625f, signals.sound, .0001f)
    }

    @Test fun invalidOrNonFiniteReadingsCannotMoveOrBreakTheArtwork() {
        val signals = QuestVisualSignals.from(state.copy(angularErrorDegrees = Double.NaN,
            latestSnapshot = DeviceContextSnapshot(
                location = LocationOutput(distanceToTargetMeters = Double.POSITIVE_INFINITY, validity = SensorValidity.VALID),
                orientation = OrientationOutput(
                    direction = DirectionOutput(headingDegrees = Double.NaN, headingValidity = SensorValidity.VALID),
                    attitude = AttitudeOutput(rollDegrees = 100.0, pitchDegrees = Double.NaN, validity = SensorValidity.UNRELIABLE),
                ),
                sound = SoundLevelOutput(decibels = -10.0, validity = SensorValidity.UNRELIABLE),
            ),
        ))
        assertEquals(QuestVisualSignals(), signals)
    }

    @Test fun proximityFallsBackToTheActualGpsCoordinates() {
        val signals = QuestVisualSignals.from(state.copy(insideRadiusMeters = 20.0,
            latestSnapshot = DeviceContextSnapshot(location = LocationOutput(
                currentLocation = GeoCoordinate(0.0001, 0.0), targetLocation = GeoCoordinate(0.0, 0.0),
                validity = SensorValidity.VALID,
            )),
        ))
        assertEquals(11.1, requireNotNull(signals.distanceMeters), .1)
        assertTrue(signals.proximity in .88f.. .9f)
    }

    @Test fun onlyFulfilledEvaluatorConditionsLightStarsAndMatchingEngravings() {
        val sample = state.copy(completed = true, actionReady = true, holdProgress = 1.0,
            conditionStates = listOf(
                ChallengeConditionState(ChallengeCondition.LOCATION_INSIDE, true),
                ChallengeConditionState(ChallengeCondition.HEADING_ALIGNED, false),
            ))
        assertTrue(sample.conditionLit(ChallengeCondition.LOCATION_INSIDE))
        assertFalse(sample.conditionLit(ChallengeCondition.HEADING_ALIGNED))
        assertFalse(sample.conditionLit(ChallengeCondition.STABLE))
    }

    @Test fun continuousDebugControlsKeepFractionalValuesAndUseSensorThresholds() {
        val config = RelicChallengeConfigs.wilsonHallObservation("wilson", GeoCoordinate(0.0, 0.0), 20.0, 350.0)
        val snapshot = DebugContextControls(headingDegrees = 5.125,
            motionMagnitude = .12345, stabilityVariation = .1375, rotationRadiansPerSecond = .2345,
        ).snapshot(config, 1L)
        assertEquals(.12345, snapshot.motionStability.motion.smoothedMagnitude!!, .000001)
        assertEquals(.1375, snapshot.motionStability.stability.variation!!, .000001)
        assertEquals(.2345, snapshot.orientation.rotation.angularVelocityMagnitude!!, .000001)
        assertEquals(MotionState.STATIONARY, snapshot.motionStability.motion.classification)
        assertEquals(StabilityState.UNSTABLE, snapshot.motionStability.stability.classification)
        assertEquals(RotationState.ROTATING, snapshot.orientation.rotation.classification)
        assertEquals(-15.125, snapshot.orientation.direction.angularErrorDegrees!!, .000001)
    }
}
