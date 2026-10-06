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
    @Test fun rotationRetainsDeduplicationButSignOutPermanentlyClosesOldController() {
        val events = mutableListOf<TreasureHapticEvent>()
        val session = TreasureHapticSessionViewModel(events::add)
        session.setPreference(true); session.setForeground(true)
        val old = session.controller
        old.nearby("a", 20.0)
        session.detach(changingConfigurations = true)
        session.beginSession(); session.setForeground(true)
        assertSame(old, session.controller)
        session.controller.nearby("a", 20.0)
        assertEquals(1, events.size)
        session.endSession()
        session.beginSession(); session.setPreference(true); session.setForeground(true)
        assertNotSame(old, session.controller)
        old.discoverySaved("late", null)
        old.enabled = true; old.foreground = true
        old.discoverySaved("even-later", null)
        session.controller.nearby("a", 20.0)
        assertEquals(listOf(TreasureHapticEvent.Nearby("a"), TreasureHapticEvent.Nearby("a")), events)
        session.detach(changingConfigurations = false)
        assertTrue(session.controller.ended)
    }

}
