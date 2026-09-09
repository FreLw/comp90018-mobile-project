package com.comp90018.app.features.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FriendProfileUiState(
    val profile: UserProfile? = null,
    val error: String? = null,
    val removing: Boolean = false,
)

class FriendProfileViewModel(
    private val repository: ProfileRepository,
    friendUid: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(FriendProfileUiState())
    val uiState: StateFlow<FriendProfileUiState> = mutableUiState.asStateFlow()
    private val profileSubscription: Subscription = repository.observeProfile(friendUid) { profile, error ->
        mutableUiState.value = mutableUiState.value.copy(profile = profile, error = error)
    }

    fun remove(onRemoveFriend: (String, (String?) -> Unit) -> Unit, friendUid: String, onRemoved: () -> Unit) {
        if (mutableUiState.value.removing) return
        mutableUiState.value = mutableUiState.value.copy(removing = true, error = null)
        onRemoveFriend(friendUid) { error ->
            mutableUiState.value = mutableUiState.value.copy(removing = false, error = error)
            if (error == null) onRemoved()
        }
    }

    override fun onCleared() {
        profileSubscription.cancel()
    }

    companion object {
        fun factory(repository: ProfileRepository, friendUid: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FriendProfileViewModel(repository, friendUid) as T
        }
    }
}
