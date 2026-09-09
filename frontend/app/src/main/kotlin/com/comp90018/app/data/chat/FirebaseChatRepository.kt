package com.comp90018.app.data.chat

import com.comp90018.app.ChatMessage
import com.comp90018.app.FirebaseSocialService
import com.comp90018.app.data.social.Subscription
import com.google.firebase.firestore.FirebaseFirestore

/** Firebase implementation of [ChatRepository]. */
class FirebaseChatRepository(
    private val firestore: FirebaseFirestore,
) : ChatRepository {
    override fun observeMessages(roomId: String, onChange: (List<ChatMessage>, String?) -> Unit): Subscription {
        val registration = FirebaseSocialService.observeMessages(firestore, roomId, onChange)
        return Subscription { registration.remove() }
    }

    override fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        text: String,
        onComplete: (String?) -> Unit,
    ) = FirebaseSocialService.sendMessage(
        firestore,
        roomId,
        senderId,
        senderName,
        senderAvatarUrl,
        text,
        onComplete,
    )
}
