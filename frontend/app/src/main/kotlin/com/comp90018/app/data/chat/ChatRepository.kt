package com.comp90018.app.data.chat

import com.comp90018.app.data.social.Subscription
import android.net.Uri

/** Feature-facing boundary for one-to-one messages. */
interface ChatRepository {
    fun observeMessages(roomId: String, onChange: (List<ChatMessage>, String?) -> Unit): Subscription
    fun markMessagesRead(currentUid: String, friendUid: String)

    fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        text: String,
        onComplete: (String?) -> Unit,
    )
    fun sendImage(roomId: String, senderId: String, senderName: String, senderAvatarUrl: String, imageUri: Uri, onComplete: (String?) -> Unit)
    fun sendTreasureSticker(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        treasureId: String,
        onComplete: (String?) -> Unit,
    )
}
