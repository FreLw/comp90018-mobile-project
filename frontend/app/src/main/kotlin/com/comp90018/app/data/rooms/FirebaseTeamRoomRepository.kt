package com.comp90018.app.data.rooms

import com.comp90018.app.data.chat.ChatMessage
import com.comp90018.app.data.social.Subscription
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri

/** Firebase adapter for team-room state, messages, and shared-hunt progress. */
class FirebaseTeamRoomRepository(
    private val firestore: FirebaseFirestore,
) : TeamRoomRepository {
    override fun observePublicRooms(onChange: (List<TeamRoom>, String?) -> Unit): Subscription {
        val registration = FirebaseTeamRoomService.observePublicRooms(firestore, onChange)
        return Subscription { registration.remove() }
    }
    override fun createRoom(userId: String, name: String, maxMembers: Int, description: String, idOnly: Boolean, onComplete: (String?, String?) -> Unit) =
        FirebaseTeamRoomService.createRoom(firestore, userId, name, maxMembers, description, onComplete, idOnly)
    override fun updateSettings(roomId: String, userId: String, name: String, maxMembers: Int, description: String, idOnly: Boolean, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.updateSettings(firestore, roomId, userId, name, maxMembers, description, onComplete, idOnly)
    override fun removeMember(roomId: String, ownerId: String, memberId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.removeMember(firestore, roomId, ownerId, memberId, onComplete)
    override fun joinPublicRoom(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.joinRoom(firestore, roomId, userId, onComplete, publicEntry = true)

    override fun observeMembership(userId: String, onChange: (String?, String?) -> Unit): Subscription {
        val registration = FirebaseTeamRoomService.observeMembership(firestore, userId, onChange)
        return Subscription { registration.remove() }
    }

    override fun observeUnreadMessages(userId: String, onChange: (Int, String?) -> Unit): Subscription {
        val registration = FirebaseTeamRoomService.observeUnreadMessages(firestore, userId, onChange)
        return Subscription { registration.remove() }
    }

    override fun createRoom(userId: String, name: String, maxMembers: Int, description: String, onComplete: (String?, String?) -> Unit) =
        FirebaseTeamRoomService.createRoom(firestore, userId, name, maxMembers, description, onComplete)

    override fun updateSettings(roomId: String, userId: String, name: String, maxMembers: Int, description: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.updateSettings(firestore, roomId, userId, name, maxMembers, description, onComplete)

    override fun sendTreasureSticker(roomId: String, senderId: String, senderName: String, senderAvatarUrl: String, treasureId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.sendTreasureSticker(firestore, roomId, senderId, senderName, senderAvatarUrl, treasureId, onComplete)

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

    override fun markMessagesRead(userId: String) = FirebaseTeamRoomService.markMessagesRead(firestore, userId)

    override fun loadMembers(memberIds: List<String>, onComplete: (List<TeamRoomMember>) -> Unit) =
        FirebaseTeamRoomService.loadMembers(firestore, memberIds, onComplete)

    override fun selectTask(roomId: String, userId: String, taskId: String, taskTitle: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.selectTask(firestore, roomId, userId, taskId, taskTitle, onComplete)

    override fun startHunt(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.startHunt(firestore, roomId, userId, onComplete)

    override fun terminateHunt(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.terminateHunt(firestore, roomId, userId, onComplete)

    override fun completeHuntTask(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.completeHuntTask(firestore, roomId, userId, onComplete)

    override fun findHuntFragment(roomId: String, userId: String, fragmentId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.findHuntFragment(firestore, roomId, userId, fragmentId, onComplete)

    override fun claimCompletedHuntTreasure(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.claimCompletedHuntTreasure(firestore, roomId, userId, onComplete)

    override fun sendMessage(roomId: String, senderId: String, recipientId: String?, senderName: String, senderAvatarUrl: String, text: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.sendMessage(firestore, roomId, senderId, recipientId, senderName, senderAvatarUrl, text, onComplete)
    override fun sendImage(roomId: String, senderId: String, recipientId: String?, senderName: String, senderAvatarUrl: String, imageUri: Uri, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.sendImage(firestore, roomId, senderId, recipientId, senderName, senderAvatarUrl, imageUri, onComplete)

    override fun leaveRoom(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.leaveRoom(firestore, roomId, userId, onComplete)

    override fun dismissRoom(roomId: String, userId: String, onComplete: (String?) -> Unit) =
        FirebaseTeamRoomService.dismissRoom(firestore, roomId, userId, onComplete)
}
