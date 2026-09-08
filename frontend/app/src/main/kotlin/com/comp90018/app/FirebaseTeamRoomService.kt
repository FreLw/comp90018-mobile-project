package com.comp90018.app

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/** Separate persistence boundary for two-person treasure teams; never used for direct messages. */
data class TeamRoom(
    val id: String,
    val creatorId: String,
    val memberIds: List<String>,
    val taskId: String,
    val taskTitle: String,
    val taskStatus: String,
)

object FirebaseTeamRoomService {
    fun observeMembership(
        firestore: FirebaseFirestore,
        userId: String,
        onChange: (String?, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("teamMemberships").document(userId)
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) onChange(null, exception.localizedMessage ?: "Unable to load team membership")
            else onChange(snapshot?.getString("roomId"), null)
        }

    fun createRoom(
        firestore: FirebaseFirestore,
        userId: String,
        onComplete: (String?, String?) -> Unit,
    ) {
        val membership = firestore.collection("teamMemberships").document(userId)
        membership.get().addOnCompleteListener { membershipTask ->
            if (!membershipTask.isSuccessful) {
                onComplete(null, membershipTask.exception?.localizedMessage ?: "Unable to check team membership")
            } else if (membershipTask.result.exists()) {
                onComplete(null, "You are already in a treasure room")
            } else {
                val room = firestore.collection("teamRooms").document()
                firestore.batch().apply {
                    set(room, mapOf(
                        "creatorId" to userId,
                        "memberIds" to listOf(userId),
                        // Reserved for the task assignment feature.
                        "taskId" to "",
                        "taskTitle" to "",
                        "taskStatus" to "unassigned",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ))
                    set(membership, mapOf("roomId" to room.id, "createdAt" to FieldValue.serverTimestamp()))
                }.commit().addOnCompleteListener { task ->
                    onComplete(if (task.isSuccessful) room.id else null, task.exception?.localizedMessage ?: "Unable to create room")
                }
            }
        }
    }

    fun joinRoom(firestore: FirebaseFirestore, roomId: String, userId: String, onComplete: (String?) -> Unit) {
        val cleanRoomId = roomId.trim()
        if (cleanRoomId.isBlank()) return onComplete("Enter a room ID")
        val membership = firestore.collection("teamMemberships").document(userId)
        val room = firestore.collection("teamRooms").document(cleanRoomId)
        firestore.runTransaction { transaction ->
            if (transaction.get(membership).exists()) throw IllegalStateException("You are already in a treasure room")
            val roomSnapshot = transaction.get(room)
            if (!roomSnapshot.exists()) throw IllegalStateException("Room not found")
            val members = roomSnapshot.get("memberIds") as? List<*> ?: emptyList<String>()
            if (members.size >= 2) throw IllegalStateException("This room already has two members")
            transaction.update(room, mapOf(
                "memberIds" to FieldValue.arrayUnion(userId),
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            transaction.set(membership, mapOf("roomId" to cleanRoomId, "createdAt" to FieldValue.serverTimestamp()))
        }.addOnCompleteListener { task -> onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to join room") }
    }
}
