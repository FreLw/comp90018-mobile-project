package com.comp90018.app.features.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.FriendshipStatus
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.data.social.SocialRepository
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoomMemberProfileUiState(
    val profile: UserProfile? = null,
    val friendship: FriendshipStatus? = null,
    val error: String? = null,
    val working: Boolean = false,
    val directRoomId: String? = null,
)

class RoomMemberProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val socialRepository: SocialRepository,
    private val currentUid: String,
    private val memberUid: String,
    private val currentUsername: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RoomMemberProfileUiState())
    val uiState: StateFlow<RoomMemberProfileUiState> = mutableUiState.asStateFlow()
    private val profileSubscription: Subscription = profileRepository.observeProfile(memberUid) { profile, error ->
        mutableUiState.value = mutableUiState.value.copy(profile = profile, error = error)
    }

    init {
        if (memberUid != currentUid) socialRepository.getFriendshipStatus(currentUid, memberUid) { status, error ->
            mutableUiState.value = mutableUiState.value.copy(friendship = status, error = error ?: mutableUiState.value.error)
        }
    }

    fun openChat(username: String) = work {
        socialRepository.openDirectRoom(currentUid, memberUid, username) { roomId, error ->
            mutableUiState.value = mutableUiState.value.copy(working = false, error = error, directRoomId = roomId)
        }
    }

    fun sendFriendRequest(username: String) = work {
        socialRepository.sendFriendRequest(currentUid, memberUid, currentUsername, username) { error ->
            mutableUiState.value = mutableUiState.value.copy(
                working = false,
                error = error ?: "Friend request sent",
                friendship = if (error == null) FriendshipStatus.OutgoingPending else mutableUiState.value.friendship,
            )
        }
    }

    fun acceptFriendRequest(username: String) = work {
        socialRepository.acceptFriendRequest(memberUid, currentUid, username, currentUsername) { error ->
            mutableUiState.value = mutableUiState.value.copy(
                working = false,
                error = error ?: "Friend request accepted",
                friendship = if (error == null) FriendshipStatus.Friends else mutableUiState.value.friendship,
            )
        }
    }

    fun consumeDirectRoom() {
        mutableUiState.value = mutableUiState.value.copy(directRoomId = null)
    }

    private fun work(action: () -> Unit) {
        if (mutableUiState.value.working) return
        mutableUiState.value = mutableUiState.value.copy(working = true, error = null)
        action()
    }

    override fun onCleared() {
        profileSubscription.cancel()
    }

    companion object {
        fun factory(profileRepository: ProfileRepository, socialRepository: SocialRepository, currentUid: String, memberUid: String, currentUsername: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RoomMemberProfileViewModel(profileRepository, socialRepository, currentUid, memberUid, currentUsername) as T
        }
    }
}
