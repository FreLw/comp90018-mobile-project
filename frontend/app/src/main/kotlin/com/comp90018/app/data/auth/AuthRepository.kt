package com.comp90018.app.data.auth

/** Authentication boundary used by the signed-out UI. */
interface AuthRepository {
    fun login(email: String, password: String, onComplete: (String?) -> Unit)
    fun register(email: String, password: String, onComplete: (String?) -> Unit)
}
