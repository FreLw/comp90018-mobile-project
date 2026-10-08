package com.comp90018.app.features.treasurechallenge

import com.comp90018.app.contextengine.challenge.ChallengeCondition
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.sensors.DirectionProcessor
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.LocationCalculator

/** A bounded presentation of live readings. Only evaluator conditions can illuminate the artwork. */
internal data class QuestVisualSignals(
    val headingError: Float = 0f,
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val motion: Float = 0f,
    val instability: Float = 0f,
    val rotation: Float = 0f,
    val sound: Float = 0f,
    val proximity: Float = 0f,
    val distanceMeters: Double? = null,
    val soundDecibels: Double? = null,
) {
    companion object {
        fun from(state: TreasureChallengeUiState): QuestVisualSignals {
            val snapshot = state.latestSnapshot ?: return QuestVisualSignals()
            val attitude = snapshot.orientation.attitude
            val direction = snapshot.orientation.direction
            val location = snapshot.location
            val validHeading = direction.headingValidity == SensorValidity.VALID
            val error = if (validHeading) state.angularErrorDegrees.finiteOrNull()
                ?: state.requiredHeadingDegrees?.let { target ->
                    direction.headingDegrees.finiteOrNull()?.let { DirectionProcessor.angularDifference(it, target) }
                } ?: direction.headingDegrees.finiteOrNull()?.let { DirectionProcessor.angularDifference(it, 0.0) }
            else null
            val distance = if (location.validity == SensorValidity.VALID) {
                location.distanceToTargetMeters.finiteOrNull() ?: location.currentLocation?.let { current ->
                    location.targetLocation?.let { target ->
                        runCatching { LocationCalculator.distanceMeters(current, target) }.getOrNull().finiteOrNull()
                    }
                }
            } else null
            val db = snapshot.sound.decibels.finiteOrNull().takeIf { snapshot.sound.validity == SensorValidity.VALID }
            fun reading(value: Double?, valid: SensorValidity, limit: Double): Float =
                if (valid == SensorValidity.VALID) (value.finiteOrNull() ?: 0.0).coerceIn(-limit, limit).toFloat() else 0f
            return QuestVisualSignals(
                headingError = (error ?: 0.0).coerceIn(-180.0, 180.0).toFloat(),
                roll = reading(attitude.rollDegrees, attitude.validity, 45.0),
                pitch = reading(attitude.pitchDegrees, attitude.validity, 45.0),
                motion = reading(snapshot.motionStability.motion.smoothedMagnitude,
                    snapshot.motionStability.motion.validity, 3.0).coerceAtLeast(0f) / 3f,
                instability = reading(snapshot.motionStability.stability.variation,
                    snapshot.motionStability.stability.validity, 1.0).coerceAtLeast(0f),
                rotation = reading(snapshot.orientation.rotation.angularVelocityMagnitude,
                    snapshot.orientation.rotation.validity, 3.0).coerceAtLeast(0f) / 3f,
                sound = if (db == null) 0f else ((db + 80.0) / 80.0).coerceIn(0.0, 1.0).toFloat(),
                proximity = if (distance == null) 0f else
                    (1.0 - distance / (state.insideRadiusMeters.coerceAtLeast(1.0) * 5.0)).coerceIn(0.0, 1.0).toFloat(),
                distanceMeters = distance,
                soundDecibels = db,
            )
        }
    }
}

internal fun TreasureChallengeUiState.conditionLit(condition: ChallengeCondition): Boolean =
    conditionStates.any { it.condition == condition && it.satisfied }

private fun Double?.finiteOrNull(): Double? = this?.takeIf { it.isFinite() }

/** Distance-linked engraving is fully gilded at arrival, empty at five times the arrival radius. */
internal fun questApproachFill(distanceMeters: Double?, insideRadiusMeters: Double): Float {
    val distance = distanceMeters.finiteOrNull()?.takeIf { it >= 0.0 } ?: return 0f
    val radius = insideRadiusMeters.coerceAtLeast(1.0)
    return (1.0 - (distance - radius) / (radius * 4.0)).coerceIn(0.0, 1.0).toFloat()
}

/** These captions name measurements, rather than adding new completion conditions. */
internal fun questMeasurement(state: TreasureChallengeUiState): Pair<String, String> {
    val signals = QuestVisualSignals.from(state)
    fun number(value: Double?, unit: String): String =
        value?.takeIf(Double::isFinite)?.let { String.format(java.util.Locale.US, "%.1f", it) + unit } ?: "—"
    return when (state.challengeType) {
        RelicChallengeType.UNION_LAWN_PHOTO -> "DISTANCE" to number(signals.distanceMeters, " m")
        RelicChallengeType.OLD_QUAD_EXCAVATION ->
            "BALANCE" to number(state.latestSnapshot?.orientation?.attitude?.takeIf { it.validity == SensorValidity.VALID }?.rollDegrees, "°")
        RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL ->
            "SOUND LEVEL" to number(signals.soundDecibels, " dBFS")
        RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE ->
            "DISTANCE TO SITE" to number(signals.distanceMeters, " m")
        RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE ->
            "DISTANCE TO ATLAS" to number(signals.distanceMeters, " m")
        RelicChallengeType.WILSON_HALL_OBSERVATION ->
            "DISTANCE TO ROSETTE" to number(signals.distanceMeters, " m")
    }
}
