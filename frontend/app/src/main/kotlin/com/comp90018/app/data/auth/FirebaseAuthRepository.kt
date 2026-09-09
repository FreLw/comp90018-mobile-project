package com.comp90018.app.data.auth

import com.comp90018.app.FirebaseAuthService
import com.google.firebase.auth.FirebaseAuth

class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    override fun login(email: String, password: String, onComplete: (String?) -> Unit) =
        FirebaseAuthService.login(auth, email, password, onComplete)

    override fun register(email: String, password: String, onComplete: (String?) -> Unit) =
        FirebaseAuthService.register(auth, email, password, onComplete)
}
