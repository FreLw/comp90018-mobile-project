package com.comp90018.app.features.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.TeamRoom
import com.comp90018.app.TeamRoomMember
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
    val publicRooms: List<TeamRoom> = emptyList(),
    val publicRoomMembers: Map<String, List<TeamRoomMember>> = emptyMap(),
    val plazaLoading: Boolean = true,
    val plazaError: String? = null,
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
    private val plazaSubscription: Subscription = repository.observePublicRooms { rooms, error ->
        val visibleRoomIds = rooms.mapTo(mutableSetOf()) { it.id }
        mutableUiState.value = mutableUiState.value.copy(
            publicRooms = rooms,
            publicRoomMembers = mutableUiState.value.publicRoomMembers.filterKeys { it in visibleRoomIds },
            plazaLoading = false,
            plazaError = error,
        )
        rooms.forEach { room ->
            repository.loadMembers(room.memberIds) { members ->
                if (mutableUiState.value.publicRooms.any { it.id == room.id }) {
                    mutableUiState.value = mutableUiState.value.copy(
                        publicRoomMembers = mutableUiState.value.publicRoomMembers + (room.id to members),
                    )
                }
            }
        }
    }

    fun updateRoomId(roomId: String) {
        mutableUiState.value = mutableUiState.value.copy(
            roomIdInput = roomId.filter(Char::isDigit).take(6),
            actionError = null,
            membershipError = null,
        )
    }

    fun clearErrors() {
        mutableUiState.value = mutableUiState.value.copy(actionError = null, membershipError = null, working = false)
    }

    fun createRoom(name: String, isPublic: Boolean, maxMembers: Int) {
        if (mutableUiState.value.working) return
        mutableUiState.value = mutableUiState.value.copy(working = true, actionError = null)
        repository.createRoom(userId, name, isPublic, maxMembers) { roomId, error ->
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
        mutableUiState.value = mutableUiState.value.copy(working = true, joining = true, actionError = null)
        repository.joinRoom(roomId, userId) { error ->
            mutableUiState.value = mutableUiState.value.copy(
                activeRoomId = if (error == null) roomId else mutableUiState.value.activeRoomId,
                working = false,
                joining = false,
                actionError = error,
            )
        }
    }

    fun joinRoom(roomId: String) {
        if (mutableUiState.value.working) return
        mutableUiState.value = mutableUiState.value.copy(roomIdInput = roomId, working = true, joining = true, actionError = null)
        repository.joinRoom(roomId, userId) { error ->
            mutableUiState.value = mutableUiState.value.copy(
                activeRoomId = if (error == null) roomId else mutableUiState.value.activeRoomId,
                working = false,
                joining = false,
                actionError = error,
            )
        }
    }

    override fun onCleared() {
        membershipSubscription.cancel()
        plazaSubscription.cancel()
    }

    companion object {
        fun factory(repository: TeamRoomRepository, userId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = RoomsViewModel(repository, userId) as T
        }
    }
}
