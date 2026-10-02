package com.comp90018.app.features.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.WbIncandescent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.contextengine.challenge.RelicChallengeConfig
import com.comp90018.app.contextengine.challenge.RelicChallengeType
import com.comp90018.app.features.treasurechallenge.TreasureChallengeViewModel
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.location.LocationPermissionState
import com.comp90018.app.sensors.location.ProximityState
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Background
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.R
import com.comp90018.app.BuildConfig
import com.comp90018.app.RelicRed
import com.comp90018.app.features.treasure.RemoteTreasureImage
import com.comp90018.app.features.treasure.TreasurePrototypeImage
import com.comp90018.app.features.treasurechallenge.TreasureChallengeRoute
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.delay
import java.util.Locale

/** Dedicated map feature boundary; location rendering belongs here. */
@Composable
fun MapScreen(
    treasures: List<MapRelic>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    discoveredTreasureIds: Set<String>,
    savingTreasureId: String?,
    preciseLocationEnabled: Boolean,
    hapticsEnabled: Boolean,
    userLocation: LocationOutput,
    onEnableLocation: () -> Unit,
    onCollectTreasure: (String, (String?) -> Unit) -> Unit,
    activeHuntTreasureId: String? = null,
    activeHuntOwnerId: String? = null,
    activeHuntMemberIds: List<String> = emptyList(),
    activeHuntCompletedMemberIds: List<String> = emptyList(),
    currentUserId: String = "",
    onCompleteActiveHuntTask: () -> Unit = {},
) {
    val teamHuntTarget = activeHuntTreasureId?.let { id -> treasures.firstOrNull { it.id == id } }
    val teamHuntActive = teamHuntTarget != null && activeHuntOwnerId != null
    val teamHuntIsOwner = activeHuntOwnerId == currentUserId
    val teamHuntAllCompleted = teamHuntActive && activeHuntMemberIds.size == 2 &&
        activeHuntMemberIds.all { it in activeHuntCompletedMemberIds }
    val teamHuntTaskPendingForCurrentUser = TeamHuntTaskEligibility.isPendingFor(
        huntActive = teamHuntActive,
        memberIds = activeHuntMemberIds,
        completedMemberIds = activeHuntCompletedMemberIds,
        currentUserId = currentUserId,
    )
    // An active team hunt locks the solo catalogue to its shared target.
    val resolvedTreasures = if (teamHuntActive) listOf(requireNotNull(teamHuntTarget)) else treasures
    var selectedRelic by remember { mutableStateOf<MapRelic?>(null) }
    var detailRelic by remember { mutableStateOf<MapRelic?>(null) }
    var challengeRelic by remember { mutableStateOf<MapRelic?>(null) }
    var memberQuizRelic by remember { mutableStateOf<MapRelic?>(null) }
    var teamArrivalPromptedForId by remember { mutableStateOf<String?>(null) }
    var debugSimulationEnabled by remember { mutableStateOf(false) }
    val revealCoordinator = remember { PostChallengeRevealCoordinator() }
    val foundRelicIds = discoveredTreasureIds
    val deviceHeading = rememberDeviceHeading()
    val activeRelic = detailRelic ?: selectedRelic
    val locationOutput = remember(activeRelic, userLocation) {
        LocationCalculator.buildOutput(
            currentLocation = userLocation.currentLocation,
            targetLocation = activeRelic?.coordinate,
            timestampNanos = userLocation.timestampNanos,
            config = LocationConfig(
                insideRadiusMeters = activeRelic?.insideRadiusMeters ?: 20.0,
                nearbyRadiusMeters = activeRelic?.radarRadiusMeters ?: 100.0,
            ),
            permission = userLocation.permission,
            availability = userLocation.availability,
            accuracyMeters = userLocation.accuracyMeters,
            lastKnownLocation = userLocation.lastKnownLocation,
        )
    }
    val teamHuntLocationOutput = remember(teamHuntTarget, userLocation) {
        LocationCalculator.buildOutput(
            currentLocation = userLocation.currentLocation,
            targetLocation = teamHuntTarget?.coordinate,
            timestampNanos = userLocation.timestampNanos,
            config = LocationConfig(
                insideRadiusMeters = teamHuntTarget?.insideRadiusMeters ?: 20.0,
                nearbyRadiusMeters = teamHuntTarget?.radarRadiusMeters ?: 100.0,
            ),
            permission = userLocation.permission,
            availability = userLocation.availability,
            accuracyMeters = userLocation.accuracyMeters,
            lastKnownLocation = userLocation.lastKnownLocation,
        )
    }
    val teamHuntProximity = teamHuntTarget?.let { target ->
        HuntProximityResolver.resolve(
            distanceMeters = teamHuntLocationOutput.distanceToTargetMeters,
            locationValidity = userLocation.validity,
            radarRadiusMeters = target.radarRadiusMeters,
            insideRadiusMeters = target.insideRadiusMeters,
        )
    }
    val isLocationStale = userLocation.permission.isGranted &&
        locationOutput.currentLocation == null &&
        locationOutput.lastKnownLocation != null
    val markerTransition = rememberInfiniteTransition(label = "map_quest_marker")
    val markerPulse by markerTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "map_quest_marker_pulse",
    )

    LaunchedEffect(resolvedTreasures) {
        selectedRelic = selectedRelic?.let { selected -> resolvedTreasures.firstOrNull { it.id == selected.id } }
        detailRelic = detailRelic?.let { detail -> resolvedTreasures.firstOrNull { it.id == detail.id } }
        challengeRelic = challengeRelic?.let { challenge -> resolvedTreasures.firstOrNull { it.id == challenge.id } }
    }
    LaunchedEffect(teamHuntAllCompleted, teamHuntTarget?.id) {
        if (teamHuntAllCompleted) detailRelic = teamHuntTarget
    }
    LaunchedEffect(teamHuntTarget?.id) {
        teamArrivalPromptedForId = null
    }
    LaunchedEffect(teamHuntTarget?.id, teamHuntProximity, teamHuntTaskPendingForCurrentUser, teamArrivalPromptedForId) {
        if (teamHuntTaskPendingForCurrentUser &&
            teamHuntProximity == HuntProximityStage.HUNT_READY &&
            teamArrivalPromptedForId != teamHuntTarget?.id
        ) {
            teamArrivalPromptedForId = teamHuntTarget?.id
        }
    }

    if (teamHuntTaskPendingForCurrentUser && teamArrivalPromptedForId == teamHuntTarget?.id) {
        TeamHuntArrivalDialog(
            isOwner = teamHuntIsOwner,
            onDismiss = { teamArrivalPromptedForId = "dismissed:${teamHuntTarget?.id}" },
            onContinue = {
                val target = requireNotNull(teamHuntTarget)
                teamArrivalPromptedForId = "dismissed:${target.id}"
                // The owner always performs the treasure's normal individual challenge. This
                // keeps the GPS/sensor rules and challenge examples identical in solo and team
                // hunts; team mode only adds the shared completion update on success.
                if (teamHuntIsOwner && target.challengeConfig != null) {
                    challengeRelic = target
                } else if (teamHuntIsOwner) {
                    detailRelic = target
                } else {
                    memberQuizRelic = target
                }
            },
        )
    }

    memberQuizRelic?.let { relic ->
        MemberHuntQuiz(
            relic = relic,
            locationOutput = LocationCalculator.buildOutput(
                currentLocation = userLocation.currentLocation,
                targetLocation = relic.coordinate,
                timestampNanos = userLocation.timestampNanos,
                config = LocationConfig(relic.insideRadiusMeters, relic.radarRadiusMeters),
                permission = userLocation.permission,
                availability = userLocation.availability,
                accuracyMeters = userLocation.accuracyMeters,
            ),
            onCompleted = { onCompleteActiveHuntTask(); memberQuizRelic = null },
            onBack = { memberQuizRelic = null },
        )
        return
    }

    revealCoordinator.session?.let { session ->
        PostChallengeRevealScreen(session) {
            revealCoordinator.clear()
            detailRelic = null
            selectedRelic = null
        }
        return
    }

    challengeRelic?.let { relic ->
        relic.challengeConfig?.let { config ->
            TreasureChallengeRoute(
                config = config,
                treasureId = relic.id,
                radarRadiusMeters = relic.radarRadiusMeters,
                preciseLocationEnabled = preciseLocationEnabled,
                // Debug builds must always expose deterministic sensor controls.  The map's
                // launcher also sets this flag, but making the build type authoritative avoids
                // falling back to real emulator GPS/sensor readings if that UI state is lost.
                debugSimulationEnabled = BuildConfig.DEBUG,
                onChallengeCompleted = { onComplete ->
                    if (teamHuntTaskPendingForCurrentUser && teamHuntIsOwner) {
                        // A Room owner's challenge is that owner's shared hunt task, not a solo
                        // discovery.  Wait for both Room members before exposing the dig action.
                        onCompleteActiveHuntTask()
                        challengeRelic = null
                        debugSimulationEnabled = false
                    } else {
                        saveRelicDiscovery(relic, onCollectTreasure, onComplete)
                    }
                },
                onDiscoverySaved = { capturedPhotoUri ->
                    revealCoordinator.openAfterSave(relic, capturedPhotoUri)
                    challengeRelic = null
                    debugSimulationEnabled = false
                },
                onBack = { challengeRelic = null; debugSimulationEnabled = false },
            )
            return
        }
    }

    detailRelic?.let { relic ->
        TreasureDetailScreen(
            relic = relic,
            locationOutput = locationOutput,
            locationValidity = userLocation.validity,
            hapticsEnabled = hapticsEnabled,
            deviceHeading = deviceHeading,
            // Team hunts remain replayable even when this relic is already in the explorer's
            // personal collection; the shared hunt completion is tracked separately.
            isFound = relic.id in foundRelicIds && !teamHuntActive,
            collecting = savingTreasureId == relic.id,
            preciseLocationEnabled = preciseLocationEnabled,
            onCollected = { onComplete -> onCollectTreasure(relic.id, onComplete) },
            onStartChallenge = relic.challengeConfig
                ?.takeUnless { it.type in LOCAL_HUNT_CHALLENGE_TYPES }
                ?.let { { debugSimulationEnabled = false; challengeRelic = relic } },
            forceReadyToDig = teamHuntAllCompleted && relic.id == teamHuntTarget?.id,
            onTeamTaskFinished = if (teamHuntTaskPendingForCurrentUser && teamHuntIsOwner) onCompleteActiveHuntTask else null,
            onBack = {
                detailRelic = null
            },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = resolvedTreasures,
            selectedRelic = selectedRelic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            markerPulse = if (selectedRelic == null) markerPulse else 1f,
            activeHuntTreasureId = activeHuntTreasureId,
            focusSelectedRelic = false,
            onRelicSelected = {
                if (teamHuntTaskPendingForCurrentUser && teamHuntIsOwner && it.id == teamHuntTarget?.id &&
                    it.challengeConfig != null
                ) {
                    challengeRelic = it
                } else if (teamHuntTaskPendingForCurrentUser && !teamHuntIsOwner && it.id == teamHuntTarget?.id) {
                    memberQuizRelic = it
                } else {
                    selectedRelic = it
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (isLocationStale) {
            StaleLocationBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            )
        }

        if (selectedRelic == null) {
            if (!userLocation.permission.isGranted) {
                LocationPermissionPrompt(
                    onPermissionGranted = onEnableLocation,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            } else if (!teamHuntActive) {
                FindTreasurePrompt(
                    message = when {
                        loading -> "Loading treasures from Firebase…"
                        error != null -> error
                        resolvedTreasures.isEmpty() -> "No enabled treasures are available right now."
                        else -> "Psst… tap a treasure and see what’s hiding nearby!"
                    },
                    loading = loading,
                    onRetry = onRetry.takeIf { error != null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
        }

        activeHuntTreasureId?.let { huntId ->
            resolvedTreasures.firstOrNull { it.id == huntId }?.let { relic ->
                Card(
                    // The hunt marker can sit directly beneath the user's location marker.  Make
                    // the persistent banner an equivalent, unambiguous way to open the target.
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .clickable {
                            if (teamHuntTaskPendingForCurrentUser && teamHuntIsOwner && relic.challengeConfig != null) {
                                challengeRelic = relic
                            } else if (teamHuntTaskPendingForCurrentUser && !teamHuntIsOwner) {
                                memberQuizRelic = relic
                            } else {
                                selectedRelic = relic
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = Brand)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when {
                                teamHuntAllCompleted -> "Both explorers are ready — dig the treasure!"
                                currentUserId in activeHuntCompletedMemberIds -> "Waiting for your teammate — you can help them."
                                else -> "Go and hunt for the treasure: ${relic.name}\nTap here to open the hunt"
                            },
                            color = Ink, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        if (BuildConfig.DEBUG && selectedRelic == null) {
            DebugChallengeLauncher(
                relics = resolvedTreasures,
                onLaunch = { relic ->
                    debugSimulationEnabled = true
                    challengeRelic = relic
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
            )
        }

        AnimatedContent(
            targetState = selectedRelic,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            transitionSpec = {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "map_treasure_header",
        ) { relic ->
            if (relic != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                ) {
                    TreasurePeekHeader(
                        relic = relic,
                        distance = locationOutput.distanceToTargetMeters.formatDistance(),
                        isFound = relic.id in foundRelicIds,
                        expanded = false,
                        onChevron = { detailRelic = relic },
                    )
                }
            }
        }

    }
}

@Composable
private fun TeamHuntArrivalDialog(
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    val glow = rememberInfiniteTransition(label = "team_hunt_lightbulb")
    val glowScale by glow.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "team_hunt_lightbulb_glow",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.WbIncandescent,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(54.dp).graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                    shadowElevation = 18f
                },
            )
        },
        title = {
            Text(
                "Location found!",
                color = Ink,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Text(
                if (isOwner) {
                    "Congratulations, you have found the location. Next, complete the following task to unlock the treasure!"
                } else {
                    "Congratulations, you have found the location. Next, answer the following questions to unlock the treasure!"
                },
                color = Muted,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(if (isOwner) "Start task" else "Answer questions")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Later") }
        },
    )
}

/** Member-only team-hunt checkpoint. Replace the template prompts with destination content later. */
@Composable
private fun MemberHuntQuiz(
    relic: MapRelic,
    locationOutput: LocationOutput,
    onCompleted: () -> Unit,
    onBack: () -> Unit,
) {
    var questionIndex by remember(relic.id) { mutableIntStateOf(0) }
    var incorrect by remember(relic.id) { mutableStateOf(false) }
    val closeEnough = locationOutput.distanceToTargetMeters?.let { it <= relic.insideRadiusMeters } == true
    val questions = listOf(
        "Template Q1",
        "Template Q2",
    )
    val answers = listOf(1, 2) // Temporary answer key: Q1=B, Q2=C.
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Team hunt checkpoint", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
        Text(relic.name, color = Brand, fontWeight = FontWeight.Medium)
        if (!closeEnough) {
            Text("Reach the treasure location to unlock your two questions.", color = Muted)
            locationOutput.distanceToTargetMeters?.let { Text("${it.formatDistance()} away", color = Ink) }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to map") }
        } else {
            Text("Question ${questionIndex + 1} of 2", color = Muted)
            Text(questions[questionIndex], style = MaterialTheme.typography.titleLarge, color = Ink)
            listOf("A", "B", "C", "D").forEachIndexed { index, option ->
                OutlinedButton(onClick = {
                    if (index == answers[questionIndex]) {
                        incorrect = false
                        if (questionIndex == questions.lastIndex) onCompleted() else questionIndex += 1
                    } else {
                        incorrect = true
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(option)
                }
            }
            if (incorrect) Text("Not quite — try again.", color = RelicRed)
        }
    }
}

private val DEBUG_SENSOR_CHALLENGES = setOf(
    RelicChallengeType.UNION_LAWN_PHOTO,
    RelicChallengeType.WILSON_HALL_OBSERVATION,
    RelicChallengeType.OLD_QUAD_EXCAVATION,
    RelicChallengeType.SOUTH_LAWN_VIEWING_ANGLE,
    // Use System Garden for the local test flow; unlike Grainger Museum, it never requests audio.
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE,
)

@Composable
private fun DebugChallengeLauncher(relics: List<MapRelic>, onLaunch: (MapRelic) -> Unit, modifier: Modifier = Modifier) {
    val challenges = relics.filter { it.challengeConfig?.type in DEBUG_SENSOR_CHALLENGES }
    if (challenges.isEmpty()) return
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("DEBUG · Challenge simulator", color = Brand, fontWeight = FontWeight.Bold)
            challenges.forEach { relic ->
                OutlinedButton(onClick = { onLaunch(relic) }, modifier = Modifier.fillMaxWidth()) {
                    Text(relic.locationName)
                }
            }
        }
    }
}

internal fun saveRelicDiscovery(
    relic: MapRelic,
    onCollectTreasure: (String, (String?) -> Unit) -> Unit,
    onComplete: (String?) -> Unit,
) = onCollectTreasure(relic.id, onComplete)

@Composable
private fun GoogleMapView(
    relics: List<MapRelic>,
    selectedRelic: MapRelic?,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    markerPulse: Float = 1f,
    activeHuntTreasureId: String? = null,
    focusSelectedRelic: Boolean = true,
    onRelicSelected: (MapRelic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }
    var renderedMapKey by remember { mutableStateOf("__unrendered__") }
    var cameraInitialised by remember { mutableStateOf(false) }
    var cameraSelectedRelicId by remember { mutableStateOf<String?>(null) }
    var cameraRelicSignature by remember { mutableStateOf("") }
    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var renderedRelicMarkers by remember { mutableStateOf<Map<String, Marker>>(emptyMap()) }
    var renderedPulseBucket by remember { mutableIntStateOf(-1) }
    var mapStyleConfigured by remember { mutableStateOf(false) }
    MapLifecycle(mapView)

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map ->
                if (!mapStyleConfigured) {
                    map.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_retro),
                    )
                    mapStyleConfigured = true
                }
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isCompassEnabled = true
                map.isBuildingsEnabled = true
                map.setOnMarkerClickListener { marker ->
                    relics.firstOrNull { it.id == marker.tag }?.let {
                        onRelicSelected(it)
                        true
                    } ?: false
                }
                // Google Maps draws its blue location layer above overlapping treasure markers,
                // which made an arrived explorer unable to tap their active hunt target. Our
                // own marker is rendered below whenever we have a usable reading, so avoid the
                // native layer and keep the target marker tappable.
                map.isMyLocationEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                val selectedRelicId = selectedRelic?.id ?: "__none__"
                val relicSignature = relics.joinToString("|") { relic ->
                    "${relic.id}:${relic.coordinate.latitude}:${relic.coordinate.longitude}"
                }
                val mapKey = "$selectedRelicId|$activeHuntTreasureId|$relicSignature"
                if (renderedMapKey != mapKey) {
                    map.clear()
                    currentLocationMarker = null
                    renderedRelicMarkers = renderRelics(context, map, relics, selectedRelic, activeHuntTreasureId, markerPulse)
                    renderedMapKey = mapKey
                }
                val pulseBucket = (markerPulse * 20).toInt()
                if (renderedPulseBucket != pulseBucket) {
                    val pulseIcon = questMarkerIcon(context, selected = false, pulseScale = markerPulse)
                    renderedRelicMarkers.forEach { (relicId, marker) ->
                        when {
                            relicId == activeHuntTreasureId -> marker.setIcon(huntMarkerIcon(context, markerPulse))
                            relicId != selectedRelic?.id -> marker.setIcon(pulseIcon)
                        }
                    }
                    renderedPulseBucket = pulseBucket
                }
                val displayLocation = locationOutput.currentLocation ?: locationOutput.lastKnownLocation
                val isStaleDisplay = locationOutput.currentLocation == null && locationOutput.lastKnownLocation != null
                currentLocationMarker = renderCurrentLocation(
                    context = context,
                    map = map,
                    currentLocationMarker = currentLocationMarker,
                    currentLocation = displayLocation,
                    headingDegrees = deviceHeading,
                    stale = isStaleDisplay,
                )
                if (!cameraInitialised) {
                    if (focusSelectedRelic && selectedRelic != null) {
                        moveCameraToRelic(map, selectedRelic)
                    } else {
                        moveCameraToCampus(map, relics, displayLocation)
                    }
                    cameraInitialised = true
                    cameraSelectedRelicId = if (focusSelectedRelic) selectedRelic?.id else null
                    cameraRelicSignature = relicSignature
                } else if (!focusSelectedRelic && relics.isNotEmpty() && cameraRelicSignature != relicSignature) {
                    moveCameraToCampus(map, relics, displayLocation)
                    cameraRelicSignature = relicSignature
                } else if (
                    focusSelectedRelic &&
                    selectedRelic != null &&
                    (cameraSelectedRelicId != selectedRelic.id || cameraRelicSignature != relicSignature)
                ) {
                    moveCameraToRelic(map, selectedRelic)
                    cameraSelectedRelicId = selectedRelic.id
                    cameraRelicSignature = relicSignature
                }
            }
        },
    )
}

@Composable
private fun FindTreasurePrompt(
    message: String,
    loading: Boolean,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp, color = Brand)
            Text(message, modifier = Modifier.weight(1f), color = if (onRetry == null) Ink else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            onRetry?.let { retry -> Button(onClick = retry) { Text("Retry") } }
        }
    }
}

/** Shown when GPS has gone stale/inaccurate and the dot on the map is a last-known fallback, not a live fix. */
@Composable
private fun StaleLocationBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF4D6),
        shadowElevation = 2.dp,
    ) {
        Text(
            "Weak signal — showing last known location",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun LocationPermissionPrompt(onPermissionGranted: () -> Unit, modifier: Modifier = Modifier) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) onPermissionGranted()
    }
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Turn on location to see how close you are to each relic.",
                color = Ink,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null)
                Text(" Enable location")
            }
        }
    }
}

/** These relics keep the local scan-and-dig hunt panel instead of the sensor-driven challenge screen. */
private val LOCAL_HUNT_CHALLENGE_TYPES = setOf(
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE,
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL,
)

/**
 * Within [LOCAL_HUNT_CHALLENGE_TYPES], these additionally drive the scan panel's "ready" state from
 * the same [TreasureChallengeViewModel] pipeline the sensor challenge screen uses, instead of the
 * fixed 4.5s timer - the visual panel is unchanged, only what decides completion.
 */
private val REAL_SENSOR_HUNT_TYPES = setOf(
    RelicChallengeType.SYSTEM_GARDEN_GLASSHOUSE,
    RelicChallengeType.GRAINGER_MUSEUM_TONE_TOOL,
)

@Composable
private fun TreasureDetailScreen(
    relic: MapRelic,
    locationOutput: LocationOutput,
    locationValidity: com.comp90018.app.sensors.SensorValidity,
    hapticsEnabled: Boolean,
    deviceHeading: Float,
    isFound: Boolean,
    collecting: Boolean,
    preciseLocationEnabled: Boolean,
    onCollected: ((String?) -> Unit) -> Unit,
    onStartChallenge: (() -> Unit)?,
    forceReadyToDig: Boolean = false,
    onTeamTaskFinished: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    var navigating by remember(relic.id) { mutableStateOf(false) }
    var nearbyNotified by remember(relic.id) { mutableStateOf(false) }
    var huntReadyNotified by remember(relic.id) { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    val proximityStage = HuntProximityResolver.resolve(
        locationOutput.distanceToTargetMeters,
        locationValidity,
        relic.radarRadiusMeters,
        relic.insideRadiusMeters,
    )
    LaunchedEffect(relic.id, navigating, proximityStage) {
        if (!navigating) return@LaunchedEffect
        when (proximityStage) {
            HuntProximityStage.HUNT_READY -> if (!huntReadyNotified) {
                huntReadyNotified = true
                if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            HuntProximityStage.NEARBY -> if (!nearbyNotified) {
                nearbyNotified = true
                if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            else -> Unit
        }
    }
    var stage by remember(relic.id, forceReadyToDig) { mutableStateOf(if (forceReadyToDig) TreasureHuntStage.READY_TO_DIG else TreasureHuntStage.DETAILS) }
    var searchStep by remember(relic.id) { mutableIntStateOf(0) }
    var detailsVisible by remember(relic.id) { mutableStateOf(false) }
    var collectionActionError by remember(relic.id) { mutableStateOf<String?>(null) }
    val realSensorHuntConfig = relic.challengeConfig?.takeIf { it.type in REAL_SENSOR_HUNT_TYPES }

    LaunchedEffect(relic.id) {
        detailsVisible = true
    }

    LaunchedEffect(stage, realSensorHuntConfig) {
        if (stage == TreasureHuntStage.SEARCHING && realSensorHuntConfig == null) {
            searchStep = 0
            delay(1_500)
            searchStep = 1
            delay(1_500)
            searchStep = 2
            delay(1_500)
            stage = TreasureHuntStage.READY_TO_DIG
        }
    }
    LaunchedEffect(stage) {
        if (stage == TreasureHuntStage.READY_TO_DIG && !forceReadyToDig) onTeamTaskFinished?.invoke()
    }

    if (stage == TreasureHuntStage.FOUND) {
        TreasureFoundPanel(relic = relic, onViewStory = { stage = TreasureHuntStage.STORY })
        return
    }
    if (stage == TreasureHuntStage.STORY) {
        TreasureStoryPanel(
            relic = relic,
            collecting = collecting,
            collectionError = collectionActionError,
            onPutInBackpack = {
                collectionActionError = null
                onCollected { error ->
                    collectionActionError = error
                    if (error == null) stage = TreasureHuntStage.DETAILS
                }
            },
        )
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        GoogleMapView(
            relics = listOf(relic),
            selectedRelic = relic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            markerPulse = 1f,
            focusSelectedRelic = true,
            onRelicSelected = {},
            modifier = Modifier.fillMaxSize(),
        )
        val sheetHeight = (maxHeight - 230.dp).coerceAtLeast(440.dp)
        AnimatedVisibility(
            visible = detailsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(sheetHeight),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color.White,
                shadowElevation = 12.dp,
            ) {
                AnimatedContent(
                    targetState = stage,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith
                            (slideOutVertically { it } + fadeOut())
                    },
                    label = "treasure_hunt_stage",
                ) { activeStage ->
                    when (activeStage) {
                        TreasureHuntStage.DETAILS -> Column(Modifier.fillMaxSize()) {
                            TreasurePeekHeader(
                                relic = relic,
                                distance = locationOutput.distanceToTargetMeters.formatDistance(),
                                isFound = isFound,
                                expanded = true,
                                onChevron = onBack,
                            )
                            TreasureInformationPanel(
                                relic = relic,
                                locationOutput = locationOutput,
                                isFound = isFound,
                                navigating = navigating,
                                proximityStage = proximityStage,
                                hasPreciseLocation = locationOutput.permission.canUnlockTreasure,
                                onNavigate = { navigating = true },
                                onArrived = onStartChallenge ?: if (relic.challengeConfig?.type in LOCAL_HUNT_CHALLENGE_TYPES) {
                                    { stage = TreasureHuntStage.SEARCHING }
                                } else null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TreasureHuntStage.SEARCHING -> if (realSensorHuntConfig != null) {
                            RealSensorHuntPanel(
                                config = realSensorHuntConfig,
                                deviceHeading = deviceHeading,
                                preciseLocationEnabled = preciseLocationEnabled,
                                onReady = { stage = TreasureHuntStage.READY_TO_DIG },
                                onBack = { stage = TreasureHuntStage.DETAILS },
                            )
                        } else {
                            TreasureSearchPanel(
                                searchStep = searchStep,
                                readyToDig = false,
                                deviceHeading = deviceHeading,
                                onDig = {},
                                onBack = { stage = TreasureHuntStage.DETAILS },
                            )
                        }
                        TreasureHuntStage.READY_TO_DIG -> TreasureSearchPanel(
                            searchStep = searchStep,
                            readyToDig = true,
                            deviceHeading = deviceHeading,
                            onDig = {
                                stage = TreasureHuntStage.FOUND
                            },
                            onBack = { stage = TreasureHuntStage.DETAILS },
                        )
                        TreasureHuntStage.FOUND, TreasureHuntStage.STORY -> Unit
                    }
                }
            }
        }
    }
}

private enum class TreasureHuntStage { DETAILS, SEARCHING, READY_TO_DIG, FOUND, STORY }

@Composable
private fun TreasurePeekHeader(
    relic: MapRelic,
    distance: String,
    isFound: Boolean,
    expanded: Boolean,
    onChevron: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(76.dp).background(BrandSoft, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            TreasureSilhouette(relic, discovered = isFound, modifier = Modifier.size(66.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                relic.name,
                color = Ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, tint = RelicRed, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${relic.locationName} · $distance",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                )
            }
        }
        IconButton(onClick = onChevron) {
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                if (expanded) "Collapse treasure details" else "Open treasure details",
                tint = Brand,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun TreasureInformationPanel(
    relic: MapRelic,
    locationOutput: LocationOutput,
    isFound: Boolean,
    navigating: Boolean,
    proximityStage: HuntProximityStage,
    hasPreciseLocation: Boolean,
    onNavigate: () -> Unit,
    onArrived: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val huntStatus = when {
        isFound -> "Discovered"
        onArrived == null -> "Not configured"
        proximityStage == HuntProximityStage.HUNT_READY -> "Search area"
        proximityStage == HuntProximityStage.NEARBY -> "Nearby"
        else -> "Locked"
    }
    val statusColor = when (huntStatus) {
        "Discovered" -> Color(0xFF3F7D4A)
        "Nearby", "Search area" -> Color(0xFFD07B2D)
        else -> Color(0xFF76665B)
    }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Navigation is optional guidance, not a prerequisite for beginning a hunt. Requiring
            // its button to be tapped left an explorer at the destination with every action
            // appearing disabled.
            Button(onClick = { onArrived?.invoke() },
                enabled = onArrived != null && hasPreciseLocation && navigating && proximityStage == HuntProximityStage.HUNT_READY,
                modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Image(painterResource(R.drawable.map_arrived_symbol), null, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(6.dp))
                Text("Start Hunt")
            }
            OutlinedButton(onClick = onNavigate, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Image(painterResource(R.drawable.nav_map_symbol), null, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (navigating) "Navigating…" else "Navigate")
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 14.dp, end = 14.dp, top = 14.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color(0xFFE9D7BD),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(relic.description, color = Ink, style = MaterialTheme.typography.bodyLarge, lineHeight = 23.sp)
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HuntFactCard(
                            title = "Treasure type",
                            value = relic.treasureTypeLabel.ifBlank { "Treasure" },
                            color = Brand,
                            modifier = Modifier.weight(1f),
                        )
                        HuntFactCard(
                            title = "Trail status",
                            value = huntStatus,
                            color = statusColor,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                item { DetailTextRow("Landmark", relic.locationName) }
                item { DetailTextRow("Distance from your trail", locationOutput.distanceToTargetMeters.formatDistance()) }
                item {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1C9)),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Clue", color = Brand, style = MaterialTheme.typography.titleMedium)
                            Text(relic.clue, color = Ink, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                relic.buildingStory.takeIf { it.isNotBlank() }?.let { story ->
                    item { DetailTextRow("Landmark story", story) }
                }
                relic.prototypeDesign.takeIf { it.isNotBlank() }?.let { design ->
                    item { DetailTextRow("Relic design", design) }
                }
                relic.coordinateSource.takeIf { it.isNotBlank() }?.let { source ->
                    item { DetailTextRow("Location source", source) }
                }
                if (navigating) item {
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandSoft),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(if (onArrived == null) "Challenge not configured" else if (!hasPreciseLocation) "Precise location required" else when (proximityStage) {
                                HuntProximityStage.HUNT_READY -> "You've reached the search area"
                                HuntProximityStage.NEARBY -> "Treasure signal detected nearby"
                                HuntProximityStage.UNKNOWN -> "Waiting for a usable location"
                                HuntProximityStage.FAR -> "The trail is awake"
                            }, color = Brand, style = MaterialTheme.typography.titleMedium)
                            Text(if (onArrived == null) "This treasure has no challenge configuration in Firestore yet." else if (!hasPreciseLocation) "Switch location permission to Precise before starting the hunt." else when (proximityStage) {
                                HuntProximityStage.HUNT_READY -> "Start Hunt when you're ready."
                                HuntProximityStage.NEARBY -> "Keep moving toward ${relic.locationName} to reach the search area."
                                HuntProximityStage.UNKNOWN -> "Check location access and wait for a current position."
                                HuntProximityStage.FAR -> "Follow the map glow toward ${relic.locationName}; the relic will call louder as you approach."
                            }, color = Ink)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HuntFactCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.13f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = color, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun TreasureSearchPanel(
    searchStep: Int,
    readyToDig: Boolean,
    deviceHeading: Float,
    onDig: () -> Unit,
    onBack: () -> Unit,
) {
    val phase = searchStep.coerceIn(0, 2)
    val motion = rememberDeviceMotion()
    val command = when {
        readyToDig -> "Signal captured — the relic is beneath your feet."
        phase == 0 -> "Freeze the trail — stop and steady your stance."
        phase == 1 -> "Balance the relic lens — hold your phone level."
        else -> "Sweep the horizon — rotate slowly until the signal blooms."
    }
    val hint = if (readyToDig) "The hidden lock has opened. Dig when your team is ready."
    else "Calm motion, centre the level spark, then turn with the compass glow."
    HuntScanPanel(
        headline = command,
        hintText = hint,
        ready = readyToDig,
        acceleration = motion.accelerationMagnitude,
        levelTilt = motion.tiltDegrees,
        heading = deviceHeading,
        primaryActionLabel = "Dig for treasure",
        onPrimaryAction = onDig,
        onBack = onBack,
    )
}

/**
 * Drives [HuntScanPanel] from the same [TreasureChallengeViewModel] pipeline the sensor challenge
 * screen uses, so the "ready to dig" transition reflects real device motion/orientation instead of a
 * fixed timer. Only used for relic types in [REAL_SENSOR_HUNT_TYPES]; the visual panel is identical
 * to [TreasureSearchPanel].
 */
@Composable
private fun RealSensorHuntPanel(
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

/**
 * Reusable scan-and-dig hunt visual: an instruction headline, the [RelicScannerVisual] gauge (or
 * [AnimatedTreasureChest] once [ready]), a hint card, and Dig/Back actions. Callers own what decides
 * [ready] and what data feeds the gauge, so this can back either a scripted or sensor-driven hunt.
 */
@Composable
fun HuntScanPanel(
    headline: String,
    hintText: String,
    ready: Boolean,
    acceleration: Float,
    levelTilt: Float,
    heading: Float,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    soundProgress: Float? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                headline,
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            if (ready) {
                AnimatedTreasureChest(Modifier.size(150.dp))
            } else {
                RelicScannerVisual(
                    acceleration = acceleration,
                    levelTilt = levelTilt,
                    heading = heading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSoft),
            ) {
                if (soundProgress != null) {
                    Column(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Blow into the mic - the bar fills while you're loud enough.",
                            color = Ink,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        LinearProgressIndicator(
                            progress = { soundProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Brand,
                            trackColor = Color.White,
                        )
                    }
                } else {
                    Text(
                        hintText,
                        modifier = Modifier.padding(16.dp),
                        color = Ink,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (ready) {
            item {
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Image(painterResource(R.drawable.nav_treasure_symbol), null, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(primaryActionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Back", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RelicScannerVisual(
    acceleration: Float,
    levelTilt: Float,
    heading: Float,
    modifier: Modifier = Modifier,
) {
    val animatedHeading by animateFloatAsState(heading, tween(700), label = "scanner_heading")
    val animatedTilt by animateFloatAsState(levelTilt, tween(700), label = "scanner_level")
    val animatedMotion by animateFloatAsState(acceleration, tween(700), label = "scanner_motion")
    Card(
        modifier = modifier.height(236.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4B3024)),
    ) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val centre = Offset(size.width / 2f, size.height * 0.43f)
                val radius = size.minDimension * 0.31f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF8A5A32), Color(0xFF342319)),
                        center = centre,
                        radius = radius * 1.2f,
                    ),
                    radius = radius,
                    center = centre,
                )
                drawCircle(Color(0xFFE9B34F), radius, centre, style = Stroke(width = 5f))
                repeat(12) { index ->
                    val angle = Math.toRadians((index * 30.0) - 90.0)
                    val outer = Offset(
                        centre.x + kotlin.math.cos(angle).toFloat() * radius,
                        centre.y + kotlin.math.sin(angle).toFloat() * radius,
                    )
                    val inner = Offset(
                        centre.x + kotlin.math.cos(angle).toFloat() * radius * 0.86f,
                        centre.y + kotlin.math.sin(angle).toFloat() * radius * 0.86f,
                    )
                    drawLine(Color(0xFFFFE3A0), inner, outer, strokeWidth = if (index % 3 == 0) 5f else 2f)
                }
                val needleAngle = Math.toRadians(animatedHeading.toDouble() - 90.0)
                val needleTip = Offset(
                    centre.x + kotlin.math.cos(needleAngle).toFloat() * radius * 0.72f,
                    centre.y + kotlin.math.sin(needleAngle).toFloat() * radius * 0.72f,
                )
                drawLine(Color(0xFFF25B45), centre, needleTip, strokeWidth = 10f)
                drawCircle(Color.White, radius = 9f, center = centre)

                val levelWidth = radius * 1.15f
                val levelY = centre.y + radius * 0.48f
                drawLine(Color.White.copy(alpha = 0.38f), Offset(centre.x - levelWidth / 2f, levelY), Offset(centre.x + levelWidth / 2f, levelY), strokeWidth = 5f)
                val bubbleOffset = (animatedTilt / 15f).coerceIn(-1f, 1f) * levelWidth * 0.42f
                drawCircle(Color(0xFF72D49B), radius = 10f, center = Offset(centre.x + bubbleOffset, levelY))

                val motionFraction = (animatedMotion / 1.5f).coerceIn(0f, 1f)
                drawArc(
                    color = Color(0xFFF2B544),
                    startAngle = 145f,
                    sweepAngle = 250f * motionFraction,
                    useCenter = false,
                    topLeft = Offset(centre.x - radius * 0.78f, centre.y - radius * 0.78f),
                    size = androidx.compose.ui.geometry.Size(radius * 1.56f, radius * 1.56f),
                    style = Stroke(width = 8f),
                )
            }
            Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 5.dp), color = Color.White)
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ScannerReading("MOTION", String.format(Locale.US, "%.2f m/s²", animatedMotion))
                ScannerReading("LEVEL", String.format(Locale.US, "%.1f°", animatedTilt))
                ScannerReading("HEADING", String.format(Locale.US, "%.0f°", animatedHeading))
            }
        }
    }
}

@Composable
private fun ScannerReading(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFFE9B34F), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AnimatedTreasureChest(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "treasure_chest")
    val lift by transition.animateFloat(
        initialValue = 0f,
        targetValue = -16f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "treasure_chest_lift",
    )
    val tilt by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "treasure_chest_tilt",
    )
    Image(
        painter = painterResource(R.drawable.treasure_chest_symbol),
        contentDescription = "Treasure chest found",
        modifier = modifier.graphicsLayer {
            translationY = lift
            rotationZ = tilt
        },
    )
}

@Composable
private fun RadarAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "treasure_radar")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing)),
        label = "radar_rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "radar_pulse",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val line = size.minDimension * 0.022f
            drawCircle(BrandSoft)
            drawCircle(Brand.copy(alpha = 0.24f), radius = size.minDimension * 0.36f, style = Stroke(line))
            drawCircle(Brand.copy(alpha = 0.34f), radius = size.minDimension * 0.22f, style = Stroke(line))
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
            drawArc(
                color = Brand.copy(alpha = 0.62f),
                startAngle = -90f,
                sweepAngle = 72f,
                useCenter = true,
                size = size,
            )
        }
        Image(
            painter = painterResource(R.drawable.map_quest_selected),
            contentDescription = "Searching radar",
            modifier = Modifier.size(72.dp).graphicsLayer { scaleX = pulse; scaleY = pulse },
        )
    }
}

@Composable
private fun TreasureArtwork(relic: MapRelic, discovered: Boolean, modifier: Modifier = Modifier) {
    TreasurePrototypeImage(relic = relic, discovered = discovered, modifier = modifier)
}

@Composable
private fun TreasureSilhouette(relic: MapRelic, discovered: Boolean, modifier: Modifier = Modifier) {
    TreasurePrototypeImage(relic = relic, discovered = discovered, modifier = modifier)
}

@Composable
private fun TreasureFoundPanel(relic: MapRelic, onViewStory: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF4E5CF)).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TreasureArtwork(relic, discovered = true, modifier = Modifier.size(270.dp))
        Spacer(Modifier.height(22.dp))
        Text(
            relic.name,
            color = Ink,
            fontFamily = GothicTreasureFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            lineHeight = 42.sp,
        )
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = onViewStory,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("View", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun TreasureStoryPanel(
    relic: MapRelic,
    collecting: Boolean,
    collectionError: String?,
    onPutInBackpack: (() -> Unit)?,
    onBackToReveal: (() -> Unit)? = null,
    onReturnToMap: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFF8F0E4)),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (onBackToReveal != null) {
            item {
                OutlinedButton(onClick = onBackToReveal, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to reveal")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TreasureArtwork(relic, discovered = true, modifier = Modifier.size(82.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(relic.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    Text(relic.locationName, color = Muted)
                }
            }
        }
        if (relic.story.isNotBlank()) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSoft),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("The story", color = Brand, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(relic.story, color = Ink, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (relic.historicalImageUrl.isNotBlank()) {
            item {
                Card(
                    Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSoft),
                ) {
                    RemoteTreasureImage(
                        imageUrl = relic.historicalImageUrl,
                        contentDescription = "Historical reference for ${relic.name}",
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                    )
                }
            }
        }
        relic.historicalImageCredit.takeIf { it.isNotBlank() }?.let { credit ->
            item { DetailTextRow("Image credit", credit) }
        }
        relic.sourceTitle.takeIf { it.isNotBlank() }?.let { source ->
            item { DetailTextRow("Source", source) }
        }
        relic.sourceUrl.takeIf { it.startsWith("https://") }?.let { sourceUrl ->
            item {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Open historical source")
                }
            }
        }
        item { DetailTextRow("Found at", relic.locationName) }
        relic.buildingStory.takeIf { it.isNotBlank() }?.let { buildingStory ->
            item { DetailTextRow("About the landmark", buildingStory) }
        }
        if (onPutInBackpack != null) item {
            Button(
                onClick = onPutInBackpack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                enabled = !collecting,
            ) {
                if (collecting) {
                    CircularProgressIndicator(Modifier.size(23.dp), strokeWidth = 3.dp, color = Color.White)
                } else {
                    Image(painterResource(R.drawable.nav_treasure_symbol), null, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (collecting) "Saving…" else "Put in backpack", fontWeight = FontWeight.Bold)
            }
        }
        if (onPutInBackpack == null) item {
            Text("Already in your collection", color = Brand, fontWeight = FontWeight.Bold)
            onReturnToMap?.let { returnToMap ->
                Button(onClick = returnToMap, modifier = Modifier.fillMaxWidth()) {
                    Text("Return to map")
                }
            }
        }
        collectionError?.let { message ->
            item {
                Text(
                    message,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun DetailTextRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
    androidx.compose.material3.HorizontalDivider(color = BrandSoft)
}

@Composable
private fun rememberDeviceHeading(): Float {
    val context = LocalContext.current
    var heading by remember { mutableStateOf(0f) }
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        rotationSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return heading
}

private data class DeviceMotionSample(val accelerationMagnitude: Float, val tiltDegrees: Float)

/** Reads the accelerometer to drive [RelicScannerVisual]'s MOTION/LEVEL readout with real device motion. */
@Composable
private fun rememberDeviceMotion(): DeviceMotionSample {
    val context = LocalContext.current
    var motion by remember { mutableStateOf(DeviceMotionSample(0f, 0f)) }
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                val linearAcceleration = kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH)
                val tiltDegrees = if (magnitude > 0f) {
                    Math.toDegrees(kotlin.math.acos((z / magnitude).coerceIn(-1f, 1f)).toDouble()).toFloat()
                } else {
                    0f
                }
                motion = DeviceMotionSample(linearAcceleration, tiltDegrees)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return motion
}

@Composable
private fun MapLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
}

private fun renderRelics(
    context: Context,
    map: GoogleMap,
    relics: List<MapRelic>,
    selectedRelic: MapRelic?,
    activeHuntTreasureId: String?,
    pulseScale: Float,
): Map<String, Marker> {
    val availableIcon = questMarkerIcon(context, selected = false, pulseScale = pulseScale)
    val selectedIcon = questMarkerIcon(context, selected = true)
    val markers = mutableMapOf<String, Marker>()
    relics.forEach { relic ->
        val marker = map.addMarker(
            MarkerOptions()
                .position(relic.coordinate.toLatLng())
                .title(relic.name)
                .snippet(relic.locationName)
                .icon(when {
                    relic.id == activeHuntTreasureId -> huntMarkerIcon(context, pulseScale)
                    relic.id == selectedRelic?.id -> selectedIcon
                    else -> availableIcon
                })
                .anchor(0.5f, 0.5f),
        )
        marker?.tag = relic.id
        marker?.let { markers[relic.id] = it }
    }
    return markers
}

/** The team-selected destination has a gold halo which gently breathes during an active hunt. */
private fun huntMarkerIcon(context: Context, pulseScale: Float): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val size = (52f * pulseScale * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val center = size / 2f
    canvas.drawCircle(center, center, size * 0.48f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(100, 255, 197, 46)
    })
    canvas.drawCircle(center, center, size * 0.33f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(242, 181, 67)
    })
    canvas.drawCircle(center, center, size * 0.17f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
    })
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * [stale] means [currentLocation] is a last-known fallback rather than a live fix (see
 * [com.comp90018.app.sensors.location.LocationOutput.lastKnownLocation]) - the dot is dimmed
 * instead of being removed so the explorer still has a position to go by.
 */
private fun renderCurrentLocation(
    context: Context,
    map: GoogleMap,
    currentLocationMarker: Marker?,
    currentLocation: GeoCoordinate?,
    headingDegrees: Float,
    stale: Boolean,
): Marker? {
    if (currentLocation == null) {
        currentLocationMarker?.remove()
        return null
    }
    val position = currentLocation.toLatLng()
    val alpha = if (stale) STALE_LOCATION_ALPHA else 1f
    if (currentLocationMarker != null) {
        currentLocationMarker.position = position
        currentLocationMarker.rotation = headingDegrees
        currentLocationMarker.alpha = alpha
        currentLocationMarker.title = if (stale) "Last known location" else "You"
        currentLocationMarker.zIndex = CURRENT_LOCATION_Z_INDEX
        return currentLocationMarker
    }
    return map.addMarker(
        MarkerOptions()
            .position(position)
            .title(if (stale) "Last known location" else "You")
            .icon(currentLocationIcon(context))
            .anchor(0.5f, 0.72f)
            .flat(true)
            .rotation(headingDegrees)
            .alpha(alpha)
            // Relics sit at zIndex 0, so when a relic shares the player's exact spot (e.g. an
            // emulator's mock location pinned on top of a relic for testing) the "you are here"
            // dot still renders above it instead of being hidden underneath.
            .zIndex(CURRENT_LOCATION_Z_INDEX),
    )
}

private const val CURRENT_LOCATION_Z_INDEX = 10f

private const val STALE_LOCATION_ALPHA = 0.5f

private fun questMarkerIcon(context: Context, selected: Boolean, pulseScale: Float = 1f): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val baseSize = if (selected) 44f else 30f * pulseScale
    val size = (baseSize * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(214, 48, 38)
        style = Paint.Style.FILL
    }
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.46f, paint)
    canvas.drawRoundRect(
        size * 0.40f,
        size * 0.18f,
        size * 0.60f,
        size * 0.60f,
        size * 0.10f,
        size * 0.10f,
        whitePaint,
    )
    canvas.drawCircle(size * 0.5f, size * 0.76f, size * 0.105f, whitePaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun currentLocationIcon(context: Context): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val size = (68 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val center = size / 2f
    val dotY = size * 0.72f
    val viewPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = RadialGradient(
            center,
            dotY,
            size * 0.72f,
            intArrayOf(
                android.graphics.Color.argb(165, 242, 181, 67),
                android.graphics.Color.argb(70, 242, 181, 67),
                android.graphics.Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.46f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(217, 84, 53)
        style = Paint.Style.FILL
    }
    val viewCone = Path().apply {
        moveTo(center, dotY)
        lineTo(size * 0.08f, size * 0.04f)
        lineTo(size * 0.92f, size * 0.04f)
        close()
    }
    canvas.drawPath(viewCone, viewPaint)
    canvas.drawCircle(center, dotY, size * 0.14f, haloPaint)
    canvas.drawCircle(center, dotY, size * 0.095f, dotPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun moveCameraToCampus(map: GoogleMap, relics: List<MapRelic>, currentLocation: GeoCoordinate?) {
    val points = buildList {
        addAll(relics.map { it.coordinate })
        currentLocation?.let(::add)
    }
    val latitudeSpan = (points.maxOfOrNull { it.latitude } ?: 0.0) - (points.minOfOrNull { it.latitude } ?: 0.0)
    val longitudeSpan = (points.maxOfOrNull { it.longitude } ?: 0.0) - (points.minOfOrNull { it.longitude } ?: 0.0)
    if (points.isNotEmpty() && latitudeSpan < 0.0001 && longitudeSpan < 0.0001) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first().toLatLng(), 17f))
        return
    }
    val bounds = LatLngBounds.builder().apply {
        relics.forEach { include(it.coordinate.toLatLng()) }
        currentLocation?.let { include(it.toLatLng()) }
    }.build()
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
}

private fun moveCameraToRelic(map: GoogleMap, selectedRelic: MapRelic) {
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedRelic.coordinate.toLatLng(), 17f))
}

private fun GeoCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun Double?.formatDistance(): String = when {
    this == null -> "unknown"
    this >= 1000.0 -> String.format(Locale.US, "%.2f km", this / 1000.0)
    else -> "${kotlin.math.round(this / 10.0).toInt() * 10} m"
}

private fun Double?.formatDegrees(): String =
    this?.let { String.format(Locale.US, "%.0f°", it) } ?: "unknown"

private fun ProximityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }
