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
    val text: String,
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
        onComplete: (String?) -> Unit,
    ) {
        val requestId = "${fromUid}_$toUid"
        val request = mapOf(
            "fromUid" to fromUid,
            "toUid" to toUid,
            "fromUsername" to fromUsername.trim().lowercase(),
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
        onComplete: (String?) -> Unit,
    ) {
        firestore.collection("friendRequests").document("${fromUid}_${toUid}")
            .update("status", "accepted")
            .addOnCompleteListener { task ->
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
            val pending = snapshot?.documents.orEmpty()
                .filter { it.getString("status") == "pending" && !isLegacyFriendship(it) }
                .mapNotNull { document ->
                    val fromUid = document.getString("fromUid") ?: return@mapNotNull null
                    fromUid to document.getString("fromUsername").orEmpty()
                }
            resolveUsernames(firestore, pending.map { it.first }) { usernames ->
                val requests = pending.map { (fromUid, storedUsername) ->
                    IncomingFriendRequest(
                        fromUid = fromUid,
                        fromUsername = usernames[fromUid] ?: storedUsername.ifBlank { fromUid.take(8) },
                    )
                }
                onChange(requests, null)
            }
        }

    fun observeFriends(
        firestore: FirebaseFirestore,
        currentUid: String,
        onChange: (List<FriendSummary>, String?) -> Unit,
    ): ListenerRegistration {
        var outgoingIds = emptyList<String>()
        var incomingIds = emptyList<String>()

        fun publish() {
            val friendIds = (outgoingIds + incomingIds).distinct()
            resolveUsernames(firestore, friendIds) { usernames ->
                onChange(
                    friendIds.map { uid -> FriendSummary(uid, usernames[uid] ?: uid.take(8)) },
                    null,
                )
            }
        }

        val outgoingRegistration = firestore.collection("friendRequests")
            .whereEqualTo("fromUid", currentUid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onChange(emptyList(), exception.localizedMessage ?: "Unable to load friends")
                    return@addSnapshotListener
                }
                outgoingIds = snapshot?.documents.orEmpty()
                    .filter { isFriendship(it) }
                    .mapNotNull { it.getString("toUid") }
                publish()
            }
        val incomingRegistration = firestore.collection("friendRequests")
            .whereEqualTo("toUid", currentUid)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onChange(emptyList(), exception.localizedMessage ?: "Unable to load friends")
                    return@addSnapshotListener
                }
                incomingIds = snapshot?.documents.orEmpty()
                    .filter { isFriendship(it) }
                    .mapNotNull { it.getString("fromUid") }
                publish()
            }
        return ListenerRegistration {
            outgoingRegistration.remove()
            incomingRegistration.remove()
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
                    request.reference.delete().addOnCompleteListener { deleteTask ->
                        onComplete(if (deleteTask.isSuccessful) null else deleteTask.exception?.localizedMessage ?: "Unable to remove friend")
                    }
                }
        }
        deleteFirstMatch(currentUid, targetUid) {
            deleteFirstMatch(targetUid, currentUid) { onComplete("Friendship not found") }
        }
    }

    private fun resolveUsernames(
        firestore: FirebaseFirestore,
        uids: List<String>,
        onComplete: (Map<String, String>) -> Unit,
    ) {
        val usernames = mutableMapOf<String, String>()
        fun resolveAt(index: Int) {
            if (index >= uids.size) {
                onComplete(usernames)
                return
            }
            val uid = uids[index]
            firestore.collection("users").document(uid).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result.getString("username")?.takeIf { it.isNotBlank() }?.let { usernames[uid] = it }
                }
                resolveAt(index + 1)
            }
        }
        resolveAt(0)
    }

    // Requests created before the friend-request workflow stored no sender username.
    // Those legacy records represent the pre-existing friendship and are shown as accepted.
    private fun isLegacyFriendship(document: com.google.firebase.firestore.DocumentSnapshot): Boolean =
        document.getString("status") == "pending" && document.getString("fromUsername").isNullOrBlank()

    private fun isFriendship(document: com.google.firebase.firestore.DocumentSnapshot): Boolean =
        document.getString("status") == "accepted" || isLegacyFriendship(document)

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
                ChatMessage(document.id, senderId, document.getString("text").orEmpty())
            }
            onChange(messages, null)
        }

    fun sendMessage(
        firestore: FirebaseFirestore,
        roomId: String,
        senderId: String,
        text: String,
        onComplete: (String?) -> Unit,
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        val message = mapOf(
            "senderId" to senderId,
            "text" to cleanText.take(1000),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("rooms").document(roomId).collection("messages").add(message)
            .addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to send message")
            }
    }
}
