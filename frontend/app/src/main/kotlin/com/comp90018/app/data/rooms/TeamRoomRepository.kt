package com.comp90018.app.data.rooms

import com.comp90018.app.data.chat.ChatMessage
import com.comp90018.app.data.social.Subscription
import android.net.Uri

/** Data boundary for team-room membership and entry operations. */
/** Feature-facing boundary for room membership, room chat, and cooperative hunts. */
interface TeamRoomRepository {
    fun observeMembership(userId: String, onChange: (String?, String?) -> Unit): Subscription
    fun observeUnreadMessages(userId: String, onChange: (Int, String?) -> Unit): Subscription
    fun createRoom(userId: String, onComplete: (String?, String?) -> Unit)
    fun joinRoom(roomId: String, userId: String, onComplete: (String?) -> Unit)
    fun observeRoom(roomId: String, onChange: (TeamRoom?, String?) -> Unit): Subscription
    fun observeMessages(roomId: String, onChange: (List<ChatMessage>, String?) -> Unit): Subscription
    fun markMessagesRead(userId: String)
    fun loadMembers(memberIds: List<String>, onComplete: (List<TeamRoomMember>) -> Unit)
    fun selectTask(roomId: String, userId: String, taskId: String, taskTitle: String, onComplete: (String?) -> Unit)
    fun startHunt(roomId: String, userId: String, onComplete: (String?) -> Unit)
    fun terminateHunt(roomId: String, userId: String, onComplete: (String?) -> Unit)
    fun completeHuntTask(roomId: String, userId: String, onComplete: (String?) -> Unit)
    fun findHuntFragment(roomId: String, userId: String, fragmentId: String, onComplete: (String?) -> Unit)
    fun claimCompletedHuntTreasure(roomId: String, userId: String, onComplete: (String?) -> Unit)
    fun sendMessage(roomId: String, senderId: String, recipientId: String?, senderName: String, senderAvatarUrl: String, text: String, onComplete: (String?) -> Unit)
    fun sendImage(roomId: String, senderId: String, recipientId: String?, senderName: String, senderAvatarUrl: String, imageUri: Uri, onComplete: (String?) -> Unit)
    fun leaveRoom(roomId: String, userId: String, onComplete: (String?) -> Unit)
    fun dismissRoom(roomId: String, userId: String, onComplete: (String?) -> Unit)
}
