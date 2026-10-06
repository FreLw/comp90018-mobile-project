package com.comp90018.app.data.social

import com.comp90018.app.data.chat.ChatMessageTypes

/** Public profile fields returned by username search. */
data class SearchUser(
    val uid: String,
    val username: String,
    val displayName: String,
    val gender: String,
    val bio: String,
    val email: String = "",
    val avatarUrl: String = "",
    val department: String = "",
    val major: String = "",
)

enum class FriendshipStatus {
    None,
    OutgoingPending,
    IncomingPending,
    Friends,
}

/** Lightweight room data needed by the Friends > Chats list. */
data class DirectChatSummary(
    val friendUid: String,
    val updatedAtMillis: Long,
    val lastMessageText: String = "",
    val lastMessageType: String = ChatMessageTypes.Text,
    val lastMessageTreasureId: String = "",
)

data class IncomingFriendRequest(
    val fromUid: String,
    val fromUsername: String,
    val message: String = "",
)

enum class FriendRequestStatus { Pending, Accepted, Declined }

data class OutgoingFriendRequest(
    val toUid: String,
    val toUsername: String,
    val status: FriendRequestStatus,
    val message: String = "",
)

/** A contact row loaded from the signed-in user's Firebase friendships. */
data class FriendSummary(
    val uid: String,
    val username: String,
    val unreadCount: Int = 0,
    val displayName: String = "",
    val avatarUrl: String = "",
)
