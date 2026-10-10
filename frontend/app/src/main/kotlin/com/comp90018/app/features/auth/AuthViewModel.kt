package com.comp90018.app.features.auth

/*
 * Owns the authentication form values, login/register mode, errors, and submitting state.
 * Repository callbacks update the state observed by AuthScreen and prevent duplicate submissions.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Form values and submission state observed by the login/register screen. */
data class AuthUiState(val loginMode: Boolean = true, val email: String = "", val password: String = "", val error: String? = null, val submitting: Boolean = false)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val mutableUiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = mutableUiState.asStateFlow()
    fun updateEmail(value: String) { mutableUiState.value = mutableUiState.value.copy(email = value) }
    fun updatePassword(value: String) { mutableUiState.value = mutableUiState.value.copy(password = value) }
    fun toggleMode() { mutableUiState.value = mutableUiState.value.copy(loginMode = !mutableUiState.value.loginMode, error = null) }
    /**
     * Keeps validation/network failures in form state and disables repeated submissions while
     * work is pending.
     */
    fun submit(validationError: String?) {
        if (mutableUiState.value.submitting) return
        if (validationError != null) { mutableUiState.value = mutableUiState.value.copy(error = validationError); return }
        val state = mutableUiState.value
        mutableUiState.value = state.copy(submitting = true, error = null)
        val complete: (String?) -> Unit = { error -> mutableUiState.value = mutableUiState.value.copy(submitting = false, error = error) }
        if (state.loginMode) repository.login(state.email, state.password, complete) else repository.register(state.email, state.password, complete)
    }
    companion object { fun factory(repository: AuthRepository) = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T } }
}
