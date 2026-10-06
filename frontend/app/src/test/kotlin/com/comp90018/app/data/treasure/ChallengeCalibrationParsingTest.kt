package com.comp90018.app.data.treasure

import com.comp90018.app.contextengine.challenge.CalibrationStatus
import com.comp90018.app.contextengine.DeviceContextSnapshot
import com.comp90018.app.contextengine.challenge.ChallengeCondition
import com.comp90018.app.contextengine.challenge.ChallengeRuleEvaluator
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChallengeCalibrationParsingTest {
    private val target = GeoCoordinate(-37.7971, 144.96189)

    @Test fun missingRequiredHeadingIsRejectedEvenWhenCalibrationIsPending() {
        assertNull(parseChallengeConfig(challenge("pending", null), target, 25.0))
    }

    @Test fun calibratedMetadataDoesNotChangeConfiguredHeading() {
        val config = requireNotNull(parseChallengeConfig(challenge("calibrated", 247.0), target, 25.0))
        assertEquals(CalibrationStatus.CALIBRATED, config.calibrationStatus)
        assertEquals("magnetic", config.headingReference)
        assertEquals(247.0, config.requiredHeadingDegrees)
    }

    @Test fun absentChallengeCannotBecomePlaceholderHeading() {
        assertNull(parseChallengeConfig(null, target, 25.0))
        assertNull(MapRelic("missing", "Missing", "Campus", coordinate = target).challengeConfig)
        assertNull(parseChallengeConfig(mapOf("enabled" to false), target, 25.0))
    }

    @Test fun unknownMetadataIsNotSilentlyCalibratedAndReferenceIsPreserved() {
        val config = requireNotNull(parseChallengeConfig(challenge("field-check-needed", 247.0)
            .plus("headingReference" to "custom-reference"), target, 25.0))
        assertEquals(CalibrationStatus.UNKNOWN, config.calibrationStatus)
        assertEquals("custom-reference", config.headingReference)
    }

    private fun challenge(status: String, heading: Double?): Map<String, Any?> = mapOf(
        "enabled" to true,
        "challengeId" to "union-lawn-photo",
        "type" to "UNION_LAWN_PHOTO",
        "calibrationStatus" to status,
        "headingReference" to "magnetic",
        "requiredHeadingDegrees" to heading,
        "headingToleranceDegrees" to 12.0,
        "holdDurationMs" to 800L,
        "photoActionRequired" to true,
    )
}
