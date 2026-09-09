package com.comp90018.app.data.social

import com.comp90018.app.FriendSummary
import com.comp90018.app.FriendshipStatus
import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.SearchUser

/** Data boundary for social features. UI and ViewModels do not depend on Firebase SDK types. */
interface SocialRepository {
    fun findUserByUsername(username: String, onComplete: (SearchUser?, String?) -> Unit)
    fun getFriendshipStatus(currentUid: String, targetUid: String, onComplete: (FriendshipStatus?, String?) -> Unit)
    fun sendFriendRequest(fromUid: String, toUid: String, fromUsername: String, toUsername: String, onComplete: (String?) -> Unit)
    fun acceptFriendRequest(fromUid: String, toUid: String, fromUsername: String, toUsername: String, onComplete: (String?) -> Unit)
    fun declineFriendRequest(fromUid: String, toUid: String, onComplete: (String?) -> Unit)
    fun removeFriend(currentUid: String, targetUid: String, onComplete: (String?) -> Unit)
    fun openDirectRoom(currentUid: String, targetUid: String, targetUsername: String, onComplete: (String?, String?) -> Unit)
    fun observeIncomingFriendRequests(currentUid: String, onChange: (List<IncomingFriendRequest>, String?) -> Unit): Subscription
    fun observeFriends(currentUid: String, onChange: (List<FriendSummary>, String?) -> Unit): Subscription
    fun migrateAcceptedFriendships(currentUid: String, currentUsername: String)
}

fun interface Subscription {
    fun cancel()
}
