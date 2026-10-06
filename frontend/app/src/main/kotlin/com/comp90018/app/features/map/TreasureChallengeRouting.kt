package com.comp90018.app.features.map

import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.contextengine.challenge.hasRequiredTaskRules

internal enum class HuntEntry { CHALLENGE, MEMBER_QUIZ }

/** A member quiz remains a room task; the owner and solo explorer use the physical challenge. */
internal fun huntEntry(memberTaskPending: Boolean, isOwner: Boolean): HuntEntry =
    if (memberTaskPending && !isOwner) HuntEntry.MEMBER_QUIZ else HuntEntry.CHALLENGE

/** Identity and rules must agree before any sensor route can be opened. */
internal fun MapRelic.validatedChallengeConfig(): RelicChallengeConfig? {
    val expected = when (id) {
        "union_lawn_lost_lake" -> RelicChallengeType.UNION_LAWN_PHOTO
        "wilson_hall_rosette" -> RelicChallengeType.WILSON_HALL_OBSERVATION
        "old_quad_fossil" -> RelicChallengeType.OLD_QUAD_EXCAVATION
        "south_lawn_atlas" -> RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE
        "system_garden_glasshouse" -> RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE
        "grainger_tone_tool" -> RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL
        else -> return null
    }
    return challengeConfig?.takeIf { it.type == expected && it.hasRequiredTaskRules() }
}
