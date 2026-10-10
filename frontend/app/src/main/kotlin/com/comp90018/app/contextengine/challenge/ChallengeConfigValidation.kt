package com.comp90018.app.contextengine.challenge

/*
 * Checks that each challenge configuration contains its required task rules.
 * Invalid catalogue rules must not open a task whose UI could suggest an unattainable completion.
 */

/** Reject incomplete or contradictory catalogue rules rather than silently weakening a task. */
fun RelicChallengeConfig.hasRequiredTaskRules(): Boolean {
    val noMotion = !requiresStationary && !requiresStability && !requiresRotationStill && !requiresHorizontal
    return when (type) {
        RelicChallengeType.UNION_LAWN_PHOTO -> requiredHeadingDegrees != null && photoActionRequired && !requiresSound && noMotion
        RelicChallengeType.WILSON_HALL_OBSERVATION -> requiredHeadingDegrees != null && requiresStability && requiresRotationStill && holdDurationNanos >= 3_000_000_000L && !photoActionRequired && !requiresSound
        RelicChallengeType.OLD_QUAD_EXCAVATION -> requiredHeadingDegrees == null && requiresHorizontal && requiresStationary && requiresStability && holdDurationNanos >= 3_000_000_000L && !photoActionRequired && !requiresSound
        RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> requiredHeadingDegrees != null && requiresStability && holdDurationNanos > 0L && !photoActionRequired && !requiresSound
        RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> requiredHeadingDegrees == null && photoActionRequired && !requiresSound && noMotion
        RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> requiredHeadingDegrees == null && requiresSound && soundThresholdDecibels != null && holdDurationNanos > 0L && !photoActionRequired && noMotion
    }
}
