package com.comp90018.app.features.map.route

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Protects the original remember-key boundaries while hunt pages live in separate files. */
class MapNavigationStateUiTest {
    @get:Rule val rule = createComposeRule()
    private val relic = MapRelic("atlas", "Atlas", "South Lawn", coordinate = GeoCoordinate(-37.8, 145.0))

    @Test fun changingHuntSessionResetsRoutesButPreservesAssemblyAndDebugPreview() {
        val session = mutableStateOf("first")
        lateinit var current: MapNavigationState
        rule.setContent { current = rememberMapNavigationState(session.value, "explorer") }
        rule.runOnIdle {
            current.selectedRelic.value = relic
            current.detailRelic.value = relic
            current.challengeRelic.value = relic
            current.compassRelic.value = relic
            current.huntRevealRelic.value = relic
            current.memberQuizRelic.value = relic
            current.huntStoryVisible.value = true
            current.atlasAssemblyRelic.value = relic
            current.debugSimulationEnabled.value = true
            current.debugTaskRelic.value = relic
            current.debugTaskSession.value = 4
            session.value = "second"
        }
        rule.runOnIdle {
            assertNull(current.selectedRelic.value)
            assertNull(current.detailRelic.value)
            assertNull(current.challengeRelic.value)
            assertNull(current.compassRelic.value)
            assertNull(current.huntRevealRelic.value)
            assertNull(current.memberQuizRelic.value)
            assertFalse(current.huntStoryVisible.value)
            assertEquals(relic, current.atlasAssemblyRelic.value)
            assertEquals(relic, current.debugTaskRelic.value)
            assertEquals(4, current.debugTaskSession.value)
            assertTrue(current.debugSimulationEnabled.value)
        }
    }

    @Test fun changingExplorerOnlyResetsExplorerScopedAssembly() {
        val explorer = mutableStateOf("first")
        lateinit var current: MapNavigationState
        rule.setContent { current = rememberMapNavigationState("hunt", explorer.value) }
        rule.runOnIdle {
            current.atlasAssemblyRelic.value = relic
            current.detailRelic.value = relic
            current.debugTaskRelic.value = relic
            explorer.value = "second"
        }
        rule.runOnIdle {
            assertNull(current.atlasAssemblyRelic.value)
            assertEquals(relic, current.detailRelic.value)
            assertEquals(relic, current.debugTaskRelic.value)
        }
    }
}
