package com.comp90018.app.data.social

import com.comp90018.app.FriendSummary
import com.comp90018.app.FriendshipStatus
import com.comp90018.app.FirebaseSocialService
import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.SearchUser
import com.google.firebase.firestore.FirebaseFirestore

/** Firebase-backed [SocialRepository]. This is the only social data class that knows Firestore. */
class FirebaseSocialRepository(
    private val firestore: FirebaseFirestore,
) : SocialRepository {
    override fun findUserByUsername(username: String, onComplete: (SearchUser?, String?) -> Unit) =
        FirebaseSocialService.findUserByUsername(firestore, username, onComplete)

    override fun getFriendshipStatus(currentUid: String, targetUid: String, onComplete: (FriendshipStatus?, String?) -> Unit) =
        FirebaseSocialService.getFriendshipStatus(firestore, currentUid, targetUid, onComplete)

    override fun sendFriendRequest(fromUid: String, toUid: String, fromUsername: String, toUsername: String, onComplete: (String?) -> Unit) =
        FirebaseSocialService.sendFriendRequest(firestore, fromUid, toUid, fromUsername, toUsername, onComplete)

    override fun acceptFriendRequest(fromUid: String, toUid: String, fromUsername: String, toUsername: String, onComplete: (String?) -> Unit) =
        FirebaseSocialService.acceptFriendRequest(firestore, fromUid, toUid, fromUsername, toUsername, onComplete)

    override fun declineFriendRequest(fromUid: String, toUid: String, onComplete: (String?) -> Unit) =
        FirebaseSocialService.declineFriendRequest(firestore, fromUid, toUid, onComplete)

    override fun removeFriend(currentUid: String, targetUid: String, onComplete: (String?) -> Unit) =
        FirebaseSocialService.removeFriend(firestore, currentUid, targetUid, onComplete)

    override fun openDirectRoom(currentUid: String, targetUid: String, targetUsername: String, onComplete: (String?, String?) -> Unit) =
        FirebaseSocialService.openDirectRoom(firestore, currentUid, targetUid, targetUsername, onComplete)

    override fun observeIncomingFriendRequests(currentUid: String, onChange: (List<IncomingFriendRequest>, String?) -> Unit): Subscription {
        val registration = FirebaseSocialService.observeIncomingFriendRequests(firestore, currentUid, onChange)
        return Subscription { registration.remove() }
    }

    override fun observeFriends(currentUid: String, onChange: (List<FriendSummary>, String?) -> Unit): Subscription {
        val registration = FirebaseSocialService.observeFriends(firestore, currentUid, onChange)
        return Subscription { registration.remove() }
    }

    override fun migrateAcceptedFriendships(currentUid: String, currentUsername: String) =
        FirebaseSocialService.migrateAcceptedFriendships(firestore, currentUid, currentUsername)
}
