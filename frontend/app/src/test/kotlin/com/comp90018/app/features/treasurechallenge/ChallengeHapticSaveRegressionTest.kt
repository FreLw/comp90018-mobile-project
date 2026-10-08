package com.comp90018.app.features.treasurechallenge

import com.comp90018.app.features.haptics.*
import org.junit.Assert.*
import org.junit.Test

class ChallengeHapticSaveRegressionTest {
    @Test fun taskCompletionRevealsAndVibratesBeforeSaveAndRetryNeverRepeatsFeedback() {
        val events = mutableListOf<TreasureHapticEvent>()
        val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }
        val discovery = ChallengeDiscoverySave()
        var callback: ((String?) -> Unit)? = null
        val save: ((String?) -> Unit) -> Unit = { done ->
            TreasureHapticSave.collect("system_garden_glasshouse", false, controller,
                { _, result -> callback = result }, done)
        }
        var reveals = 0
        val ready = { reveals++; controller.challengeCompleted("system_garden_glasshouse", "task-1") }
        discovery.onChallengeCompleted(save, ready)
        discovery.onChallengeCompleted(save, ready)
        assertEquals(1, reveals)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("system_garden_glasshouse")), events)
        assertTrue(canOpenTreasureReveal(true))
        requireNotNull(callback)("offline")
        assertEquals(1, events.size)
        assertTrue(canOpenTreasureReveal(true))
        discovery.retry(save)
        assertEquals(1, events.size)
        requireNotNull(callback)(null)
        requireNotNull(callback)(null)
        assertEquals(listOf(TreasureHapticEvent.Unlocked("system_garden_glasshouse")), events)
        assertEquals(1, reveals)
        assertTrue(canOpenTreasureReveal(true))
    }
}
