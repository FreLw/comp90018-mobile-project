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

    /** Team success requires both collection save and the existing shared claim transaction. */
    fun collectTeam(
        treasureId: String,
        controller: TreasureHapticController?,
        save: (String, (String?) -> Unit) -> Unit,
        claim: ((String?) -> Unit) -> Unit,
        onComplete: (String?) -> Unit,
    ) {
        save(treasureId) { error ->
            if (error == null) claim { claimError -> controller?.discoverySaved(treasureId, claimError) }
            // Preserve the original UI/collection callback timing; claim errors remain in Room state.
            onComplete(error)
        }
    }
}
