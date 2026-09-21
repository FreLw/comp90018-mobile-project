package com.comp90018.app.sensors

import kotlin.math.exp
import kotlin.math.sqrt

/** Single-owner processor. Samples must have nonnegative, strictly increasing timestamps. */
class RotationProcessor(private val config: SensorConfig = SensorConfig()) {
    private var previousTime: Long? = null
    private var firstTime = 0L
    private var count = 0
    private var smoothed = 0.0
    private var rotation = RotationState.UNKNOWN

    fun reset() {
        previousTime = null
        firstTime = 0L
        count = 0
        smoothed = 0.0
        rotation = RotationState.UNKNOWN
    }

    /** x, y, z are gyroscope angular velocities in rad/s around each axis. */
    fun process(timestampNanos: Long, x: Double, y: Double, z: Double): RotationOutput {
        val previous = previousTime
        if (timestampNanos < 0 || (previous != null && timestampNanos <= previous) ||
            !x.isFinite() || !y.isFinite() || !z.isFinite()
        ) return RotationOutput(validity = SensorValidity.UNRELIABLE)
        if (previous != null && timestampNanos - previous > config.sampleGapTimeoutNanos) reset()
        val dt = previousTime?.let { timestampNanos - it }
        if (dt == null) firstTime = timestampNanos
        val magnitude = sqrt(x * x + y * y + z * z)
        if (!magnitude.isFinite()) return RotationOutput(validity = SensorValidity.UNRELIABLE)
        val weight = dt?.let { 1.0 - exp(-it.toDouble() / config.rotationTimeConstantNanos) } ?: 1.0
        smoothed += weight * (magnitude - smoothed)
        previousTime = timestampNanos
        if (count < config.rotationMinSamples) count++
        val ready = count >= config.rotationMinSamples && timestampNanos - firstTime >= config.rotationWarmUpNanos
        if (ready) rotation = when {
            smoothed >= config.rotationEnterThreshold -> RotationState.ROTATING
            smoothed <= config.rotationExitThreshold -> RotationState.STILL
            rotation == RotationState.UNKNOWN -> RotationState.STILL
            else -> rotation
        }
        return RotationOutput(
            classification = rotation,
            validity = if (ready) SensorValidity.VALID else SensorValidity.WARMING_UP,
            angularVelocityMagnitude = smoothed,
            timestampNanos = timestampNanos,
        )
    }
}
