package com.comp90018.app.features.rooms

/*
 * Owns room membership, public-room browsing, create/join inputs, and operation errors.
 * The entry and plaza screens observe this state and invoke its actions.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Membership, browse results, entry fields, and errors shown by room-entry and plaza screens. */
data class RoomsUiState(
    val publicRooms: List<TeamRoom> = emptyList(),
    val browsingLoading: Boolean = false,
    val browsingError: String? = null,
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
        val previous = mutableUiState.value
        val roomWasExited = previous.activeRoomId != null && roomId == null
        mutableUiState.value = previous.copy(
            activeRoomId = roomId,
            membershipError = if (roomWasExited) null else error,
            actionError = if (roomWasExited || roomId != null) null else previous.actionError,
            working = if (roomWasExited) false else previous.working,
        )
    }

    private var browsingSubscription: Subscription? = null
    fun startBrowsing() {
        if (browsingSubscription != null) return
        mutableUiState.value = mutableUiState.value.copy(browsingLoading = true)
        browsingSubscription = repository.observePublicRooms { rooms, error ->
            mutableUiState.value = mutableUiState.value.copy(publicRooms = rooms, browsingLoading = false, browsingError = error)
        }
    }
    fun stopBrowsing() { browsingSubscription?.cancel(); browsingSubscription = null }
    fun joinPublicRoom(room: TeamRoom) {
        if (mutableUiState.value.working) return
        mutableUiState.value = mutableUiState.value.copy(working = true, actionError = null)
        repository.joinPublicRoom(room.id, userId) { error ->
            mutableUiState.value = mutableUiState.value.copy(working = false, actionError = error,
                activeRoomId = if (error == null) room.id else mutableUiState.value.activeRoomId)
        }
    }

    fun updateRoomId(roomId: String) {
        mutableUiState.value = mutableUiState.value.copy(roomIdInput = roomId, actionError = null, membershipError = null)
    }

    fun beginJoin() {
        mutableUiState.value = mutableUiState.value.copy(joining = true, actionError = null)
    }

    fun clearErrors() {
        mutableUiState.value = mutableUiState.value.copy(actionError = null, membershipError = null, working = false)
    }

    fun createRoom(name: String, maxMembers: Int, description: String, idOnly: Boolean = true) {
        if (mutableUiState.value.working) return
        mutableUiState.value = mutableUiState.value.copy(working = true, actionError = null)
        repository.createRoom(userId, name, maxMembers, description, idOnly) { roomId, error ->
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
        stopBrowsing()
    }

    companion object {
        fun factory(repository: TeamRoomRepository, userId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RoomsViewModel(repository, userId) as T
        }
    }
}
