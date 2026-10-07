package com.comp90018.app.data.rooms

import com.comp90018.app.data.social.Subscription
import com.comp90018.app.sensors.location.GeoCoordinate
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import java.util.Date

data class TeamHuntLocation(
    val uid: String,
    val sessionId: String,
    val coordinate: GeoCoordinate,
    val updatedAtMillis: Long,
    val observedAtMillis: Long,
)

interface TeamHuntLocationRepository {
    fun observe(roomId: String, sessionId: String, onChange: (List<TeamHuntLocation>, String?) -> Unit): Subscription
    fun publish(roomId: String, sessionId: String, uid: String, coordinate: GeoCoordinate, observedAtMillis: Long, onComplete: (String?) -> Unit)
    fun clear(roomId: String, uid: String, onComplete: (String?) -> Unit)
}

/** Only server-confirmed positions count as online; offline caches cannot restore markers. */
class FirebaseTeamHuntLocationRepository(private val firestore: FirebaseFirestore) : TeamHuntLocationRepository {
    private fun positions(roomId: String) = firestore.collection("teamRooms").document(roomId).collection("locations")

    override fun observe(roomId: String, sessionId: String, onChange: (List<TeamHuntLocation>, String?) -> Unit): Subscription {
        var confirmed = emptyList<TeamHuntLocation>()
        var active = true
        val listener = positions(roomId).whereEqualTo("sessionId", sessionId)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (!active) return@addSnapshotListener
                if (error != null || snapshot == null || snapshot.metadata.isFromCache) {
                    confirmed = emptyList()
                    onChange(confirmed, error?.localizedMessage)
                    return@addSnapshotListener
                }
                confirmed = snapshot.documents.mapNotNull { document ->
                    // Retain the prior committed heartbeat until a pending write is acknowledged.
                    if (document.metadata.hasPendingWrites()) {
                        return@mapNotNull confirmed.firstOrNull { it.uid == document.id }
                    }
                    val latitude = document.getDouble("latitude") ?: return@mapNotNull null
                    val longitude = document.getDouble("longitude") ?: return@mapNotNull null
                    val updated = document.getTimestamp("updatedAt") ?: return@mapNotNull null
                    val observed = document.getTimestamp("observedAt") ?: return@mapNotNull null
                    TeamHuntLocation(document.id, sessionId, GeoCoordinate(latitude, longitude),
                        updated.toDate().time, observed.toDate().time)
                }
                onChange(confirmed, null)
            }
        return Subscription { active = false; listener.remove() }
    }

    override fun publish(roomId: String, sessionId: String, uid: String, coordinate: GeoCoordinate, observedAtMillis: Long, onComplete: (String?) -> Unit) {
        positions(roomId).document(uid).set(mapOf(
            "sessionId" to sessionId,
            "latitude" to coordinate.latitude,
            "longitude" to coordinate.longitude,
            "observedAt" to Timestamp(Date(observedAtMillis)),
            "updatedAt" to FieldValue.serverTimestamp(),
        )).addOnSuccessListener { onComplete(null) }
            .addOnFailureListener { onComplete(it.localizedMessage ?: "Unable to share location") }
    }

    override fun clear(roomId: String, uid: String, onComplete: (String?) -> Unit) {
        positions(roomId).document(uid).delete().addOnSuccessListener { onComplete(null) }
            .addOnFailureListener { onComplete(it.localizedMessage ?: "Unable to clear location") }
    }
}
