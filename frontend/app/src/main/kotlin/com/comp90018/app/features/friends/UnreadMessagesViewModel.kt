package com.comp90018.app.features.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.comp90018.app.data.social.SocialRepository
import com.comp90018.app.data.social.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnreadMessagesViewModel(
    repository: SocialRepository,
    currentUid: String,
) : ViewModel() {
    private val mutableUnreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = mutableUnreadCount.asStateFlow()

    private val friendsSubscription: Subscription = repository.observeFriends(currentUid) { friends, _ ->
        mutableUnreadCount.value = friends.sumOf { it.unreadCount.toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    override fun onCleared() {
        friendsSubscription.cancel()
    }

    companion object {
        fun factory(repository: SocialRepository, currentUid: String) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UnreadMessagesViewModel(repository, currentUid) as T
        }
    }
}
