package com.comp90018.app.sensors

/** Acceleration is m/s²; timestamps and durations are monotonic nanoseconds. */
data class SensorConfig(
    val gravityTimeConstantNanos: Long = 800_000_000L,
    val motionTimeConstantNanos: Long = 150_000_000L,
    val motionWarmUpNanos: Long = 500_000_000L,
    val motionMinSamples: Int = 5,
    val motionEnterThreshold: Double = 0.8,
    val motionExitThreshold: Double = 0.35,
    val stabilityWindowNanos: Long = 600_000_000L,
    val stabilityMinSamples: Int = 10,
    val stabilityMaxSamples: Int = 256,
    val stabilityEnterThreshold: Double = 0.12,
    val stabilityExitThreshold: Double = 0.25,
    val headingTimeConstantNanos: Long = 200_000_000L,
    val sampleGapTimeoutNanos: Long = 2_000_000_000L,
    val samplingPeriodMicros: Int = 20_000,
    val staleCheckIntervalMillis: Long = 250L,
    val minimumHeadingProjection: Double = 0.01,
) {
    init {
        require(samplingPeriodMicros >= 5_000)
        require(staleCheckIntervalMillis > 0 && staleCheckIntervalMillis <= sampleGapTimeoutNanos / 1_000_000L)
        require(minimumHeadingProjection.isFinite() && minimumHeadingProjection > 0 && minimumHeadingProjection < 1)
        require(gravityTimeConstantNanos > 0 && motionTimeConstantNanos > 0)
        require(headingTimeConstantNanos > 0 && motionWarmUpNanos > 0)
        require(stabilityWindowNanos > 0 && sampleGapTimeoutNanos > 0)
        require(motionMinSamples >= 2 && stabilityMinSamples >= 2)
        require(stabilityMaxSamples >= stabilityMinSamples)
        require(motionExitThreshold.isFinite() && motionEnterThreshold.isFinite())
        require(motionExitThreshold >= 0 && motionEnterThreshold > motionExitThreshold)
        require(stabilityEnterThreshold.isFinite() && stabilityExitThreshold.isFinite())
        require(stabilityEnterThreshold >= 0 && stabilityExitThreshold > stabilityEnterThreshold)
    }

    companion object {
        const val DEFAULT_ALIGNMENT_TOLERANCE_DEGREES = 15.0
    }
}
