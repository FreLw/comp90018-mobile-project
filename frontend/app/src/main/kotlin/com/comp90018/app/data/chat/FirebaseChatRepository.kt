package com.comp90018.app.data.chat

import com.comp90018.app.data.social.Subscription
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri

/** Firebase implementation of [ChatRepository]. */
class FirebaseChatRepository(
    private val firestore: FirebaseFirestore,
) : ChatRepository {
    override fun observeMessages(roomId: String, onChange: (List<ChatMessage>, String?) -> Unit): Subscription {
        val registration = FirebaseDirectChatService.observeMessages(firestore, roomId, onChange)
        return Subscription { registration.remove() }
    }

    override fun markMessagesRead(currentUid: String, friendUid: String) =
        FirebaseDirectChatService.markMessagesRead(firestore, currentUid, friendUid)

    override fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        text: String,
        onComplete: (String?) -> Unit,
    ) = FirebaseDirectChatService.sendText(
        firestore,
        roomId,
        senderId,
        senderName,
        senderAvatarUrl,
        text,
        onComplete,
    )
    override fun sendImage(roomId: String, senderId: String, senderName: String, senderAvatarUrl: String, imageUri: Uri, onComplete: (String?) -> Unit) =
        FirebaseDirectChatService.sendImage(firestore, roomId, senderId, senderName, senderAvatarUrl, imageUri, onComplete)

    override fun sendTreasureSticker(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        treasureId: String,
        onComplete: (String?) -> Unit,
    ) = FirebaseDirectChatService.sendTreasureSticker(
        firestore,
        roomId,
        senderId,
        senderName,
        senderAvatarUrl,
        treasureId,
        onComplete,
    )
}
