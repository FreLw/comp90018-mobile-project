package com.comp90018.app.features.sensors

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.comp90018.app.sensors.AndroidSensorController
import com.comp90018.app.sensors.DirectionAlignment
import com.comp90018.app.sensors.DirectionProcessor
import com.comp90018.app.sensors.MotionState
import com.comp90018.app.sensors.SensorConfig
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.StabilityState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Development diagnostics only. Acceleration metrics are m/s²; angles are degrees. */
data class SensorDiagnosticsUiState(
    val motion: MotionState = MotionState.UNKNOWN,
    val motionIntensity: Double? = null,
    val motionValidity: SensorValidity = SensorValidity.INACTIVE,
    val stability: StabilityState = StabilityState.UNKNOWN,
    val stabilityMetric: Double? = null,
    val stabilityValidity: SensorValidity = SensorValidity.INACTIVE,
    val headingDegrees: Double? = null,
    val targetBearingDegrees: Double? = null,
    val alignmentToleranceDegrees: Double = SensorConfig.DEFAULT_ALIGNMENT_TOLERANCE_DEGREES,
    val angularErrorDegrees: Double? = null,
    val alignment: DirectionAlignment = DirectionAlignment.UNKNOWN,
    val directionValidity: SensorValidity = SensorValidity.INACTIVE,
    val comparisonValidity: SensorValidity = SensorValidity.INACTIVE,
)

/**
 * Exclusively owns [controller], which must be newly created and stopped when supplied.
 * The future screen explicitly calls start/stop with its visible lifecycle; collecting
 * uiState does not start acquisition. Call public methods on the main thread.
 */
class SensorDiagnosticsViewModel(private val controller: AndroidSensorController) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SensorDiagnosticsUiState())
    val uiState: StateFlow<SensorDiagnosticsUiState> = mutableUiState.asStateFlow()
    private var targetBearing: Double? = null
    private var alignmentTolerance = SensorConfig.DEFAULT_ALIGNMENT_TOLERANCE_DEGREES
    private var cleared = false
    private val collection = viewModelScope.launch {
        // Read the latest snapshot, so queued emissions cannot restore readings after stop().
        controller.state.collect { publishCurrentState() }
    }

    @MainThread
    fun start() {
        if (cleared) return
        controller.start()
        publishCurrentState()
    }

    @MainThread
    fun stop() {
        if (cleared) return
        controller.stop()
        publishCurrentState()
    }

    /** Null clears the temporary target. Supply magnetic-north bearings; no GPS is used. */
    @MainThread
    fun setTargetBearing(
        degrees: Double?,
        toleranceDegrees: Double = SensorConfig.DEFAULT_ALIGNMENT_TOLERANCE_DEGREES,
    ) {
        if (cleared) return
        // Delegate validation and comparison to the existing controller/processor APIs.
        controller.setTargetBearing(degrees, toleranceDegrees)
        targetBearing = degrees?.let(DirectionProcessor::normalize)
        alignmentTolerance = toleranceDegrees
        publishCurrentState()
    }

    private fun publishCurrentState() {
        if (cleared) return
        val sensor = controller.state.value
        mutableUiState.value = SensorDiagnosticsUiState(
            motion = sensor.motion.classification,
            motionIntensity = sensor.motion.smoothedMagnitude,
            motionValidity = sensor.motion.validity,
            stability = sensor.stability.classification,
            stabilityMetric = sensor.stability.variation,
            stabilityValidity = sensor.stability.validity,
            headingDegrees = sensor.direction.headingDegrees,
            // Keep the user's setting visible even when no valid heading is available.
            targetBearingDegrees = targetBearing,
            alignmentToleranceDegrees = alignmentTolerance,
            angularErrorDegrees = sensor.direction.angularErrorDegrees,
            alignment = sensor.direction.alignment,
            directionValidity = sensor.direction.headingValidity,
            comparisonValidity = sensor.direction.comparisonValidity,
        )
    }

    override fun onCleared() {
        cleared = true
        collection.cancel()
        controller.stop()
        mutableUiState.value = SensorDiagnosticsUiState(
            targetBearingDegrees = targetBearing,
            alignmentToleranceDegrees = alignmentTolerance,
        )
        super.onCleared()
    }

    companion object {
        /** Create a fresh controller per ViewModel; do not capture an Activity in its provider. */
        fun factory(createController: () -> AndroidSensorController) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SensorDiagnosticsViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return SensorDiagnosticsViewModel(createController()) as T
            }
        }
    }
}
