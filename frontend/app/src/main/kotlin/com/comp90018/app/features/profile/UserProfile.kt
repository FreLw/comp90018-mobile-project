package com.comp90018.app.features.profile

data class ProfileExtras(
    val phone: String = "",
    val department: String = "",
    val major: String = "",
)

data class AppSettings(
    val notifications: Boolean = true,
    val searchable: Boolean = true,
    val soundEffects: Boolean = true,
    val haptics: Boolean = true,
    val preciseLocation: Boolean = true,
)

/** Data owned by the Profile feature and stored in the `users` Firestore collection. */
data class UserProfile(
    val uid: String,
    val email: String,
    val username: String,
    val displayName: String,
    val gender: String,
    val bio: String,
    val avatarUrl: String,
    val extras: ProfileExtras = ProfileExtras(),
    val settings: AppSettings = AppSettings(),
)
