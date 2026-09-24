package com.comp90018.app.contextengine.challenge

import com.comp90018.app.sensors.location.GeoCoordinate

/**
 * Initial challenge tuning. Target coordinates and fixed viewing headings are deliberately
 * required from the caller because their final values must be calibrated on site.
 */
object RelicChallengeConfigs {
    fun unionLawnPhoto(
        challengeId: String,
        targetLocation: GeoCoordinate,
        insideRadiusMeters: Double,
        requiredHeadingDegrees: Double,
    ) = RelicChallengeConfig(
        challengeId = challengeId,
        type = RelicChallengeType.UNION_LAWN_PHOTO,
        targetLocation = targetLocation,
        insideRadiusMeters = insideRadiusMeters,
        requiredHeadingDegrees = requiredHeadingDegrees,
        headingToleranceDegrees = 12.0,
        requiresStationary = false,
        requiresStability = false,
        requiresRotationStill = false,
        requiresHorizontal = false,
        holdDurationNanos = 800_000_000L,
        photoActionRequired = true,
    )

    fun wilsonHallObservation(
        challengeId: String,
        targetLocation: GeoCoordinate,
        insideRadiusMeters: Double,
        requiredHeadingDegrees: Double,
    ) = RelicChallengeConfig(
        challengeId = challengeId,
        type = RelicChallengeType.WILSON_HALL_OBSERVATION,
        targetLocation = targetLocation,
        insideRadiusMeters = insideRadiusMeters,
        requiredHeadingDegrees = requiredHeadingDegrees,
        headingToleranceDegrees = 10.0,
        requiresStationary = true,
        requiresStability = true,
        requiresRotationStill = true,
        requiresHorizontal = false,
        holdDurationNanos = 3_000_000_000L,
        photoActionRequired = false,
    )

    fun oldQuadExcavation(
        challengeId: String,
        targetLocation: GeoCoordinate,
        insideRadiusMeters: Double,
    ) = RelicChallengeConfig(
        challengeId = challengeId,
        type = RelicChallengeType.OLD_QUAD_EXCAVATION,
        targetLocation = targetLocation,
        insideRadiusMeters = insideRadiusMeters,
        requiredHeadingDegrees = null,
        headingToleranceDegrees = 0.0,
        requiresStationary = true,
        requiresStability = true,
        requiresRotationStill = true,
        requiresHorizontal = true,
        holdDurationNanos = 3_000_000_000L,
        photoActionRequired = false,
    )

    fun southLawnViewingAngle(
        challengeId: String,
        targetLocation: GeoCoordinate,
        insideRadiusMeters: Double,
        requiredHeadingDegrees: Double,
    ) = RelicChallengeConfig(
        challengeId = challengeId,
        type = RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE,
        targetLocation = targetLocation,
        insideRadiusMeters = insideRadiusMeters,
        requiredHeadingDegrees = requiredHeadingDegrees,
        headingToleranceDegrees = 8.0,
        requiresStationary = true,
        requiresStability = true,
        requiresRotationStill = true,
        requiresHorizontal = false,
        holdDurationNanos = 1_200_000_000L,
        photoActionRequired = false,
    )
}
