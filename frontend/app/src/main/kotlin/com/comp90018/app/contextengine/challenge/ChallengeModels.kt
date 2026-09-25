package com.comp90018.app.contextengine.challenge

import com.comp90018.app.sensors.location.GeoCoordinate

enum class RelicChallengeType {
    UNION_LAWN_PHOTO,
    WILSON_HALL_OBSERVATION,
    OLD_QUAD_EXCAVATION,
    SOUTH_LAWN_VIEWING_ANGLE,
    SYSTEM_GARDEN_GLASSHOUSE,
    GRAINGER_MUSEUM_TONE_TOOL,
}

data class RelicChallengeConfig(
    val challengeId: String,
    val type: RelicChallengeType,
    val targetLocation: GeoCoordinate,
    val insideRadiusMeters: Double,
    val requiredHeadingDegrees: Double?,
    val headingToleranceDegrees: Double,
    val requiresStationary: Boolean,
    val requiresStability: Boolean,
    val requiresRotationStill: Boolean,
    val requiresHorizontal: Boolean,
    val holdDurationNanos: Long,
    val photoActionRequired: Boolean,
    val requiresSound: Boolean = false,
    val soundThresholdDecibels: Double? = null,
) {
    init {
        require(challengeId.isNotBlank())
        require(targetLocation.latitude.isFinite() && targetLocation.latitude in -90.0..90.0)
        require(targetLocation.longitude.isFinite() && targetLocation.longitude in -180.0..180.0)
        require(insideRadiusMeters.isFinite() && insideRadiusMeters >= 0.0)
        require(requiredHeadingDegrees == null || requiredHeadingDegrees.isFinite())
        require(headingToleranceDegrees.isFinite() && headingToleranceDegrees in 0.0..180.0)
        require(holdDurationNanos >= 0L)
        require(!requiresSound || soundThresholdDecibels != null) {
            "soundThresholdDecibels is required when requiresSound is true"
        }
        require(soundThresholdDecibels == null || soundThresholdDecibels.isFinite())
    }
}

enum class ChallengeCondition {
    LOCATION_INSIDE,
    PHONE_HORIZONTAL,
    HEADING_ALIGNED,
    STATIONARY,
    STABLE,
    ROTATION_STILL,
    SOUND_DETECTED,
}

data class ChallengeConditionState(
    val condition: ChallengeCondition,
    val satisfied: Boolean,
)

enum class ChallengeInstruction {
    MOVE_CLOSER,
    KEEP_PHONE_LEVEL,
    FIND_VIEWING_DIRECTION,
    TURN_LEFT,
    TURN_RIGHT,
    STOP_MOVING,
    HOLD_STILL,
    HOLD_ALIGNMENT,
    HOLD_OBSERVATION,
    HOLD_EXCAVATION_POSITION,
    HOLD_VIEWING_ANGLE,
    HOLD_GLASSHOUSE_POSITION,
    MAKE_SOUND,
    TAKE_PHOTO,
    COMPLETED,
}

enum class ChallengeEvent {
    PHOTO_CAPTURED,
}

data class ChallengeProgress(
    val challengeId: String,
    val challengeType: RelicChallengeType,
    val requiredConditions: List<ChallengeConditionState>,
    val instruction: ChallengeInstruction,
    val holdProgress: Double,
    val actionReady: Boolean,
    val completed: Boolean,
)
