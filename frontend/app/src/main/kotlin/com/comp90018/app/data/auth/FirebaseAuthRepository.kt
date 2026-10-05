package com.comp90018.app.data.auth

import com.google.firebase.auth.FirebaseAuth

/** Firebase implementation kept behind [AuthRepository] for a testable auth feature. */
class FirebaseAuthRepository(private val auth: FirebaseAuth) : AuthRepository {
    override fun login(email: String, password: String, onComplete: (String?) -> Unit) =
        FirebaseAuthService.login(auth, email, password, onComplete)

    override fun register(email: String, password: String, onComplete: (String?) -> Unit) =
        FirebaseAuthService.register(auth, email, password, onComplete)
}
