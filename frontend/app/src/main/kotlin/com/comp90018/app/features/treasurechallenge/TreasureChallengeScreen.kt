package com.comp90018.app.features.treasurechallenge

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.camera.view.PreviewView
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
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
import com.comp90018.app.sensors.camera.CameraXCapture
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs

@Composable
fun TreasureChallengeRoute(
    config: RelicChallengeConfig,
    @DrawableRes historicalImageResId: Int? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: TreasureChallengeViewModel = viewModel(
        key = "treasure_challenge_${config.challengeId}",
        factory = TreasureChallengeViewModel.factory(context, config),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
        historicalImageResId = historicalImageResId,
        onBack = onBack,
        onPhotoCaptureStarted = viewModel::onPhotoCaptureStarted,
        onPhotoCaptured = viewModel::onPhotoCaptured,
        onCameraError = viewModel::onCameraError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureChallengeScreen(
    state: TreasureChallengeUiState,
    @DrawableRes historicalImageResId: Int? = null,
    onBack: () -> Unit,
    onPhotoCaptureStarted: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onCameraError: (String) -> Unit,
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ChallengeStatusCard(state)
            LinearProgressIndicator(
                progress = { state.holdProgress.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.challengeType == RelicChallengeType.UNION_LAWN_PHOTO &&
                (state.actionReady || state.capturedPhotoUri != null)
            ) {
                UnionPhotoPanel(
                    state = state,
                    historicalImageResId = historicalImageResId,
                    onPhotoCaptureStarted = onPhotoCaptureStarted,
                    onPhotoCaptured = onPhotoCaptured,
                    onCameraError = onCameraError,
                )
            }
            if (state.completed) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Return to map")
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
private fun UnionPhotoPanel(
    state: TreasureChallengeUiState,
    @DrawableRes historicalImageResId: Int?,
    onPhotoCaptureStarted: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onCameraError: (String) -> Unit,
) {
    val capturedUri = state.capturedPhotoUri
    if (capturedUri != null) {
        CapturedPhotoReveal(
            capturedPhotoUri = capturedUri,
            completed = state.completed,
            historicalImageResId = historicalImageResId,
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
                            uri != null -> onPhotoCaptured(uri.toString())
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
    completed: Boolean,
    @DrawableRes historicalImageResId: Int?,
) {
    var revealHistorical by remember(capturedPhotoUri) { mutableStateOf(false) }
    LaunchedEffect(completed, historicalImageResId, capturedPhotoUri) {
        if (completed && historicalImageResId != null) {
            delay(1_200L)
            revealHistorical = true
        }
    }
    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Crossfade(targetState = revealHistorical, label = "lost_lake_reveal") { showHistorical ->
                Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f), contentAlignment = Alignment.Center) {
                    if (showHistorical && historicalImageResId != null) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(historicalImageResId),
                            contentDescription = "Historical Lost Lake",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
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
            if (completed && historicalImageResId == null) {
                Text(
                    "Historical Lost Lake image pending asset integration",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
