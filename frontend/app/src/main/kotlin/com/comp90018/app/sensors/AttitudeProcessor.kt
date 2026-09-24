package com.comp90018.app.sensors

import kotlin.math.abs

/** Pure, Android-independent classification of a device's pitch and roll. */
class AttitudeProcessor(private val config: SensorConfig = SensorConfig()) {
    fun process(timestampNanos: Long, pitchDegrees: Double, rollDegrees: Double): AttitudeOutput {
        if (timestampNanos < 0 || !pitchDegrees.isFinite() || !rollDegrees.isFinite()) {
            return AttitudeOutput(validity = SensorValidity.UNRELIABLE)
        }

        val isHorizontal = abs(pitchDegrees) <= config.horizontalToleranceDegrees &&
            abs(rollDegrees) <= config.horizontalToleranceDegrees
        return AttitudeOutput(
            pitchDegrees = pitchDegrees,
            rollDegrees = rollDegrees,
            horizontalState = if (isHorizontal) HorizontalState.HORIZONTAL else HorizontalState.NOT_HORIZONTAL,
            validity = SensorValidity.VALID,
            timestampNanos = timestampNanos,
        )
    }
}
