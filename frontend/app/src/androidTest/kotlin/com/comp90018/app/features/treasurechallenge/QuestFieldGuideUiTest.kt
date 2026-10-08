package com.comp90018.app.features.treasurechallenge

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.comp90018.app.RelicColorScheme
import com.comp90018.app.RelicTypography
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.debugChallengeRelics
import com.comp90018.app.features.map.PostChallengeRevealCoordinator
import com.comp90018.app.features.map.PostChallengeRevealScreen
import com.comp90018.app.features.haptics.*
import com.comp90018.app.contextengine.challenge.*
import com.comp90018.app.sensors.location.GeoCoordinate
import java.io.File
import org.junit.Rule
import org.junit.Test

class QuestFieldGuideUiTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun allSixGuidesUnfurlInlineAndKeepTheirOwnInstructions() {
        val current = mutableStateOf(previewState(configs().first()))
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
                Surface(color = QuestForest) {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        TreasureChallengeScreen(
                            state = current.value, onBack = {}, onPhotoCaptureStarted = {},
                            onPhotoCaptured = {}, onCameraError = {},
                        )
                    }
                }
            }
        }
        configs().forEach { config ->
            rule.runOnIdle { current.value = previewState(config) }
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
            rule.mainClock.advanceTimeBy(2000)
            rule.onNodeWithText(config.type.questTitle()).assertIsDisplayed()
            screenshot(config.type.name.lowercase())
            rule.onNodeWithText("How to play").performScrollTo().performClick()
            // The in-page scroll also animates its parent ScrollState into reading position.
            rule.mainClock.autoAdvance = true
            rule.mainClock.advanceTimeBy(1000)
            rule.onNodeWithTag("field_guide_book").assertDoesNotExist()
            rule.onAllNodes(isDialog()).assertCountEquals(0)
            rule.onNodeWithTag("field_guide_scroll").performScrollTo().assertIsDisplayed()
            rule.onNodeWithText(config.type.taskInstructions()).assertIsDisplayed()
            screenshot(config.type.name.lowercase() + "_guide")
            rule.onNodeWithContentDescription("Close field guide").performScrollTo().performClick()
            rule.mainClock.advanceTimeBy(500)
            rule.onNodeWithTag("field_guide_scroll").assertDoesNotExist()
            rule.mainClock.autoAdvance = false
        }
    }

    @Test
    fun southLawnUsesOneRowOfFourStarsAndAnInlineScrollWithoutAHoldBar() {
        val config = configs()[3]
        val calm = DebugContextControls(distanceMeters = 5.0, headingDegrees = 0.0,
            motionMagnitude = .05, stabilityVariation = .03, rotationRadiansPerSecond = .05)
        val current = mutableStateOf(previewState(config, calm.copy(distanceMeters = 100.0)))
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
                Surface(color = QuestForest) {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        TreasureChallengeScreen(current.value, onBack = {}, onPhotoCaptureStarted = {},
                            onPhotoCaptured = {}, onCameraError = {})
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(2000)
        rule.onNodeWithTag("quest_hold_meter").assertDoesNotExist()
        rule.onAllNodesWithTag("quest_condition_row").assertCountEquals(1)
        val labels = listOf("Near Atlas", "Aligned", "Stillness", "No turning")
        val stars = labels.map { rule.onNodeWithContentDescription(it + " condition").fetchSemanticsNode() }
        org.junit.Assert.assertTrue(stars.all { kotlin.math.abs(it.boundsInRoot.top - stars.first().boundsInRoot.top) < 1f })
        screenshot("south_lawn_distance_far")
        listOf(
            "distance_half" to calm.copy(distanceMeters = 60.0),
            "moving" to calm.copy(motionMagnitude = 1.2, stabilityVariation = .35),
            "turning" to calm.copy(rotationRadiansPerSecond = .65),
            "poised" to calm,
        ).forEach { (name, controls) ->
            rule.runOnIdle { current.value = previewState(config, controls) }
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeBy(1400)
            val still = rule.onNodeWithContentDescription("Stillness condition").fetchSemanticsNode()
            val turn = rule.onNodeWithContentDescription("No turning condition").fetchSemanticsNode()
            org.junit.Assert.assertEquals(if (name == "moving") "Star unlit" else "Star lit",
                still.config[SemanticsProperties.StateDescription])
            org.junit.Assert.assertEquals(if (name == "turning") "Star unlit" else "Star lit",
                turn.config[SemanticsProperties.StateDescription])
            if (name == "moving" || name == "turning") rule.onNodeWithText("Remain perfectly still").assertIsDisplayed()
            screenshot("south_lawn_" + name)
        }
        rule.onNodeWithText("How to play").performClick()
        rule.mainClock.autoAdvance = true
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag("field_guide_book").assertDoesNotExist()
        rule.onAllNodes(isDialog()).assertCountEquals(0)
        rule.onNodeWithTag("field_guide_scroll").performScrollTo().assertIsDisplayed()
        screenshot("south_lawn_inline_scroll_guide")
        rule.onNodeWithContentDescription("Close field guide").performScrollTo().performClick()
        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithTag("field_guide_scroll").assertDoesNotExist()
    }

    @Test
    fun wilsonHallUsesFourStarsDistanceLitWaterAndAnAlignmentRipple() {
        val config = configs()[1]
        val calm = DebugContextControls(distanceMeters = 100.0, headingDegrees = 28.0,
            motionMagnitude = .05, stabilityVariation = .03, rotationRadiansPerSecond = .05)
        val current = mutableStateOf(previewState(config, calm))
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
                Surface(color = QuestForest) {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        TreasureChallengeScreen(current.value, onBack = {}, onPhotoCaptureStarted = {},
                            onPhotoCaptured = {}, onCameraError = {})
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(2000)
        rule.onAllNodesWithTag("quest_condition_row").assertCountEquals(1)
        val labels = listOf("In range", "Aligned", "Stillness", "No turning")
        val stars = labels.map { rule.onNodeWithContentDescription(it + " condition").fetchSemanticsNode() }
        org.junit.Assert.assertTrue(stars.all { kotlin.math.abs(it.boundsInRoot.top - stars.first().boundsInRoot.top) < 1f })
        org.junit.Assert.assertEquals(rule.onNodeWithTag("quest_screen").fetchSemanticsNode().boundsInRoot,
            rule.onNodeWithTag("wilson_fullscreen_water").fetchSemanticsNode().boundsInRoot)
        rule.onNodeWithTag("wilson_fullscreen_water").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "0 of 10 water stars illuminated"))
        rule.onNodeWithTag("quest_emblem").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            "Turn petals: 4 of 4 lit. Stillness petals: 4 of 4 lit. Heading pointer seeking star."))
        screenshot("wilson_water_unlit")
        rule.runOnIdle { current.value = previewState(config, calm.copy(distanceMeters = 60.0)) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1500)
        rule.onNodeWithTag("wilson_fullscreen_water").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "6 of 10 water stars illuminated"))
        screenshot("wilson_water_half_lit")
        val near = calm.copy(distanceMeters = 5.0, headingDegrees = 0.0)
        rule.runOnIdle { current.value = previewState(config, near) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(650)
        rule.onNodeWithTag("wilson_heading_ripple").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Heading ripple expanding"))
        rule.onNodeWithTag("wilson_fullscreen_water").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "10 of 10 water stars illuminated"))
        rule.onNodeWithTag("quest_emblem").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            "Turn petals: 4 of 4 lit. Stillness petals: 4 of 4 lit. Heading pointer aligned with star."))
        screenshot("wilson_heading_resonance")
        rule.runOnIdle { current.value = previewState(config, near.copy(motionMagnitude = 1.2, stabilityVariation = .35)) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1300)
        rule.onNodeWithContentDescription("Stillness condition").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Star unlit"))
        rule.onNodeWithText("Remain perfectly still").assertIsDisplayed()
        rule.onNodeWithTag("quest_emblem").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            "Turn petals: 4 of 4 lit. Stillness petals: 0 of 4 lit. Heading pointer aligned with star."))
        screenshot("wilson_independent_flower_motion")
        rule.runOnIdle { current.value = previewState(config, near.copy(rotationRadiansPerSecond = .65)) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1300)
        rule.onNodeWithContentDescription("No turning condition").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Star unlit"))
        rule.onNodeWithTag("quest_emblem").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            "Turn petals: 0 of 4 lit. Stillness petals: 4 of 4 lit. Heading pointer aligned with star."))
        screenshot("wilson_flowers_turning")
        rule.runOnIdle { current.value = previewState(config, near.copy(
            motionMagnitude = 1.2, stabilityVariation = .35, rotationRadiansPerSecond = .65)) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1300)
        rule.onNodeWithTag("quest_emblem").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription,
            "Turn petals: 0 of 4 lit. Stillness petals: 0 of 4 lit. Heading pointer aligned with star."))
        screenshot("wilson_all_petals_gray")
        rule.runOnIdle { current.value = previewState(config, near) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1300)
        rule.onNodeWithTag("wilson_heading_ripple").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Heading aligned"))
        screenshot("wilson_all_four_stars")
        // Ordinary snapshot updates must not retrigger alignment; losing and regaining it must.
        rule.runOnIdle { current.value = previewState(config, near.copy(headingDegrees = 25.0)) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(300)
        rule.runOnIdle { current.value = previewState(config, near) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithTag("wilson_heading_ripple").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, "Heading ripple expanding"))
    }

    @Test
    fun testButtonOpensContinuousControlsAboveTheArtwork() {
        val coordinator = PostChallengeRevealCoordinator()
        val events = mutableListOf<TreasureHapticEvent>()
        val androidDriver = AndroidTreasureHapticDriver(InstrumentationRegistry.getInstrumentation().targetContext)
        val controller = TreasureHapticController({ event -> events += event; androidDriver.play(event) }).apply {
            enabled = true; foreground = true
        }
        var saves = 0
        var saved: ((String?) -> Unit)? = null
        val persist: ((String?) -> Unit) -> Unit = { complete ->
            TreasureHapticSave.collect("wilson", false, controller,
                { _, callback -> saves++; saved = callback }, complete)
        }
        // Live phone sensors never stop publishing; drive animation frames explicitly in this test.
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
                Surface(color = QuestForest) {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        val session = coordinator.session
                        if (session != null) PostChallengeRevealScreen(session, coordinator::clear)
                        else TreasureChallengeRoute(
                            config = configs()[1], treasureId = "wilson", radarRadiusMeters = 100.0,
                            onChallengeCompleted = persist,
                            onChallengeSatisfied = { controller.challengeCompleted("wilson", it) },
                            onDiscoveryReady = { photo, discovery ->
                                val relic = MapRelic("wilson", "Stone Rosette", "Wilson Hall",
                                    coordinate = configs()[1].targetLocation, artworkKey = "treasure_rosette")
                                coordinator.openOnCompletion(relic, photo, discovery) { discovery.retry(persist) }
                            }, onBack = {},
                        )
                    }
                }
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1200)
        val button = rule.onNodeWithText("TEST").fetchSemanticsNode().boundsInRoot
        val emblem = rule.onNodeWithTag("quest_emblem").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(button.left > emblem.right * .75f)
        rule.onNodeWithText("LIVE SENSOR TEST").assertDoesNotExist()
        rule.onNodeWithText("TEST").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1200)
        rule.onNodeWithText("LIVE SENSOR TEST").assertIsDisplayed()
        org.junit.Assert.assertTrue(
            rule.onNodeWithText("LIVE SENSOR TEST").fetchSemanticsNode().boundsInRoot.top <
                rule.onNodeWithTag("quest_emblem").fetchSemanticsNode().boundsInRoot.top,
        )
        val heading = rule.onNodeWithContentDescription("Heading · °")
        org.junit.Assert.assertEquals(0,
            heading.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].steps)
        heading.performSemanticsAction(SemanticsActions.SetProgress) { it(147.125f) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Heading · °: 147.1").assertIsDisplayed()
        val distance = rule.onNodeWithContentDescription("Distance · m")
        distance.performSemanticsAction(SemanticsActions.SetProgress) { it(12.345f) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Distance · m: 12.3").assertIsDisplayed()
        rule.onNodeWithText("TEST •").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1200)
        rule.onNodeWithText("LIVE SENSOR TEST").assertDoesNotExist()
        rule.onNodeWithText("TEST").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1200)
        rule.onNodeWithContentDescription("Heading · °")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(82.625f) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(1200)
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithText("82.6°").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Heading · °")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        rule.mainClock.advanceTimeByFrame()
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("treasure_discovery_page").fetchSemanticsNodes().isNotEmpty()
        }
        rule.mainClock.advanceTimeBy(1500)
        rule.onNodeWithTag("treasure_discovery_page").assertIsDisplayed()
        rule.onNodeWithText("Finish test").assertDoesNotExist()
        rule.onNodeWithText("Saving to your collection…").assertIsDisplayed()
        rule.runOnIdle {
            org.junit.Assert.assertEquals(1, saves)
            org.junit.Assert.assertEquals(listOf(TreasureHapticEvent.Unlocked("wilson")), events)
            requireNotNull(saved)("offline")
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Retry save").performScrollTo().performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            org.junit.Assert.assertEquals(2, saves)
            org.junit.Assert.assertEquals(1, events.size)
            requireNotNull(saved)(null)
            requireNotNull(saved)(null)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("treasure_discovery_page").assertIsDisplayed()
        rule.onNodeWithText("Saving to your collection…").assertDoesNotExist()
        rule.runOnIdle { org.junit.Assert.assertEquals(1, events.size) }
        screenshot("wilson_automatic_discovery")
    }

    @Test
    fun everySensorTaskAutomaticallyOpensItsTreasureAndVibratesOnce() {
        val sensorRelics = debugChallengeRelics().filterNot { requireNotNull(it.challengeConfig).photoActionRequired }
        val current = mutableStateOf(sensorRelics.first())
        val coordinator = PostChallengeRevealCoordinator()
        val events = mutableListOf<TreasureHapticEvent>()
        val controller = TreasureHapticController(events::add).apply { enabled = true; foreground = true }
        var saves = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            MaterialTheme(colorScheme = RelicColorScheme, typography = RelicTypography) {
                Surface(color = QuestForest) {
                    Box(Modifier.fillMaxSize().systemBarsPadding()) {
                        val relic = current.value
                        val config = requireNotNull(relic.challengeConfig)
                        val session = coordinator.session
                        if (session != null) PostChallengeRevealScreen(session, coordinator::clear)
                        else TreasureChallengeRoute(
                            config, relic.id, 100.0, debugSimulationEnabled = true,
                            onChallengeCompleted = { done -> saves++; done(null) },
                            onChallengeSatisfied = { controller.challengeCompleted(relic.id, it) },
                            onDiscoveryReady = { photo, discovery ->
                                coordinator.openOnCompletion(relic, photo, discovery)
                            }, onBack = {},
                        )
                    }
                }
            }
        }
        sensorRelics.forEachIndexed { index, relic ->
            val config = requireNotNull(relic.challengeConfig)
            rule.runOnIdle { coordinator.clear(); current.value = relic }
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeBy(1000)
            rule.onNodeWithContentDescription("Distance · m")
                .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
            rule.mainClock.advanceTimeByFrame()
            if (config.requiresSound) {
                rule.onNodeWithContentDescription("Sound · dB")
                    .performSemanticsAction(SemanticsActions.SetProgress) { it(-20f) }
                rule.mainClock.advanceTimeByFrame()
            }
            rule.waitUntil(5000) {
                rule.mainClock.advanceTimeByFrame()
                rule.onAllNodesWithTag("treasure_discovery_page").fetchSemanticsNodes().isNotEmpty()
            }
            rule.mainClock.advanceTimeBy(1500)
            rule.onNodeWithTag("treasure_discovery_page").assertIsDisplayed()
            rule.runOnIdle {
                org.junit.Assert.assertEquals(index + 1, saves)
                org.junit.Assert.assertEquals(index + 1, events.size)
                org.junit.Assert.assertEquals(TreasureHapticEvent.Unlocked(relic.id), events.last())
                org.junit.Assert.assertEquals(relic.id, coordinator.session?.relic?.id)
            }
            screenshot(config.type.name.lowercase() + "_automatic_discovery")
        }
        // A retained completed task must reopen its discovery instead of sticking on the green screen.
        rule.runOnIdle { coordinator.clear() }
        rule.mainClock.advanceTimeByFrame()
        rule.waitUntil(5000) {
            rule.mainClock.advanceTimeByFrame()
            rule.onAllNodesWithTag("treasure_discovery_page").fetchSemanticsNodes().isNotEmpty()
        }
        rule.runOnIdle {
            org.junit.Assert.assertEquals(sensorRelics.size, saves)
            org.junit.Assert.assertEquals(sensorRelics.size, events.size)
        }
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "quest-preview").apply { mkdirs() }
        rule.waitForIdle()
        // Request a complete native frame after driving animation time in large test-clock steps.
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach {
                it.window.decorView.invalidate()
            }
        }
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(160)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(directory, name + ".png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

    }

    private fun previewState(config: RelicChallengeConfig, controls: DebugContextControls = DebugContextControls(
            distanceMeters = 5.0, headingDegrees = 28.0,
            rollDegrees = if (config.requiresHorizontal) 16.0 else 8.0,
            pitchDegrees = 5.0, horizontal = !config.requiresHorizontal,
            stationary = true, stable = true, rotationStill = false,
            soundDecibels = -24.0,
        )): TreasureChallengeUiState {
        val snapshot = controls.snapshot(config, 2L)
        val evaluator = ChallengeRuleEvaluator(config)
        evaluator.evaluate(controls.snapshot(config, 1L), 0L)
        val progress = evaluator.evaluate(snapshot, 1L)
        return TreasureChallengeUiState(
            config.challengeId, config.type, config.type.questTitle(),
            instruction = progress.instruction,
            instructionText = progress.instruction.displayText(config.type),
            angularErrorDegrees = snapshot.orientation.direction.angularErrorDegrees,
            latestSnapshot = snapshot, conditionStates = progress.requiredConditions,
            holdProgress = if (config.requiresSound) .45 else 0.0,
            requiredHeadingDegrees = config.requiredHeadingDegrees,
            insideRadiusMeters = config.insideRadiusMeters,
            soundThresholdDecibels = config.soundThresholdDecibels,
        )
    }

    private fun configs(): List<RelicChallengeConfig> {
        val target = GeoCoordinate(-37.8, 145.0)
        return listOf(
            RelicChallengeConfigs.unionLawnPhoto("union", target, 20.0, 0.0),
            RelicChallengeConfigs.wilsonHallObservation("wilson", target, 20.0, 0.0),
            RelicChallengeConfigs.oldQuadExcavation("quad", target, 20.0),
            RelicChallengeConfigs.southLawnViewingAngle("atlas", target, 20.0, 0.0),
            RelicChallengeConfigs.systemGardenGlasshouse("garden", target, 20.0),
            RelicChallengeConfigs.graingerMuseumToneTool("tone", target, 20.0),
        )
    }
}
