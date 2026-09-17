package com.comp90018.app.features.rooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnreadRoomMessagesViewModel(
    repository: TeamRoomRepository,
    userId: String,
) : ViewModel() {
    private val mutableUnreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = mutableUnreadCount.asStateFlow()

    private val membershipSubscription: Subscription = repository.observeUnreadMessages(userId) { unreadCount, _ ->
        mutableUnreadCount.value = unreadCount
    }

    override fun onCleared() {
        membershipSubscription.cancel()
    }

    companion object {
        fun factory(repository: TeamRoomRepository, userId: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UnreadRoomMessagesViewModel(repository, userId) as T
        }
    }
}
