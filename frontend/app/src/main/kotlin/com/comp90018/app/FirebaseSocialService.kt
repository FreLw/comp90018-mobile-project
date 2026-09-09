package com.comp90018.app

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class SearchUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val gender: String,
    val bio: String,
)

enum class FriendshipStatus {
    None,
    OutgoingPending,
    IncomingPending,
    Friends,
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String,
    val text: String,
    val sentAtMillis: Long,
)

data class ChatRoomSummary(
    val id: String,
    val title: String,
    val updatedAtMillis: Long,
)

data class IncomingFriendRequest(
    val fromUid: String,
    val fromUsername: String,
)

data class FriendSummary(
    val uid: String,
    val username: String,
)

object FirebaseSocialService {
    fun searchUsers(
        firestore: FirebaseFirestore,
        currentUid: String,
        username: String,
        onComplete: (List<SearchUser>, String?) -> Unit,
    ) {
        val normalized = username.trim().lowercase()
        if (normalized.isBlank()) {
            onComplete(emptyList(), "Enter a username")
            return
        }

        firestore.collection("users")
            .whereEqualTo("username", normalized)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onComplete(emptyList(), task.exception?.localizedMessage ?: "Search failed")
                    return@addOnCompleteListener
                }
                val users = task.result.documents.mapNotNull { document ->
                    val uid = document.getString("uid") ?: return@mapNotNull null
                    if (uid == currentUid) return@mapNotNull null
                    SearchUser(
                        uid = uid,
                        username = document.getString("username").orEmpty(),
                        displayName = document.getString("displayName").orEmpty(),
                        gender = document.getString("gender") ?: "unspecified",
                        bio = document.getString("bio").orEmpty(),
                    )
                }
                onComplete(users, null)
            }
    }

    fun findUserByUsername(
        firestore: FirebaseFirestore,
        username: String,
        onComplete: (SearchUser?, String?) -> Unit,
    ) {
        val normalized = username.trim().lowercase()
        if (normalized.isBlank()) {
            onComplete(null, "Enter a username")
            return
        }

        firestore.collection("users")
            .whereEqualTo("username", normalized)
            .limit(1)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onComplete(null, task.exception?.localizedMessage ?: "Unable to find user")
                    return@addOnCompleteListener
                }
                val document = task.result.documents.firstOrNull()
                if (document == null) {
                    onComplete(null, null)
                    return@addOnCompleteListener
                }
                val uid = document.getString("uid")
                if (uid == null) {
                    onComplete(null, "User profile is incomplete")
                    return@addOnCompleteListener
                }
                onComplete(
                    SearchUser(
                        uid = uid,
                        username = document.getString("username").orEmpty(),
                        displayName = document.getString("displayName").orEmpty(),
                        gender = document.getString("gender") ?: "unspecified",
                        bio = document.getString("bio").orEmpty(),
                    ),
                    null,
                )
            }
    }

    fun sendFriendRequest(
        firestore: FirebaseFirestore,
        fromUid: String,
        toUid: String,
        fromUsername: String,
        toUsername: String,
        onComplete: (String?) -> Unit,
    ) {
        val requestId = "${fromUid}_$toUid"
        val request = mapOf(
            "fromUid" to fromUid,
            "toUid" to toUid,
            "fromUsername" to fromUsername.trim().lowercase(),
            "toUsername" to toUsername.trim().lowercase(),
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("friendRequests").document(requestId).set(request)
            .addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to send friend request")
            }
    }

    fun getFriendshipStatus(
        firestore: FirebaseFirestore,
        currentUid: String,
        targetUid: String,
        onComplete: (FriendshipStatus?, String?) -> Unit,
    ) {
        // Query by participant fields instead of reading an assumed document ID.
        // Firestore rules cannot authorize a get() for a document that does not exist.
        val outgoing = firestore.collection("friendRequests")
            .whereEqualTo("fromUid", currentUid)
            .whereEqualTo("toUid", targetUid)
            .limit(1)
        val incoming = firestore.collection("friendRequests")
            .whereEqualTo("fromUid", targetUid)
            .whereEqualTo("toUid", currentUid)
            .limit(1)
        outgoing.get().addOnCompleteListener { outgoingTask ->
            if (!outgoingTask.isSuccessful) {
                onComplete(null, outgoingTask.exception?.localizedMessage ?: "Unable to check friend status")
                return@addOnCompleteListener
            }
            incoming.get().addOnCompleteListener { incomingTask ->
                if (!incomingTask.isSuccessful) {
                    onComplete(null, incomingTask.exception?.localizedMessage ?: "Unable to check friend status")
                    return@addOnCompleteListener
                }
                val outgoingStatus = friendshipStatusFor(outgoingTask.result.documents.firstOrNull())
                val incomingStatus = friendshipStatusFor(incomingTask.result.documents.firstOrNull())
                val status = when {
                    outgoingStatus == "accepted" || incomingStatus == "accepted" -> FriendshipStatus.Friends
                    outgoingStatus == "pending" -> FriendshipStatus.OutgoingPending
                    incomingStatus == "pending" -> FriendshipStatus.IncomingPending
                    else -> FriendshipStatus.None
                }
                onComplete(status, null)
            }
        }
    }

    fun acceptFriendRequest(
        firestore: FirebaseFirestore,
        fromUid: String,
        toUid: String,
        fromUsername: String,
        toUsername: String,
        onComplete: (String?) -> Unit,
    ) {
        val request = firestore.collection("friendRequests").document("${fromUid}_${toUid}")
        val sourceRequestId = "${fromUid}_${toUid}"
        firestore.batch().apply {
            update(request, "status", "accepted")
            set(userFriendReference(firestore, fromUid, toUid), userFriendData(fromUid, toUid, toUsername, sourceRequestId))
            set(userFriendReference(firestore, toUid, fromUid), userFriendData(toUid, fromUid, fromUsername, sourceRequestId))
        }.commit().addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to accept friend request")
            }
    }

    fun declineFriendRequest(
        firestore: FirebaseFirestore,
        fromUid: String,
        toUid: String,
        onComplete: (String?) -> Unit,
    ) {
        firestore.collection("friendRequests").document("${fromUid}_${toUid}")
            .update("status", "declined")
            .addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to decline friend request")
            }
    }

    fun observeIncomingFriendRequests(
        firestore: FirebaseFirestore,
        currentUid: String,
        onChange: (List<IncomingFriendRequest>, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("friendRequests")
        .whereEqualTo("toUid", currentUid)
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onChange(emptyList(), exception.localizedMessage ?: "Unable to load friend requests")
                return@addSnapshotListener
            }
            val requests = snapshot?.documents.orEmpty()
                .filter { it.getString("status") == "pending" && !isLegacyFriendship(it) }
                .mapNotNull { document ->
                    val fromUid = document.getString("fromUid") ?: return@mapNotNull null
                    IncomingFriendRequest(
                        fromUid = fromUid,
                        fromUsername = document.getString("fromUsername").orEmpty().ifBlank { fromUid.take(8) },
                    )
                }
            onChange(requests, null)
        }

    fun observeFriends(
        firestore: FirebaseFirestore,
        currentUid: String,
        onChange: (List<FriendSummary>, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("users").document(currentUid).collection("friends")
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onChange(emptyList(), exception.localizedMessage ?: "Unable to load friends")
                return@addSnapshotListener
            }
            val friends = snapshot?.documents.orEmpty().mapNotNull { document ->
                val friendUid = document.getString("friendUid") ?: return@mapNotNull null
                val friendName = document.getString("username").orEmpty().ifBlank { friendUid.take(8) }
                FriendSummary(friendUid, friendName)
            }.sortedBy { it.username }
            onChange(friends, null)
        }

    fun migrateAcceptedFriendships(
        firestore: FirebaseFirestore,
        currentUid: String,
        currentUsername: String,
    ) {
        if (currentUsername.isBlank()) return
        val outgoing = firestore.collection("friendRequests").whereEqualTo("fromUid", currentUid).get()
        val incoming = firestore.collection("friendRequests").whereEqualTo("toUid", currentUid).get()

        outgoing.addOnCompleteListener { outgoingTask ->
            if (!outgoingTask.isSuccessful) return@addOnCompleteListener
            incoming.addOnCompleteListener { incomingTask ->
                if (!incomingTask.isSuccessful) return@addOnCompleteListener
                (outgoingTask.result.documents + incomingTask.result.documents)
                    .filter { isFriendship(it) }
                    .forEach { document ->
                        val fromUid = document.getString("fromUid") ?: return@forEach
                        val toUid = document.getString("toUid") ?: return@forEach
                        val otherUid = if (fromUid == currentUid) toUid else fromUid
                        val storedName = if (fromUid == currentUid) {
                            document.getString("toUsername").orEmpty()
                        } else {
                            document.getString("fromUsername").orEmpty()
                        }
                        fun createIfMissing(otherUsername: String) {
                            val friend = userFriendReference(firestore, currentUid, otherUid)
                            friend.get().addOnCompleteListener { friendTask ->
                                if (friendTask.isSuccessful && !friendTask.result.exists()) {
                                    friend.set(userFriendData(currentUid, otherUid, otherUsername, document.id))
                                }
                            }
                        }
                        if (storedName.isNotBlank()) createIfMissing(storedName)
                        else firestore.collection("users").document(otherUid).get().addOnCompleteListener { userTask ->
                            if (userTask.isSuccessful) createIfMissing(userTask.result.getString("username").orEmpty())
                        }
                    }
            }
        }
    }

    fun removeFriend(
        firestore: FirebaseFirestore,
        currentUid: String,
        targetUid: String,
        onComplete: (String?) -> Unit,
    ) {
        fun deleteFirstMatch(fromUid: String, toUid: String, fallback: () -> Unit) {
            firestore.collection("friendRequests")
                .whereEqualTo("fromUid", fromUid)
                .whereEqualTo("toUid", toUid)
                .limit(1)
                .get()
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        onComplete(task.exception?.localizedMessage ?: "Unable to remove friend")
                        return@addOnCompleteListener
                    }
                    val request = task.result.documents.firstOrNull()
                    if (request == null) {
                        fallback()
                        return@addOnCompleteListener
                    }
                    val from = request.getString("fromUid") ?: fromUid
                    val to = request.getString("toUid") ?: toUid
                    firestore.batch().apply {
                        delete(request.reference)
                        delete(userFriendReference(firestore, from, to))
                        delete(userFriendReference(firestore, to, from))
                    }.commit().addOnCompleteListener { deleteTask ->
                        onComplete(if (deleteTask.isSuccessful) null else deleteTask.exception?.localizedMessage ?: "Unable to remove friend")
                    }
                }
        }
        deleteFirstMatch(currentUid, targetUid) {
            deleteFirstMatch(targetUid, currentUid) { onComplete("Friendship not found") }
        }
    }

    // Requests created before the friend-request workflow stored no sender username.
    // Those legacy records represent the pre-existing friendship and are shown as accepted.
    private fun isLegacyFriendship(document: com.google.firebase.firestore.DocumentSnapshot): Boolean =
        document.getString("status") == "pending" && document.getString("fromUsername").isNullOrBlank()

    private fun isFriendship(document: com.google.firebase.firestore.DocumentSnapshot): Boolean =
        document.getString("status") == "accepted" || isLegacyFriendship(document)

    private fun userFriendReference(firestore: FirebaseFirestore, ownerUid: String, friendUid: String) =
        firestore.collection("users").document(ownerUid).collection("friends").document(friendUid)

    private fun userFriendData(
        ownerUid: String,
        friendUid: String,
        friendUsername: String,
        sourceRequestId: String,
    ): Map<String, Any> {
        return mapOf(
            "ownerUid" to ownerUid,
            "friendUid" to friendUid,
            "username" to friendUsername.trim().lowercase(),
            "sourceRequestId" to sourceRequestId,
            "createdAt" to FieldValue.serverTimestamp(),
        )
    }

    private fun friendshipStatusFor(document: com.google.firebase.firestore.DocumentSnapshot?): String? = when {
        document == null -> null
        isLegacyFriendship(document) -> "accepted"
        else -> document.getString("status")
    }

    fun openDirectRoom(
        firestore: FirebaseFirestore,
        currentUid: String,
        targetUid: String,
        targetUsername: String,
        onComplete: (String?, String?) -> Unit,
    ) {
        val members = listOf(currentUid, targetUid).sorted()
        val roomId = "direct_${members[0]}_${members[1]}"
        val reference = firestore.collection("rooms").document(roomId)
        reference.get().addOnCompleteListener { readTask ->
            if (!readTask.isSuccessful) {
                onComplete(null, readTask.exception?.localizedMessage ?: "Unable to open chat")
                return@addOnCompleteListener
            }
            if (readTask.result.exists()) {
                onComplete(roomId, null)
                return@addOnCompleteListener
            }
            reference.set(
                mapOf(
                    "title" to "Chat with ${targetUsername.ifBlank { "friend" }}",
                    "creatorId" to currentUid,
                    "memberIds" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).addOnCompleteListener { writeTask ->
                if (writeTask.isSuccessful) onComplete(roomId, null)
                else onComplete(null, writeTask.exception?.localizedMessage ?: "Unable to create chat")
            }
        }
    }

    fun createRoom(
        firestore: FirebaseFirestore,
        ownerUid: String,
        ownerUsername: String,
        onComplete: (String?, String?) -> Unit,
    ) {
        val room = mapOf(
            "title" to "${ownerUsername.ifBlank { "New" }}'s room",
            "creatorId" to ownerUid,
            "memberIds" to listOf(ownerUid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("rooms").add(room).addOnCompleteListener { task ->
            if (task.isSuccessful) onComplete(task.result.id, null)
            else onComplete(null, task.exception?.localizedMessage ?: "Unable to create room")
        }
    }

    fun joinRoom(
        firestore: FirebaseFirestore,
        roomId: String,
        currentUid: String,
        onComplete: (String?) -> Unit,
    ) {
        val cleanRoomId = roomId.trim()
        if (cleanRoomId.isBlank()) {
            onComplete("Enter a room ID")
            return
        }
        val reference = firestore.collection("rooms").document(cleanRoomId)
        reference.get().addOnCompleteListener { readTask ->
            if (!readTask.isSuccessful) {
                onComplete(readTask.exception?.localizedMessage ?: "Unable to find room")
            } else if (!readTask.result.exists()) {
                onComplete("Room not found")
            } else if (currentUid in (readTask.result.get("memberIds") as? List<*>).orEmpty()) {
                onComplete(null)
            } else {
                reference.update(
                    "memberIds", FieldValue.arrayUnion(currentUid),
                    "updatedAt", FieldValue.serverTimestamp(),
                ).addOnCompleteListener { updateTask ->
                    onComplete(if (updateTask.isSuccessful) null else updateTask.exception?.localizedMessage ?: "Unable to join room")
                }
            }
        }
    }

    fun observeRooms(
        firestore: FirebaseFirestore,
        currentUid: String,
        onChange: (List<ChatRoomSummary>, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("rooms")
        .whereArrayContains("memberIds", currentUid)
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onChange(emptyList(), exception.localizedMessage ?: "Unable to load chats")
                return@addSnapshotListener
            }
            val rooms = snapshot?.documents.orEmpty().map { document ->
                ChatRoomSummary(
                    id = document.id,
                    title = document.getString("title").orEmpty().ifBlank { "Chat" },
                    updatedAtMillis = document.getTimestamp("updatedAt")?.toDate()?.time ?: 0L,
                )
            }.sortedByDescending { it.updatedAtMillis }
            onChange(rooms, null)
        }

    fun observeMessages(
        firestore: FirebaseFirestore,
        roomId: String,
        onChange: (List<ChatMessage>, String?) -> Unit,
    ): ListenerRegistration = firestore.collection("rooms").document(roomId)
        .collection("messages")
        .orderBy("createdAt")
        .limitToLast(100)
        .addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                onChange(emptyList(), exception.localizedMessage ?: "Unable to load messages")
                return@addSnapshotListener
            }
            val messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                val senderId = document.getString("senderId") ?: return@mapNotNull null
                ChatMessage(
                    id = document.id,
                    senderId = senderId,
                    senderName = document.getString("senderName").orEmpty(),
                    senderAvatarUrl = document.getString("senderAvatarUrl").orEmpty(),
                    text = document.getString("text").orEmpty(),
                    sentAtMillis = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                )
            }
            onChange(messages, null)
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
        if (cleanText.isBlank()) return
        val message = mapOf(
            "senderId" to senderId,
            "senderName" to senderName.trim().take(30),
            "senderAvatarUrl" to senderAvatarUrl,
            "text" to cleanText.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        val room = firestore.collection("rooms").document(roomId)
        firestore.batch().apply {
            set(room.collection("messages").document(), message)
            update(room, "updatedAt", FieldValue.serverTimestamp())
        }.commit().addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to send message")
            }
    }
}
