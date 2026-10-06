package com.comp90018.app.features.haptics

/** Decorates existing persistence, forwarding its result without altering collection behavior. */
object TreasureHapticSave {
    fun collect(
        treasureId: String,
        alreadyDiscovered: Boolean,
        controller: TreasureHapticController?,
        save: (String, (String?) -> Unit) -> Unit,
        onComplete: (String?) -> Unit,
    ) {
        save(treasureId) { error ->
            controller?.discoverySaved(treasureId, error, alreadyDiscovered)
            onComplete(error)
        }
    }
}
