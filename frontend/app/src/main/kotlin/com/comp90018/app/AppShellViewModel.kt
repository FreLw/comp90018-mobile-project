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
    private var initializationError: String? = null
    private var observationError: String? = null
    private var profileSubscription: Subscription? = null

    init {
        // New accounts must be bootstrapped even before the first profile snapshot arrives.
        ensureProfile()
        observeProfile()
    }

    fun retry() {
        observeProfile()
        ensureProfile()
    }

    private fun observeProfile() {
        profileSubscription?.cancel()
        observationError = null
        profileSubscription = repository.observeProfile(uid) { profile, error ->
            observationError = error
            mutableUiState.value = mutableUiState.value.copy(
                profile = profile,
                profileError = error ?: initializationError,
            )
        }
    }

    private fun ensureProfile() {
        if (ensuringProfile) return
        initializationError = null
        ensuringProfile = true
        mutableUiState.value = mutableUiState.value.copy(profileError = observationError)
        repository.ensureProfile(uid, email) { error ->
            ensuringProfile = false
            initializationError = error
            mutableUiState.value = mutableUiState.value.copy(profileError = observationError ?: error)
        }
    }

    override fun onCleared() {
        profileSubscription?.cancel()
    }

    companion object {
        fun factory(repository: ProfileRepository, uid: String, email: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppShellViewModel(repository, uid, email) as T
        }
    }
}
