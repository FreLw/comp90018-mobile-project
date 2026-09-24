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
    val horizontalToleranceDegrees: Double = 12.0,
    val sampleGapTimeoutNanos: Long = 2_000_000_000L,
    /** Angular velocity magnitude is rad/s (raw gyroscope units). */
    val rotationTimeConstantNanos: Long = 150_000_000L,
    val rotationWarmUpNanos: Long = 300_000_000L,
    val rotationMinSamples: Int = 3,
    val rotationEnterThreshold: Double = 0.35,
    val rotationExitThreshold: Double = 0.15,
) {
    init {
        require(gravityTimeConstantNanos > 0 && motionTimeConstantNanos > 0)
        require(headingTimeConstantNanos > 0 && motionWarmUpNanos > 0)
        require(stabilityWindowNanos > 0 && sampleGapTimeoutNanos > 0)
        require(motionMinSamples >= 2 && stabilityMinSamples >= 2)
        require(stabilityMaxSamples >= stabilityMinSamples)
        require(motionExitThreshold.isFinite() && motionEnterThreshold.isFinite())
        require(motionExitThreshold >= 0 && motionEnterThreshold > motionExitThreshold)
        require(stabilityEnterThreshold.isFinite() && stabilityExitThreshold.isFinite())
        require(stabilityEnterThreshold >= 0 && stabilityExitThreshold > stabilityEnterThreshold)
        require(horizontalToleranceDegrees.isFinite() && horizontalToleranceDegrees in 0.0..90.0)
        require(rotationTimeConstantNanos > 0 && rotationWarmUpNanos > 0 && rotationMinSamples >= 2)
        require(rotationExitThreshold.isFinite() && rotationEnterThreshold.isFinite())
        require(rotationExitThreshold >= 0 && rotationEnterThreshold > rotationExitThreshold)
    }

    companion object {
        const val DEFAULT_ALIGNMENT_TOLERANCE_DEGREES = 15.0
    }
}
