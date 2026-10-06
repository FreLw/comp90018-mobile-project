package com.comp90018.app.features.treasurechallenge

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import com.comp90018.app.contextengine.challenge.*
import org.junit.Rule
import org.junit.Test

class TreasureChallengeUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun gardenShowsGpsWithoutExcavationOrMicrophoneControls() {
        rule.setContent {
            MaterialTheme {
                TreasureChallengeScreen(
                    state = TreasureChallengeUiState("garden", RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE,
                        "System Garden Glasshouse", cameraState = ChallengeCameraState.LOCKED,
                        conditionStates = listOf(ChallengeConditionState(ChallengeCondition.LOCATION_INSIDE, false))),
                    onBack = {}, onPhotoCaptureStarted = {}, onPhotoCaptured = { error("Unexpected capture") },
                    onCameraError = {},
                )
            }
        }
        rule.onNodeWithText("○ Valid GPS within this treasure's radius").assertIsDisplayed()
        rule.onNodeWithText(RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE.taskInstructions()).assertIsDisplayed()
        rule.onNodeWithText("Enable microphone", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Take photo", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Phone horizontal", substring = true).assertDoesNotExist()
    }

    @Test fun audioShowsSoundConditionWithoutCameraOrCompassControls() {
        rule.setContent {
            MaterialTheme {
                TreasureChallengeScreen(
                    state = TreasureChallengeUiState("tone", RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL,
                        "Grainger Museum Tone-Tool",
                        conditionStates = listOf(ChallengeConditionState(ChallengeCondition.SOUND_DETECTED, false))),
                    onBack = {}, onPhotoCaptureStarted = {}, onPhotoCaptured = { error("Unexpected capture") },
                    onCameraError = {},
                )
            }
        }
        rule.onNodeWithText("○ Sound above threshold").assertIsDisplayed()
        rule.onNodeWithText(RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL.taskInstructions()).assertIsDisplayed()
        rule.onNodeWithText("Take photo", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Compass direction aligned", substring = true).assertDoesNotExist()
    }
}
