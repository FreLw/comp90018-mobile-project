package com.comp90018.app

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class SearchUser(
    val uid: String,
    val username: String,
    val displayName: String,
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
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
            onComplete(emptyList(), "请输入用户名")
            return
        }

        firestore.collection("users")
            .orderBy("username")
            .startAt(normalized)
            .endAt("$normalized\uf8ff")
            .limit(20)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onComplete(emptyList(), task.exception?.localizedMessage ?: "搜索失败")
                    return@addOnCompleteListener
                }
                val users = task.result.documents.mapNotNull { document ->
                    val uid = document.getString("uid") ?: return@mapNotNull null
                    if (uid == currentUid) return@mapNotNull null
                    SearchUser(
                        uid = uid,
                        username = document.getString("username").orEmpty(),
                        displayName = document.getString("displayName").orEmpty(),
                    )
                }
                onComplete(users, null)
            }
    }

    fun sendFriendRequest(
        firestore: FirebaseFirestore,
        fromUid: String,
        toUid: String,
        onComplete: (String?) -> Unit,
    ) {
        val requestId = "${fromUid}_$toUid"
        val request = mapOf(
            "fromUid" to fromUid,
            "toUid" to toUid,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("friendRequests").document(requestId).set(request)
            .addOnCompleteListener { task ->
                onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "发送失败")
            }
    }

    fun createRoom(
        firestore: FirebaseFirestore,
        ownerUid: String,
        ownerUsername: String,
        onComplete: (String?, String?) -> Unit,
    ) {
        val room = mapOf(
            "title" to "${ownerUsername.ifBlank { "新" }}的房间",
            "creatorId" to ownerUid,
            "memberIds" to listOf(ownerUid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("rooms").add(room).addOnCompleteListener { task ->
            if (task.isSuccessful) onComplete(task.result.id, null)
            else onComplete(null, task.exception?.localizedMessage ?: "创建房间失败")
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
                onChange(emptyList(), exception.localizedMessage ?: "无法读取消息")
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
                onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "发送失败")
            }
    }
}
