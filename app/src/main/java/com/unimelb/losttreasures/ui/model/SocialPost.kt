package com.unimelb.losttreasures.ui.model

data class SocialPost(
    val author: String,
    val location: String,
    val message: String,
    val timeAgo: String,
    val likes: Int
)
