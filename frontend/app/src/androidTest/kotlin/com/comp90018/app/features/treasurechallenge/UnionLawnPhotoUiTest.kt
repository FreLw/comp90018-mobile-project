package com.comp90018.app.features.treasurechallenge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.comp90018.app.RelicColorScheme
import com.comp90018.app.RelicTypography
import com.comp90018.app.contextengine.challenge.*
import com.comp90018.app.features.map.*
import com.comp90018.app.sensors.location.GeoCoordinate
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runners.MethodSorters
import java.io.File

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class UnionLawnPhotoUiTest {
    @get:Rule val rule = createComposeRule()
    private val config = RelicChallengeConfigs.unionLawnPhoto("union-camera", GeoCoordinate(-37.8, 145.0), 20.0, 0.0)

    @Test
    fun a_eachHalfLightsAndRipplesIndependentlyThenThePromptPushesTheGuideDown() {
        val current = mutableStateOf(state(distance = 100.0, heading = 40.0))
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    TreasureChallengeScreen(current.value, onBack = {}, onPhotoCaptureStarted = {},
                        onPhotoCaptured = { error("Unexpected capture") }, onCameraError = {})
                }
            }
        }
        settle()
        rule.onNodeWithTag("union_camera_trigger").assertIsNotEnabled()
        rule.onNodeWithTag("union_camera_preview").assertDoesNotExist()
        rule.onNodeWithTag("union_photo_prompt").assertDoesNotExist()
        val guideTop = rule.onNodeWithTag("how_to_play").fetchSemanticsNode().boundsInRoot.top
        assertEngraving("Distance semicircle: unlit.", "Heading semicircle: unlit.")
        assertEngraving("Camera gold water: 0%.")
        val distanceLabel = rule.onNodeWithText("DISTANCE").fetchSemanticsNode().boundsInRoot
        val headingLabel = rule.onNodeWithText("DIRECTION TO ALIGN").fetchSemanticsNode().boundsInRoot
        assertEquals(distanceLabel.top, headingLabel.top, 1f)
        assertTrue(distanceLabel.right < headingLabel.left)
        screenshot("union_two_halves_unlit")

        rule.runOnIdle { current.value = state(distance = 60.0, heading = 40.0) }
        settle()
        assertEngraving("Camera gold water: 50%.", "Distance semicircle: unlit.")
        screenshot("union_distance_gold_half")

        rule.runOnIdle { current.value = state(distance = 5.0, heading = 40.0) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(500)
        assertEngraving("Distance semicircle: lit.", "Heading semicircle: unlit.", "Distance ripple: expanding.",
            "Camera gold water: 100%.")
        rule.onNodeWithTag("union_photo_prompt").assertDoesNotExist()
        screenshot("union_distance_ripple")
        settle()
        assertEngraving("Distance ripple: settled.")

        rule.runOnIdle { current.value = state(distance = 5.0, heading = 0.0) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(500)
        assertEngraving("Distance semicircle: lit.", "Heading semicircle: lit.",
            "Distance ripple: settled.", "Heading ripple: expanding.")
        rule.onNodeWithTag("union_camera_trigger").assertIsEnabled()
        rule.onNodeWithText("Tap the camera to take a photograph.").assertIsDisplayed()
        val prompt = rule.onNodeWithTag("union_photo_prompt").fetchSemanticsNode().boundsInRoot
        val guide = rule.onNodeWithTag("how_to_play").fetchSemanticsNode().boundsInRoot
        assertTrue(prompt.bottom <= guide.top)
        assertTrue(guide.top > guideTop + 20f)
        // No extra arc is tied to the unlit Photograph star, and no preview opens without a tap.
        rule.onNodeWithContentDescription("Photograph condition").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Star unlit"))
        rule.onNodeWithTag("union_camera_preview").assertDoesNotExist()
        screenshot("union_both_halves_ready")

        rule.runOnIdle { current.value = state(distance = 100.0, heading = 0.0) }
        settle()
        rule.onNodeWithTag("union_camera_trigger").assertIsNotEnabled()
        rule.onNodeWithTag("union_photo_prompt").assertDoesNotExist()
        rule.runOnIdle { current.value = state(distance = 5.0, heading = 0.0) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(400)
        assertEngraving("Distance ripple: expanding.", "Heading ripple: settled.")
    }

    @Test
    fun b_permissionDenialCanBeRetriedAndARealPhotographRevealsTheTreasure() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertEquals("Run this flow with camera permission initially revoked", PackageManager.PERMISSION_DENIED,
            ContextCompat.checkSelfPermission(instrumentation.targetContext, Manifest.permission.CAMERA))
        val relic = MapRelic("union", "Lost Lake Photograph", "Union Lawn", coordinate = config.targetLocation,
            artworkKey = "treasure_postcard")
        val coordinator = PostChallengeRevealCoordinator()
        var photoUri: String? = null
        var saves = 0
        var feedback = 0
        var pendingSave: ((String?) -> Unit)? = null
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    val session = coordinator.session
                    if (session != null) PostChallengeRevealScreen(session, coordinator::clear)
                    else TreasureChallengeRoute(config, "union", 100.0, debugSimulationEnabled = true,
                        onChallengeCompleted = { saves++; pendingSave = it },
                        onChallengeSatisfied = { feedback++ },
                        onDiscoveryReady = { photo, save ->
                            photoUri = photo
                            coordinator.openOnCompletion(relic, photo, save)
                        }, onBack = {})
                }
            }
        }
        settle()
        rule.onNodeWithContentDescription("Distance · m")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(5f) }
        settle()
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("union_photo_prompt").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("union_camera_trigger").performScrollTo()
        val slot = rule.onNodeWithTag("union_camera_trigger").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("union_camera_trigger").performClick()
        clickSystemPermission("permission_deny_button")
        settle()
        rule.onNodeWithTag("union_viewfinder").assertDoesNotExist()
        rule.onNodeWithText("Allow camera access to take your photograph. Tap the camera to try again.")
            .performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("union_camera_trigger").performScrollTo().performClick()
        clickSystemPermission("permission_allow_foreground_only_button", "permission_allow_button")
        settle()
        rule.onNodeWithTag("union_camera_trigger").assertDoesNotExist()
        rule.onNodeWithTag("quest_emblem").assertDoesNotExist()
        rule.onNodeWithTag("union_viewfinder").performScrollTo().assertIsDisplayed()
        val viewfinder = rule.onNodeWithTag("union_viewfinder").fetchSemanticsNode().boundsInRoot
        assertEquals(slot.width, viewfinder.width, 1f)
        assertEquals(slot.height, viewfinder.height, 1f)
        rule.onAllNodesWithTag("union_camera_preview").assertCountEquals(1)
        val shutter = rule.onNodeWithTag("union_photo_shutter").fetchSemanticsNode().boundsInRoot
        val frame = rule.onNodeWithTag("union_photo_frame").fetchSemanticsNode().boundsInRoot
        assertTrue(shutter.top >= frame.bottom)
        assertTrue(shutter.top >= viewfinder.bottom)
        waitForCamera()
        screenshot("union_camera_in_emblem")
        rule.onNodeWithTag("union_photo_shutter").performScrollTo().performClick()
        rule.waitUntil(15000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("union_photo_morph").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("treasure_discovery_page").assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, saves); assertEquals(1, feedback) }
        rule.mainClock.advanceTimeBy(700)
        rule.onNodeWithTag("union_photo_morph").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Photograph blending into the lake postcard"))
        screenshot("union_photo_blending")
        rule.mainClock.advanceTimeBy(900)
        rule.onNodeWithTag("treasure_discovery_page").assertDoesNotExist()
        screenshot("union_photo_becomes_postcard")
        settle()
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("treasure_discovery_page").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("treasure_discovery_page").assertIsDisplayed()
        rule.onNodeWithText("Saving to your collection…").assertIsDisplayed()
        rule.runOnIdle {
            assertTrue(isCapturedPhotoUri(requireNotNull(photoUri)))
            instrumentation.targetContext.contentResolver.openInputStream(android.net.Uri.parse(photoUri)).use {
                assertNotNull(it)
                assertTrue(requireNotNull(it).read() >= 0)
            }
            assertEquals(1, saves)
            assertEquals(1, feedback)
            requireNotNull(pendingSave)(null)
        }
        screenshot("union_photo_discovery")
        rule.runOnIdle { coordinator.clear() }
        settle()
        rule.onNodeWithTag("treasure_discovery_page").assertIsDisplayed()
        rule.runOnIdle { assertEquals(1, saves); assertEquals(1, feedback) }
    }

    @Test
    fun c_existingCameraPermissionStillRequiresATapAndTheShutterTracksReadiness() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.CAMERA)
        val current = mutableStateOf(state(distance = 5.0, heading = 0.0))
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    TreasureChallengeScreen(current.value, onBack = {}, onPhotoCaptureStarted = {},
                        onPhotoCaptured = {}, onCameraError = {})
                }
            }
        }
        settle()
        rule.onNodeWithTag("union_camera_trigger").assertIsDisplayed()
        rule.onNodeWithTag("union_viewfinder").assertDoesNotExist()
        rule.onNodeWithTag("union_camera_trigger").performClick()
        settle()
        rule.onNodeWithTag("union_viewfinder").assertIsDisplayed()
        waitForCamera()
        rule.onNodeWithTag("union_photo_shutter").assertIsEnabled()
        screenshot("union_sepia_frame_external_shutter")
        rule.runOnIdle { current.value = state(distance = 5.0, heading = 40.0) }
        settle()
        rule.onNodeWithTag("union_viewfinder").assertIsDisplayed()
        rule.onNodeWithTag("union_photo_shutter").assertIsNotEnabled()
        rule.onNodeWithText("Align distance and heading again to take your photograph.").assertIsDisplayed()
        rule.runOnIdle { current.value = state(distance = 5.0, heading = 0.0) }
        settle()
        rule.onNodeWithTag("union_photo_shutter").assertIsEnabled()
    }

    private fun state(distance: Double, heading: Double): TreasureChallengeUiState {
        val controls = DebugContextControls(distanceMeters = distance, headingDegrees = heading)
        val evaluator = ChallengeRuleEvaluator(config.copy(holdDurationNanos = 0))
        // Arrival requires two distinct accurate GPS fixes, just as it does on a real device.
        evaluator.evaluate(controls.snapshot(config, 1L), 0L)
        val snapshot = controls.snapshot(config, 2L)
        val progress = evaluator.evaluate(snapshot, 0L)
        return TreasureChallengeUiState(config.challengeId, config.type, config.type.questTitle(),
            instruction = progress.instruction, instructionText = progress.instruction.displayText(config.type),
            actionReady = progress.actionReady, conditionStates = progress.requiredConditions,
            cameraState = if (progress.actionReady) ChallengeCameraState.READY else ChallengeCameraState.LOCKED,
            latestSnapshot = snapshot, angularErrorDegrees = snapshot.orientation.direction.angularErrorDegrees,
            requiredHeadingDegrees = 0.0, insideRadiusMeters = 20.0)
    }

    private fun assertEngraving(vararg descriptions: String) {
        val actual = rule.onNodeWithTag("quest_emblem", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsProperties.StateDescription]
        descriptions.forEach { assertTrue("Expected '$it' in '$actual'", actual.contains(it)) }
    }

    private fun settle() {
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(2200)
        rule.waitForIdle()
    }

    private fun waitForCamera() {
        rule.waitUntil(10000) {
            rule.mainClock.advanceTimeByFrame()
            !rule.onNodeWithTag("union_photo_shutter").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
    }

    private fun clickSystemPermission(vararg buttons: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        var button: AccessibilityNodeInfo? = null
        rule.waitUntil(7000) {
            button = buttons.firstNotNullOfOrNull { id ->
                automation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId("com.android.permissioncontroller:id/$id")?.firstOrNull()
            }
            button != null
        }
        assertTrue(requireNotNull(button).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        rule.waitUntil(7000) {
            automation.rootInActiveWindow?.packageName != "com.android.permissioncontroller"
        }
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        rule.waitForIdle()
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach {
                it.window.decorView.invalidate()
            }
        }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(160)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "quest-preview").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
