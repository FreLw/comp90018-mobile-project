package com.comp90018.app.data.treasure

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
        historicalImageUrl = getString("historicalImageUrl")?.trim().orEmpty(),
        historicalImageCredit = getString("historicalImageCredit")?.trim().orEmpty(),
        sourceTitle = getString("sourceTitle")?.trim().orEmpty(),
        sourceUrl = getString("sourceUrl")?.trim().orEmpty(),
        treasureType = getString("treasureType")?.trim().orEmpty(),
        coordinate = GeoCoordinate(latitude, longitude),
        insideRadiusMeters = number("insideRadiusMeters") ?: 20.0,
        nearbyRadiusMeters = number("nearbyRadiusMeters") ?: 60.0,
        sortOrder = number("sortOrder")?.toInt() ?: Int.MAX_VALUE,
    )
}

private fun DocumentSnapshot.number(field: String): Double? = (get(field) as? Number)?.toDouble()

private fun Exception.toTreasureMessage(): String = when {
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "Treasure access is not enabled in Firestore Rules yet."
    else -> localizedMessage ?: "Unable to load treasures from Firebase."
}
