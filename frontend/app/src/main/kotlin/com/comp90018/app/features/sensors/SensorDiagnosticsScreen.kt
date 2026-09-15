package com.comp90018.app.features.sensors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.app.sensors.DirectionAlignment
import com.comp90018.app.sensors.MotionState
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.StabilityState
import java.util.Locale

/** Temporary manual diagnostics. Acquisition starts only through the Start button. */
@Composable
fun SensorDiagnosticsScreen(
    viewModel: SensorDiagnosticsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.stop()
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }
    SensorDiagnosticsContent(
        state = state,
        onStart = {
            if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) viewModel.start()
        },
        onStop = viewModel::stop,
        onSetTarget = { viewModel.setTargetBearing(it, state.alignmentToleranceDegrees) },
        modifier = modifier,
    )
}

@Composable
private fun SensorDiagnosticsContent(
    state: SensorDiagnosticsUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSetTarget: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var targetText by rememberSaveable { mutableStateOf("") }
    var inputError by rememberSaveable { mutableStateOf(false) }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Development sensor diagnostics", style = MaterialTheme.typography.headlineSmall)
            Text("Temporary manual validation · Magnetic north")
            Text("Tap Start to acquire samples. Leaving or backgrounding this screen stops sensors; tap Start again on return.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onStart) { Text("Start sensors") }
                OutlinedButton(onClick = onStop) { Text("Stop sensors") }
            }

            Text("Motion", style = MaterialTheme.typography.titleMedium)
            Text("Classification: ${state.motion}")
            Text("Intensity: ${metric(state.motionIntensity)} m/s²")
            Text("Validity: ${state.motionValidity}")

            Text("Stability", style = MaterialTheme.typography.titleMedium)
            Text("Classification: ${state.stability}")
            Text("Metric (standard deviation): ${metric(state.stabilityMetric)} m/s²")
            Text("Validity: ${state.stabilityValidity}")

            Text("Direction", style = MaterialTheme.typography.titleMedium)
            Text("Heading: ${metric(state.headingDegrees)}°")
            Text("Target bearing: ${metric(state.targetBearingDegrees)}°")
            Text("Signed angular error: ${metric(state.angularErrorDegrees, signed = true)}°")
            Text("Alignment: ${state.alignment}")
            Text("Tolerance: ${metric(state.alignmentToleranceDegrees)}°")
            Text("Heading validity: ${state.directionValidity}")
            Text("Comparison validity: ${state.comparisonValidity}")
            val error = state.angularErrorDegrees
            val turn = when {
                state.directionValidity != SensorValidity.VALID ||
                    state.comparisonValidity != SensorValidity.VALID || error == null || !error.isFinite() -> "UNKNOWN"
                state.alignment == DirectionAlignment.ALIGNED -> "ALIGNED"
                state.alignment != DirectionAlignment.MISALIGNED -> "UNKNOWN"
                error > 0 -> "TURN RIGHT"
                error < 0 -> "TURN LEFT"
                else -> "UNKNOWN"
            }
            Text("Diagnostic turn: $turn")

            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it; inputError = false },
                label = { Text("Temporary target bearing (degrees)") },
                supportingText = {
                    Text(if (inputError) "Enter a finite number from 0 to less than 360."
                        else "0 ≤ bearing < 360; use a decimal point for fractions.")
                },
                isError = inputError,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    val bearing = targetText.trim().toDoubleOrNull()
                    if (bearing == null || !bearing.isFinite() || bearing < 0 || bearing >= 360) {
                        inputError = true
                    } else {
                        inputError = false
                        onSetTarget(bearing)
                    }
                }) { Text("Set target") }
                OutlinedButton(onClick = {
                    targetText = ""
                    inputError = false
                    onSetTarget(null)
                }) { Text("Clear target") }
            }
        }
    }
}

private fun metric(value: Double?, signed: Boolean = false): String =
    if (value == null || !value.isFinite()) "—"
    else String.format(Locale.ROOT, if (signed) "%+.2f" else "%.2f", value)

@Preview(showBackground = true)
@Composable
private fun InactiveSensorDiagnosticsPreview() {
    MaterialTheme {
        SensorDiagnosticsContent(SensorDiagnosticsUiState(), {}, {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveSensorDiagnosticsPreview() {
    MaterialTheme {
        SensorDiagnosticsContent(
            SensorDiagnosticsUiState(
                motion = MotionState.STATIONARY, motionIntensity = 0.04,
                motionValidity = SensorValidity.VALID,
                stability = StabilityState.STABLE, stabilityMetric = 0.02,
                stabilityValidity = SensorValidity.VALID,
                headingDegrees = 350.0, targetBearingDegrees = 10.0, angularErrorDegrees = 20.0,
                alignment = DirectionAlignment.MISALIGNED,
                directionValidity = SensorValidity.VALID, comparisonValidity = SensorValidity.VALID,
            ), {}, {}, {},
        )
    }
}
