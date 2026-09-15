package com.comp90018.app.sensors

import kotlin.math.abs
import kotlin.math.exp

/** Single-owner processor; input heading and target must share the same north reference. */
class DirectionProcessor(private val config: SensorConfig = SensorConfig()) {
    private var heading: Double? = null
    private var previousTime: Long? = null

    fun reset() {
        heading = null
        previousTime = null
    }

    /** Circular exponential smoothing follows the shortest arc, including across north. */
    fun process(
        timestampNanos: Long,
        headingDegrees: Double?,
        targetBearingDegrees: Double? = null,
        toleranceDegrees: Double = SensorConfig.DEFAULT_ALIGNMENT_TOLERANCE_DEGREES,
    ): DirectionOutput {
        val previous = previousTime
        if (timestampNanos < 0 || (previous != null && timestampNanos <= previous) ||
            (headingDegrees != null && !headingDegrees.isFinite())
        ) return DirectionOutput(headingValidity = SensorValidity.UNRELIABLE)
        if (headingDegrees == null) return DirectionOutput()
        if (previous != null && timestampNanos - previous > config.sampleGapTimeoutNanos) reset()
        val normalized = normalize(headingDegrees)
        val old = heading
        val time = previousTime
        val next = if (old == null || time == null) normalized else {
            val weight = 1.0 - exp(-(timestampNanos - time).toDouble() / config.headingTimeConstantNanos)
            normalize(old + weight * angularDifference(old, normalized))
        }
        heading = next
        previousTime = timestampNanos
        return evaluate(next, targetBearingDegrees, toleranceDegrees).copy(timestampNanos = timestampNanos)
    }

    companion object {
        fun normalize(degrees: Double): Double {
            require(degrees.isFinite())
            val result = ((degrees % 360.0) + 360.0) % 360.0
            return if (result == 0.0) 0.0 else result
        }

        /** target - current in [-180, 180). Exactly opposite always means -180 / TURN_LEFT. */
        fun angularDifference(currentHeading: Double, targetBearing: Double): Double =
            normalize(normalize(targetBearing) - normalize(currentHeading) + 180.0) - 180.0

        fun evaluate(
            headingDegrees: Double?,
            targetBearingDegrees: Double?,
            toleranceDegrees: Double = SensorConfig.DEFAULT_ALIGNMENT_TOLERANCE_DEGREES,
        ): DirectionOutput {
            if (headingDegrees == null) return DirectionOutput()
            if (!headingDegrees.isFinite()) return DirectionOutput(headingValidity = SensorValidity.UNRELIABLE)
            val base = DirectionOutput(headingDegrees = normalize(headingDegrees), headingValidity = SensorValidity.VALID)
            if (!toleranceDegrees.isFinite() || toleranceDegrees !in 0.0..180.0 ||
                (targetBearingDegrees != null && !targetBearingDegrees.isFinite())
            ) return base.copy(comparisonValidity = SensorValidity.UNRELIABLE)
            if (targetBearingDegrees == null) return base
            val error = angularDifference(headingDegrees, targetBearingDegrees)
            val aligned = abs(error) <= toleranceDegrees
            return base.copy(
                targetBearingDegrees = normalize(targetBearingDegrees), angularErrorDegrees = error,
                alignment = if (aligned) DirectionAlignment.ALIGNED else DirectionAlignment.MISALIGNED,
                turn = if (aligned) TurnDirection.NONE else if (error > 0) TurnDirection.TURN_RIGHT else TurnDirection.TURN_LEFT,
                comparisonValidity = SensorValidity.VALID,
            )
        }
    }
}
