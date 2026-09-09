package com.comp90018.app.data.rooms

import com.comp90018.app.ChatMessage
import com.comp90018.app.TeamRoom
import com.comp90018.app.TeamRoomMember
import com.comp90018.app.data.social.Subscription

/** Data boundary for team-room membership and entry operations. */
interface TeamRoomRepository {
    fun observeMembership(userId: String, onChange: (String?, String?) -> Unit): Subscription
    fun createRoom(userId: String, onComplete: (String?, String?) -> Unit)
    fun joinRoom(roomId: String, userId: String, onComplete: (String?) -> Unit)
    fun observeRoom(roomId: String, onChange: (TeamRoom?, String?) -> Unit): Subscription
    fun observeMessages(roomId: String, onChange: (List<ChatMessage>, String?) -> Unit): Subscription
    fun loadMembers(memberIds: List<String>, onComplete: (List<TeamRoomMember>) -> Unit)
    fun sendMessage(roomId: String, senderId: String, senderName: String, senderAvatarUrl: String, text: String, onComplete: (String?) -> Unit)
    fun leaveRoom(roomId: String, userId: String, onComplete: (String?) -> Unit)
    fun dismissRoom(roomId: String, userId: String, onComplete: (String?) -> Unit)
}
