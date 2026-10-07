package com.comp90018.app.features.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RelicNavigationPanelsUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun targetAndGuidanceHaveSeparateAccessiblePanelsAndStopAction() {
        var stops = 0
        val state = RelicNavigationUiState(resonanceStage = RelicResonanceStage.DRAWN,
            resonanceProgress = 0.5f, distanceMeters = 80.0,
            directionHint = NavigationDirectionHint.TURN_RIGHT, headingErrorDegrees = 180.0,
            locationReadiness = NavigationLocationReadiness.READY)
        rule.setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    RelicNavigationTargetPanel("Archive Specimen", "Old Quad", state.distanceMeters, { stops++ })
                    RelicNavigationResonancePanel(state, relicArrivalPresentation(state, true), 0f, 1f, {})
                }
            }
        }
        val target = hasAnyAncestor(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Navigation target"))
        val guidance = hasAnyAncestor(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Relic resonance guidance"))
        listOf("Archive Specimen", "Old Quad", "80 m", "Stop").forEach { text ->
            rule.onNode(hasText(text) and target).assertExists()
            rule.onNode(hasText(text) and guidance).assertDoesNotExist()
        }
        listOf("Relic Resonance", "Drawn closer", "Turn right · 180°").forEach { text ->
            rule.onNode(hasText(text) and guidance).assertExists()
            rule.onNode(hasText(text) and target).assertDoesNotExist()
        }
        rule.onNodeWithText("Stop").performClick()
        rule.runOnIdle { assertEquals(1, stops) }
    }

    @Test
    fun confirmingAndIncompleteTransitionCannotShowBeginHunt() {
        var state by mutableStateOf(RelicNavigationUiState(
            resonanceStage = RelicResonanceStage.CONFIRMING, resonanceProgress = 1f,
            locationReadiness = NavigationLocationReadiness.READY))
        var transitionComplete by mutableStateOf(false)
        var hunts = 0
        rule.setContent {
            MaterialTheme {
                RelicNavigationResonancePanel(state, relicArrivalPresentation(state, transitionComplete),
                    1f, 1f, { hunts++ })
            }
        }
        rule.onNodeWithText("Confirming the resonance…").assertExists()
        rule.onNodeWithText("Begin Hunt").assertDoesNotExist()
        rule.runOnIdle { transitionComplete = true }
        rule.onNodeWithText("Begin Hunt").assertDoesNotExist()
        rule.runOnIdle {
            transitionComplete = false
            state = state.copy(resonanceStage = RelicResonanceStage.ARRIVED,
                arrivalConfirmed = true, canBeginHunt = true)
        }
        rule.onNodeWithText("Begin Hunt").assertDoesNotExist()
        rule.runOnIdle { transitionComplete = true }
        rule.onNodeWithText("Begin Hunt").performClick()
        rule.runOnIdle { assertEquals(1, hunts) }
    }
}
