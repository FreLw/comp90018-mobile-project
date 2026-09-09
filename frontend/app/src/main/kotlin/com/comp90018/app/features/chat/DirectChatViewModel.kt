package com.comp90018.app.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.ChatMessage
import com.comp90018.app.data.chat.ChatRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DirectChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val error: String? = null,
    val sending: Boolean = false,
)

class DirectChatViewModel(
    private val repository: ChatRepository,
    private val roomId: String,
    private val currentUid: String,
    private var currentUsername: String,
    private var currentAvatarUrl: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DirectChatUiState())
    val uiState: StateFlow<DirectChatUiState> = mutableUiState.asStateFlow()
    private val messagesSubscription: Subscription

    init {
        messagesSubscription = repository.observeMessages(roomId) { messages, error ->
            mutableUiState.value = mutableUiState.value.copy(messages = messages, error = error)
        }
    }

    fun updateInput(input: String) {
        mutableUiState.value = mutableUiState.value.copy(input = input)
    }

    fun updateCurrentUser(username: String, avatarUrl: String) {
        currentUsername = username
        currentAvatarUrl = avatarUrl
    }

    fun send() {
        val text = mutableUiState.value.input.trim()
        if (text.isBlank() || mutableUiState.value.sending) return
        mutableUiState.value = mutableUiState.value.copy(input = "", error = null, sending = true)
        repository.sendMessage(roomId, currentUid, currentUsername.ifBlank { "You" }, currentAvatarUrl, text) { error ->
            mutableUiState.value = mutableUiState.value.copy(sending = false, error = error)
        }
    }

    override fun onCleared() {
        messagesSubscription.cancel()
    }

    companion object {
        fun factory(
            repository: ChatRepository,
            roomId: String,
            currentUid: String,
            currentUsername: String,
            currentAvatarUrl: String,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DirectChatViewModel(repository, roomId, currentUid, currentUsername, currentAvatarUrl) as T
        }
    }
}
