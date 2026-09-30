package com.comp90018.app.features.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.FriendshipStatus
import com.comp90018.app.SearchUser
import com.comp90018.app.data.social.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FriendFinderUiState(
    val query: String = "",
    val results: List<SearchUser> = emptyList(),
    val target: SearchUser? = null,
    val friendship: FriendshipStatus? = null,
    val message: String? = null,
    val searching: Boolean = false,
    val working: Boolean = false,
    val roomId: String? = null,
    val sentUserIds: Set<String> = emptySet(),
    val requestMode: Boolean = false,
)

class FriendFinderViewModel(
    private val repository: SocialRepository,
    private val currentUid: String,
    private var currentUsername: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FriendFinderUiState())
    val uiState: StateFlow<FriendFinderUiState> = mutableUiState.asStateFlow()

    fun updateQuery(query: String) {
        mutableUiState.value = mutableUiState.value.copy(
            query = query,
            results = if (query.isBlank()) emptyList() else mutableUiState.value.results,
            searching = false,
            message = null,
        )
    }

    fun updateCurrentUsername(username: String) {
        if (username.isNotBlank()) currentUsername = username
    }

    fun search() {
        val searchedQuery = mutableUiState.value.query.trim().lowercase()
        if (searchedQuery.isBlank()) return
        mutableUiState.value = mutableUiState.value.copy(searching = true, message = null)
        repository.searchUsers(currentUid, searchedQuery) { found, error ->
            if (mutableUiState.value.query.trim().lowercase() != searchedQuery) return@searchUsers
            if (error != null) {
                mutableUiState.value = mutableUiState.value.copy(searching = false, message = error)
            } else {
                mutableUiState.value = mutableUiState.value.copy(
                    searching = false,
                    results = found,
                )
            }
        }
    }

    fun selectUser(user: SearchUser, knownFriend: Boolean = false, requestMode: Boolean = false) {
        mutableUiState.value = mutableUiState.value.copy(
            target = user,
            friendship = when {
                knownFriend -> FriendshipStatus.Friends
                user.uid in mutableUiState.value.sentUserIds -> FriendshipStatus.OutgoingPending
                else -> null
            },
            requestMode = requestMode,
            message = null,
        )
        if (!knownFriend) {
            repository.getFriendshipStatus(currentUid, user.uid) { status, statusError ->
                mutableUiState.value = mutableUiState.value.copy(friendship = status, message = statusError)
            }
        }
    }

    fun searchAgain() {
        mutableUiState.value = mutableUiState.value.copy(target = null, friendship = null, message = null, roomId = null, requestMode = false)
    }

    fun startRequest() {
        mutableUiState.value = mutableUiState.value.copy(requestMode = true, message = null)
    }

    fun sendFriendRequest(message: String, candidate: SearchUser? = null) {
        val target = candidate ?: mutableUiState.value.target ?: return
        mutableUiState.value = mutableUiState.value.copy(working = true, message = null)
        repository.sendFriendRequest(currentUid, target.uid, currentUsername, target.username, message) { error ->
            mutableUiState.value = mutableUiState.value.copy(
                working = false,
                message = error ?: "Friend request sent",
                friendship = if (error == null && mutableUiState.value.target?.uid == target.uid) FriendshipStatus.OutgoingPending else mutableUiState.value.friendship,
                sentUserIds = if (error == null) mutableUiState.value.sentUserIds + target.uid else mutableUiState.value.sentUserIds,
            )
        }
    }

    fun acceptFriendRequest() = withTarget { target ->
        repository.acceptFriendRequest(target.uid, currentUid, target.username, currentUsername) { error ->
            mutableUiState.value = mutableUiState.value.copy(working = false, message = error ?: "Friend request accepted", friendship = if (error == null) FriendshipStatus.Friends else mutableUiState.value.friendship)
        }
    }

    fun openChat() = withTarget { target ->
        repository.openDirectRoom(currentUid, target.uid, target.username) { roomId, error ->
            mutableUiState.value = mutableUiState.value.copy(working = false, message = error, roomId = roomId)
        }
    }

    fun closeChat() { mutableUiState.value = mutableUiState.value.copy(roomId = null) }

    private fun withTarget(action: (SearchUser) -> Unit) {
        val target = mutableUiState.value.target ?: return
        mutableUiState.value = mutableUiState.value.copy(working = true, message = null)
        action(target)
    }

    companion object {
        fun factory(repository: SocialRepository, currentUid: String, currentUsername: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FriendFinderViewModel(repository, currentUid, currentUsername) as T
        }
    }
}
