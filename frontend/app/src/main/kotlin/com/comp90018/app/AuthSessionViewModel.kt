package com.comp90018.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/** Retains signed-in state across rotation and releases all listeners when the session ends. */
class AuthSessionViewModel : ViewModel() {
    private var userId: String? = null
    private var sessionOwner: ViewModelStoreOwner? = null

    fun updateUser(uid: String?) {
        if (uid == userId) return
        sessionOwner?.viewModelStore?.clear()
        sessionOwner = null
        userId = uid
    }

    fun ownerFor(uid: String): ViewModelStoreOwner {
        updateUser(uid)
        return sessionOwner ?: object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }.also { sessionOwner = it }
    }

    override fun onCleared() {
        sessionOwner?.viewModelStore?.clear()
    }
}
