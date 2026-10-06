package com.comp90018.app.features.haptics

import org.junit.Assert.*
import org.junit.Test

class TreasureHapticSessionViewModelTest {
    @Test fun unknownAndDisabledPreferencesSuppressActualSessionEvents() {
        val events = mutableListOf<TreasureHapticEvent>()
        var cancellations = 0
        val session = TreasureHapticSessionViewModel(events::add) { cancellations++ }
        session.setForeground(true)
        session.setPreference(null)
        session.controller.nearby("a", 20.0)
        session.controller.discoverySaved("unknown", null)
        session.setPreference(false)
        session.controller.nearby("a", 20.0)
        assertTrue(events.isEmpty())
        session.setPreference(true)
        session.controller.nearby("a", 20.0)
        session.controller.discoverySaved("known", null)
        assertEquals(listOf(TreasureHapticEvent.Nearby("a"), TreasureHapticEvent.Unlocked("known")), events)
        session.setPreference(null)
        assertFalse(session.controller.enabled)
        assertEquals(3, cancellations)
    }
}
