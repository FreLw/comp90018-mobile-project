package com.comp90018.app.contextengine

import com.comp90018.app.sensors.MotionState
import com.comp90018.app.sensors.MotionStabilityOutput
import com.comp90018.app.sensors.RotationState
import com.comp90018.app.sensors.address.AddressResult
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.orientation.OrientationOutput

data class DeviceContextSnapshot(
    val location: LocationOutput = LocationOutput(),
    val address: AddressResult = AddressResult(),
    val orientation: OrientationOutput = OrientationOutput(),
    val motionStability: MotionStabilityOutput = MotionStabilityOutput(),
) {
    /** Convenience accessors for the questions this engine most commonly answers. */
    val isMoving: Boolean get() = motionStability.motion.classification == MotionState.MOVING
    val isStationary: Boolean get() = motionStability.motion.classification == MotionState.STATIONARY
    val isRotating: Boolean get() = orientation.rotation.classification == RotationState.ROTATING
    val headingDegrees: Double? get() = orientation.direction.headingDegrees
}
