package com.comp90018.app.features.treasurechallenge

/*
 * Connects task lifecycle, simulation, completion/save callbacks, and the green-task interface.
 * Photo tasks retain the logo while text exits and the background blends into the discovery page.
 */

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.BuildConfig
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TreasureDiscoveryReveal

/**
 * Binds lifecycle, ViewModel, TEST controls, immediate feedback/save, and the final discovery
 * callback.
 */
@Composable
fun TreasureChallengeRoute(
    config: RelicChallengeConfig,
    treasureId: String,
    radarRadiusMeters: Double,
    preciseLocationEnabled: Boolean = true,
    debugSimulationEnabled: Boolean = false,
    challengeSessionId: String = "",
    onChallengeCompleted: ((String?) -> Unit) -> Unit,
    onDiscoveryReady: (String?, ChallengeDiscoverySave, PhotoRevealArrival?) -> Unit,
    onBack: () -> Unit,
    onChallengeSatisfied: (String) -> Unit = {},
    revealRelic: MapRelic? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var testPanelEnabled by remember(config.challengeId, challengeSessionId) {
        mutableStateOf(BuildConfig.DEBUG && debugSimulationEnabled)
    }
    val viewModel: TreasureChallengeViewModel = viewModel(
        key = "treasure_challenge_${config.challengeId}_${preciseLocationEnabled}_${testPanelEnabled}_$challengeSessionId",
        factory = TreasureChallengeViewModel.factory(context, config, preciseLocationEnabled,
            simulationFactory = if (testPanelEnabled) { { ChallengeSimulationFactory.create(config) } } else null),
    )
    // The retained ViewModel owns the simulator and engine across reopening and rotation.
    val simulation = viewModel.simulationSession
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val discoverySave = viewModel.discoverySave
    // Physical completion starts saving and feedback immediately; photo navigation waits for the visual handoff.
    val photoRevealPending = state.challengeType.photoRevealStyle() != null &&
        state.capturedPhotoUri != null && !viewModel.photoRevealFinished

    LaunchedEffect(config.challengeId, state.completed, photoRevealPending) {
        if (canOpenTreasureReveal(state.completed)) {
            if (discoverySave.status == DiscoverySaveStatus.WAITING) {
                discoverySave.onChallengeCompleted(onChallengeCompleted) {
                    onChallengeSatisfied(viewModel.completionId)
                    if (!photoRevealPending) onDiscoveryReady(state.capturedPhotoUri, discoverySave, viewModel.photoRevealArrival)
                }
            } else if (!photoRevealPending) {
                // Re-entering a retained completed task restores its page without repeating save or feedback.
                onDiscoveryReady(state.capturedPhotoUri, discoverySave, viewModel.photoRevealArrival)
            }
        }
    }

    DisposableEffect(lifecycleOwner.lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.start()
                Lifecycle.Event.ON_STOP -> viewModel.stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) viewModel.start()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stop()
        }
    }

    TreasureChallengeScreen(
        state = state,
        saveStatus = discoverySave.status,
        saveError = discoverySave.error,
        onRetrySave = { discoverySave.retry(onChallengeCompleted) },
        simulation = simulation,
        testPanelToggle = if (BuildConfig.DEBUG && !state.completed) {
            {
                TextButton(onClick = { testPanelEnabled = !testPanelEnabled },
                    colors = ButtonDefaults.textButtonColors(contentColor = if (testPanelEnabled) QuestGold else QuestStone)) {
                    Text(if (testPanelEnabled) "TEST •" else "TEST", fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }
        } else null,
        debugCalibrationInfo = if (BuildConfig.DEBUG) {
            {
                ChallengeSimulationFactory.CalibrationPanel(
                    treasureId = treasureId,
                    config = config,
                    radarRadiusMeters = radarRadiusMeters,
                    state = state,
                    onSimulateSound = viewModel::simulateSustainedSoundForDebug,
                )
            }
        } else null,
        onBack = onBack,
        onPhotoCaptureStarted = viewModel::onPhotoCaptureStarted,
        onPhotoCaptured = viewModel::onPhotoCaptured,
        onCameraError = viewModel::onCameraError,
        onPhotoRevealFinished = viewModel::finishPhotoReveal,
        revealRelic = revealRelic ?: MapRelic(treasureId, state.title, "", coordinate = config.targetLocation),
        onMicPermissionGranted = viewModel::restart,
        onCompleteWithDebugSnapshot = viewModel::completeWithDebugSnapshot,
    )
}

/** Renders the green task and its photo-to-discovery transition while preserving the measured logo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureChallengeScreen(
    state: TreasureChallengeUiState,
    saveStatus: DiscoverySaveStatus = DiscoverySaveStatus.WAITING,
    saveError: String? = null,
    onRetrySave: () -> Unit = {},
    simulation: ChallengeSimulationSession? = null,
    debugCalibrationInfo: (@Composable () -> Unit)? = null,
    onBack: () -> Unit,
    onPhotoCaptureStarted: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onCameraError: (String) -> Unit,
    onMicPermissionGranted: () -> Unit = {},
    onCompleteWithDebugSnapshot: (com.comp90018.app.contextengine.DeviceContextSnapshot) -> Unit = {},
    testPanelToggle: (@Composable () -> Unit)? = null,
    onPhotoRevealFinished: (PhotoRevealArrival) -> Unit = {},
    revealRelic: MapRelic? = null,
) {
    var emblemCenterInRoot by remember(state.challengeId) { mutableStateOf<Offset?>(null) }
    var backdropTopLeftInRoot by remember(state.challengeId) { mutableStateOf(Offset.Zero) }
    val fullscreenWater = state.challengeType == RelicChallengeType.WILSON_HALL_OBSERVATION
    val photoTask = state.challengeType.photoRevealStyle() != null
    val photoDeveloping = photoTask && state.capturedPhotoUri != null
    var pageOrigin by remember { mutableStateOf(Offset.Zero) }
    var logoBounds by remember(state.challengeId) { mutableStateOf<Rect?>(null) }
    var photoArrival by remember(state.challengeId) { mutableStateOf<PhotoRevealArrival?>(null) }
    // This second transition keeps the developed logo fixed, moves task text away, and warms the page to yellow.
    val departure = remember(state.challengeId) { Animatable(0f) }
    val transitioning = photoArrival != null
    val exit = (departure.value / .42f).coerceIn(0f, 1f)
    LaunchedEffect(photoArrival) {
        photoArrival?.let { arrival ->
            departure.animateTo(1f, tween(QuestPhotoRevealDurationMillis, easing = FastOutSlowInEasing))
            onPhotoRevealFinished(arrival)
        }
    }
    Box(Modifier.fillMaxSize().testTag("quest_screen").onGloballyPositioned { coordinates ->
        pageOrigin = coordinates.positionInRoot()
        if (fullscreenWater) backdropTopLeftInRoot = coordinates.localToRoot(Offset.Zero)
    }) {
        if (fullscreenWater) {
            QuestBackdrop(state, Modifier.fillMaxSize(),
                emblemCenter = emblemCenterInRoot?.minus(backdropTopLeftInRoot))
        }
        Scaffold(
            containerColor = if (fullscreenWater) Color.Transparent else
                lerp(if (photoTask) QuestForest else Color(0xFF142D25), QuestPhotoRevealPaper, departure.value),
            contentColor = Color(0xFFFFE6B1),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(Modifier.fillMaxWidth().graphicsLayer {
                    alpha = 1f - exit
                    translationY = -80.dp.toPx() * exit
                }, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, enabled = !transitioning) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to map",
                            modifier = Modifier.size(20.dp), tint = QuestParchment.copy(alpha = .75f))
                    }
                    Spacer(Modifier.weight(1f))
                    testPanelToggle?.invoke()
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).onGloballyPositioned { coordinates ->
                if (!fullscreenWater) backdropTopLeftInRoot = coordinates.localToRoot(Offset.Zero)
            }) {
                if (!fullscreenWater) {
                    QuestBackdrop(state, Modifier.fillMaxSize().graphicsLayer { alpha = 1f - departure.value },
                        emblemCenter = emblemCenterInRoot?.minus(backdropTopLeftInRoot), drawBackground = !photoTask)
                }
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState(), enabled = !transitioning)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedVisibility(visible = simulation != null && (!state.completed || photoDeveloping),
                        modifier = Modifier.graphicsLayer { alpha = 1f - exit; translationY = -100.dp.toPx() * exit },
                        enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        simulation?.Controls(
                            state = state,
                            onPhotoCaptured = onPhotoCaptured,
                            onCompleteWithDebugSnapshot = onCompleteWithDebugSnapshot,
                        )
                    }
                    if (state.challengeType.photoRevealStyle() != null) {
                        QuestPhotoExperience(
                            state = state,
                            onEmblemCenterChanged = { emblemCenterInRoot = it },
                            onPhotoCaptureStarted = onPhotoCaptureStarted,
                            onPhotoCaptured = onPhotoCaptured,
                            onCameraError = onCameraError,
                            onPhotoMorphFinished = {
                                val bounds = logoBounds
                                val style = state.challengeType.photoRevealStyle()
                                if (photoArrival == null && bounds != null && style != null) {
                                    // Convert root measurements to page-local bounds so the discovery logo has the same position and size.
                                    val arrival = PhotoRevealArrival(bounds.translate(-pageOrigin), style.artworkResId)
                                    if (revealRelic != null) photoArrival = arrival else onPhotoRevealFinished(arrival)
                                }
                            },
                            onLogoBoundsChanged = { logoBounds = it },
                            departureProgress = departure.value,
                            artworkTransferred = transitioning,
                        )
                    } else {
                        ChallengeExperience(state, onEmblemCenterChanged = { emblemCenterInRoot = it })
                    }
                    Box(Modifier.graphicsLayer { alpha = 1f - exit; translationY = 140.dp.toPx() * exit }) {
                        ChallengeStatusCard(state)
                    }
                    if (state.challengeType == RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL && !state.completed && simulation == null) {
                        MicrophonePermissionPanel(onPermissionGranted = onMicPermissionGranted)
                    }
                    if (debugCalibrationInfo != null && simulation != null) {
                        Column(Modifier.graphicsLayer { alpha = 1f - exit; translationY = 140.dp.toPx() * exit },
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            var showDiagnostics by remember { mutableStateOf(false) }
                            androidx.compose.material3.TextButton(onClick = { showDiagnostics = !showDiagnostics }) {
                                Text(if (showDiagnostics) "Hide diagnostics" else "Developer diagnostics")
                            }
                            if (showDiagnostics) debugCalibrationInfo.invoke()
                        }
                    }
                    if (state.completed && !photoDeveloping) {
                        when (saveStatus) {
                            DiscoverySaveStatus.WAITING, DiscoverySaveStatus.SAVING -> {
                                CircularProgressIndicator()
                                Text("Saving discovery…")
                            }
                            DiscoverySaveStatus.SAVED -> Text("Discovery saved to your collection")
                            DiscoverySaveStatus.FAILED -> {
                                Text("Couldn't save discovery: ${saveError ?: "Please try again."}",
                                    color = MaterialTheme.colorScheme.error)
                                Button(onClick = onRetrySave, modifier = Modifier.fillMaxWidth()) {
                                    Text("Retry Save")
                                }
                            }
                        }
                    }
                }
            }
        }
        val arrival = photoArrival
        if (arrival != null && revealRelic != null) {
            Box(Modifier.fillMaxSize().testTag("photo_reveal_transition").semantics {
                stateDescription = if (departure.value < .48f) "Background warming; quest text departing"
                    else "Discovery lettering appearing"
            }.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }) {
                TreasureDiscoveryReveal(revealRelic, {}, {}, onRetrySave = onRetrySave, saveStatus = saveStatus,
                    photoArrival = arrival, arrivalProgress = departure.value, drawBackground = false)
            }
        }
    }
}

/** Offers runtime microphone access before a real sound challenge can read its environment. */
@Composable
private fun MicrophonePermissionPanel(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    var permissionRefreshKey by remember { mutableIntStateOf(0) }
    val hasMicPermission = remember(permissionRefreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRefreshKey++
        if (granted) onPermissionGranted()
    }

    if (hasMicPermission) return

    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFF1B3A2F), contentColor = QuestParchment),
        border = androidx.compose.foundation.BorderStroke(.7.dp, QuestGold.copy(alpha = .25f))) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "This relic listens for sound. Allow microphone access to continue.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Icon(Icons.Rounded.Mic, contentDescription = null)
                Text(" Enable microphone")
            }
        }
    }
}

/** Instruction copy rendered inside the shared How to play parchment scroll. */
internal fun RelicChallengeType.taskInstructions(): String = when (this) {
    RelicChallengeType.UNION_LAWN_PHOTO -> "Approach the lost lake site: golden water rises through the camera as the distance falls. Align with the compass direction to light the other half of the ring. Once both halves glow, tap the camera and allow camera access. Frame the scene in sepia, then press the button beneath the frame. Your photograph tilts and blends into a keepsake of the lost lake."
    RelicChallengeType.WILSON_HALL_OBSERVATION -> "Approach the rosette: water and floating stars turn to gold across the whole field. Guide the needle towards the star to awaken a spreading ripple. Four petals glow when you stop turning; the other four glow when movement and shake are still. Light all four stars to reveal the relic immediately."
    RelicChallengeType.OLD_QUAD_EXCAVATION -> "Excavate the fossil by holding your phone horizontal, stationary and stable. Light every star to reveal the relic immediately."
    RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> "Approach Atlas and watch the vines turn to gold, from crown to root. Find the viewing direction, then remain perfectly still. Movement and shake share one stillness star; the orbit settles only when your phone stops turning. Light all four stars to reveal the relic immediately."
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> "Approach the lost glasshouse site to light the distance star. Tap the glasshouse and allow camera access. Frame the scene in sepia, then press the button beneath the frame. Your photograph blends into the upright glasshouse keepsake. No phone leveling or compass alignment is needed."
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> "At the museum, enable the microphone and make a sound above the configured threshold. Light both stars to reveal the relic immediately."
}
