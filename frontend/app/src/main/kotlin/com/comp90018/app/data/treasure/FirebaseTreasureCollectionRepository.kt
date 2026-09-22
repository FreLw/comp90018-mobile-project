package com.comp90018.app.data.treasure

import com.comp90018.app.data.social.Subscription
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

/** Firestore-backed collection of treasures discovered by one signed-in user. */
class FirebaseTreasureCollectionRepository(
    private val firestore: FirebaseFirestore,
) : TreasureCollectionRepository {
    override fun observeDiscoveredTreasureIds(
        userId: String,
        onChange: (Set<String>, String?) -> Unit,
    ): Subscription {
        val registration = collection(userId).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onChange(emptySet(), exception.toCollectionMessage())
                return@addSnapshotListener
            }

            val discoveredIds = snapshot?.documents.orEmpty()
                .filter { document ->
                    document.getString("ownerUid") == userId &&
                        document.getString("status") == DISCOVERED_STATUS
                }
                .mapTo(mutableSetOf()) { document ->
                    document.getString("treasureId")?.takeIf(String::isNotBlank) ?: document.id
                }
            onChange(discoveredIds, null)
        }
        return Subscription { registration.remove() }
    }

    override fun addDiscoveredTreasure(
        userId: String,
        treasureId: String,
        onComplete: (String?) -> Unit,
    ) {
        val cleanTreasureId = treasureId.trim()
        if (userId.isBlank() || cleanTreasureId.isBlank()) {
            onComplete("Unable to save this treasure.")
            return
        }

        val reference = collection(userId).document(cleanTreasureId)
        reference.get().addOnCompleteListener { readTask ->
            if (!readTask.isSuccessful) {
                onComplete(readTask.exception?.toCollectionMessage() ?: "Unable to check your collection.")
                return@addOnCompleteListener
            }
            if (readTask.result.exists()) {
                onComplete(null)
                return@addOnCompleteListener
            }

            reference.set(
                mapOf(
                    "ownerUid" to userId,
                    "treasureId" to cleanTreasureId,
                    "status" to DISCOVERED_STATUS,
                    "discoveredAt" to FieldValue.serverTimestamp(),
                ),
            ).addOnCompleteListener { writeTask ->
                onComplete(if (writeTask.isSuccessful) null else writeTask.exception?.toCollectionMessage())
            }
        }
    }

    private fun collection(userId: String) = firestore.collection("users")
        .document(userId)
        .collection("treasureCollection")

    private companion object {
        const val DISCOVERED_STATUS = "discovered"
    }
}

private fun Exception.toCollectionMessage(): String = when {
    this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "Treasure collection access is not enabled in Firestore Rules yet."
    else -> localizedMessage ?: "Unable to update your treasure collection."
}
