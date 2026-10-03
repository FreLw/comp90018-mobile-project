package com.comp90018.app.data.social

/**
 * Feature-facing boundary for friends and requests.
 *
 * ViewModels depend on this interface rather than Firebase, which keeps them testable.
 */
interface SocialRepository {
    fun searchUsers(currentUid: String, usernamePrefix: String, onComplete: (List<SearchUser>, String?) -> Unit)
    fun findUserByUsername(username: String, onComplete: (SearchUser?, String?) -> Unit)
    fun getFriendshipStatus(currentUid: String, targetUid: String, onComplete: (FriendshipStatus?, String?) -> Unit)
    fun sendFriendRequest(fromUid: String, toUid: String, fromUsername: String, toUsername: String, message: String, onComplete: (String?) -> Unit)
    fun acceptFriendRequest(fromUid: String, toUid: String, fromUsername: String, toUsername: String, onComplete: (String?) -> Unit)
    fun declineFriendRequest(fromUid: String, toUid: String, onComplete: (String?) -> Unit)
    fun removeFriend(currentUid: String, targetUid: String, onComplete: (String?) -> Unit)
    fun openDirectRoom(currentUid: String, targetUid: String, targetUsername: String, onComplete: (String?, String?) -> Unit)
    fun observeIncomingFriendRequests(currentUid: String, onChange: (List<IncomingFriendRequest>, String?) -> Unit): Subscription
    fun observeOutgoingFriendRequests(currentUid: String, onChange: (List<OutgoingFriendRequest>, String?) -> Unit): Subscription
    fun observeFriends(currentUid: String, onChange: (List<FriendSummary>, String?) -> Unit): Subscription
    fun observeDirectChats(currentUid: String, onChange: (List<DirectChatSummary>, String?) -> Unit): Subscription
    fun migrateAcceptedFriendships(currentUid: String, currentUsername: String)
}

fun interface Subscription {
    fun cancel()
}
