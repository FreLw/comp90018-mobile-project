package com.comp90018.app

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
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

data class TeamRoomMember(
    val uid: String,
    val name: String,
    val avatarUrl: String,
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

    fun observeRoom(
        firestore: FirebaseFirestore,
        roomId: String,
        onChange: (TeamRoom?, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("teamRooms").document(roomId)
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onChange(null, exception.localizedMessage ?: "Unable to load room")
            } else if (snapshot == null || !snapshot.exists()) {
                onChange(null, "Room not found")
            } else {
                onChange(TeamRoom(
                    id = snapshot.id,
                    creatorId = snapshot.getString("creatorId").orEmpty(),
                    memberIds = (snapshot.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                    taskId = snapshot.getString("taskId").orEmpty(),
                    taskTitle = snapshot.getString("taskTitle").orEmpty(),
                    taskStatus = snapshot.getString("taskStatus").orEmpty(),
                ), null)
            }
        }

    fun observeMessages(
        firestore: FirebaseFirestore,
        roomId: String,
        onChange: (List<ChatMessage>, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("teamRooms").document(roomId).collection("messages")
        .orderBy("createdAt")
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onChange(emptyList(), exception.localizedMessage ?: "Unable to load messages")
            } else {
                onChange(snapshot?.documents.orEmpty().map { document ->
                    ChatMessage(
                        id = document.id,
                        senderId = document.getString("senderId").orEmpty(),
                        senderName = document.getString("senderName").orEmpty(),
                        senderAvatarUrl = document.getString("senderAvatarUrl").orEmpty(),
                        text = document.getString("text").orEmpty(),
                        sentAtMillis = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                }, null)
            }
        }

    fun sendMessage(
        firestore: FirebaseFirestore,
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        text: String,
        onComplete: (String?) -> Unit,
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return onComplete(null)
        firestore.collection("teamRooms").document(roomId).collection("messages").document().set(mapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "senderAvatarUrl" to senderAvatarUrl,
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp(),
        )).addOnCompleteListener { task ->
            onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to send message")
        }
    }

    fun loadMembers(
        firestore: FirebaseFirestore,
        memberIds: List<String>,
        onComplete: (List<TeamRoomMember>) -> Unit,
    ) {
        if (memberIds.isEmpty()) return onComplete(emptyList())
        firestore.collection("users").whereIn(FieldPath.documentId(), memberIds).get()
            .addOnCompleteListener { task ->
                val membersById = (if (task.isSuccessful) task.result?.documents.orEmpty() else emptyList()).associate { document ->
                    document.id to TeamRoomMember(
                        uid = document.id,
                        name = document.getString("displayName").orEmpty().ifBlank { document.getString("username").orEmpty() },
                        avatarUrl = document.getString("avatarUrl").orEmpty(),
                    )
                }.orEmpty()
                onComplete(memberIds.map { id -> membersById[id] ?: TeamRoomMember(id, "Explorer ${id.takeLast(5)}", "") })
            }
    }

    fun leaveRoom(firestore: FirebaseFirestore, roomId: String, userId: String, onComplete: (String?) -> Unit) {
        val room = firestore.collection("teamRooms").document(roomId)
        val membership = firestore.collection("teamMemberships").document(userId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(room)
            if (!snapshot.exists()) throw IllegalStateException("Room not found")
            if (snapshot.getString("creatorId") == userId) throw IllegalStateException("The room owner must dismiss the room")
            val members = (snapshot.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (userId !in members) throw IllegalStateException("You are not a member of this room")
            transaction.update(room, mapOf(
                "memberIds" to FieldValue.arrayRemove(userId),
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            transaction.delete(membership)
        }.addOnCompleteListener { task ->
            onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to leave room")
        }
    }

    fun dismissRoom(firestore: FirebaseFirestore, roomId: String, userId: String, onComplete: (String?) -> Unit) {
        val room = firestore.collection("teamRooms").document(roomId)
        room.get().addOnCompleteListener { roomTask ->
            if (!roomTask.isSuccessful) {
                onComplete(roomTask.exception?.localizedMessage ?: "Unable to load room")
                return@addOnCompleteListener
            }
            val snapshot = roomTask.result
            if (!snapshot.exists()) {
                onComplete(null)
                return@addOnCompleteListener
            }
            if (snapshot.getString("creatorId") != userId) {
                onComplete("Only the room owner can dismiss this room")
                return@addOnCompleteListener
            }
            val memberIds = (snapshot.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            fun deleteMessageBatch() {
                room.collection("messages").limit(400).get().addOnCompleteListener { messagesTask ->
                    if (!messagesTask.isSuccessful) {
                        onComplete(messagesTask.exception?.localizedMessage ?: "Unable to dismiss room")
                        return@addOnCompleteListener
                    }
                    val messages = messagesTask.result.documents
                    if (messages.isNotEmpty()) {
                        firestore.batch().apply { messages.forEach { delete(it.reference) } }.commit()
                            .addOnCompleteListener { batch ->
                                if (batch.isSuccessful) deleteMessageBatch()
                                else onComplete(batch.exception?.localizedMessage ?: "Unable to dismiss room")
                            }
                    } else {
                        firestore.batch().apply {
                            memberIds.forEach { delete(firestore.collection("teamMemberships").document(it)) }
                            delete(room)
                        }.commit().addOnCompleteListener { batch ->
                            onComplete(if (batch.isSuccessful) null else batch.exception?.localizedMessage ?: "Unable to dismiss room")
                        }
                    }
                }
            }
            deleteMessageBatch()
        }
    }
}
