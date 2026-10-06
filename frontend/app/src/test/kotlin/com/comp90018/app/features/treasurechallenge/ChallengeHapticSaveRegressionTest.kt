package com.comp90018.app.features.treasurechallenge

import com.comp90018.app.features.haptics.*
import org.junit.Assert.*
import org.junit.Test

class ChallengeHapticSaveRegressionTest {
    @Test fun photoChallengeCompletionWaitsForSaveAndFailedRetryBeforeRevealAndHaptic() {
        val events = mutableListOf<TreasureHapticEvent>()
        val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }
        val discovery = ChallengeDiscoverySave()
        var callback: ((String?) -> Unit)? = null
        val save: ((String?) -> Unit) -> Unit = { done ->
            TreasureHapticSave.collect("system_garden_glasshouse", false, controller,
                { _, result -> callback = result }, done)
        }
        discovery.onChallengeCompleted(save)
        assertTrue(events.isEmpty())
        assertFalse(canOpenTreasureReveal(true, discovery.status))
        requireNotNull(callback)("offline")
        assertTrue(events.isEmpty())
        assertFalse(canOpenTreasureReveal(true, discovery.status))
        discovery.retry(save)
        assertTrue(events.isEmpty())
        requireNotNull(callback)(null)
        requireNotNull(callback)(null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("system_garden_glasshouse")), events)
        assertTrue(canOpenTreasureReveal(true, discovery.status))
    }
}
