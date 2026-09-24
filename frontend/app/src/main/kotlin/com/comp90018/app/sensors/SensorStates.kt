package com.comp90018.app.sensors

enum class MotionState { UNKNOWN, STATIONARY, MOVING }
enum class StabilityState { UNKNOWN, STABLE, UNSTABLE }
enum class DirectionAlignment { UNKNOWN, ALIGNED, MISALIGNED }
enum class TurnDirection { UNKNOWN, NONE, TURN_LEFT, TURN_RIGHT }
enum class RotationState { UNKNOWN, STILL, ROTATING }
enum class HorizontalState { UNKNOWN, HORIZONTAL, NOT_HORIZONTAL }

/** UNKNOWN includes absent input; UNRELIABLE means rejected input. */
enum class SensorValidity { UNKNOWN, WARMING_UP, VALID, UNRELIABLE }

data class MotionOutput(
    val classification: MotionState = MotionState.UNKNOWN,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
    val linearAccelerationMagnitude: Double? = null,
    val smoothedMagnitude: Double? = null,
    val timestampNanos: Long? = null,
)

data class StabilityOutput(
    val classification: StabilityState = StabilityState.UNKNOWN,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
    /** Population standard deviation of linear-acceleration magnitudes, in m/s². */
    val variation: Double? = null,
    val timestampNanos: Long? = null,
)

data class MotionStabilityOutput(
    val motion: MotionOutput = MotionOutput(),
    val stability: StabilityOutput = StabilityOutput(),
)

data class DirectionOutput(
    val headingDegrees: Double? = null,
    val targetBearingDegrees: Double? = null,
    val angularErrorDegrees: Double? = null,
    val alignment: DirectionAlignment = DirectionAlignment.UNKNOWN,
    val turn: TurnDirection = TurnDirection.UNKNOWN,
    val headingValidity: SensorValidity = SensorValidity.UNKNOWN,
    val comparisonValidity: SensorValidity = SensorValidity.UNKNOWN,
    val timestampNanos: Long? = null,
)

data class RotationOutput(
    val classification: RotationState = RotationState.UNKNOWN,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
    /** Smoothed magnitude of the gyroscope's angular velocity vector, in rad/s. */
    val angularVelocityMagnitude: Double? = null,
    val timestampNanos: Long? = null,
)

data class AttitudeOutput(
    val pitchDegrees: Double? = null,
    val rollDegrees: Double? = null,
    val horizontalState: HorizontalState = HorizontalState.UNKNOWN,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
    val timestampNanos: Long? = null,
)
