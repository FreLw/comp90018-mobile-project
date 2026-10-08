package com.comp90018.app.features.treasurechallenge

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.contextengine.FakeDeviceContextEngine
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.sensors.AttitudeOutput
import com.comp90018.app.sensors.DirectionProcessor
import com.comp90018.app.sensors.HorizontalState
import com.comp90018.app.sensors.MotionOutput
import com.comp90018.app.sensors.MotionStabilityOutput
import com.comp90018.app.sensors.MotionState
import com.comp90018.app.sensors.RotationOutput
import com.comp90018.app.sensors.RotationState
import com.comp90018.app.sensors.SensorConfig
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
    val soundDecibels: Double? = null,
    val motionMagnitude: Double? = null,
    val stabilityVariation: Double? = null,
    val rotationRadiansPerSecond: Double? = null,
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
                direction = DirectionProcessor.evaluate(
                    headingDegrees, config.requiredHeadingDegrees, config.headingToleranceDegrees,
                ).copy(timestampNanos = timestampNanos),
                attitude = AttitudeOutput(
                    pitchDegrees = pitchDegrees,
                    rollDegrees = rollDegrees,
                    horizontalState = if (horizontal) HorizontalState.HORIZONTAL else HorizontalState.NOT_HORIZONTAL,
                    validity = SensorValidity.VALID,
                    timestampNanos = timestampNanos,
                ),
                rotation = RotationOutput(
                    classification = if (rotationRadiansPerSecond?.let {
                        it <= SensorConfig().rotationExitThreshold
                    } ?: rotationStill) RotationState.STILL else RotationState.ROTATING,
                    angularVelocityMagnitude = rotationRadiansPerSecond ?: if (rotationStill) .05 else .8,
                    validity = SensorValidity.VALID,
                    timestampNanos = timestampNanos,
                ),
            ),
            motionStability = MotionStabilityOutput(
                motion = MotionOutput(
                    classification = if (motionMagnitude?.let {
                        it <= SensorConfig().motionExitThreshold
                    } ?: stationary) MotionState.STATIONARY else MotionState.MOVING,
                    smoothedMagnitude = motionMagnitude ?: if (stationary) .05 else 1.5,
                    linearAccelerationMagnitude = motionMagnitude ?: if (stationary) .05 else 1.5,
                    validity = SensorValidity.VALID,
                    timestampNanos = timestampNanos,
                ),
                stability = StabilityOutput(
                    classification = if (stabilityVariation?.let {
                        it <= SensorConfig().stabilityEnterThreshold
                    } ?: stable) StabilityState.STABLE else StabilityState.UNSTABLE,
                    variation = stabilityVariation ?: if (stable) .03 else .5,
                    validity = SensorValidity.VALID,
                    timestampNanos = timestampNanos,
                ),
            ),
            sound = SoundLevelOutput(
                decibels = soundDecibels ?: if (soundDetected) {
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
        ) + headingScenario + DebugChallengeScenario("Photo ready", valid)
        RelicChallengeType.WILSON_HALL_OBSERVATION -> listOf(
            DebugChallengeScenario("Inside but moving", valid.copy(stationary = false)),
            DebugChallengeScenario("Stationary but unstable", valid.copy(stable = false)),
            DebugChallengeScenario("Stable but rotating", valid.copy(rotationStill = false)),
        ) + headingScenario + DebugChallengeScenario("All stars ready", valid)
        RelicChallengeType.OLD_QUAD_EXCAVATION -> listOf(
            DebugChallengeScenario("Not horizontal", valid.copy(horizontal = false)),
            DebugChallengeScenario("Horizontal but moving", valid.copy(stationary = false)),
            DebugChallengeScenario("All stars ready", valid),
        )
        RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> headingScenario + listOf(
            DebugChallengeScenario("Aligned but rotating", valid.copy(rotationStill = false)),
            DebugChallengeScenario("All stars ready", valid),
        )
        RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> listOf(
            DebugChallengeScenario("Outside target", valid.copy(distanceMeters = config.insideRadiusMeters + 50.0)),
            DebugChallengeScenario("Photo ready (GPS only)", valid.copy(horizontal = false, stationary = false, stable = false)),
        )
        RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> listOf(
            DebugChallengeScenario("Quiet (inside target)", valid),
            DebugChallengeScenario("Sound detected", valid.copy(soundDetected = true)),
        )
    }
}

private class DebugChallengeSimulationSession(private val config: RelicChallengeConfig) : ChallengeSimulationSession {
    override val engine = FakeDeviceContextEngine()
    private var controls by mutableStateOf(DebugContextControls(
        locationValid = true,
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
        var showSensorControls by remember { mutableStateOf(false) }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
            containerColor = Color(0xE6203C30), contentColor = Color(0xFFFFE6B1))) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                val scenarios = remember(config) { debugScenarios(config) }
                var showPresets by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("LIVE SENSOR TEST", color = QuestStone,
                        style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    androidx.compose.foundation.layout.Box {
                        TextButton(onClick = { showPresets = true }) {
                            Text("Scenarios", color = QuestGold, style = MaterialTheme.typography.labelSmall)
                        }
                        DropdownMenu(expanded = showPresets, onDismissRequest = { showPresets = false }) {
                            scenarios.forEach { scenario ->
                                DropdownMenuItem(text = { Text(scenario.label) }, onClick = {
                                    controls = scenario.controls
                                    showPresets = false
                                })
                            }
                        }
                    }
                }
                NumberControl("Distance · m", controls.distanceMeters,
                    0f..(config.insideRadiusMeters * 6.0).coerceAtLeast(100.0).toFloat()) {
                    controls = controls.copy(distanceMeters = it)
                }
                if (config.requiredHeadingDegrees != null) {
                    NumberControl("Heading · °", controls.headingDegrees, 0f..359.9f) {
                        controls = controls.copy(headingDegrees = it)
                    }
                }
                if (config.requiresHorizontal) {
                    NumberControl("Roll · °", controls.rollDegrees, -90f..90f) {
                        controls = controls.copy(rollDegrees = it,
                            horizontal = kotlin.math.abs(it) <= 12.0 && kotlin.math.abs(controls.pitchDegrees) <= 12.0)
                    }
                    NumberControl("Pitch · °", controls.pitchDegrees, -90f..90f) {
                        controls = controls.copy(pitchDegrees = it,
                            horizontal = kotlin.math.abs(it) <= 12.0 && kotlin.math.abs(controls.rollDegrees) <= 12.0)
                    }
                }
                if (config.requiresStationary) {
                    if (config.type == RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE ||
                        config.type == RelicChallengeType.WILSON_HALL_OBSERVATION) {
                        val score = com.comp90018.app.contextengine.challenge.ChallengePoise.stillnessScore(controls.snapshot(config, 0L))
                        Text("Stillness · motion + shake: " + String.format(java.util.Locale.US, "%.2f / 1.00", score),
                            color = QuestGold, style = MaterialTheme.typography.labelSmall)
                    }
                    NumberControl("Motion · m/s²", controls.motionMagnitude ?: if (controls.stationary) .05 else 1.5, 0f..3f) {
                        controls = controls.copy(motionMagnitude = it)
                    }
                }
                if (config.requiresStability) {
                    NumberControl("Shake · m/s²", controls.stabilityVariation ?: if (controls.stable) .03 else .5, 0f..1f) {
                        controls = controls.copy(stabilityVariation = it)
                    }
                }
                if (config.requiresRotationStill) {
                    NumberControl("Turn · rad/s", controls.rotationRadiansPerSecond ?: if (controls.rotationStill) .05 else .8, 0f..2f) {
                        controls = controls.copy(rotationRadiansPerSecond = it)
                    }
                }
                if (config.requiresSound) {
                    NumberControl("Sound · dB", controls.soundDecibels ?: (config.soundThresholdDecibels ?: -30.0) - 20,
                        -80f..0f) { controls = controls.copy(soundDecibels = it) }
                }
                TextButton(onClick = { showSensorControls = !showSensorControls }) {
                    Text(if (showSensorControls) "Hide extra sensors" else "Extra sensors",
                        color = QuestStone, style = MaterialTheme.typography.labelSmall)
                }
                if (showSensorControls) {
                    Toggle("GPS available", controls.locationValid) { controls = controls.copy(locationValid = it) }
                    NumberControl("Accuracy · m", controls.accuracyMeters, 0f..100f) {
                        controls = controls.copy(accuracyMeters = it)
                    }
                    if (!config.requiresHorizontal) {
                        NumberControl("Pitch · °", controls.pitchDegrees, -90f..90f) {
                            controls = controls.copy(pitchDegrees = it)
                        }
                        NumberControl("Roll · °", controls.rollDegrees, -90f..90f) {
                            controls = controls.copy(rollDegrees = it)
                        }
                    }
                    if (config.photoActionRequired) {
                        Text("Tap the central engraving when ready.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ${if (checked) "ON" else "OFF"}", style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = QuestGold, checkedTrackColor = QuestForest))
    }
}

@Composable
private fun NumberControl(label: String, value: Double, range: ClosedFloatingPointRange<Float>, onChange: (Double) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label + ": " + String.format(java.util.Locale.US, "%.1f", value), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
        Slider(colors = debugSliderColors(), value = value.toFloat().coerceIn(range.start, range.endInclusive),
            onValueChange = { onChange(it.toDouble()) }, valueRange = range, modifier = Modifier.weight(2f).semantics { contentDescription = label })
    }
}

@Composable
private fun debugSliderColors() = SliderDefaults.colors(
    thumbColor = Color(0xFFE8B75B), activeTrackColor = Color(0xFFE8B75B),
    inactiveTrackColor = Color(0xFF507052), activeTickColor = Color(0xFF203C30),
    inactiveTickColor = Color(0xFFE8B75B),
)
