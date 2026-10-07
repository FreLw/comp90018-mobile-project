package com.comp90018.app.features.map

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationOutput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TreasureCompassGateUiTest {
    @get:Rule val rule = createComposeRule()
    private val relic = MapRelic("wilson_hall_rosette", "Stone Rosette", "Wilson Hall",
        coordinate = GeoCoordinate(-37.798, 144.96))

    @Test fun distanceAndDirectionAreRequiredBeforeOpeningTheTask() {
        var location by mutableStateOf(LocationOutput(
            distanceToTargetMeters = 10.01, targetBearingDegrees = 0.0, validity = SensorValidity.VALID))
        var heading by mutableStateOf(0f)
        var enteredTask by mutableStateOf(false)
        var transitions = 0
        rule.setContent {
            MaterialTheme {
                if (enteredTask) Text("Task challenge")
                else TreasureCompassGate(relic, location, heading,
                    soundEffectsEnabled = false,
                    onContinue = { transitions++; enteredTask = true }, onBack = {})
            }
        }
        rule.onNodeWithText("Hunt", substring = true).assertDoesNotExist()
        rule.runOnIdle { location = location.copy(distanceToTargetMeters = 5.0); heading = 15.01f }
        rule.onNodeWithText("Hunt", substring = true).assertDoesNotExist()
        rule.onNodeWithText("The Balance").assertDoesNotExist()
        rule.onNodeWithText("LIBELLA").assertDoesNotExist()
        rule.onNodeWithText("LEVEL", substring = true).assertDoesNotExist()
        rule.runOnIdle { assertEquals(0, transitions); heading = 0f }
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Hunt", substring = true).fetchSemanticsNodes().size == 1
        }
        rule.onNodeWithText("Hunt", substring = true).performScrollTo().performClick()
        rule.waitUntil(5_000) { enteredTask }
        rule.onNodeWithText("Task challenge").assertExists()
        rule.runOnIdle { assertEquals(1, transitions) }
    }

    @Test fun normalDebugBuildStillRequiresValidGpsAndBackingOutDoesNotStartTheTask() {
        var backCount = 0
        var transitions = 0
        rule.setContent {
            MaterialTheme {
                TreasureCompassGate(relic,
                    LocationOutput(distanceToTargetMeters = 5.0, targetBearingDegrees = 0.0,
                        validity = SensorValidity.UNKNOWN),
                    0f, soundEffectsEnabled = false,
                    onContinue = { transitions++ }, onBack = { backCount++ })
            }
        }
        rule.onNodeWithText("Hunt", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Waiting for a reliable location signal", substring = true).assertExists()
        // Navigate back without completing any task or saving a discovery.
        rule.onNodeWithContentDescription("Back to map").performClick()
        rule.onNodeWithText("Task challenge").assertDoesNotExist()
        rule.runOnIdle { assertEquals(0, transitions); assertEquals(1, backCount) }
    }
}
