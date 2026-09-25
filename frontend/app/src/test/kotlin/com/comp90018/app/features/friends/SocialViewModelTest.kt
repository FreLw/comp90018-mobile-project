package com.comp90018.app.features.friends

import com.comp90018.app.FriendRequestStatus
import com.comp90018.app.FriendSummary
import com.comp90018.app.FriendshipStatus
import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.OutgoingFriendRequest
import com.comp90018.app.SearchUser
import com.comp90018.app.data.social.SocialRepository
import com.comp90018.app.data.social.Subscription
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialViewModelTest {
    @Test
    fun friendRequestPersistsComposedMessageAndUpdatesStatus() {
        val repository = FakeSocialRepository()
        val viewModel = FriendFinderViewModel(repository, "current-user", "current_user")
        val target = SearchUser(
            uid = "target-user",
            username = "target_user",
            displayName = "Target User",
            gender = "unspecified",
            bio = "",
        )

        viewModel.selectUser(target)
        viewModel.sendFriendRequest("Let's find the Old Quad relic.", target)

        assertEquals("Let's find the Old Quad relic.", repository.sentMessage)
        assertEquals(FriendshipStatus.OutgoingPending, viewModel.uiState.value.friendship)
        assertEquals(setOf(target.uid), viewModel.uiState.value.sentUserIds)
    }

    @Test
    fun outgoingRequestObserverFeedsPersistentNotificationState() {
        val outgoing = OutgoingFriendRequest(
            toUid = "target-user",
            toUsername = "target_user",
            status = FriendRequestStatus.Accepted,
            message = "Team up?",
        )
        val repository = FakeSocialRepository(outgoingRequests = listOf(outgoing))
        val viewModel = FriendsViewModel(repository, "current-user", "current_user")

        assertEquals(listOf(outgoing), viewModel.uiState.value.outgoingRequests)
    }
}

private class FakeSocialRepository(
    private val outgoingRequests: List<OutgoingFriendRequest> = emptyList(),
) : SocialRepository {
    var sentMessage: String? = null

    override fun searchUsers(currentUid: String, usernamePrefix: String, onComplete: (List<SearchUser>, String?) -> Unit) =
        onComplete(emptyList(), null)

    override fun findUserByUsername(username: String, onComplete: (SearchUser?, String?) -> Unit) =
        onComplete(null, null)

    override fun getFriendshipStatus(currentUid: String, targetUid: String, onComplete: (FriendshipStatus?, String?) -> Unit) =
        onComplete(FriendshipStatus.None, null)

    override fun sendFriendRequest(
        fromUid: String,
        toUid: String,
        fromUsername: String,
        toUsername: String,
        message: String,
        onComplete: (String?) -> Unit,
    ) {
        sentMessage = message
        onComplete(null)
    }

    override fun acceptFriendRequest(
        fromUid: String,
        toUid: String,
        fromUsername: String,
        toUsername: String,
        onComplete: (String?) -> Unit,
    ) = onComplete(null)

    override fun declineFriendRequest(fromUid: String, toUid: String, onComplete: (String?) -> Unit) = onComplete(null)

    override fun removeFriend(currentUid: String, targetUid: String, onComplete: (String?) -> Unit) = onComplete(null)

    override fun openDirectRoom(
        currentUid: String,
        targetUid: String,
        targetUsername: String,
        onComplete: (String?, String?) -> Unit,
    ) = onComplete("direct-room", null)

    override fun observeIncomingFriendRequests(
        currentUid: String,
        onChange: (List<IncomingFriendRequest>, String?) -> Unit,
    ): Subscription {
        onChange(emptyList(), null)
        return Subscription { }
    }

    override fun observeOutgoingFriendRequests(
        currentUid: String,
        onChange: (List<OutgoingFriendRequest>, String?) -> Unit,
    ): Subscription {
        onChange(outgoingRequests, null)
        return Subscription { }
    }

    override fun observeFriends(
        currentUid: String,
        onChange: (List<FriendSummary>, String?) -> Unit,
    ): Subscription {
        onChange(emptyList(), null)
        return Subscription { }
    }

    override fun observeDirectChatActivity(
        currentUid: String,
        onChange: (Map<String, Long>, String?) -> Unit,
    ): Subscription {
        onChange(emptyMap(), null)
        return Subscription { }
    }

    override fun migrateAcceptedFriendships(currentUid: String, currentUsername: String) = Unit
}
