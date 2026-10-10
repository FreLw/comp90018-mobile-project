package com.comp90018.app.features.haptics

/*
 * Checks that the Compose/lifecycle binding applies preferences and cancels feedback at the correct time.
 * Run these device/Compose checks when changing the corresponding interface or interaction contract.
 */

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Uses the exact lifecycle/settings binding installed in AppShell, with a recording driver. */
class HapticSessionBindingTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val events = mutableListOf<TreasureHapticEvent>()

    @Test fun preferenceLoadingAndRecompositionDoNotProduceDuplicateEvents() {
        val session = TreasureHapticSessionViewModel(events::add)
        val preference = mutableStateOf<Boolean?>(null)
        rule.setContent { TreasureHapticSessionBinding(session, preference.value) }
        rule.runOnIdle { session.controller.nearby("a", 20.0); assertTrue(events.isEmpty()); preference.value = true }
        rule.runOnIdle { session.controller.nearby("a", 20.0); preference.value = false }
        rule.runOnIdle { session.controller.nearby("b", 20.0); preference.value = true }
        rule.runOnIdle {
            session.controller.nearby("a", 20.0)
            assertEquals(listOf(TreasureHapticEvent.Nearby("a")), events)
        }
    }

    @Test fun actualActivityPauseAndResumeSuppressBackgroundSuccess() {
        val session = TreasureHapticSessionViewModel(events::add)
        rule.setContent { TreasureHapticSessionBinding(session, true) }
        rule.waitForIdle()
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertFalse(session.controller.foreground)
        session.controller.discoverySaved("background", null)
        rule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        rule.runOnIdle {
            session.controller.discoverySaved("background", null)
            session.controller.discoverySaved("foreground", null)
            assertEquals(listOf(TreasureHapticEvent.Unlocked("foreground")), events)
        }
    }

    @Test fun activityRecreationRetainsControllerAndInFlightAttempt() {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TreasureHapticSessionViewModel(events::add) as T
        }
        lateinit var session: TreasureHapticSessionViewModel
        rule.activityRule.scenario.onActivity {
            session = ViewModelProvider(it, factory).get("haptic-test", TreasureHapticSessionViewModel::class.java)
        }
        rule.setContent { TreasureHapticSessionBinding(session, true) }
        lateinit var attempt: TreasureHapticAttempt
        rule.runOnIdle { session.controller.nearby("a", 20.0); attempt = session.controller.beginAttempt("b") }
        val original = session.controller
        rule.activityRule.scenario.recreate()
        rule.activityRule.scenario.onActivity { activity ->
            val retained = ViewModelProvider(activity, factory).get("haptic-test", TreasureHapticSessionViewModel::class.java)
            assertSame(session, retained)
            activity.setContent { TreasureHapticSessionBinding(retained, true) }
        }
        rule.runOnIdle {
            assertSame(original, session.controller)
            session.controller.nearby("a", 20.0)
            session.controller.completeAttempt(attempt, null)
            assertEquals(listOf(TreasureHapticEvent.Nearby("a"), TreasureHapticEvent.Unlocked("b")), events)
        }
    }

    @Test fun leavingSignedInCompositionClosesOldCallbacksAndReentryStartsFresh() {
        val session = TreasureHapticSessionViewModel(events::add)
        val signedIn = mutableStateOf(true)
        rule.setContent { if (signedIn.value) TreasureHapticSessionBinding(session, true) }
        lateinit var old: TreasureHapticController
        lateinit var pending: TreasureHapticAttempt
        rule.runOnIdle { old = session.controller; pending = old.beginAttempt("a"); signedIn.value = false }
        rule.runOnIdle { assertTrue(old.ended); signedIn.value = true }
        rule.runOnIdle {
            assertNotSame(old, session.controller)
            old.completeAttempt(pending, null)
            assertTrue(events.isEmpty())
            session.controller.nearby("a", 20.0)
            assertEquals(listOf(TreasureHapticEvent.Nearby("a")), events)
        }
    }
}
