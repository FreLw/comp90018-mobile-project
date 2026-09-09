package com.comp90018.app.features.profile

/** Data owned by the Profile feature and stored in the `users` Firestore collection. */
data class UserProfile(
    val uid: String,
    val email: String,
    val username: String,
    val displayName: String,
    val gender: String,
    val bio: String,
    val avatarUrl: String,
)
