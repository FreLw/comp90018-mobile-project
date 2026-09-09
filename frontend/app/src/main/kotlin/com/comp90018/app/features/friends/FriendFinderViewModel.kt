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
    val target: SearchUser? = null,
    val friendship: FriendshipStatus? = null,
    val message: String? = null,
    val searching: Boolean = false,
    val working: Boolean = false,
    val roomId: String? = null,
)

class FriendFinderViewModel(
    private val repository: SocialRepository,
    private val currentUid: String,
    private var currentUsername: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FriendFinderUiState())
    val uiState: StateFlow<FriendFinderUiState> = mutableUiState.asStateFlow()

    fun updateQuery(query: String) { mutableUiState.value = mutableUiState.value.copy(query = query) }

    fun updateCurrentUsername(username: String) {
        if (username.isNotBlank()) currentUsername = username
    }

    fun search() {
        mutableUiState.value = mutableUiState.value.copy(searching = true, message = null)
        repository.findUserByUsername(mutableUiState.value.query) { found, error ->
            if (error != null) {
                mutableUiState.value = mutableUiState.value.copy(searching = false, message = error)
            } else if (found == null || found.uid == currentUid) {
                mutableUiState.value = mutableUiState.value.copy(searching = false, message = "Explorer not found")
            } else {
                mutableUiState.value = mutableUiState.value.copy(searching = false, target = found)
                repository.getFriendshipStatus(currentUid, found.uid) { status, statusError ->
                    mutableUiState.value = mutableUiState.value.copy(friendship = status, message = statusError)
                }
            }
        }
    }

    fun searchAgain() { mutableUiState.value = FriendFinderUiState(query = mutableUiState.value.query) }

    fun sendFriendRequest() = withTarget { target ->
        repository.sendFriendRequest(currentUid, target.uid, currentUsername, target.username) { error ->
            mutableUiState.value = mutableUiState.value.copy(working = false, message = error ?: "Friend request sent", friendship = if (error == null) FriendshipStatus.OutgoingPending else mutableUiState.value.friendship)
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
