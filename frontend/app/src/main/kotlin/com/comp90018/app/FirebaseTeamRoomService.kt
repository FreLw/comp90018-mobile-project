package com.comp90018.app

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import java.security.SecureRandom

/** Separate persistence boundary for treasure teams; never used for direct messages. */
data class TeamRoom(
    val id: String,
    val creatorId: String,
    val memberIds: List<String>,
    val name: String,
    val isPublic: Boolean,
    val maxMembers: Int,
    val pinnedTreasureId: String,
    val taskId: String,
    val taskTitle: String,
    val taskStatus: String,
    val createdAtMillis: Long = 0L,
)

data class TeamRoomMember(
    val uid: String,
    val name: String,
    val avatarUrl: String,
)

private class RoomIdCollisionException : IllegalStateException("Generated room ID is already in use")

object FirebaseTeamRoomService {
    private val roomIdRandom = SecureRandom()

    fun observeMembership(
        firestore: FirebaseFirestore,
        userId: String,
        onChange: (String?, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("teamMemberships").document(userId)
        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
            when {
                exception != null -> onChange(null, exception.localizedMessage ?: "Unable to load team membership")
                snapshot?.metadata?.hasPendingWrites() == true -> Unit
                else -> onChange(snapshot?.getString("roomId"), null)
            }
        }

    fun observeUnreadMessages(
        firestore: FirebaseFirestore,
        userId: String,
        onChange: (Int, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("teamMemberships").document(userId)
        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, exception ->
            if (exception != null) {
                onChange(0, exception.localizedMessage ?: "Unable to load room unread messages")
            } else if (snapshot?.metadata?.hasPendingWrites() == true) {
                return@addSnapshotListener
            } else {
                onChange(
                    (snapshot?.getLong("unreadCount") ?: 0L)
                        .coerceIn(0L, Int.MAX_VALUE.toLong())
                        .toInt(),
                    null,
                )
            }
        }

    fun createRoom(
        firestore: FirebaseFirestore,
        userId: String,
        name: String,
        isPublic: Boolean,
        maxMembers: Int,
        onComplete: (String?, String?) -> Unit,
    ) {
        val cleanName = name.trim().take(40)
        if (cleanName.isBlank()) return onComplete(null, "Give your room a name")
        if (maxMembers !in 2..6) return onComplete(null, "Room size must be between 2 and 6")
        createRoomWithGeneratedId(firestore, userId, cleanName, isPublic, maxMembers, attemptsLeft = 8, onComplete)
    }

    private fun createRoomWithGeneratedId(
        firestore: FirebaseFirestore,
        userId: String,
        name: String,
        isPublic: Boolean,
        maxMembers: Int,
        attemptsLeft: Int,
        onComplete: (String?, String?) -> Unit,
    ) {
        val roomId = roomIdRandom.nextInt(900_000).plus(100_000).toString()
        val room = firestore.collection("teamRooms").document(roomId)
        val membership = firestore.collection("teamMemberships").document(userId)
        firestore.runTransaction { transaction ->
            if (transaction.get(membership).exists()) throw IllegalStateException("You are already in a treasure room")
            if (transaction.get(room).exists()) throw RoomIdCollisionException()
            transaction.set(room, mapOf(
                "creatorId" to userId,
                "memberIds" to listOf(userId),
                "name" to name,
                "isPublic" to isPublic,
                "maxMembers" to maxMembers,
                "pinnedTreasureId" to "",
                "taskId" to "",
                "taskTitle" to "",
                "taskStatus" to "unassigned",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            transaction.set(membership, mapOf(
                "roomId" to roomId,
                "createdAt" to FieldValue.serverTimestamp(),
                "unreadCount" to 0,
            ))
        }.addOnCompleteListener { task ->
            val collision = task.exception is RoomIdCollisionException || task.exception?.cause is RoomIdCollisionException
            if (!task.isSuccessful && collision && attemptsLeft > 1) {
                createRoomWithGeneratedId(firestore, userId, name, isPublic, maxMembers, attemptsLeft - 1, onComplete)
            } else {
                onComplete(
                    if (task.isSuccessful) roomId else null,
                    if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to create room",
                )
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
            val maxMembers = (roomSnapshot.getLong("maxMembers") ?: 2L).toInt().coerceIn(2, 6)
            if (members.size >= maxMembers) throw IllegalStateException("This room is full")
            transaction.update(room, mapOf(
                "memberIds" to FieldValue.arrayUnion(userId),
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
            transaction.set(membership, mapOf("roomId" to cleanRoomId, "createdAt" to FieldValue.serverTimestamp(), "unreadCount" to 0))
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
                    name = snapshot.getString("name").orEmpty().ifBlank { "Treasure Room" },
                    isPublic = snapshot.getBoolean("isPublic") ?: false,
                    maxMembers = (snapshot.getLong("maxMembers") ?: 2L).toInt().coerceIn(2, 6),
                    pinnedTreasureId = snapshot.getString("pinnedTreasureId").orEmpty(),
                    taskId = snapshot.getString("taskId").orEmpty(),
                    taskTitle = snapshot.getString("taskTitle").orEmpty(),
                    taskStatus = snapshot.getString("taskStatus").orEmpty(),
                    createdAtMillis = snapshot.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                ), null)
            }
        }

    fun observePublicRooms(
        firestore: FirebaseFirestore,
        onChange: (List<TeamRoom>, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("teamRooms")
        .whereEqualTo("isPublic", true)
        .limit(40)
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onChange(emptyList(), exception.localizedMessage ?: "Unable to load the room plaza")
            } else {
                val rooms = snapshot?.documents.orEmpty().map { document ->
                    TeamRoom(
                        id = document.id,
                        creatorId = document.getString("creatorId").orEmpty(),
                        memberIds = (document.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                        name = document.getString("name").orEmpty().ifBlank { "Treasure Room" },
                        isPublic = true,
                        maxMembers = (document.getLong("maxMembers") ?: 2L).toInt().coerceIn(2, 6),
                        pinnedTreasureId = document.getString("pinnedTreasureId").orEmpty(),
                        taskId = document.getString("taskId").orEmpty(),
                        taskTitle = document.getString("taskTitle").orEmpty(),
                        taskStatus = document.getString("taskStatus").orEmpty(),
                        createdAtMillis = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                }.filter { it.memberIds.size < it.maxMembers }.sortedByDescending { it.createdAtMillis }
                onChange(rooms, null)
            }
        }

    fun updateRoomSettings(
        firestore: FirebaseFirestore,
        roomId: String,
        userId: String,
        name: String,
        isPublic: Boolean,
        maxMembers: Int,
        onComplete: (String?) -> Unit,
    ) {
        val cleanName = name.trim().take(40)
        if (cleanName.isBlank()) return onComplete("Give your room a name")
        if (maxMembers !in 2..6) return onComplete("Room size must be between 2 and 6")
        val room = firestore.collection("teamRooms").document(roomId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(room)
            if (!snapshot.exists()) throw IllegalStateException("Room not found")
            if (snapshot.getString("creatorId") != userId) throw IllegalStateException("Only the room owner can edit room settings")
            val memberCount = (snapshot.get("memberIds") as? List<*>)?.size ?: 0
            if (maxMembers < memberCount) throw IllegalStateException("Room size cannot be smaller than the current team")
            transaction.update(room, mapOf(
                "name" to cleanName,
                "isPublic" to isPublic,
                "maxMembers" to maxMembers,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
        }.addOnCompleteListener { task ->
            onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to update room")
        }
    }

    fun pinCollectedTreasure(
        firestore: FirebaseFirestore,
        roomId: String,
        userId: String,
        treasureId: String,
        onComplete: (String?) -> Unit,
    ) {
        val cleanTreasureId = treasureId.trim()
        if (cleanTreasureId.isBlank()) return onComplete("Choose a collected treasure")
        val room = firestore.collection("teamRooms").document(roomId)
        val collectionItem = firestore.collection("users").document(userId)
            .collection("treasureCollection").document(cleanTreasureId)
        firestore.runTransaction { transaction ->
            val roomSnapshot = transaction.get(room)
            if (!roomSnapshot.exists()) throw IllegalStateException("Room not found")
            val members = (roomSnapshot.get("memberIds") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (userId !in members) throw IllegalStateException("Only room members can place a treasure")
            val treasureSnapshot = transaction.get(collectionItem)
            if (!treasureSnapshot.exists() || treasureSnapshot.getString("status") != "discovered") {
                throw IllegalStateException("Only treasures in your collection can be placed here")
            }
            transaction.update(room, mapOf(
                "pinnedTreasureId" to cleanTreasureId,
                "updatedAt" to FieldValue.serverTimestamp(),
            ))
        }.addOnCompleteListener { task ->
            onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to place treasure")
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
                        imageUrl = document.getString("imageUrl").orEmpty(),
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
        val message = mapOf(
            "senderId" to senderId,
            "senderName" to senderName,
            "senderAvatarUrl" to senderAvatarUrl,
            "text" to cleanText,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        sendTeamMessage(firestore, roomId, senderId, message, onComplete)
    }

    fun sendImage(firestore: FirebaseFirestore, roomId: String, senderId: String, senderName: String, senderAvatarUrl: String, imageUri: Uri, onComplete: (String?) -> Unit) {
        FirebaseSocialService.uploadChatImage("teamRooms", roomId, senderId, imageUri) { url, error ->
            if (url == null) return@uploadChatImage onComplete(error)
            val message = mapOf(
                "senderId" to senderId, "senderName" to senderName.take(30), "senderAvatarUrl" to senderAvatarUrl,
                "text" to "", "imageUrl" to url, "createdAt" to FieldValue.serverTimestamp(),
            )
            sendTeamMessage(firestore, roomId, senderId, message, onComplete)
        }
    }

    fun markMessagesRead(firestore: FirebaseFirestore, userId: String) {
        firestore.collection("teamMemberships").document(userId).update("unreadCount", 0)
    }

    private fun sendTeamMessage(
        firestore: FirebaseFirestore,
        roomId: String,
        senderId: String,
        message: Map<String, Any>,
        onComplete: (String?) -> Unit,
    ) {
        val room = firestore.collection("teamRooms").document(roomId)
        room.get().addOnCompleteListener { roomTask ->
            if (!roomTask.isSuccessful) {
                onComplete(roomTask.exception?.localizedMessage ?: "Unable to send message")
                return@addOnCompleteListener
            }
            val recipients = (roomTask.result.get("memberIds") as? List<*>)
                ?.filterIsInstance<String>()
                ?.filter { it != senderId }
                .orEmpty()
            firestore.batch().apply {
                set(room.collection("messages").document(), message)
                recipients.forEach { recipient ->
                    update(firestore.collection("teamMemberships").document(recipient), "unreadCount", FieldValue.increment(1))
                }
            }.commit().addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to send message")
            }
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
