package com.comp90018.app.features.haptics

import org.junit.Assert.*
import org.junit.Test

class TreasureHapticSaveTest {
    @Test fun teamUiWaitsForDatabaseClaimAndReceivesClaimFailure() {
        val results = mutableListOf<String?>()
        var claimCallback: ((String?) -> Unit)? = null
        TreasureHapticSave.collectTeam("a", null, { _, complete -> complete(null) },
            { _, complete -> claimCallback = complete }, results::add)
        assertTrue(results.isEmpty())
        requireNotNull(claimCallback)("claim failed")
        requireNotNull(claimCallback)(null)
        assertEquals(listOf("claim failed"), results)
    }
    @Test fun waitsForPersistenceAndPreservesFailureRetryCallbacks() {
        val events = mutableListOf<TreasureHapticEvent>()
        val results = mutableListOf<String?>()
        val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }
        var callback: ((String?) -> Unit)? = null
        val save: (String, (String?) -> Unit) -> Unit = { id, complete ->
            assertEquals("a", id); callback = complete
        }
        TreasureHapticSave.collect("a", false, controller, save, results::add)
        assertTrue(events.isEmpty()) // challenge completion alone, before asynchronous confirmation
        requireNotNull(callback)("failed")
        assertEquals(listOf("failed"), results)
        assertTrue(events.isEmpty())
        TreasureHapticSave.collect("a", false, controller, save, results::add)
        requireNotNull(callback)(null)
        requireNotNull(callback)(null) // repeated callbacks still forward, but never duplicate feedback
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
        assertEquals(listOf("failed", null, null), results)
    }
    @Test fun cancelledSaveAndAlreadyCollectedTreasureDoNotVibrate() {
        val events = mutableListOf<TreasureHapticEvent>()
        val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }
        TreasureHapticSave.collect("a", false, controller, { _, _ -> }, {})
        TreasureHapticSave.collect("b", true, controller, { _, complete -> complete(null) }, {})
        assertTrue(events.isEmpty())
    }
    @Test fun teamUnlockWaitsForClaimAndNeverVibratesForFailedClaim() {
        val events = mutableListOf<TreasureHapticEvent>()
        val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }
        var claimCallback: ((String?) -> Unit)? = null
        var claimCount = 0
        val save: (String, (String?) -> Unit) -> Unit = { _, complete -> complete(null) }
        val claim: (TreasureHapticAttempt?, (String?) -> Unit) -> Unit = { attempt, complete ->
            claimCount++
            claimCallback = { error ->
                if (attempt != null) controller.completeAttempt(attempt, error, completionId = "team:confirmed")
                complete(error)
            }
        }
        TreasureHapticSave.collectTeam("a", controller, save, claim, {})
        assertTrue(events.isEmpty())
        requireNotNull(claimCallback)("claim failed")
        assertTrue(events.isEmpty())
        TreasureHapticSave.collectTeam("a", controller, save, claim, {})
        requireNotNull(claimCallback)(null)
        requireNotNull(claimCallback)(null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("a")), events)
        TreasureHapticSave.collectTeam("b", controller, { _, complete -> complete("save failed") }, claim, {})
        assertEquals(2, claimCount)
    }

}
