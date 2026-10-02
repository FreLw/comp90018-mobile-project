package com.comp90018.app.data.treasure

import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.contextengine.challenge.CalibrationStatus
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.location.GeoCoordinate
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/** Firestore-backed, read-only treasure catalogue. */
class FirebaseTreasureRepository(
    private val firestore: FirebaseFirestore,
) : TreasureRepository {
    override fun observeEnabledTreasures(
        onChange: (List<MapRelic>, String?) -> Unit,
    ): Subscription {
        val registration = firestore.collection("treasures")
            .whereEqualTo("enabled", true)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onChange(emptyList(), exception.toTreasureMessage())
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents.orEmpty()
                val treasures = documents.mapNotNull(DocumentSnapshot::toMapRelic)
                    .sortedWith(compareBy<MapRelic> { it.sortOrder }.thenBy { it.name })
                val invalidCount = documents.size - treasures.size
                onChange(
                    treasures,
                    if (invalidCount > 0) "$invalidCount treasure ${if (invalidCount == 1) "record is" else "records are"} missing required fields." else null,
                )
            }
        return Subscription { registration.remove() }
    }
}

private fun DocumentSnapshot.toMapRelic(): MapRelic? {
    val title = getString("title")?.trim().orEmpty()
    val locationName = getString("locationName")?.trim().orEmpty()
    val latitude = number("latitude") ?: return null
    val longitude = number("longitude") ?: return null
    if (title.isBlank() || locationName.isBlank()) return null

    val coordinate = GeoCoordinate(latitude, longitude)
    val insideRadiusMeters = number("insideRadiusMeters") ?: 20.0

    return MapRelic(
        id = getString("id")?.trim().takeUnless { it.isNullOrBlank() } ?: id,
        name = title,
        locationName = locationName,
        description = getString("shortDescription")?.trim().orEmpty(),
        buildingStory = getString("buildingStory")?.trim().orEmpty(),
        story = getString("historicalStory")?.trim().orEmpty(),
        clue = getString("clue")?.trim().orEmpty(),
        coordinateSource = getString("coordinateSource")?.trim().orEmpty(),
        prototypeDesign = getString("prototypeDesign")?.trim().orEmpty(),
        prototypeImageUrl = getString("prototypeImageUrl")?.trim().orEmpty(),
        artworkKey = getString("artworkKey")?.trim().orEmpty(),
        historicalImageUrl = getString("historicalImageUrl")?.trim().orEmpty(),
        historicalImageCredit = getString("historicalImageCredit")?.trim().orEmpty(),
        sourceTitle = getString("sourceTitle")?.trim().orEmpty(),
        sourceUrl = getString("sourceUrl")?.trim().orEmpty(),
        treasureType = getString("treasureType")?.trim().orEmpty(),
        coordinate = coordinate,
        insideRadiusMeters = insideRadiusMeters,
        radarRadiusMeters = number("radarRadiusMeters")?.takeIf { it.isFinite() && it >= insideRadiusMeters } ?: 100.0,
        nearbyRadiusMeters = number("nearbyRadiusMeters") ?: 60.0,
        sortOrder = number("sortOrder")?.toInt() ?: Int.MAX_VALUE,
        challengeConfig = parseChallengeConfig(get("challenge") as? Map<*, *>, coordinate, insideRadiusMeters),
    )
}

/** Parses Firestore challenge data without manufacturing a challenge for a missing map. */
internal fun parseChallengeConfig(
    challenge: Map<*, *>?,
    targetLocation: GeoCoordinate,
    insideRadiusMeters: Double,
): RelicChallengeConfig? {
    challenge ?: return null
    if (challenge["enabled"] == false) return null
    val challengeId = (challenge["challengeId"] as? String)?.trim().takeUnless { it.isNullOrBlank() } ?: return null
    val type = (challenge["type"] as? String)?.let { raw -> runCatching { RelicChallengeType.valueOf(raw) }.getOrNull() }
        ?: return null
    val requiresSound = challenge["requiresSound"] as? Boolean ?: false
    val soundThresholdDecibels = (challenge["soundThresholdDecibels"] as? Number)?.toDouble()

    return runCatching {
        RelicChallengeConfig(
            challengeId = challengeId,
            type = type,
            targetLocation = targetLocation,
            insideRadiusMeters = insideRadiusMeters,
            requiredHeadingDegrees = (challenge["requiredHeadingDegrees"] as? Number)?.toDouble(),
            headingToleranceDegrees = (challenge["headingToleranceDegrees"] as? Number)?.toDouble() ?: 0.0,
            requiresStationary = challenge["requiresStationary"] as? Boolean ?: false,
            requiresStability = challenge["requiresStability"] as? Boolean ?: false,
            requiresRotationStill = challenge["requiresRotationStill"] as? Boolean ?: false,
            requiresHorizontal = challenge["requiresHorizontal"] as? Boolean ?: false,
            holdDurationNanos = ((challenge["holdDurationMs"] as? Number)?.toLong() ?: 0L) * 1_000_000L,
            photoActionRequired = challenge["photoActionRequired"] as? Boolean ?: false,
            requiresSound = requiresSound,
            soundThresholdDecibels = soundThresholdDecibels,
            calibrationStatus = when ((challenge["calibrationStatus"] as? String)?.lowercase()) {
                "pending" -> CalibrationStatus.PENDING
                "calibrated" -> CalibrationStatus.CALIBRATED
                else -> CalibrationStatus.UNKNOWN
            },
            headingReference = (challenge["headingReference"] as? String)?.trim()?.takeIf(String::isNotBlank),
        )
    }.getOrNull()
}

private fun DocumentSnapshot.number(field: String): Double? = (get(field) as? Number)?.toDouble()

private fun Exception.toTreasureMessage(): String = when {
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "Treasure access is not enabled in Firestore Rules yet."
    else -> localizedMessage ?: "Unable to load treasures from Firebase."
}
