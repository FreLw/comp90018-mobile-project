package com.comp90018.app.features.treasurechallenge

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.comp90018.app.sensors.camera.CameraXCapture
import com.comp90018.app.R

/** Camera access is requested by a deliberate tap; the viewfinder occupies the engraving's slot. */
@Composable
internal fun UnionLawnPhotoExperience(
    state: TreasureChallengeUiState,
    onEmblemCenterChanged: (Offset) -> Unit,
    onPhotoCaptureStarted: () -> Unit,
    onPhotoCaptured: (String) -> Unit,
    onCameraError: (String) -> Unit,
    onPhotoRevealFinished: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraOpened by rememberSaveable(state.challengeId) { mutableStateOf(false) }
    fun cameraGranted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    var hasCameraPermission by remember(state.challengeId) { mutableStateOf(cameraGranted()) }
    var permissionDenied by remember(state.challengeId) { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        cameraOpened = granted
        permissionDenied = !granted
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasCameraPermission = cameraGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val showViewfinder = (cameraOpened && hasCameraPermission) || state.capturedPhotoUri != null
    val cameraCapture = remember(context) { CameraXCapture(context.applicationContext) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            // Filter the TextureView's composited pixels, leaving the frame and controls in gold.
            setLayerType(View.LAYER_TYPE_HARDWARE, Paint().apply { colorFilter = lostLakePhotoColorFilter() })
        }
    }
    var frozenPreview by remember(state.challengeId) { mutableStateOf<Bitmap?>(null) }
    var captureInFlight by remember(state.challengeId) { mutableStateOf(false) }
    var previewReady by remember(previewView) { mutableStateOf(false) }
    val currentCameraError by rememberUpdatedState(onCameraError)
    DisposableEffect(cameraCapture, lifecycleOwner, previewView, showViewfinder, state.completed) {
        val observer = Observer<PreviewView.StreamState> { previewReady = it == PreviewView.StreamState.STREAMING }
        previewView.previewStreamState.observe(lifecycleOwner, observer)
        if (showViewfinder && !state.completed && hasCameraPermission) {
            cameraCapture.bindPreview(lifecycleOwner, previewView, {}, { currentCameraError(it) })
        }
        onDispose {
            previewView.previewStreamState.removeObserver(observer)
            cameraCapture.unbind()
            previewReady = false
        }
    }
    ChallengeExperience(
        state = state,
        onEmblemCenterChanged = onEmblemCenterChanged,
        emblemContent = { modifier ->
            if (showViewfinder) {
                UnionLawnViewfinder(state, modifier, previewView, previewReady, frozenPreview, onPhotoRevealFinished)
            } else {
                Box(modifier.testTag("union_camera_trigger")
                    .clickable(enabled = state.actionReady && !state.completed, role = Role.Button,
                        onClickLabel = "Open camera") {
                        permissionDenied = false
                        hasCameraPermission = cameraGranted()
                        if (hasCameraPermission) cameraOpened = true
                        else permissionLauncher.launch(Manifest.permission.CAMERA)
                    }.semantics { contentDescription = "Open the lost lake camera" }) {
                    QuestEmblem(state, Modifier.fillMaxSize())
                }
            }
        },
        afterEmblemContent = {
            if (showViewfinder) Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                // Reserve this space while the picture develops, so the camera slot never jumps.
                if (state.capturedPhotoUri == null) Button(onClick = {
                    captureInFlight = true
                    frozenPreview = previewView.bitmap
                    onPhotoCaptureStarted()
                    cameraCapture.capturePhoto { uri, error ->
                        captureInFlight = false
                        if (uri != null && error == null && isCapturedPhotoUri(uri.toString())) onPhotoCaptured(uri.toString())
                        else {
                            frozenPreview = null
                            onCameraError(error ?: "Capture failed. Please try again.")
                        }
                    }
                }, enabled = state.actionReady && previewReady && !captureInFlight && state.cameraState != ChallengeCameraState.CAPTURING,
                    modifier = Modifier.testTag("union_photo_shutter"),
                    colors = ButtonDefaults.buttonColors(containerColor = QuestGold, contentColor = QuestForest,
                        disabledContainerColor = QuestForest.copy(alpha = .85f), disabledContentColor = QuestStone)) {
                    if (captureInFlight || state.cameraState == ChallengeCameraState.CAPTURING) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = QuestGold)
                        Spacer(Modifier.width(8.dp))
                        Text("Capturing…")
                    } else if (!previewReady) {
                        Text("Opening camera…", fontFamily = FontFamily.Serif)
                    } else {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Take photograph", fontFamily = FontFamily.Serif)
                    }
                }
            }
        },
        beforeGuideContent = {
            // Expanding in the column pushes the guide, stars and other interactions down.
            AnimatedVisibility(visible = state.actionReady || showViewfinder,
                enter = fadeIn(tween(350)) + expandVertically(tween(350)),
                exit = fadeOut(tween(250)) + shrinkVertically(tween(250))) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("union_photo_prompt"), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Canvas(Modifier.width(110.dp).height(12.dp)) {
                        drawCrossStar(center, 4.dp.toPx(), QuestGold)
                        for (side in listOf(-1f, 1f)) {
                            drawLine(QuestGold.copy(alpha = .45f), center + Offset(side * 12.dp.toPx(), 0f),
                                center + Offset(side * 48.dp.toPx(), 0f), .65.dp.toPx())
                        }
                    }
                    Text(when {
                        state.completed -> "A memory of the lost lake is taking shape."
                        permissionDenied -> "Allow camera access to take your photograph. Tap the camera to try again."
                        !showViewfinder -> "Tap the camera to take a photograph."
                        !state.actionReady -> "Align distance and heading again to take your photograph."
                        else -> "Frame the scene, then take your photograph."
                    }, color = QuestParchment, fontFamily = FontFamily.Serif, fontSize = 16.sp,
                        lineHeight = 22.sp, textAlign = TextAlign.Center)
                    if (showViewfinder) state.cameraError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        },
    )
}

@Composable
private fun UnionLawnViewfinder(
    state: TreasureChallengeUiState,
    modifier: Modifier,
    previewView: PreviewView,
    previewReady: Boolean,
    frozenPreview: Bitmap?,
    onPhotoRevealFinished: () -> Unit,
) {
    val capturedUri = state.capturedPhotoUri
    val morph = remember(state.challengeId, capturedUri) { Animatable(0f) }
    val flash = remember(state.challengeId) { Animatable(0f) }
    val currentRevealFinished by rememberUpdatedState(onPhotoRevealFinished)
    LaunchedEffect(state.completed, capturedUri) {
        if (state.completed && capturedUri != null) {
            morph.animateTo(1f, tween(LostLakePhotoMorphDurationMillis, easing = FastOutSlowInEasing))
            currentRevealFinished()
        }
    }
    LaunchedEffect(state.cameraState) {
        if (state.cameraState == ChallengeCameraState.CAPTURING || state.cameraState == ChallengeCameraState.CAPTURED) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(420))
        } else flash.snapTo(0f)
    }
    val sepia = remember { ColorFilter.colorMatrix(ColorMatrix(LostLakeSepiaMatrix)) }
    val frozenImage = remember(frozenPreview) { frozenPreview?.asImageBitmap() }
    BoxWithConstraints(modifier.testTag("union_viewfinder"), contentAlignment = Alignment.Center) {
        val logoSize = minOf(maxWidth, maxHeight)
        val p = morph.value
        val shape = (p / .65f).coerceIn(0f, 1f)
        val blend = ((p - .25f) / .6f).coerceIn(0f, 1f)
        // These proportions match the inner photograph in the 512px bundled postcard.
        val frameWidth = (maxWidth - 52.dp) * (1f - shape) + logoSize * .69f * shape
        val frameHeight = (maxHeight - 20.dp) * (1f - shape) + logoSize * .56f * shape
        Box(Modifier.size(frameWidth, frameHeight).offset(x = logoSize * .008f * shape, y = logoSize * .046f * shape)
            .graphicsLayer { rotationZ = LostLakePostcardTiltDegrees * shape; alpha = 1f - blend }
            .clip(RoundedCornerShape(16.dp * (1f - shape) + 2.dp * shape)).background(QuestForest)
            .testTag("union_photo_frame").semantics { stateDescription = "Vintage sepia photograph" }) {
            if (frozenImage != null) {
                Image(frozenImage, "Captured scene in sepia", Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    colorFilter = sepia)
            } else if (capturedUri == null) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()
                    .testTag("union_camera_preview"))
            } else {
                AndroidView(factory = {
                    ImageView(it).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        colorFilter = lostLakePhotoColorFilter()
                    }
                }, update = { it.setImageURI(Uri.parse(capturedUri)) }, modifier = Modifier.fillMaxSize())
            }
            Canvas(Modifier.matchParentSize()) {
                drawRect(QuestParchment.copy(alpha = flash.value * .5f))
                drawRoundRect(QuestGold, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    style = Stroke(2.dp.toPx()))
                val inset = 7.dp.toPx()
                drawRoundRect(QuestGold.copy(alpha = .6f), topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()), style = Stroke(.65.dp.toPx()))
                for (x in listOf(16.dp.toPx(), size.width - 16.dp.toPx())) {
                    for (y in listOf(16.dp.toPx(), size.height - 16.dp.toPx())) {
                        drawCrossStar(Offset(x, y), 4.dp.toPx(), QuestGold)
                    }
                }
            }
            if (!previewReady && capturedUri == null && frozenImage == null) {
                CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.Center), color = QuestGold,
                    strokeWidth = 1.5.dp)
            }
        }
        if (capturedUri != null) Image(painterResource(R.drawable.treasure_postcard), "The lost lake postcard",
            Modifier.size(logoSize).graphicsLayer { alpha = blend }.testTag("union_photo_morph").semantics {
                stateDescription = if (p < 1f) "Photograph blending into the lake postcard" else "Lake postcard revealed"
            }, contentScale = ContentScale.Fit)
    }
}
