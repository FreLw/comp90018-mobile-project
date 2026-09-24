package com.comp90018.app.features.treasurechallenge

import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.contextengine.FakeDeviceContextEngine
import com.comp90018.app.contextengine.challenge.ChallengeInstruction
import com.comp90018.app.contextengine.challenge.RelicChallengeConfigs
import com.comp90018.app.contextengine.challenge.RelicChallengeType
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
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.orientation.OrientationOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TreasureChallengeViewModelTest {
    private val target = GeoCoordinate(0.0, 0.0)

    @Test fun unionFlowsFromOutsideThroughPhotoCaptureAndKeepsReadyDuringShutterMovement() {
        withHarness(unionConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(inside = false))
            assertEquals(ChallengeInstruction.MOVE_CLOSER, viewModel.uiState.value.instruction)

            engine.emit(snapshot(headingDegrees = 340.0))
            assertEquals(ChallengeInstruction.TURN_RIGHT, viewModel.uiState.value.instruction)

            engine.emit(snapshot())
            assertEquals(ChallengeInstruction.HOLD_ALIGNMENT, viewModel.uiState.value.instruction)
            clock.now = 800_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertTrue(viewModel.uiState.value.actionReady)
            assertEquals(ChallengeInstruction.TAKE_PHOTO, viewModel.uiState.value.instruction)

            clock.now = 900_000_000L
            engine.emit(snapshot(headingDegrees = 90.0))
            assertTrue(viewModel.uiState.value.actionReady)

            viewModel.onPhotoCaptureStarted()
            assertEquals(ChallengeCameraState.CAPTURING, viewModel.uiState.value.cameraState)
            viewModel.onPhotoCaptured("content://lost-treasures/union-photo")
            assertTrue(viewModel.uiState.value.completed)
            assertFalse(viewModel.uiState.value.actionReady)
            assertEquals(ChallengeInstruction.COMPLETED, viewModel.uiState.value.instruction)
            assertEquals(ChallengeCameraState.CAPTURED, viewModel.uiState.value.cameraState)
        }
    }

    @Test fun wilsonInterruptionResetsProgressBeforeSuccessfulObservation() {
        withHarness(wilsonConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(headingDegrees = 30.0))
            assertEquals(ChallengeInstruction.TURN_LEFT, viewModel.uiState.value.instruction)

            engine.emit(snapshot())
            clock.now = 1_500_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertEquals(0.5, viewModel.uiState.value.holdProgress, 1e-9)

            clock.now = 1_600_000_000L
            engine.emit(snapshot(stationary = false))
            assertEquals(ChallengeInstruction.STOP_MOVING, viewModel.uiState.value.instruction)
            assertEquals(0.0, viewModel.uiState.value.holdProgress, 0.0)

            clock.now = 2_000_000_000L
            engine.emit(snapshot())
            clock.now = 5_000_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertTrue(viewModel.uiState.value.completed)
        }
    }

    @Test fun oldQuadTiltResetsExcavationBeforeSuccessfulHold() {
        withHarness(oldQuadConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(horizontal = false))
            assertEquals(ChallengeInstruction.KEEP_PHONE_LEVEL, viewModel.uiState.value.instruction)

            engine.emit(snapshot())
            assertEquals(ChallengeInstruction.HOLD_EXCAVATION_POSITION, viewModel.uiState.value.instruction)
            clock.now = 1_500_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertEquals(0.5, viewModel.uiState.value.holdProgress, 1e-9)

            clock.now = 1_600_000_000L
            engine.emit(snapshot(horizontal = false))
            assertEquals(0.0, viewModel.uiState.value.holdProgress, 0.0)

            clock.now = 2_000_000_000L
            engine.emit(snapshot())
            clock.now = 5_000_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertTrue(viewModel.uiState.value.completed)
        }
    }

    @Test fun southLawnPrioritisesTurnGuidanceThenRequiresStillFinalLock() {
        withHarness(southConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(headingDegrees = 340.0, rotationStill = false))
            assertEquals(ChallengeInstruction.TURN_RIGHT, viewModel.uiState.value.instruction)

            engine.emit(snapshot(rotationStill = false))
            assertEquals(ChallengeInstruction.HOLD_STILL, viewModel.uiState.value.instruction)
            assertEquals(0.0, viewModel.uiState.value.holdProgress, 0.0)

            clock.now = 1_000_000_000L
            engine.emit(snapshot())
            assertEquals(ChallengeInstruction.HOLD_VIEWING_ANGLE, viewModel.uiState.value.instruction)
            clock.now = 1_600_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertEquals(0.5, viewModel.uiState.value.holdProgress, 1e-9)
            clock.now = 2_200_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertTrue(viewModel.uiState.value.completed)
        }
    }

    @Test fun stoppingChallengeStopsEngineAndClearsActiveSensorUiState() {
        withHarness(wilsonConfig()) { viewModel, engine, _ ->
            viewModel.start()
            engine.emit(snapshot())
            assertTrue(engine.isStarted)
            assertTrue(viewModel.uiState.value.active)

            viewModel.stop()
            assertFalse(engine.isStarted)
            assertEquals(DeviceContextSnapshot(), engine.output.value)
            assertFalse(viewModel.uiState.value.active)
            assertEquals(0.0, viewModel.uiState.value.holdProgress, 0.0)
            assertFalse(viewModel.uiState.value.completed)
        }
    }

    @Test fun switchingChallengesCreatesFreshEvaluatorAndClearsPhotoReadiness() {
        withHarness(unionConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot())
            clock.now = 800_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertTrue(viewModel.uiState.value.actionReady)

            viewModel.activateChallenge(oldQuadConfig())
            assertEquals(RelicChallengeType.OLD_QUAD_EXCAVATION, viewModel.uiState.value.challengeType)
            assertFalse(viewModel.uiState.value.actionReady)
            assertFalse(viewModel.uiState.value.completed)
            assertEquals(null, engine.challengeTargetHeadingDegrees)
            assertTrue(engine.isStarted)

            engine.emit(snapshot())
            assertEquals(0.0, viewModel.uiState.value.holdProgress, 0.0)
            assertEquals(ChallengeInstruction.HOLD_EXCAVATION_POSITION, viewModel.uiState.value.instruction)
        }
    }

    private inline fun withHarness(
        config: com.comp90018.app.contextengine.challenge.RelicChallengeConfig,
        block: (TreasureChallengeViewModel, FakeDeviceContextEngine, FakeClock) -> Unit,
    ) {
        val engine = FakeDeviceContextEngine()
        val clock = FakeClock()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = TreasureChallengeViewModel(
            initialConfig = config,
            engineFactory = { engine },
            timeSource = clock,
            scopeOverride = scope,
        )
        try {
            block(viewModel, engine, clock)
        } finally {
            viewModel.stop()
            scope.cancel()
        }
    }

    private fun unionConfig() = RelicChallengeConfigs.unionLawnPhoto(
        challengeId = "union-test",
        targetLocation = target,
        insideRadiusMeters = 20.0,
        requiredHeadingDegrees = 0.0,
    )

    private fun wilsonConfig() = RelicChallengeConfigs.wilsonHallObservation(
        challengeId = "wilson-test",
        targetLocation = target,
        insideRadiusMeters = 20.0,
        requiredHeadingDegrees = 0.0,
    )

    private fun oldQuadConfig() = RelicChallengeConfigs.oldQuadExcavation(
        challengeId = "old-quad-test",
        targetLocation = target,
        insideRadiusMeters = 20.0,
    )

    private fun southConfig() = RelicChallengeConfigs.southLawnViewingAngle(
        challengeId = "south-test",
        targetLocation = target,
        insideRadiusMeters = 20.0,
        requiredHeadingDegrees = 0.0,
    )

    private fun snapshot(
        inside: Boolean = true,
        headingDegrees: Double = 0.0,
        horizontal: Boolean = true,
        stationary: Boolean = true,
        stable: Boolean = true,
        rotationStill: Boolean = true,
        timestampNanos: Long = 0L,
    ) = DeviceContextSnapshot(
        location = LocationOutput(
            currentLocation = if (inside) target else GeoCoordinate(0.0, 0.001),
            validity = SensorValidity.VALID,
            timestampNanos = timestampNanos,
        ),
        orientation = OrientationOutput(
            direction = DirectionOutput(
                headingDegrees = headingDegrees,
                angularErrorDegrees = headingDegrees,
                headingValidity = SensorValidity.VALID,
                timestampNanos = timestampNanos,
            ),
            rotation = RotationOutput(
                classification = if (rotationStill) RotationState.STILL else RotationState.ROTATING,
                validity = SensorValidity.VALID,
                timestampNanos = timestampNanos,
            ),
            attitude = AttitudeOutput(
                horizontalState = if (horizontal) HorizontalState.HORIZONTAL else HorizontalState.NOT_HORIZONTAL,
                validity = SensorValidity.VALID,
                timestampNanos = timestampNanos,
            ),
        ),
        motionStability = MotionStabilityOutput(
            motion = MotionOutput(
                classification = if (stationary) MotionState.STATIONARY else MotionState.MOVING,
                validity = SensorValidity.VALID,
                timestampNanos = timestampNanos,
            ),
            stability = StabilityOutput(
                classification = if (stable) StabilityState.STABLE else StabilityState.UNSTABLE,
                validity = SensorValidity.VALID,
                timestampNanos = timestampNanos,
            ),
        ),
    )

    private class FakeClock(var now: Long = 0L) : MonotonicTimeSource {
        override fun nowNanos(): Long = now
    }
}
