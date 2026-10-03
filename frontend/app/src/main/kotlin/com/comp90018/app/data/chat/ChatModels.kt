package com.comp90018.app.data.chat

/** One message rendered by either a direct chat or a team-room chat. */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String,
    val text: String,
    val sentAtMillis: Long,
    val imageUrl: String = "",
    val messageType: String = ChatMessageTypes.Text,
    val treasureId: String = "",
)

/** Firestore values used to distinguish text, image, and collectible sticker messages. */
object ChatMessageTypes {
    const val Text = "text"
    const val Image = "image"
    const val TreasureSticker = "treasure_sticker"
}
