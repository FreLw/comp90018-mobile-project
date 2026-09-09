package com.comp90018.app.data.rooms

import com.comp90018.app.FirebaseTeamRoomService
import com.comp90018.app.ChatMessage
import com.comp90018.app.TeamRoom
import com.comp90018.app.TeamRoomMember
import com.comp90018.app.data.social.Subscription
import com.google.firebase.firestore.FirebaseFirestore

/** Firebase implementation of [TeamRoomRepository]. */
class FirebaseTeamRoomRepository(
    private val firestore: FirebaseFirestore,
) : TeamRoomRepository {
    override fun observeMembership(userId: String, onChange: (String?, String?) -> Unit): Subscription {
        val registration = FirebaseTeamRoomService.observeMembership(firestore, userId, onChange)
        return Subscription { registration.remove() }
    }

    override fun createRoom(userId: String, onComplete: (String?, String?) -> Unit) =
        FirebaseTeamRoomService.createRoom(firestore, userId, onComplete)

    override fun joinRoom(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.joinRoom(firestore, roomId, userId, onComplete)

    override fun observeRoom(roomId: String, onChange: (TeamRoom?, String?) -> Unit): Subscription {
        val registration = FirebaseTeamRoomService.observeRoom(firestore, roomId, onChange)
        return Subscription { registration.remove() }
    }

    override fun observeMessages(roomId: String, onChange: (List<ChatMessage>, String?) -> Unit): Subscription {
        val registration = FirebaseTeamRoomService.observeMessages(firestore, roomId, onChange)
        return Subscription { registration.remove() }
    }

    override fun loadMembers(memberIds: List<String>, onComplete: (List<TeamRoomMember>) -> Unit) =
        FirebaseTeamRoomService.loadMembers(firestore, memberIds, onComplete)

    override fun sendMessage(roomId: String, senderId: String, senderName: String, senderAvatarUrl: String, text: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.sendMessage(firestore, roomId, senderId, senderName, senderAvatarUrl, text, onComplete)

    override fun leaveRoom(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.leaveRoom(firestore, roomId, userId, onComplete)

    override fun dismissRoom(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.dismissRoom(firestore, roomId, userId, onComplete)
}
