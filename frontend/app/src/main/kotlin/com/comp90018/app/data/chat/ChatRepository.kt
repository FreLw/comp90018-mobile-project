package com.comp90018.app.data.chat

import com.comp90018.app.ChatMessage
import com.comp90018.app.data.social.Subscription

/** Data boundary for direct-message operations. */
interface ChatRepository {
    fun observeMessages(roomId: String, onChange: (List<ChatMessage>, String?) -> Unit): Subscription

    fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String,
        text: String,
        onComplete: (String?) -> Unit,
    )
}
