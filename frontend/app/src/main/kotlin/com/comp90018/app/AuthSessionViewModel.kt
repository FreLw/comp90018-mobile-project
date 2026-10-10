package com.comp90018.app

/*
 * Keeps feature ViewModels alive during one signed-in session, including activity recreation.
 * Changing or signing out of the account clears that session store and its active listeners.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/** Retains signed-in state across rotation and releases all listeners when the session ends. */
class AuthSessionViewModel : ViewModel() {
    private var userId: String? = null
    private var sessionOwner: ViewModelStoreOwner? = null

    /** Releases the previous account state before installing another account or returning to login. */
    fun updateUser(uid: String?) {
        if (uid == userId) return
        sessionOwner?.viewModelStore?.clear()
        sessionOwner = null
        userId = uid
    }

    /** Returns the retained ViewModel store that all screens in this signed-in session share. */
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
