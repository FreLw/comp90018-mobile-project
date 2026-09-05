package com.unimelb.losttreasures.ui.model

data class ChatMessage(
    val author: String,
    val text: String,
    val mine: Boolean
)
