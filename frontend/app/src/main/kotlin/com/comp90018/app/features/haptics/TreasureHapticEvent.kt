package com.comp90018.app.features.haptics

/*
 * Defines semantic treasure feedback events and the driver interface that plays them.
 * Separates UI feedback intent from the Android vibration implementation.
 */

sealed interface TreasureHapticEvent {
    val treasureId: String
    data class Nearby(override val treasureId: String) : TreasureHapticEvent
    data class Unlocked(override val treasureId: String) : TreasureHapticEvent
}

/** Driver boundary also permits hardware-free tests. */
fun interface TreasureHapticDriver {
    fun play(event: TreasureHapticEvent)
}
