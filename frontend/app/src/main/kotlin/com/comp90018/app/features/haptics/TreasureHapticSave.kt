package com.comp90018.app.features.haptics

/*
 * Decorates collection and team-claim callbacks with confirmed feedback handling.
 * Forwards persistence errors unchanged so the existing UI can still show and retry failed saves.
 */

/** Decorates existing persistence, forwarding its result without altering collection behavior. */
object TreasureHapticSave {
    fun collect(
        treasureId: String,
        alreadyDiscovered: Boolean,
        controller: TreasureHapticController?,
        save: (String, (String?) -> Unit) -> Unit,
        onComplete: (String?) -> Unit,
    ) {
        val attempt = controller?.beginAttempt(treasureId)
        save(treasureId) { error ->
            if (attempt != null) controller.completeAttempt(attempt, error, alreadyDiscovered)
            onComplete(error)
        }
    }

    /** Team success requires both collection save and the existing shared claim transaction. */
    fun collectTeam(
        treasureId: String,
        controller: TreasureHapticController?,
        save: (String, (String?) -> Unit) -> Unit,
        claim: (TreasureHapticAttempt?, (String?) -> Unit) -> Unit,
        onComplete: (String?) -> Unit,
    ) {
        val attempt = controller?.beginAttempt(treasureId)
        var resolved = false
        var claimResolved = false
        save(treasureId) { error ->
            if (!resolved) {
                resolved = true
                if (error == null) {
                    claim(attempt) { claimError ->
                        if (!claimResolved) {
                            claimResolved = true
                            onComplete(claimError)
                        }
                    }
                } else {
                    if (attempt != null) controller.completeAttempt(attempt, error)
                    onComplete(error)
                }
            }
        }
    }
}
