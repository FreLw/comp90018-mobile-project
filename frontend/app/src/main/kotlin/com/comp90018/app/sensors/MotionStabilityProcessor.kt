package com.comp90018.app.sensors

import kotlin.math.exp
import kotlin.math.sqrt

/** Single-owner processor. Samples must have nonnegative, strictly increasing timestamps. */
class MotionStabilityProcessor(private val config: SensorConfig = SensorConfig()) {
    private data class Sample(val time: Long, val magnitude: Double)
    private val window = ArrayDeque<Sample>()
    private var previousTime: Long? = null
    private var firstTime = 0L
    private var count = 0
    private var gx = 0.0
    private var gy = 0.0
    private var gz = 0.0
    private var smoothed = 0.0
    private var motion = MotionState.UNKNOWN
    private var stability = StabilityState.UNKNOWN

    fun reset() {
        window.clear()
        previousTime = null
        firstTime = 0L
        count = 0
        gx = 0.0
        gy = 0.0
        gz = 0.0
        smoothed = 0.0
        motion = MotionState.UNKNOWN
        stability = StabilityState.UNKNOWN
    }

    /** Invalid samples are rejected without changing processing history. */
    fun process(timestampNanos: Long, x: Double, y: Double, z: Double): MotionStabilityOutput {
        val previous = previousTime
        if (timestampNanos < 0 || (previous != null && timestampNanos <= previous) ||
            !x.isFinite() || !y.isFinite() || !z.isFinite()
        ) return MotionStabilityOutput(
            MotionOutput(validity = SensorValidity.UNRELIABLE),
            StabilityOutput(validity = SensorValidity.UNRELIABLE),
        )
        if (previous != null && timestampNanos - previous > config.sampleGapTimeoutNanos) reset()
        val dt = previousTime?.let { timestampNanos - it }
        if (dt == null) {
            gx = x; gy = y; gz = z
            firstTime = timestampNanos
        }
        val gravityWeight = dt?.let { 1.0 - exp(-it.toDouble() / config.gravityTimeConstantNanos) } ?: 1.0
        val nextGx = gx * (1 - gravityWeight) + x * gravityWeight
        val nextGy = gy * (1 - gravityWeight) + y * gravityWeight
        val nextGz = gz * (1 - gravityWeight) + z * gravityWeight
        val magnitude = Math.hypot(Math.hypot(x - nextGx, y - nextGy), z - nextGz)
        if (!magnitude.isFinite()) return MotionStabilityOutput(
            MotionOutput(validity = SensorValidity.UNRELIABLE),
            StabilityOutput(validity = SensorValidity.UNRELIABLE),
        )
        gx = nextGx; gy = nextGy; gz = nextGz
        val weight = dt?.let { 1.0 - exp(-it.toDouble() / config.motionTimeConstantNanos) } ?: 1.0
        smoothed += weight * (magnitude - smoothed)
        previousTime = timestampNanos
        if (count < config.motionMinSamples) count++
        window.addLast(Sample(timestampNanos, magnitude))
        // Retain one boundary sample so the window can span its full duration.
        while (window.size > 1 && timestampNanos - window[1].time >= config.stabilityWindowNanos) window.removeFirst()
        while (window.size > config.stabilityMaxSamples) window.removeFirst()
        val motionReady = count >= config.motionMinSamples && timestampNanos - firstTime >= config.motionWarmUpNanos
        if (motionReady) motion = when {
            smoothed >= config.motionEnterThreshold -> MotionState.MOVING
            smoothed <= config.motionExitThreshold -> MotionState.STATIONARY
            motion == MotionState.UNKNOWN -> MotionState.STATIONARY
            else -> motion
        }
        val stabilityReady = window.size >= config.stabilityMinSamples &&
            timestampNanos - window.first().time >= config.stabilityWindowNanos
        val mean = window.sumOf { it.magnitude / window.size }
        val variation = sqrt(window.sumOf { val d = it.magnitude - mean; d * d / window.size })
        if (!stabilityReady || !variation.isFinite()) stability = StabilityState.UNKNOWN
        if (stabilityReady && variation.isFinite()) stability = when {
            variation <= config.stabilityEnterThreshold -> StabilityState.STABLE
            variation >= config.stabilityExitThreshold -> StabilityState.UNSTABLE
            stability == StabilityState.UNKNOWN -> StabilityState.UNSTABLE
            else -> stability
        }
        return MotionStabilityOutput(
            MotionOutput(motion, if (motionReady) SensorValidity.VALID else SensorValidity.WARMING_UP,
                magnitude, smoothed, timestampNanos),
            StabilityOutput(stability, if (!variation.isFinite()) SensorValidity.UNRELIABLE
                else if (stabilityReady) SensorValidity.VALID else SensorValidity.WARMING_UP,
                variation.takeIf { it.isFinite() }, timestampNanos),
        )
    }
}
