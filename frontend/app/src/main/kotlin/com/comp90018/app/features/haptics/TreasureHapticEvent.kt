package com.comp90018.app.features.haptics

sealed interface TreasureHapticEvent {
    val treasureId: String
    data class Nearby(override val treasureId: String) : TreasureHapticEvent
    data class Unlocked(override val treasureId: String) : TreasureHapticEvent
}

/** Driver boundary also permits hardware-free tests. */
fun interface TreasureHapticDriver {
    fun play(event: TreasureHapticEvent)
}
