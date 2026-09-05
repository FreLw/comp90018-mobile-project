package com.unimelb.losttreasures.ui.model

enum class HuntType {
    Solo,
    Collaborative
}

enum class RelicTone {
    Green,
    Blue,
    Gold,
    Red
}

data class Relic(
    val id: String,
    val name: String,
    val place: String,
    val distance: String,
    val type: HuntType,
    val condition: String,
    val progress: Float,
    val mapX: Float,
    val mapY: Float,
    val tone: RelicTone,
    val story: String
)
