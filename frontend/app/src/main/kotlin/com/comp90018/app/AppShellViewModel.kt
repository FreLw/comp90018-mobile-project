package com.comp90018.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.profile.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppShellUiState(
    val profile: UserProfile? = null,
    val profileError: String? = null,
)

class AppShellViewModel(
    private val repository: ProfileRepository,
    private val uid: String,
    private val email: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AppShellUiState())
    val uiState: StateFlow<AppShellUiState> = mutableUiState.asStateFlow()
    private var ensuringProfile = false
    private val profileSubscription: Subscription = repository.observeProfile(uid) { profile, error ->
        mutableUiState.value = mutableUiState.value.copy(profile = profile, profileError = error)
        if (error == null && (profile == null || profile.username.isBlank())) ensureProfile()
    }

    fun retry() {
        mutableUiState.value = mutableUiState.value.copy(profileError = null)
        ensureProfile()
    }

    private fun ensureProfile() {
        if (ensuringProfile) return
        ensuringProfile = true
        repository.ensureProfile(uid, email) { error ->
            ensuringProfile = false
            if (error != null) mutableUiState.value = mutableUiState.value.copy(profileError = error)
        }
    }

    override fun onCleared() {
        profileSubscription.cancel()
    }

    companion object {
        fun factory(repository: ProfileRepository, uid: String, email: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppShellViewModel(repository, uid, email) as T
        }
    }
}
