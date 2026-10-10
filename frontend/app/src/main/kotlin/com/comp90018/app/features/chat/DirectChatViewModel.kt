package com.comp90018.app.features.chat

/*
 * Maintains a direct conversation, its draft, message subscription, and send failures.
 * Visibility controls read acknowledgements; confirmed writes determine when the draft is cleared.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.chat.ChatMessage
import com.comp90018.app.data.chat.ChatRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.net.Uri

/** Messages, draft, send status, and errors observed by one conversation. */
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
    private val friendUid: String,
    private var currentUsername: String,
    private var currentAvatarUrl: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(DirectChatUiState())
    val uiState: StateFlow<DirectChatUiState> = mutableUiState.asStateFlow()
    private val messagesSubscription: Subscription
    private var isScreenVisible = false

    init {
        messagesSubscription = repository.observeMessages(roomId) { messages, error ->
            mutableUiState.value = mutableUiState.value.copy(messages = if (error == null) messages else mutableUiState.value.messages, error = error)
            if (isScreenVisible) repository.markMessagesRead(currentUid, friendUid)
        }
    }

    /** Only clear the badge while this conversation is actually on screen. */
    fun setScreenVisible(visible: Boolean) {
        isScreenVisible = visible
        if (visible && mutableUiState.value.messages.isNotEmpty()) {
            repository.markMessagesRead(currentUid, friendUid)
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
        // Keep the draft visible until Firestore confirms the write. This prevents
        // permission/network errors from making the user's message disappear.
        mutableUiState.value = mutableUiState.value.copy(error = null, sending = true)
        repository.sendMessage(roomId, currentUid, currentUsername.ifBlank { "You" }, currentAvatarUrl, text) { error ->
            val currentState = mutableUiState.value
            mutableUiState.value = currentState.copy(
                input = if (error == null && currentState.input.trim() == text) "" else currentState.input,
                sending = false,
                error = error,
            )
        }
    }
    /** Uploads a selected image through the repository and exposes send errors to the conversation UI. */
    fun sendImage(uri: Uri) {
        if (mutableUiState.value.sending) return
        mutableUiState.value = mutableUiState.value.copy(error = null, sending = true)
        repository.sendImage(roomId, currentUid, currentUsername.ifBlank { "You" }, currentAvatarUrl, uri) { error ->
            mutableUiState.value = mutableUiState.value.copy(sending = false, error = error)
        }
    }

    fun sendTreasureSticker(treasureId: String) {
        if (mutableUiState.value.sending) return
        mutableUiState.value = mutableUiState.value.copy(error = null, sending = true)
        repository.sendTreasureSticker(
            roomId,
            currentUid,
            currentUsername.ifBlank { "You" },
            currentAvatarUrl,
            treasureId,
        ) { error ->
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
            friendUid: String,
            currentUsername: String,
            currentAvatarUrl: String,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DirectChatViewModel(repository, roomId, currentUid, friendUid, currentUsername, currentAvatarUrl) as T
        }
    }
}
