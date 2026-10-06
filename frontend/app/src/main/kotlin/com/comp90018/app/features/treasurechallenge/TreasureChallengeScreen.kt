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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.contextengine.challenge.ChallengeInstruction
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.BuildConfig
import com.comp90018.app.sensors.camera.CameraXCapture
import java.util.Locale
import kotlin.math.abs

@Composable
fun TreasureChallengeRoute(
    config: RelicChallengeConfig,
    treasureId: String,
    radarRadiusMeters: Double,
    preciseLocationEnabled: Boolean = true,
    debugSimulationEnabled: Boolean = false,
    onChallengeCompleted: ((String?) -> Unit) -> Unit,
    onDiscoverySaved: (String?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val simulation = remember(config.challengeId, debugSimulationEnabled) {
        if (debugSimulationEnabled) ChallengeSimulationFactory.create(config) else null
    }
    val viewModel: TreasureChallengeViewModel = viewModel(
        key = "treasure_challenge_${config.challengeId}_${preciseLocationEnabled}_$debugSimulationEnabled",
        factory = TreasureChallengeViewModel.factory(context, config, preciseLocationEnabled,
            simulation?.let { session -> { _: kotlinx.coroutines.CoroutineScope -> session.engine } }),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val discoverySave = remember(config.challengeId) { ChallengeDiscoverySave() }

    LaunchedEffect(config.challengeId, state.completed) {
        if (state.completed) discoverySave.onChallengeCompleted(onChallengeCompleted)
    }
    LaunchedEffect(config.challengeId, state.completed, discoverySave.status) {
        if (canOpenTreasureReveal(state.completed, discoverySave.status)) {
            onDiscoverySaved(state.capturedPhotoUri)
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
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to map")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChallengeStatusCard(state)
            debugCalibrationInfo?.invoke()
            LinearProgressIndicator(
                progress = { state.holdProgress.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
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
            if (state.challengeType == RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL && !state.completed) {
                MicrophonePermissionPanel(onPermissionGranted = onMicPermissionGranted)
            }
            if (simulation != null && !state.completed) {
                simulation.Controls(
                    state = state,
                    onPhotoCaptured = onPhotoCaptured,
                    onCompleteWithDebugSnapshot = onCompleteWithDebugSnapshot,
                )
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

@Composable
private fun ChallengeStatusCard(state: TreasureChallengeUiState) {
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.completed) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(state.instructionText, style = MaterialTheme.typography.headlineSmall)
            if (state.instruction == ChallengeInstruction.TURN_LEFT ||
                state.instruction == ChallengeInstruction.TURN_RIGHT
            ) {
                state.angularErrorDegrees?.let {
                    Text(
                        String.format(Locale.US, "%.0f° remaining", abs(it)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (!state.completed && state.holdProgress > 0.0) {
                Text(
                    String.format(Locale.US, "%.0f%%", state.holdProgress * 100.0),
                    style = MaterialTheme.typography.labelLarge,
                )
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

    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
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

    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
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
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
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
