package com.comp90018.app.features.treasurechallenge

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.comp90018.app.R
import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.contextengine.FakeDeviceContextEngine
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.sensors.AttitudeOutput
import com.comp90018.app.sensors.DirectionProcessor
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
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.orientation.OrientationOutput
import kotlinx.coroutines.delay

/** Only compiled into debug builds. All completion still comes from ChallengeRuleEvaluator. */
object ChallengeSimulationFactory {
    fun create(config: RelicChallengeConfig): ChallengeSimulationSession = DebugChallengeSimulationSession(config)

    @Composable
    fun CalibrationPanel(treasureId: String, config: RelicChallengeConfig, radarRadiusMeters: Double,
        state: TreasureChallengeUiState, onSimulateSound: () -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(if (expanded) "Hide calibration diagnostics" else "Show calibration diagnostics")
        }
        if (!expanded) return
        val snapshot = state.latestSnapshot
        val location = snapshot?.location
        val current = location?.currentLocation
        val direction = snapshot?.orientation?.direction
        val headingError = DirectionProcessor.evaluate(direction?.headingDegrees,
            config.requiredHeadingDegrees, config.headingToleranceDegrees).angularErrorDegrees
        val distance = current?.let { runCatching { LocationCalculator.distanceMeters(it, config.targetLocation) }.getOrNull() }
        Card(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Treasure ID: $treasureId")
                Text("Calibration: ${config.calibrationStatus}")
                Text("Heading reference: ${config.headingReference ?: "unspecified"}")
                Text("Target: ${config.targetLocation.latitude}, ${config.targetLocation.longitude}")
                Text("Current: ${current?.latitude ?: "—"}, ${current?.longitude ?: "—"}")
                Text("GPS validity: ${location?.validity ?: "—"}; accuracy: ${location?.accuracyMeters ?: "—"} m")
                Text("Distance: ${distance ?: "—"} m; radar: $radarRadiusMeters m; inside: ${config.insideRadiusMeters} m")
                Text("Heading: ${direction?.headingDegrees ?: "—"}°; required: ${config.requiredHeadingDegrees ?: "—"}°")
                Text("Tolerance: ${config.headingToleranceDegrees}°; error: ${headingError ?: "—"}°")
                Text("Pitch: ${snapshot?.orientation?.attitude?.pitchDegrees ?: "—"}°; roll: ${snapshot?.orientation?.attitude?.rollDegrees ?: "—"}°")
                Text("Horizontal: ${snapshot?.orientation?.attitude?.horizontalState ?: "—"}")
                Text("Motion: ${snapshot?.motionStability?.motion?.classification ?: "—"}")
                Text("Stability: ${snapshot?.motionStability?.stability?.classification ?: "—"}")
                Text("Rotation: ${snapshot?.orientation?.rotation?.classification ?: "—"}")
                state.conditionStates.forEach { condition ->
                    Text("${condition.condition}: ${if (condition.satisfied) "pass" else "wait"}")
                }
                if (config.requiresSound && !state.completed) {
                    Button(onClick = onSimulateSound, modifier = Modifier.fillMaxWidth()) {
                        Text("DEBUG: simulate sustained noise")
                    }
                }
            }
        }
    }
}

data class DebugContextControls(
    val locationValid: Boolean = true,
    val distanceMeters: Double = 0.0,
    val accuracyMeters: Double = 5.0,
    val headingDegrees: Double = 0.0,
    val pitchDegrees: Double = 0.0,
    val rollDegrees: Double = 0.0,
    val horizontal: Boolean = true,
    val stationary: Boolean = true,
    val stable: Boolean = true,
    val rotationStill: Boolean = true,
    val soundDetected: Boolean = false,
) {
    fun snapshot(config: RelicChallengeConfig, timestampNanos: Long): DeviceContextSnapshot {
        val coordinate = GeoCoordinate(
            config.targetLocation.latitude + distanceMeters / 111_000.0,
            config.targetLocation.longitude,
        )
        val validity = if (locationValid) SensorValidity.VALID else SensorValidity.UNRELIABLE
        return DeviceContextSnapshot(
            location = LocationOutput(
                currentLocation = coordinate,
                targetLocation = config.targetLocation,
                accuracyMeters = accuracyMeters,
                validity = validity,
                timestampNanos = timestampNanos,
            ),
            orientation = OrientationOutput(
                direction = DirectionOutput(
                    headingDegrees = headingDegrees,
                    headingValidity = SensorValidity.VALID,
                    timestampNanos = timestampNanos,
                ),
                attitude = AttitudeOutput(
                    pitchDegrees = pitchDegrees,
                    rollDegrees = rollDegrees,
                    horizontalState = if (horizontal) HorizontalState.HORIZONTAL else HorizontalState.NOT_HORIZONTAL,
                    validity = SensorValidity.VALID,
                    timestampNanos = timestampNanos,
                ),
                rotation = RotationOutput(
                    classification = if (rotationStill) RotationState.STILL else RotationState.ROTATING,
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
            sound = SoundLevelOutput(
                decibels = if (soundDetected) {
                    (config.soundThresholdDecibels ?: -30.0) + 5.0
                } else {
                    (config.soundThresholdDecibels ?: -30.0) - 20.0
                },
                validity = SensorValidity.VALID,
                timestampNanos = timestampNanos,
            ),
        )
    }
}

data class DebugChallengeScenario(val label: String, val controls: DebugContextControls)

fun debugScenarios(config: RelicChallengeConfig): List<DebugChallengeScenario> {
    val alignedHeading = config.requiredHeadingDegrees ?: 0.0
    val valid = DebugContextControls(headingDegrees = alignedHeading)
    val misaligned = valid.copy(headingDegrees = (alignedHeading + 90.0) % 360.0)
    val headingScenario = if (config.requiredHeadingDegrees != null) {
        listOf(DebugChallengeScenario("Heading misaligned", misaligned), DebugChallengeScenario("Heading aligned", valid))
    } else emptyList()
    return when (config.type) {
        RelicChallengeType.UNION_LAWN_PHOTO -> listOf(
            DebugChallengeScenario("Outside target", valid.copy(distanceMeters = config.insideRadiusMeters + 50.0)),
            DebugChallengeScenario("Inside target", valid),
        ) + headingScenario + DebugChallengeScenario("Photo ready (hold)", valid)
        RelicChallengeType.WILSON_HALL_OBSERVATION -> listOf(
            DebugChallengeScenario("Inside but moving", valid.copy(stationary = false)),
            DebugChallengeScenario("Stationary but unstable", valid.copy(stable = false)),
            DebugChallengeScenario("Stable but rotating", valid.copy(rotationStill = false)),
        ) + headingScenario + DebugChallengeScenario("All valid (3s hold)", valid)
        RelicChallengeType.OLD_QUAD_EXCAVATION -> listOf(
            DebugChallengeScenario("Not horizontal", valid.copy(horizontal = false)),
            DebugChallengeScenario("Horizontal but moving", valid.copy(stationary = false)),
            DebugChallengeScenario("All valid (hold)", valid),
        )
        RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> headingScenario + listOf(
            DebugChallengeScenario("Aligned but rotating", valid.copy(rotationStill = false)),
            DebugChallengeScenario("All valid (hold)", valid),
        )
        RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> listOf(
            DebugChallengeScenario("Not horizontal", valid.copy(horizontal = false)),
            DebugChallengeScenario("Horizontal but moving", valid.copy(stationary = false)),
            DebugChallengeScenario("All valid (3s hold)", valid),
        )
        RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> listOf(
            DebugChallengeScenario("Quiet (inside target)", valid),
            DebugChallengeScenario("Make noise (hold)", valid.copy(soundDetected = true)),
        )
        else -> emptyList()
    }
}

private class DebugChallengeSimulationSession(private val config: RelicChallengeConfig) : ChallengeSimulationSession {
    override val engine = FakeDeviceContextEngine()
    private var controls by mutableStateOf(DebugContextControls(
        locationValid = false,
        distanceMeters = config.insideRadiusMeters + 50.0,
        headingDegrees = config.requiredHeadingDegrees ?: 0.0,
    ))

    @Composable
    override fun Controls(
        state: TreasureChallengeUiState,
        onPhotoCaptured: (String) -> Unit,
        onCompleteWithDebugSnapshot: (DeviceContextSnapshot) -> Unit,
    ) {
        LaunchedEffect(this, controls, state.completed) {
            while (!state.completed) {
                if (engine.isStarted) engine.emit(controls.snapshot(config, SystemClock.elapsedRealtimeNanos()))
                delay(250L)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("DEBUG · Fake sensor context")
                debugScenarios(config).forEach { scenario ->
                    OutlinedButton(onClick = {
                        controls = scenario.controls
                        // This remains a real evaluator completion, then uses the normal save/reveal flow.
                        if (!config.photoActionRequired && scenario.label.startsWith("All valid")) {
                            onCompleteWithDebugSnapshot(
                                scenario.controls.snapshot(config, SystemClock.elapsedRealtimeNanos()),
                            )
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(scenario.label)
                    }
                }
                Toggle("Location valid", controls.locationValid) { controls = controls.copy(locationValid = it) }
                NumberControl("Distance", controls.distanceMeters, 0f..200f) { controls = controls.copy(distanceMeters = it) }
                NumberControl("GPS accuracy", controls.accuracyMeters, 0f..100f) { controls = controls.copy(accuracyMeters = it) }
                NumberControl("Heading", controls.headingDegrees, 0f..359f) { controls = controls.copy(headingDegrees = it) }
                NumberControl("Pitch", controls.pitchDegrees, -90f..90f) { controls = controls.copy(pitchDegrees = it) }
                NumberControl("Roll", controls.rollDegrees, -90f..90f) { controls = controls.copy(rollDegrees = it) }
                Toggle("Horizontal", controls.horizontal) { controls = controls.copy(horizontal = it) }
                Toggle("Stationary", controls.stationary) { controls = controls.copy(stationary = it) }
                Toggle("Stable", controls.stable) { controls = controls.copy(stable = it) }
                Toggle("Gyroscope still", controls.rotationStill) { controls = controls.copy(rotationStill = it) }
                if (config.requiresSound) {
                    Toggle("Noise detected", controls.soundDetected) { controls = controls.copy(soundDetected = it) }
                }
                if (config.photoActionRequired) {
                    Button(
                        onClick = { onPhotoCaptured("android.resource://com.comp90018.app/${R.drawable.treasure_postcard}") },
                        enabled = state.actionReady && !state.completed,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Simulate PHOTO_CAPTURED") }
                }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NumberControl(label: String, value: Double, range: ClosedFloatingPointRange<Float>, onChange: (Double) -> Unit) {
    Text("$label: ${value.toInt()}")
    Slider(value = value.toFloat().coerceIn(range.start, range.endInclusive),
        onValueChange = { onChange(it.toDouble()) }, valueRange = range)
}
