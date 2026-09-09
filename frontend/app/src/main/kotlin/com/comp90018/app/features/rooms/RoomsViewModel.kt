package com.comp90018.app.features.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RoomsUiState(
    val activeRoomId: String? = null,
    val membershipError: String? = null,
    val roomIdInput: String = "",
    val joining: Boolean = false,
    val working: Boolean = false,
    val actionError: String? = null,
)

class RoomsViewModel(
    private val repository: TeamRoomRepository,
    private val userId: String,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(RoomsUiState())
    val uiState: StateFlow<RoomsUiState> = mutableUiState.asStateFlow()
    private val membershipSubscription: Subscription = repository.observeMembership(userId) { roomId, error ->
        mutableUiState.value = mutableUiState.value.copy(activeRoomId = roomId, membershipError = error)
    }

    fun updateRoomId(roomId: String) {
        mutableUiState.value = mutableUiState.value.copy(roomIdInput = roomId)
    }

    fun beginJoin() {
        mutableUiState.value = mutableUiState.value.copy(joining = true, actionError = null)
    }

    fun createRoom() {
        if (mutableUiState.value.working) return
        mutableUiState.value = mutableUiState.value.copy(working = true, actionError = null)
        repository.createRoom(userId) { roomId, error ->
            mutableUiState.value = mutableUiState.value.copy(
                activeRoomId = roomId ?: mutableUiState.value.activeRoomId,
                working = false,
                actionError = error,
            )
        }
    }

    fun joinRoom() {
        val roomId = mutableUiState.value.roomIdInput.trim()
        if (roomId.isBlank() || mutableUiState.value.working) return
        mutableUiState.value = mutableUiState.value.copy(working = true, actionError = null)
        repository.joinRoom(roomId, userId) { error ->
            mutableUiState.value = mutableUiState.value.copy(
                activeRoomId = if (error == null) roomId else mutableUiState.value.activeRoomId,
                working = false,
                actionError = error,
            )
        }
    }

    override fun onCleared() {
        membershipSubscription.cancel()
    }

    companion object {
        fun factory(repository: TeamRoomRepository, userId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RoomsViewModel(repository, userId) as T
        }
    }
}
