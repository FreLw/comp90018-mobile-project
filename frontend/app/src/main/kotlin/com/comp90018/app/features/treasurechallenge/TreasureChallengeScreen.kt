package com.comp90018.app.features.treasurechallenge

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
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
import com.comp90018.app.GothicTreasureFontFamily
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.BuildConfig
import com.comp90018.app.sensors.camera.CameraXCapture

@Composable
fun TreasureChallengeRoute(
    config: RelicChallengeConfig,
    treasureId: String,
    radarRadiusMeters: Double,
    preciseLocationEnabled: Boolean = true,
    debugSimulationEnabled: Boolean = false,
    challengeSessionId: String = "",
    onChallengeCompleted: ((String?) -> Unit) -> Unit,
    onDiscoveryReady: (String?, ChallengeDiscoverySave) -> Unit,
    onBack: () -> Unit,
    onChallengeSatisfied: (String) -> Unit = {},
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

    LaunchedEffect(config.challengeId, state.completed) {
        if (canOpenTreasureReveal(state.completed)) {
            if (discoverySave.status == DiscoverySaveStatus.WAITING) {
                discoverySave.onChallengeCompleted(onChallengeCompleted) {
                    onChallengeSatisfied(viewModel.completionId)
                    onDiscoveryReady(state.capturedPhotoUri, discoverySave)
                }
            } else {
                // Re-entering a retained completed task restores its page without repeating save or feedback.
                onDiscoveryReady(state.capturedPhotoUri, discoverySave)
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
        onMicPermissionGranted = viewModel::restart,
        onCompleteWithDebugSnapshot = viewModel::completeWithDebugSnapshot,
    )
}

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
) {
    var emblemCenterInRoot by remember(state.challengeId) { mutableStateOf<Offset?>(null) }
    var backdropTopLeftInRoot by remember(state.challengeId) { mutableStateOf(Offset.Zero) }
    val fullscreenWater = state.challengeType == RelicChallengeType.WILSON_HALL_OBSERVATION
    Box(Modifier.fillMaxSize().testTag("quest_screen").onGloballyPositioned { coordinates ->
        if (fullscreenWater) backdropTopLeftInRoot = coordinates.localToRoot(Offset.Zero)
    }) {
        if (fullscreenWater) {
            QuestBackdrop(state, Modifier.fillMaxSize(),
                emblemCenter = emblemCenterInRoot?.minus(backdropTopLeftInRoot))
        }
        Scaffold(
            containerColor = if (fullscreenWater) Color.Transparent else Color(0xFF142D25),
            contentColor = Color(0xFFFFE6B1),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
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
                    QuestBackdrop(state, Modifier.fillMaxSize(),
                        emblemCenter = emblemCenterInRoot?.minus(backdropTopLeftInRoot))
                }
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedVisibility(visible = simulation != null && !state.completed,
                        enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        simulation?.Controls(
                            state = state,
                            onPhotoCaptured = onPhotoCaptured,
                            onCompleteWithDebugSnapshot = onCompleteWithDebugSnapshot,
                        )
                    }
                    ChallengeExperience(state, onEmblemCenterChanged = { emblemCenterInRoot = it })
                    ChallengeStatusCard(state)
                    if (state.cameraState != ChallengeCameraState.NOT_REQUIRED &&
                        (state.actionReady || state.capturedPhotoUri != null)
                    ) {
                        UnionPhotoPanel(
                            state = state,
                            onPhotoCaptureStarted = onPhotoCaptureStarted,
                            onPhotoCaptured = onPhotoCaptured,
                            onCameraError = onCameraError,
                        )
                    }
                    if (state.challengeType == RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL && !state.completed && simulation == null) {
                        MicrophonePermissionPanel(onPermissionGranted = onMicPermissionGranted)
                    }
                    if (debugCalibrationInfo != null && simulation != null) {
                        var showDiagnostics by remember { mutableStateOf(false) }
                        androidx.compose.material3.TextButton(onClick = { showDiagnostics = !showDiagnostics }) {
                            Text(if (showDiagnostics) "Hide diagnostics" else "Developer diagnostics")
                        }
                        if (showDiagnostics) debugCalibrationInfo.invoke()
                    }
                    if (state.completed) {
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
    }
}

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

@Composable
private fun UnionPhotoPanel(
    state: TreasureChallengeUiState,
    onPhotoCaptureStarted: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onCameraError: (String) -> Unit,
) {
    val capturedUri = state.capturedPhotoUri
    if (capturedUri != null) {
        CapturedPhotoReveal(
            capturedPhotoUri = capturedUri,
        )
        return
    }

    val context = LocalContext.current
    var permissionRefreshKey by remember { mutableIntStateOf(0) }
    val hasCameraPermission = remember(permissionRefreshKey) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRefreshKey++
        if (!it) onCameraError("Camera permission is required to take the treasure photo")
    }

    if (!hasCameraPermission) {
        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Text(" Enable camera")
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraCapture = remember(context) { CameraXCapture(context.applicationContext) }
    val previewView = remember(context) {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    DisposableEffect(cameraCapture, lifecycleOwner, previewView) {
        cameraCapture.bindPreview(
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            onQrCodeDetected = {},
            onError = onCameraError,
        )
        onDispose { cameraCapture.unbind() }
    }

    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFF1B3A2F), contentColor = QuestParchment),
        border = androidx.compose.foundation.BorderStroke(.7.dp, QuestGold.copy(alpha = .25f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
            )
            state.cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = state.actionReady && state.cameraState != ChallengeCameraState.CAPTURING,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onPhotoCaptureStarted()
                    cameraCapture.capturePhoto { uri, error ->
                        when {
                            uri != null && error == null && isCapturedPhotoUri(uri.toString()) -> onPhotoCaptured(uri.toString())
                            else -> onCameraError(error ?: "Capture failed")
                        }
                    }
                },
            ) {
                if (state.cameraState == ChallengeCameraState.CAPTURING) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                    Text(" Take photo")
                }
            }
        }
    }
}

@Composable
private fun CapturedPhotoReveal(
    capturedPhotoUri: String,
) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFF1B3A2F), contentColor = QuestParchment),
        border = androidx.compose.foundation.BorderStroke(.7.dp, QuestGold.copy(alpha = .25f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = {
                        ImageView(it).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            setImageURI(Uri.parse(capturedPhotoUri))
                        }
                    },
                    update = { it.setImageURI(Uri.parse(capturedPhotoUri)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

internal fun RelicChallengeType.taskInstructions(): String = when (this) {
    RelicChallengeType.UNION_LAWN_PHOTO -> "Find the lost lake site, align with the configured compass direction, then take a photograph."
    RelicChallengeType.WILSON_HALL_OBSERVATION -> "Approach the rosette: water and floating stars turn to gold across the whole field. Guide the needle towards the star to awaken a spreading ripple. Four petals glow when you stop turning; the other four glow when movement and shake are still. Light all four stars to reveal the relic immediately."
    RelicChallengeType.OLD_QUAD_EXCAVATION -> "Excavate the fossil by holding your phone horizontal, stationary and stable. Light every star to reveal the relic immediately."
    RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE -> "Approach Atlas and watch the vines turn to gold, from crown to root. Find the viewing direction, then remain perfectly still. Movement and shake share one stillness star; the orbit settles only when your phone stops turning. Light all four stars to reveal the relic immediately."
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE -> "Find the lost glasshouse site and take a photograph. No phone leveling or compass alignment is needed."
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL -> "At the museum, enable the microphone and make a sound above the configured threshold. Light both stars to reveal the relic immediately."
}
