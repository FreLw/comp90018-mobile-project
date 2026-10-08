package com.comp90018.app.features.treasurechallenge

import androidx.compose.ui.geometry.Rect
import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.contextengine.FakeDeviceContextEngine
import com.comp90018.app.contextengine.challenge.ChallengeInstruction
import com.comp90018.app.contextengine.challenge.ChallengeCondition
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

    /** Each call represents a fresh device reading unless a test pins an explicit timestamp. */
    private var nextAutoTimestampNanos = 1L

    @Test fun bothPhotoRevealsRequireACompletedCaptureAndSurviveActivityStopsButResetForANewTask() {
        val photoConfigs = listOf(unionConfig(), RelicChallengeConfigs.systemGardenGlasshouse("garden", target, 20.0))
        photoConfigs.forEach { config ->
            withHarness(config) { model, engine, _ ->
                model.start()
                model.finishPhotoReveal()
                assertFalse(model.photoRevealFinished)
                engine.emit(snapshot())
                engine.emit(snapshot())
                model.finishPhotoReveal()
                assertFalse(model.photoRevealFinished)
                model.onPhotoCaptureStarted()
                model.onPhotoCaptured("content://test/captured-photo")
                assertTrue(model.uiState.value.completed)
                assertFalse(model.photoRevealFinished)
                val arrival = PhotoRevealArrival(Rect(10f, 120f, 290f, 400f), 123)
                model.finishPhotoReveal(arrival)
                assertTrue(model.photoRevealFinished)
                assertEquals(arrival, model.photoRevealArrival)
                model.stop()
                model.start()
                assertTrue(model.photoRevealFinished)
                assertEquals(arrival, model.photoRevealArrival)
                assertTrue(model.uiState.value.completed)
                model.activateChallenge(config.copy(challengeId = "another-photo-visit"))
                assertFalse(model.photoRevealFinished)
                assertEquals(null, model.photoRevealArrival)
                assertFalse(model.uiState.value.completed)
            }
        }
    }

    @Test fun allSixTasksCompleteAtZeroElapsedTimeAsSoonAsEveryStarIsLit() {
        val configs = listOf(unionConfig(), wilsonConfig(), oldQuadConfig(), southConfig(),
            RelicChallengeConfigs.systemGardenGlasshouse("garden", target, 20.0), toneToolConfig())
        configs.forEach { config ->
            listOf(false, true).forEach { simulated ->
                withHarness(config, simulationMode = simulated) { model, engine, clock ->
                    model.start()
                    val controls = DebugContextControls(soundDetected = true)
                    engine.emit(controls.snapshot(config, 1L))
                    assertFalse(model.uiState.value.completed)
                    engine.emit(controls.snapshot(config, 2L))
                    if (config.photoActionRequired) {
                        assertTrue(model.uiState.value.actionReady)
                        assertFalse(model.uiState.value.conditionStates.all { it.satisfied })
                        assertFalse(model.uiState.value.conditionStates.single {
                            it.condition == ChallengeCondition.PHOTO_CAPTURED
                        }.satisfied)
                        model.onPhotoCaptureStarted()
                        model.onPhotoCaptured("content://test/captured-photo")
                    }
                    assertTrue(config.type.name, model.uiState.value.completed)
                    assertTrue(model.uiState.value.conditionStates.all { it.satisfied })
                    assertEquals(0L, clock.now)
                }
            }
        }
    }

    @Test fun unionFlowsFromOutsideThroughPhotoCaptureAndKeepsReadyDuringShutterMovement() {
        withHarness(unionConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(inside = false))
            assertEquals(ChallengeInstruction.MOVE_CLOSER, viewModel.uiState.value.instruction)

            engine.emit(snapshot())
            engine.emit(snapshot(headingDegrees = 340.0))
            assertEquals(ChallengeInstruction.TURN_RIGHT, viewModel.uiState.value.instruction)

            engine.emit(snapshot())
            assertEquals(ChallengeInstruction.TAKE_PHOTO, viewModel.uiState.value.instruction)
            clock.now = 800_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertTrue(viewModel.uiState.value.actionReady)
            assertEquals(ChallengeInstruction.TAKE_PHOTO, viewModel.uiState.value.instruction)

            clock.now = 900_000_000L
            engine.emit(snapshot(headingDegrees = 90.0))
            assertTrue(viewModel.uiState.value.actionReady)
            assertEquals(90.0, viewModel.uiState.value.latestSnapshot?.orientation?.direction?.headingDegrees)
            assertEquals(ChallengeInstruction.TAKE_PHOTO, viewModel.uiState.value.instruction)

            viewModel.onPhotoCaptureStarted()
            assertEquals(ChallengeCameraState.CAPTURING, viewModel.uiState.value.cameraState)
            viewModel.onPhotoCaptured("content://lost-treasures/union-photo")
            assertTrue(viewModel.uiState.value.completed)
            assertFalse(viewModel.uiState.value.actionReady)
            assertEquals(ChallengeInstruction.COMPLETED, viewModel.uiState.value.instruction)
            assertEquals(ChallengeCameraState.CAPTURED, viewModel.uiState.value.cameraState)
        }
    }

    @Test fun wilsonCompletesOnTheFirstReadingThatLightsAllStarsWithoutWaiting() {
        withHarness(wilsonConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(headingDegrees = 30.0))
            engine.emit(snapshot(headingDegrees = 30.0))
            assertEquals(ChallengeInstruction.TURN_LEFT, viewModel.uiState.value.instruction)
            engine.emit(snapshot(stationary = false))
            assertEquals(ChallengeInstruction.STOP_MOVING, viewModel.uiState.value.instruction)
            assertFalse(viewModel.uiState.value.completed)
            engine.emit(snapshot())
            assertTrue(viewModel.uiState.value.completed)
            assertTrue(viewModel.uiState.value.conditionStates.all { it.satisfied })
            assertEquals(0L, clock.now)
            // Sensor changes cannot undo a completion while the discovery page opens.
            engine.emit(snapshot(stationary = false))
            assertTrue(viewModel.uiState.value.completed)
        }
    }

    @Test fun debugTestAutomaticallyCompletesExactlyLikeLiveSensors() {
        withHarness(wilsonConfig(), simulationMode = true) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(headingDegrees = 28.0))
            engine.emit(snapshot(headingDegrees = 28.0))
            assertFalse(viewModel.uiState.value.completed)
            assertEquals(28.0, viewModel.uiState.value.angularErrorDegrees!!, 0.0)
            engine.emit(snapshot())
            assertTrue(viewModel.uiState.value.completed)
            assertEquals(0L, clock.now)
            val discovery = viewModel.discoverySave
            val completionId = viewModel.completionId
            viewModel.stop()
            viewModel.start()
            assertTrue(viewModel.uiState.value.completed)
            assertTrue(discovery === viewModel.discoverySave)
            assertEquals(completionId, viewModel.completionId)
        }
    }

    @Test fun oldQuadCompletesImmediatelyWhenLevelAndStill() {
        withHarness(oldQuadConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(horizontal = false))
            engine.emit(snapshot(horizontal = false))
            assertEquals(ChallengeInstruction.KEEP_PHONE_LEVEL, viewModel.uiState.value.instruction)
            assertFalse(viewModel.uiState.value.completed)
            engine.emit(snapshot())
            assertTrue(viewModel.uiState.value.completed)
            assertEquals(0L, clock.now)
        }
    }

    @Test fun southLawnCompletesImmediatelyAfterTheFinalTurningStarLights() {
        withHarness(southConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot(headingDegrees = 340.0, rotationStill = false))
            engine.emit(snapshot(headingDegrees = 340.0, rotationStill = false))
            assertEquals(ChallengeInstruction.TURN_RIGHT, viewModel.uiState.value.instruction)
            engine.emit(snapshot(rotationStill = false))
            assertEquals(ChallengeInstruction.HOLD_STILL, viewModel.uiState.value.instruction)
            assertFalse(viewModel.uiState.value.completed)
            engine.emit(snapshot())
            assertTrue(viewModel.uiState.value.completed)
            assertEquals(0L, clock.now)
        }
    }

    @Test fun debugSoundSimulationCompletesToneToolAfterLocationIsConfirmed() {
        withHarness(toneToolConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot())
            engine.emit(snapshot())
            assertEquals(ChallengeInstruction.MAKE_SOUND, viewModel.uiState.value.instruction)

            clock.now = 100L
            viewModel.simulateSustainedSoundForDebug()
            assertTrue(viewModel.uiState.value.completed)
            assertEquals(ChallengeInstruction.COMPLETED, viewModel.uiState.value.instruction)
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

    @Test fun completedChallengeSurvivesLifecycleStopAndStartForSaveRetry() {
        withHarness(wilsonConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot())
            engine.emit(snapshot())
            clock.now = 3_200_000_000L
            engine.emit(snapshot(timestampNanos = clock.now))
            assertTrue(viewModel.uiState.value.completed)

            viewModel.stop()
            assertTrue(viewModel.uiState.value.completed)
            assertFalse(viewModel.uiState.value.active)
            viewModel.start()
            assertTrue(viewModel.uiState.value.completed)
            assertFalse(engine.isStarted)
        }
    }

    @Test fun switchingChallengesCreatesFreshEvaluatorAndClearsPhotoReadiness() {
        withHarness(unionConfig()) { viewModel, engine, clock ->
            viewModel.start()
            engine.emit(snapshot())
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
            engine.emit(snapshot())
            assertTrue(viewModel.uiState.value.completed)
            assertEquals(ChallengeInstruction.COMPLETED, viewModel.uiState.value.instruction)
        }
    }

    @Test fun gardenRequiresSuccessfulCaptureAndCanRetryWithoutLeveling() {
        withHarness(RelicChallengeConfigs.systemGardenGlasshouse("garden", target, 22.0)) { model, engine, _ ->
            model.start()
            model.onPhotoCaptured("content://media/external/images/1")
            assertFalse(model.uiState.value.completed)
            engine.emit(snapshot(horizontal = false, stationary = false, stable = false, rotationStill = false))
            engine.emit(snapshot(horizontal = false, stationary = false, stable = false, rotationStill = false))
            assertTrue(model.uiState.value.actionReady)
            model.onPhotoCaptured("content://media/external/images/1")
            assertFalse(model.uiState.value.completed)
            model.onPhotoCaptureStarted()
            assertFalse(model.uiState.value.completed)
            model.onCameraError("Capture failed")
            model.onPhotoCaptured("content://media/external/images/1")
            assertFalse(model.uiState.value.completed)
            model.onPhotoCaptureStarted()
            model.onPhotoCaptured("")
            assertEquals(ChallengeCameraState.ERROR, model.uiState.value.cameraState)
            assertFalse(model.uiState.value.completed)
            model.onPhotoCaptureStarted()
            model.onPhotoCaptured("content://media/external/images/1")
            assertTrue(model.uiState.value.completed)
        }
    }

    @Test fun unionCannotStartPhotoWhileHeadingIsMisaligned() {
        withHarness(unionConfig()) { model, engine, clock ->
            model.start()
            engine.emit(snapshot(headingDegrees = 90.0))
            engine.emit(snapshot(headingDegrees = 90.0))
            clock.now = 3_000_000_000L
            engine.emit(snapshot(headingDegrees = 90.0))
            model.onPhotoCaptureStarted()
            model.onPhotoCaptured("content://media/external/images/1")
            assertFalse(model.uiState.value.actionReady)
            assertFalse(model.uiState.value.completed)
        }
    }

    @Test fun microphoneUnavailableCannotCompleteAndValidSoundFinishesImmediately() {
        withHarness(toneToolConfig()) { model, engine, clock ->
            model.start()
            engine.emit(snapshot())
            engine.emit(snapshot())
            assertFalse(model.uiState.value.completed)
            model.restart()
            assertTrue(engine.isStarted)
            engine.emit(snapshot())
            engine.emit(snapshot())
            assertFalse(model.uiState.value.completed)
            val sound = com.comp90018.app.sensors.audio.SoundLevelOutput(
                decibels = -20.0, validity = SensorValidity.VALID, timestampNanos = clock.now)
            engine.emit(snapshot().copy(sound = sound))
            assertTrue(model.uiState.value.completed)
            assertEquals(0L, clock.now)
        }
    }

    @Test fun capturedUriMustBeLocalNonEmptyContentAddress() {
        listOf("", " ", "content://media", "not a uri", "https://example.com/photo", "android.resource://app/image")
            .forEach { assertFalse(it, isCapturedPhotoUri(it)) }
        assertTrue(isCapturedPhotoUri("content://media/external/images/media/7"))
    }

    private inline fun withHarness(
        config: com.comp90018.app.contextengine.challenge.RelicChallengeConfig,
        simulationMode: Boolean = false,
        block: (TreasureChallengeViewModel, FakeDeviceContextEngine, FakeClock) -> Unit,
    ) {
        val engine = FakeDeviceContextEngine()
        val clock = FakeClock()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val simulationEngine = engine
        val viewModel = TreasureChallengeViewModel(
            initialConfig = config,
            engineFactory = { engine },
            timeSource = clock,
            scopeOverride = scope,
            simulationSession = if (simulationMode) object : ChallengeSimulationSession {
                override val engine = simulationEngine
                @androidx.compose.runtime.Composable
                override fun Controls(state: TreasureChallengeUiState, onPhotoCaptured: (String) -> Unit,
                    onCompleteWithDebugSnapshot: (DeviceContextSnapshot) -> Unit) = Unit
            } else null,
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

    private fun toneToolConfig() = RelicChallengeConfigs.graingerMuseumToneTool(
        challengeId = "tone-tool-test",
        targetLocation = target,
        insideRadiusMeters = 20.0,
    )

    private fun snapshot(
        inside: Boolean = true,
        headingDegrees: Double = 0.0,
        horizontal: Boolean = true,
        stationary: Boolean = true,
        stable: Boolean = true,
        rotationStill: Boolean = true,
        timestampNanos: Long = nextAutoTimestampNanos++,
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
                angularVelocityMagnitude = if (rotationStill) .05 else .8,
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
                smoothedMagnitude = if (stationary) .05 else 1.5,
                classification = if (stationary) MotionState.STATIONARY else MotionState.MOVING,
                validity = SensorValidity.VALID,
                timestampNanos = timestampNanos,
            ),
            stability = StabilityOutput(
                variation = if (stable) .03 else .5,
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
