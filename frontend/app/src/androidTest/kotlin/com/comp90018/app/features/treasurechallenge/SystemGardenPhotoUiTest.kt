package com.comp90018.app.features.treasurechallenge

/*
 * Checks glasshouse camera readiness, the external shutter, upright photo-to-logo morph, and discovery handoff.
 * Run these device/Compose checks when changing the corresponding interface or interaction contract.
 */

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
import androidx.compose.ui.geometry.Rect
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
class SystemGardenPhotoUiTest {
    @get:Rule val rule = createComposeRule()
    private val config = RelicChallengeConfigs.systemGardenGlasshouse("garden-camera", GeoCoordinate(-37.8, 145.0), 20.0)

    @Test
    fun a_distanceAloneUnlocksTheCentralCameraAndPushesTheGuideDown() {
        val current = mutableStateOf(state(distance = 100.0, heading = 180.0))
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
        rule.onNodeWithTag("garden_camera_trigger").assertIsNotEnabled()
        rule.onNodeWithTag("garden_camera_preview").assertDoesNotExist()
        rule.onNodeWithTag("garden_photo_prompt").assertDoesNotExist()
        rule.onNodeWithTag("garden_photo_shutter").assertDoesNotExist()
        val guideTop = rule.onNodeWithTag("how_to_play").fetchSemanticsNode().boundsInRoot.top
        rule.runOnIdle { current.value = state(distance = 5.0, heading = 180.0) }
        settle()
        rule.onNodeWithTag("garden_camera_trigger").assertIsEnabled()
        rule.onNodeWithText("Tap the glasshouse to take a photograph.").assertIsDisplayed()
        rule.onNodeWithTag("garden_camera_preview").assertDoesNotExist()
        rule.onNodeWithText("DIRECTION TO ALIGN").assertDoesNotExist()
        val prompt = rule.onNodeWithTag("garden_photo_prompt").fetchSemanticsNode().boundsInRoot
        val guide = rule.onNodeWithTag("how_to_play").fetchSemanticsNode().boundsInRoot
        assertTrue(prompt.bottom <= guide.top)
        assertTrue(guide.top > guideTop + 20f)
        rule.onNodeWithContentDescription("Photograph condition").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Star unlit"))
        screenshot("garden_distance_ready")
    }

    @Test
    fun b_permissionDenialCanBeRetriedAndARealPhotographRevealsTheTreasure() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertEquals("Run this flow with camera permission initially revoked", PackageManager.PERMISSION_DENIED,
            ContextCompat.checkSelfPermission(instrumentation.targetContext, Manifest.permission.CAMERA))
        val relic = MapRelic("garden", "The Lost Glasshouse", "System Garden", coordinate = config.targetLocation,
            artworkKey = "treasure_glasshouse")
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
                    else TreasureChallengeRoute(config, "garden", 100.0, debugSimulationEnabled = true,
                        revealRelic = relic,
                        onChallengeCompleted = { saves++; pendingSave = it },
                        onChallengeSatisfied = { feedback++ },
                        onDiscoveryReady = { photo, save, arrival ->
                            photoUri = photo
                            coordinator.openOnCompletion(relic, photo, save, arrival)
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
            rule.onAllNodesWithTag("garden_photo_prompt").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("garden_camera_trigger").performScrollTo()
        val slot = rule.onNodeWithTag("garden_camera_trigger").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("garden_camera_trigger").performClick()
        clickSystemPermission("permission_deny_button")
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithText("Allow camera access to take your photograph. Tap the glasshouse to try again.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        settle()
        rule.onNodeWithTag("garden_viewfinder").assertDoesNotExist()
        rule.onNodeWithText("Allow camera access to take your photograph. Tap the glasshouse to try again.")
            .performScrollTo().assertIsDisplayed()
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            !rule.onNodeWithTag("garden_camera_trigger").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
        rule.onNodeWithTag("garden_camera_trigger").performScrollTo().performClick()
        clickSystemPermission("permission_allow_foreground_only_button", "permission_allow_button")
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("garden_viewfinder").fetchSemanticsNodes().isNotEmpty()
        }
        settle()
        rule.onNodeWithTag("garden_camera_trigger").assertDoesNotExist()
        rule.onNodeWithTag("quest_emblem").assertDoesNotExist()
        rule.onNodeWithTag("garden_viewfinder").performScrollTo().assertIsDisplayed()
        val viewfinder = rule.onNodeWithTag("garden_viewfinder").fetchSemanticsNode().boundsInRoot
        assertEquals(slot.width, viewfinder.width, 1f)
        assertEquals(slot.height, viewfinder.height, 1f)
        rule.onAllNodesWithTag("garden_camera_preview").assertCountEquals(1)
        val shutter = rule.onNodeWithTag("garden_photo_shutter").fetchSemanticsNode().boundsInRoot
        val frame = rule.onNodeWithTag("garden_photo_frame").fetchSemanticsNode().boundsInRoot
        assertTrue(shutter.top >= frame.bottom)
        assertTrue(shutter.top >= viewfinder.bottom)
        waitForCamera()
        screenshot("garden_camera_in_emblem")
        rule.onNodeWithTag("garden_photo_shutter").performScrollTo().performClick()
        rule.waitUntil(15000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("garden_photo_morph").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("treasure_discovery_page").assertDoesNotExist()
        rule.runOnIdle { assertEquals(1, saves); assertEquals(1, feedback) }
        rule.mainClock.advanceTimeBy(700)
        rule.onNodeWithTag("garden_photo_morph").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Photograph blending into the glasshouse"))
        screenshot("garden_photo_blending")
        rule.mainClock.advanceTimeBy(900)
        rule.onNodeWithTag("treasure_discovery_page").assertDoesNotExist()
        screenshot("garden_photo_becomes_glasshouse")
        val logoBounds = rule.onNodeWithTag("garden_photo_morph").fetchSemanticsNode().boundsInRoot
        val oldTitle = rule.onNodeWithText(config.type.questTitle()).fetchSemanticsNode().boundsInRoot
        val oldDistance = rule.onNodeWithText("DISTANCE TO SITE").fetchSemanticsNode().boundsInRoot
        rule.mainClock.advanceTimeBy(650)
        rule.onNodeWithTag("photo_reveal_transition").assertExists()
        assertLogoStaysAt(logoBounds)
        rule.mainClock.advanceTimeBy(350)
        assertLogoStaysAt(logoBounds)
        assertTrue(rule.onNodeWithText(config.type.questTitle()).fetchSemanticsNode().boundsInRoot.top < oldTitle.top)
        assertTrue(rule.onNodeWithText("DISTANCE TO SITE").fetchSemanticsNode().boundsInRoot.top > oldDistance.top)
        screenshot("garden_reveal_background_warming")
        rule.mainClock.advanceTimeBy(600)
        assertLogoStaysAt(logoBounds)
        rule.onNodeWithTag("photo_reveal_transition").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Discovery lettering appearing"))
        screenshot("garden_reveal_letters_appearing")
        rule.mainClock.advanceTimeBy(900)
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("photo_reveal_transition").fetchSemanticsNodes().isEmpty() &&
                rule.onAllNodesWithTag("treasure_discovery_page").fetchSemanticsNodes().isNotEmpty()
        }
        assertLogoStaysAt(logoBounds)
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
        screenshot("garden_photo_discovery")
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
        rule.onNodeWithTag("garden_camera_trigger").assertIsDisplayed()
        rule.onNodeWithTag("garden_viewfinder").assertDoesNotExist()
        rule.onNodeWithTag("garden_camera_trigger").performClick()
        settle()
        rule.onNodeWithTag("garden_viewfinder").assertIsDisplayed()
        waitForCamera()
        rule.onNodeWithTag("garden_photo_shutter").assertIsEnabled()
        screenshot("garden_sepia_frame_external_shutter")
        rule.runOnIdle { current.value = state(distance = 100.0, heading = 40.0) }
        settle()
        rule.onNodeWithTag("garden_viewfinder").assertIsDisplayed()
        rule.onNodeWithTag("garden_photo_shutter").assertIsNotEnabled()
        rule.onNodeWithText("Return to the glasshouse site to take your photograph.").assertIsDisplayed()
        rule.runOnIdle { current.value = state(distance = 5.0, heading = 0.0) }
        settle()
        rule.onNodeWithTag("garden_photo_shutter").assertIsEnabled()
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
            requiredHeadingDegrees = null, insideRadiusMeters = 20.0)
    }

    private fun assertLogoStaysAt(expected: Rect) {
        val actual = rule.onNodeWithTag("photo_reveal_logo").fetchSemanticsNode().boundsInRoot
        assertEquals("Logo left", expected.left, actual.left, 1f)
        assertEquals("Logo top", expected.top, actual.top, 1f)
        assertEquals("Logo width", expected.width, actual.width, 1f)
        assertEquals("Logo height", expected.height, actual.height, 1f)
    }

    private fun settle() {
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(2200)
        rule.waitForIdle()
    }

    private fun waitForCamera() {
        rule.waitUntil(10000) {
            rule.mainClock.advanceTimeByFrame()
            !rule.onNodeWithTag("garden_photo_shutter").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
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
