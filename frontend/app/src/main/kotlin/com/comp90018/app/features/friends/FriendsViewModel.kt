package com.comp90018.app.features.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.FriendSummary
import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.data.social.SocialRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatTarget(val roomId: String, val friend: FriendSummary)

data class FriendsUiState(
    val friends: List<FriendSummary> = emptyList(),
    val requests: List<IncomingFriendRequest> = emptyList(),
    val showRequests: Boolean = false,
    val message: String? = null,
    val chatTarget: ChatTarget? = null,
)

class FriendsViewModel(
    private val repository: SocialRepository,
    private val currentUid: String,
    private var currentUsername: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = mutableUiState.asStateFlow()
    private var requestsSubscription: Subscription? = null
    private var friendsSubscription: Subscription? = null

    init {
        repository.migrateAcceptedFriendships(currentUid, currentUsername)
        requestsSubscription = repository.observeIncomingFriendRequests(currentUid) { requests, error ->
            mutableUiState.value = mutableUiState.value.copy(requests = requests, message = error ?: mutableUiState.value.message)
        }
        friendsSubscription = repository.observeFriends(currentUid) { friends, error ->
            mutableUiState.value = mutableUiState.value.copy(friends = friends, message = error ?: mutableUiState.value.message)
        }
    }

    fun toggleRequests() {
        mutableUiState.value = mutableUiState.value.copy(showRequests = !mutableUiState.value.showRequests)
    }

    fun updateCurrentUsername(username: String) {
        if (username.isBlank() || username == currentUsername) return
        currentUsername = username
        repository.migrateAcceptedFriendships(currentUid, username)
    }

    fun accept(request: IncomingFriendRequest) {
        repository.acceptFriendRequest(request.fromUid, currentUid, request.fromUsername, currentUsername) { error ->
            mutableUiState.value = mutableUiState.value.copy(message = error)
        }
    }

    fun decline(request: IncomingFriendRequest) {
        repository.declineFriendRequest(request.fromUid, currentUid) { error ->
            mutableUiState.value = mutableUiState.value.copy(message = error)
        }
    }

    fun openChat(friend: FriendSummary) {
        repository.openDirectRoom(currentUid, friend.uid, friend.username) { roomId, error ->
            mutableUiState.value = if (roomId == null) mutableUiState.value.copy(message = error)
            else mutableUiState.value.copy(message = null, chatTarget = ChatTarget(roomId, friend))
        }
    }

    fun removeFriend(friendUid: String, onComplete: (String?) -> Unit) {
        repository.removeFriend(currentUid, friendUid) { error ->
            mutableUiState.value = mutableUiState.value.copy(message = error)
            onComplete(error)
        }
    }

    fun closeChat() {
        mutableUiState.value = mutableUiState.value.copy(chatTarget = null)
    }

    override fun onCleared() {
        requestsSubscription?.cancel()
        friendsSubscription?.cancel()
    }

    companion object {
        fun factory(repository: SocialRepository, currentUid: String, currentUsername: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FriendsViewModel(repository, currentUid, currentUsername) as T
        }
    }
}
