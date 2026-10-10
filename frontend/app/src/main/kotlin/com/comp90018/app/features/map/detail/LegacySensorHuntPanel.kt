package com.comp90018.app.features.map.detail

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.features.map.compass.LOCAL_HUNT_CHALLENGE_TYPES
import com.comp90018.app.features.map.sensors.rememberDeviceMotion
import com.comp90018.app.features.treasurechallenge.TreasureChallengeViewModel
import kotlinx.coroutines.launch

/**
 * Within [LOCAL_HUNT_CHALLENGE_TYPES], these additionally drive the scan panel's "ready" state from
 * the same [TreasureChallengeViewModel] pipeline the sensor challenge screen uses, instead of the
 * fixed 4.5s timer - the visual panel is unchanged, only what decides completion.
 */
internal val REAL_SENSOR_HUNT_TYPES = setOf(
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE,
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL,
)

/**
 * Drives [HuntScanPanel] from the same [TreasureChallengeViewModel] pipeline the sensor challenge
 * screen uses, so the "ready to dig" transition reflects real device motion/orientation instead of a
 * fixed timer. Only used for relic types in [REAL_SENSOR_HUNT_TYPES]; the visual panel is identical
 * to [TreasureSearchPanel].
 */
@Composable
internal fun RealSensorHuntPanel(
    config: RelicChallengeConfig,
    deviceHeading: Float,
    preciseLocationEnabled: Boolean,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = remember(context) { context.findActivity() }

    fun checkMicPermission() = !config.requiresSound ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    var hasMicPermission by remember { mutableStateOf(checkMicPermission()) }
    // Set once a request comes back denied with the system no longer willing to show its own
    // rationale - that means the user picked "Don't allow" in a way Android now treats as
    // permanent (or chose "Deny" a second time), so re-launching the same request is a silent
    // no-op and the only way forward is the app's system Settings page.
    var micPermissionPermanentlyDenied by remember { mutableStateOf(false) }
    var micPermissionRequested by remember(config.challengeId) { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMicPermission = granted
        if (!granted) {
            micPermissionPermanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        }
    }

    // The relic needs the microphone to judge completion, so ask for it as soon as this hunt
    // opens (i.e. right after "Start Hunt" is tapped) instead of silently never detecting sound.
    LaunchedEffect(config.requiresSound, hasMicPermission) {
        if (config.requiresSound && !hasMicPermission && !micPermissionRequested) {
            micPermissionRequested = true
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val viewModel: TreasureChallengeViewModel = viewModel(
        key = "local_hunt_${config.challengeId}_$preciseLocationEnabled",
        factory = TreasureChallengeViewModel.factory(context, config, preciseLocationEnabled),
    )
    val challengeState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner.lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.start()
                // Catches permission changes made outside this flow (e.g. the user backs out to
                // system Settings and grants it there, then returns).
                Lifecycle.Event.ON_RESUME -> hasMicPermission = checkMicPermission()
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

    LaunchedEffect(challengeState.completed) {
        if (challengeState.completed) onReady()
    }

    val motion = rememberDeviceMotion()
    HuntScanPanel(
        headline = challengeState.instructionText,
        hintText = "Calm motion, centre the level spark, then turn with the compass glow.",
        ready = false,
        acceleration = motion.accelerationMagnitude,
        levelTilt = motion.tiltDegrees,
        heading = deviceHeading,
        primaryActionLabel = "",
        onPrimaryAction = {},
        onBack = onBack,
        soundProgress = if (config.requiresSound) challengeState.holdProgress.toFloat() else null,
    )

    if (config.requiresSound && !hasMicPermission) {
        MicrophonePermissionDialog(
            permanentlyDenied = micPermissionPermanentlyDenied,
            onRequestPermission = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            onOpenSettings = { context.startActivity(appSettingsIntent(context)) },
            onCancel = onBack,
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun appSettingsIntent(context: Context): Intent = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", context.packageName, null),
)

/**
 * Fallback for when the automatic request on entering [RealSensorHuntPanel] was denied: keeps
 * offering a way to grant microphone access instead of leaving a sound-based hunt stuck with no
 * explanation. Once Android reports the permission as [permanentlyDenied] (the user declined in a
 * way the system won't show its own rationale for again), re-requesting is a silent no-op, so this
 * routes to the app's system Settings page instead.
 */
@Composable
private fun MicrophonePermissionDialog(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Microphone needed") },
        text = {
            Text(
                if (permanentlyDenied) {
                    "This relic listens for sound, but microphone access was blocked. Enable it in Settings to keep hunting."
                } else {
                    "This relic listens for sound. Allow microphone access to keep hunting."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = if (permanentlyDenied) onOpenSettings else onRequestPermission) {
                Text(if (permanentlyDenied) "Open Settings" else "Allow microphone")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        },
    )
}
