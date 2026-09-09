package com.comp90018.app.features.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.ChatMessage
import com.comp90018.app.TeamRoom
import com.comp90018.app.TeamRoomMember
import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TeamRoomChatUiState(
    val room: TeamRoom? = null,
    val messages: List<ChatMessage> = emptyList(),
    val members: List<TeamRoomMember> = emptyList(),
    val input: String = "",
    val error: String? = null,
    val sending: Boolean = false,
    val leaving: Boolean = false,
)

class TeamRoomChatViewModel(
    private val repository: TeamRoomRepository,
    private val roomId: String,
    private val userId: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TeamRoomChatUiState())
    val uiState: StateFlow<TeamRoomChatUiState> = mutableUiState.asStateFlow()
    private val roomSubscription: Subscription
    private val messagesSubscription: Subscription

    init {
        roomSubscription = repository.observeRoom(roomId) { room, error ->
            mutableUiState.value = mutableUiState.value.copy(room = room, error = error)
            room?.memberIds?.let { memberIds -> repository.loadMembers(memberIds) { members ->
                mutableUiState.value = mutableUiState.value.copy(members = members)
            } }
        }
        messagesSubscription = repository.observeMessages(roomId) { messages, error ->
            mutableUiState.value = mutableUiState.value.copy(messages = messages, error = error ?: mutableUiState.value.error)
        }
    }

    fun updateInput(input: String) { mutableUiState.value = mutableUiState.value.copy(input = input) }

    fun send(senderName: String, senderAvatarUrl: String) {
        val text = mutableUiState.value.input.trim()
        if (text.isBlank() || mutableUiState.value.sending) return
        mutableUiState.value = mutableUiState.value.copy(input = "", error = null, sending = true)
        repository.sendMessage(roomId, userId, senderName.ifBlank { "You" }, senderAvatarUrl, text) { error ->
            mutableUiState.value = mutableUiState.value.copy(sending = false, error = error)
        }
    }

    fun leave(onExited: () -> Unit) = exit(onExited) { complete -> repository.leaveRoom(roomId, userId, complete) }
    fun dismiss(onExited: () -> Unit) = exit(onExited) { complete -> repository.dismissRoom(roomId, userId, complete) }

    private fun exit(onExited: () -> Unit, action: ((String?) -> Unit) -> Unit) {
        if (mutableUiState.value.leaving) return
        mutableUiState.value = mutableUiState.value.copy(leaving = true, error = null)
        action { error ->
            mutableUiState.value = mutableUiState.value.copy(leaving = false, error = error)
            if (error == null) onExited()
        }
    }

    override fun onCleared() {
        roomSubscription.cancel()
        messagesSubscription.cancel()
    }

    companion object {
        fun factory(repository: TeamRoomRepository, roomId: String, userId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                TeamRoomChatViewModel(repository, roomId, userId) as T
        }
    }
}
