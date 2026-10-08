package com.comp90018.app.contextengine.challenge

import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.sensors.SensorConfig
import com.comp90018.app.sensors.SensorValidity

/** Observation challenges combine acceleration and shake into one shared stillness budget. */
internal object ChallengePoise {
    private val thresholds = SensorConfig()

    // Both terms are nonnegative: one quiet sensor cannot cancel movement in the other.
    fun stillnessScore(snapshot: DeviceContextSnapshot): Double? {
        val motion = snapshot.motionStability.motion
        val shake = snapshot.motionStability.stability
        val acceleration = reading(motion.smoothedMagnitude, motion.validity) ?: return null
        val variation = reading(shake.variation, shake.validity) ?: return null
        return acceleration / thresholds.motionExitThreshold + variation / thresholds.stabilityEnterThreshold
    }

    fun turnScore(snapshot: DeviceContextSnapshot): Double? {
        val turn = snapshot.orientation.rotation
        return reading(turn.angularVelocityMagnitude, turn.validity)?.div(thresholds.rotationExitThreshold)
    }

    fun isStill(snapshot: DeviceContextSnapshot): Boolean = stillnessScore(snapshot)?.let { it <= 1.0 } == true
    fun isTurnStill(snapshot: DeviceContextSnapshot): Boolean = turnScore(snapshot)?.let { it <= 1.0 } == true

    private fun reading(value: Double?, validity: SensorValidity): Double? =
        value?.takeIf { validity == SensorValidity.VALID && it.isFinite() && it >= 0.0 }
}
