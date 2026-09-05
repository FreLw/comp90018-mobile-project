package com.unimelb.losttreasures.ui.model

data class UserProfile(
    val displayName: String,
    val studentId: String,
    val email: String,
    val level: Int,
    val discoveredCount: Int,
    val teamCode: String,
    val currentTitle: String
)
