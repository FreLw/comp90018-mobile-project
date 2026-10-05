package com.comp90018.app.data.chat

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/** Low-level Firestore and Storage operations for one-to-one conversations. */
object FirebaseDirectChatService {
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
                val imageUrl = document.getString("imageUrl").orEmpty()
                val treasureId = document.getString("treasureId").orEmpty()
                ChatMessage(
                    id = document.id,
                    senderId = senderId,
                    senderName = document.getString("senderName").orEmpty(),
                    senderAvatarUrl = document.getString("senderAvatarUrl").orEmpty(),
                    text = document.getString("text").orEmpty(),
                    sentAtMillis = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    imageUrl = imageUrl,
                    messageType = document.getString("messageType") ?: when {
                        treasureId.isNotBlank() -> ChatMessageTypes.TreasureSticker
                        imageUrl.isNotBlank() -> ChatMessageTypes.Image
                        else -> ChatMessageTypes.Text
                    },
                    treasureId = treasureId,
                )
            }
            onChange(messages, null)
        }

    fun sendText(
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
        send(
            firestore = firestore,
            roomId = roomId,
            senderId = senderId,
            message = mapOf(
                "senderId" to senderId,
                "senderName" to senderName.trim().take(30),
                "senderAvatarUrl" to senderAvatarUrl,
                "text" to cleanText.take(1000),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
            onComplete = onComplete,
        )
    }

    fun sendImage(
        firestore: FirebaseFirestore,
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        imageUri: Uri,
        onComplete: (String?) -> Unit,
    ) {
        FirebaseChatImageStorage.upload("rooms", roomId, senderId, imageUri) { url, error ->
            if (url == null) return@upload onComplete(error)
            send(
                firestore = firestore,
                roomId = roomId,
                senderId = senderId,
                message = mapOf(
                    "senderId" to senderId,
                    "senderName" to senderName.trim().take(30),
                    "senderAvatarUrl" to senderAvatarUrl,
                    "text" to "",
                    "imageUrl" to url,
                    "messageType" to ChatMessageTypes.Image,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
                onComplete = onComplete,
            )
        }
    }

    fun sendTreasureSticker(
        firestore: FirebaseFirestore,
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        treasureId: String,
        onComplete: (String?) -> Unit,
    ) {
        val cleanTreasureId = treasureId.trim()
        if (cleanTreasureId.isBlank()) return onComplete("Choose a treasure sticker")
        send(
            firestore = firestore,
            roomId = roomId,
            senderId = senderId,
            message = mapOf(
                "senderId" to senderId,
                "senderName" to senderName.trim().take(30),
                "senderAvatarUrl" to senderAvatarUrl,
                "text" to "",
                "messageType" to ChatMessageTypes.TreasureSticker,
                "treasureId" to cleanTreasureId,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
            onComplete = onComplete,
        )
    }

    fun markMessagesRead(firestore: FirebaseFirestore, currentUid: String, friendUid: String) {
        firestore.collection("users").document(currentUid).collection("friends").document(friendUid)
            .update("unreadCount", 0)
    }

    private fun send(
        firestore: FirebaseFirestore,
        roomId: String,
        senderId: String,
        message: Map<String, Any>,
        onComplete: (String?) -> Unit,
    ) {
        val room = firestore.collection("rooms").document(roomId)
        room.get().addOnCompleteListener { roomTask ->
            if (!roomTask.isSuccessful) {
                onComplete(roomTask.exception?.localizedMessage ?: "Unable to send message")
                return@addOnCompleteListener
            }
            val recipientUid = (roomTask.result.get("memberIds") as? List<*>)
                ?.filterIsInstance<String>()
                ?.firstOrNull { it != senderId }
            if (recipientUid == null) {
                onComplete("Unable to find message recipient")
                return@addOnCompleteListener
            }
            val recipientFriend = firestore.collection("users").document(recipientUid)
                .collection("friends").document(senderId)
            firestore.batch().apply {
                set(room.collection("messages").document(), message)
                // This atomic part intentionally remains compatible with the first ruleset.
                update(room, "updatedAt", FieldValue.serverTimestamp())
            }.commit().addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onComplete(task.exception?.localizedMessage ?: "Unable to send message")
                    return@addOnCompleteListener
                }
                // Starter contacts have no friend document, so these two denormalised
                // updates are best-effort and never roll back a successfully stored message.
                recipientFriend.update("unreadCount", FieldValue.increment(1))
                room.update(
                    mapOf(
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastMessageText" to (message["text"] as? String).orEmpty(),
                        "lastMessageType" to ((message["messageType"] as? String) ?: ChatMessageTypes.Text),
                        "lastMessageTreasureId" to (message["treasureId"] as? String).orEmpty(),
                    ),
                )
                onComplete(null)
            }
        }
    }
}
