package com.comp90018.app.features.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.chat.ChatMessage
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TeamRoomMember
import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.net.Uri
import com.comp90018.app.BuildConfig
import com.comp90018.app.features.map.SouthLawnFragmentIds

data class TeamRoomChatUiState(
    val room: TeamRoom? = null,
    val messages: List<ChatMessage> = emptyList(),
    val members: List<TeamRoomMember> = emptyList(),
    val input: String = "",
    val error: String? = null,
    val sending: Boolean = false,
    val leaving: Boolean = false,
    val updatingTask: Boolean = false,
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
    private var isScreenVisible = false

    init {
        roomSubscription = repository.observeRoom(roomId) { room, error ->
            mutableUiState.value = mutableUiState.value.copy(room = room, error = error)
            room?.memberIds?.let { memberIds -> repository.loadMembers(memberIds) { members ->
                // Profile lookups are asynchronous. Ignore an older lookup when a
                // newer room snapshot (for example, a teammate joining) arrived first.
                if (mutableUiState.value.room?.memberIds == memberIds) {
                    mutableUiState.value = mutableUiState.value.copy(members = members)
                }
            } }
        }
        messagesSubscription = repository.observeMessages(roomId) { messages, error ->
            mutableUiState.value = mutableUiState.value.copy(messages = messages, error = error ?: mutableUiState.value.error)
            if (isScreenVisible) repository.markMessagesRead(userId)
        }
    }

    /** Only clear the badge while this conversation is actually on screen. */
    fun setScreenVisible(visible: Boolean) {
        isScreenVisible = visible
        if (visible && mutableUiState.value.messages.isNotEmpty()) {
            repository.markMessagesRead(userId)
        }
    }

    fun updateInput(input: String) { mutableUiState.value = mutableUiState.value.copy(input = input) }

    fun send(senderName: String, senderAvatarUrl: String) {
        val text = mutableUiState.value.input.trim()
        if (text.isBlank() || mutableUiState.value.sending) return
        mutableUiState.value = mutableUiState.value.copy(input = "", error = null, sending = true)
        val recipientId = mutableUiState.value.room?.memberIds?.firstOrNull { it != userId }
        repository.sendMessage(roomId, userId, recipientId, senderName.ifBlank { "You" }, senderAvatarUrl, text) { error ->
            mutableUiState.value = mutableUiState.value.copy(sending = false, error = error)
        }
    }
    fun sendImage(uri: Uri, senderName: String, senderAvatarUrl: String) {
        if (mutableUiState.value.sending) return
        mutableUiState.value = mutableUiState.value.copy(error = null, sending = true)
        val recipientId = mutableUiState.value.room?.memberIds?.firstOrNull { it != userId }
        repository.sendImage(roomId, userId, recipientId, senderName.ifBlank { "You" }, senderAvatarUrl, uri) { error ->
            mutableUiState.value = mutableUiState.value.copy(sending = false, error = error)
        }
    }

    fun sendTreasureSticker(treasureId: String, senderName: String, senderAvatarUrl: String) {
        if (mutableUiState.value.sending) return
        mutableUiState.value = mutableUiState.value.copy(sending = true, error = null)
        repository.sendTreasureSticker(roomId, userId, senderName.ifBlank { "You" }, senderAvatarUrl, treasureId) { error ->
            mutableUiState.value = mutableUiState.value.copy(sending = false, error = error)
        }
    }

    fun updateSettings(name: String, maxMembers: Int, description: String, idOnly: Boolean = true, onSaved: () -> Unit) {
        if (mutableUiState.value.updatingTask) return
        mutableUiState.value = mutableUiState.value.copy(updatingTask = true, error = null)
        repository.updateSettings(roomId, userId, name, maxMembers, description, idOnly) { error ->
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
            if (error == null) onSaved()
        }
    }

    fun removeMember(memberId: String, onRemoved: () -> Unit) {
        val state = mutableUiState.value
        if (state.updatingTask || state.room?.creatorId != userId || memberId == userId) return
        mutableUiState.value = state.copy(updatingTask = true, error = null)
        repository.removeMember(roomId, userId, memberId) { error ->
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
            if (error == null) onRemoved()
        }
    }

    fun selectDestination(taskId: String, taskTitle: String) {
        if (mutableUiState.value.updatingTask) return
        mutableUiState.value = mutableUiState.value.copy(updatingTask = true, error = null)
        repository.selectTask(roomId, userId, taskId, taskTitle) { error ->
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
        }
    }

    fun startHunt(onStarted: () -> Unit) {
        val state = mutableUiState.value
        if (state.updatingTask || state.room?.creatorId != userId || state.room.memberIds.size < 2 || state.room.taskId.isBlank()) return
        mutableUiState.value = state.copy(updatingTask = true, error = null)
        repository.startHunt(roomId, userId) { error ->
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
            if (error == null) onStarted()
        }
    }

    fun terminateHunt() {
        val state = mutableUiState.value
        if (state.updatingTask || state.room?.creatorId != userId || state.room.taskStatus != "hunting") return
        mutableUiState.value = state.copy(updatingTask = true, error = null)
        repository.terminateHunt(roomId, userId) { error ->
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
        }
    }

    fun completeHuntTask() = completeHuntTaskWithConfirmation {}

    fun completeHuntTaskWithConfirmation(onComplete: (String?) -> Unit) {
        if (mutableUiState.value.updatingTask) {
            onComplete("A hunt update is already in progress")
            return
        }
        mutableUiState.value = mutableUiState.value.copy(updatingTask = true, error = null)
        repository.completeHuntTask(roomId, userId) { error ->
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
            onComplete(error)
        }
    }

    fun findHuntFragment(fragmentId: String) {
        if (mutableUiState.value.updatingTask) return
        mutableUiState.value = mutableUiState.value.copy(updatingTask = true, error = null)
        repository.findHuntFragment(roomId, userId, fragmentId) { error ->
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
        }
    }

    /** Debug shortcut uses the normal shared-fragment transactions sequentially. */
    fun debugUnlockHuntFragments(fragmentIds: List<String>, onComplete: (String?) -> Unit) {
        if (!BuildConfig.DEBUG) {
            onComplete("Available in Debug builds only")
            return
        }
        if (fragmentIds.isEmpty() || fragmentIds.distinct().size != fragmentIds.size ||
            fragmentIds.any { it !in SouthLawnFragmentIds }) {
            onComplete("Invalid fragment configuration")
            return
        }
        if (mutableUiState.value.updatingTask) {
            onComplete("A hunt update is already in progress")
            return
        }
        mutableUiState.value = mutableUiState.value.copy(updatingTask = true, error = null)
        fun finish(error: String?) {
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
            onComplete(error)
        }
        fun collectNext(index: Int) {
            if (index == fragmentIds.size) {
                finish(null)
                return
            }
            repository.findHuntFragment(roomId, userId, fragmentIds[index]) { error ->
                if (error != null) finish(error) else collectNext(index + 1)
            }
        }
        collectNext(0)
    }

    fun claimCompletedHuntTreasure() = claimCompletedHuntTreasureWithConfirmation {}

    /** Exposes the existing transaction result to consumers such as success feedback. */
    fun claimCompletedHuntTreasureWithConfirmation(onComplete: (String?) -> Unit) {
        if (mutableUiState.value.updatingTask) {
            onComplete("A hunt update is already in progress")
            return
        }
        mutableUiState.value = mutableUiState.value.copy(updatingTask = true, error = null)
        repository.claimCompletedHuntTreasure(roomId, userId) { error ->
            mutableUiState.value = mutableUiState.value.copy(updatingTask = false, error = error)
            onComplete(error)
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
