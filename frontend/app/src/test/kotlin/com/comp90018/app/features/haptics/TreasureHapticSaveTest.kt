package com.comp90018.app.features.haptics

import org.junit.Assert.*
import org.junit.Test

class TreasureHapticSaveTest {
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
        callback!!("failed")
        assertEquals(listOf("failed"), results)
        assertTrue(events.isEmpty())
        TreasureHapticSave.collect("a", false, controller, save, results::add)
        callback!!(null)
        callback!!(null) // repeated callbacks still forward, but never duplicate feedback
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
}
