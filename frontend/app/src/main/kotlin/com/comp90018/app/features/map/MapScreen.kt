package com.comp90018.app.features.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.comp90018.app.features.treasure.treasureArtworkResource
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.WbIncandescent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.MapStyleOptions
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private enum class MapPerspective { GOD, HUNT }

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
    soundEffectsEnabled: Boolean = true,
    onEnableLocation: () -> Unit,
    onCollectTreasure: (String, (String?) -> Unit) -> Unit,
    activeHuntTreasureId: String? = null,
    activeHuntOwnerId: String? = null,
    activeHuntMemberIds: List<String> = emptyList(),
    activeHuntCompletedMemberIds: List<String> = emptyList(),
    activeHuntFoundFragmentIds: List<String> = emptyList(),
    activeHuntClaimedMemberIds: List<String> = emptyList(),
    currentUserId: String = "",
    onCompleteActiveHuntTask: () -> Unit = {},
    onFindActiveHuntFragment: (String) -> Unit = {},
    onClaimCompletedHuntTreasure: () -> Unit = {},
    requestedTreasureId: String? = null,
    onTreasureRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val teamHuntTarget = activeHuntTreasureId?.let { id -> treasures.firstOrNull { it.id == id } }
    val teamHuntActive = teamHuntTarget != null && activeHuntOwnerId != null
    val teamHuntIsOwner = activeHuntOwnerId == currentUserId
    val teamHuntAllCompleted = teamHuntActive && activeHuntMemberIds.size in 2..4 &&
        activeHuntMemberIds.all { it in activeHuntCompletedMemberIds }
    val teamHuntTaskPendingForCurrentUser = TeamHuntTaskEligibility.isPendingFor(
        huntActive = teamHuntActive,
        memberIds = activeHuntMemberIds,
        completedMemberIds = activeHuntCompletedMemberIds,
        currentUserId = currentUserId,
    )
    val isSouthLawnFragmentHunt = teamHuntActive && teamHuntTarget.id == SOUTH_LAWN_ATLAS_ID &&
        SouthLawnFragments.any { it.id !in activeHuntFoundFragmentIds }
    // An active team hunt locks the solo catalogue to its shared target.
    val resolvedTreasures = if (teamHuntActive) listOf(requireNotNull(teamHuntTarget)) else treasures
    var selectedRelic by remember { mutableStateOf<MapRelic?>(null) }
    var detailRelic by remember { mutableStateOf<MapRelic?>(null) }
    var challengeRelic by remember { mutableStateOf<MapRelic?>(null) }
    var huntRevealRelic by remember { mutableStateOf<MapRelic?>(null) }
    var huntStoryVisible by remember { mutableStateOf(false) }
    var compassRelic by remember { mutableStateOf<MapRelic?>(null) }
    var memberQuizRelic by remember { mutableStateOf<MapRelic?>(null) }
    var perspective by remember { mutableStateOf(MapPerspective.GOD) }
    var simulation by remember { mutableStateOf<Double?>(null) }
    // This acknowledgement must survive leaving/re-entering MapScreen (and app restarts).
    // Otherwise a location update can repeatedly reopen the arrival dialog after the explorer
    // has already chosen either action. It is scoped to the signed-in explorer and destination.
    val teamHuntArrivalPreferences = remember(context) {
        context.getSharedPreferences("team_hunt_arrival_prompts", Context.MODE_PRIVATE)
    }
    val teamHuntArrivalPreferenceKey = remember(currentUserId, teamHuntTarget?.id) {
        "seen_${currentUserId.ifBlank { "anonymous" }}_${teamHuntTarget?.id.orEmpty()}"
    }
    var showTeamHuntArrival by remember(teamHuntArrivalPreferenceKey) {
        mutableStateOf(false)
    }
    fun acknowledgeTeamHuntArrival() {
        // Hide this composable first. This is deliberately separate from navigation so the
        // dialog cannot remain over the task screen while the next screen is being composed.
        showTeamHuntArrival = false
        teamHuntArrivalPreferences.edit()
            .putBoolean(teamHuntArrivalPreferenceKey, true)
            .apply()
    }
    var debugSimulationEnabled by remember { mutableStateOf(false) }
    val revealCoordinator = remember { PostChallengeRevealCoordinator() }
    var revealedIds by remember(currentUserId) { mutableStateOf(emptySet<String>()) }
    var returningRelicId by remember { mutableStateOf<String?>(null) }
    val foundRelicIds = discoveredTreasureIds + revealedIds
    fun returnFromReveal(relic: MapRelic) {
        returningRelicId = relic.id
        huntRevealRelic = null
        selectedRelic = null
    }
    val deviceHeading = rememberDeviceHeading()
    val deviceMotion = rememberDeviceMotion()
    val runningInEmulator = remember { isProbablyEmulator() }
    val huntCandidates = remember(resolvedTreasures, foundRelicIds, teamHuntActive) {
        if (teamHuntActive) resolvedTreasures else resolvedTreasures.filterNot { it.id in foundRelicIds }
    }
    val simulationTarget = remember(huntCandidates, teamHuntTarget) {
        teamHuntTarget ?: huntCandidates.minByOrNull {
            LocationCalculator.distanceMeters(DEFAULT_CAMPUS_CENTRE, it.coordinate)
        }
    }
    val simulatedCoordinate = simulation?.let { distance ->
        simulationTarget?.coordinate?.let { coordinateAtDistance(it, distance) }
    }
    val actionableCoordinate = simulatedCoordinate ?: LocationActionPolicy.actionableCoordinate(userLocation)
    val mapDisplayCoordinate = simulatedCoordinate ?: LocationActionPolicy.mapDisplayCoordinate(
        userLocation, runningInEmulator,
    )
    val treasureDistances = remember(huntCandidates, actionableCoordinate, simulation, simulationTarget) {
        val candidates = if (simulation == null || simulationTarget == null) {
            huntCandidates
        } else {
            listOf(simulationTarget)
        }
        actionableCoordinate?.let { current ->
            candidates.map { it to LocationCalculator.distanceMeters(current, it.coordinate) }
                .sortedBy { it.second }
        }.orEmpty()
    }
    val nearestTreasure = treasureDistances.firstOrNull()
    // Map visibility includes recovered treasures; only the hunt candidates exclude them.
    val visibleRelics = remember(resolvedTreasures, mapDisplayCoordinate, foundRelicIds, returningRelicId, perspective) {
        visibleMapRelics(
            resolvedTreasures, mapDisplayCoordinate, REVEAL_RADIUS_METERS, foundRelicIds, returningRelicId,
            showAllRelics = perspective == MapPerspective.GOD,
        )
    }
    val huntReadyRelic = nearestTreasure?.takeIf { (_, distance) -> distance <= HUNT_READY_RADIUS_METERS }?.first
    val discoveryHaptics = LocalHapticFeedback.current
    val discoveryTarget = selectedRelic?.takeIf { relic ->
        actionableCoordinate?.let { LocationCalculator.distanceMeters(it, relic.coordinate) <= HUNT_READY_RADIUS_METERS } == true
    } ?: huntReadyRelic
    LaunchedEffect(discoveryTarget?.id) {
        if (discoveryTarget != null && hapticsEnabled) {
            discoveryHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(160)
            discoveryHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val proximityMessage = remember(nearestTreasure, huntCandidates, actionableCoordinate) {
        when {
            actionableCoordinate == null && huntCandidates.isNotEmpty() ->
                "Waiting for a usable GPS location. Explore the map while waiting; move outdoors to begin hunting."
            huntCandidates.isEmpty() -> "Every campus relic has been recovered — legendary work, explorer!"
            nearestTreasure == null -> "The trail has gone quiet — follow the hint and venture closer!"
            else -> treasureProximityMessage(
                nearestTreasure.second,
                requireNotNull(actionableCoordinate),
                nearestTreasure.first.coordinate,
            )
        }
    }
    val activeRelic = compassRelic ?: detailRelic ?: selectedRelic
    val locationOutput = remember(activeRelic, userLocation, simulatedCoordinate) {
        LocationActionPolicy.targetOutput(
            location = userLocation,
            target = activeRelic?.coordinate,
            config = LocationConfig(
                insideRadiusMeters = activeRelic?.insideRadiusMeters ?: 20.0,
                nearbyRadiusMeters = activeRelic?.radarRadiusMeters ?: 100.0,
            ),
            simulatedCoordinate = simulatedCoordinate,
        )
    }
    val teamHuntLocationOutput = remember(teamHuntTarget, userLocation, simulatedCoordinate) {
        LocationActionPolicy.targetOutput(
            location = userLocation,
            target = teamHuntTarget?.coordinate,
            config = LocationConfig(
                insideRadiusMeters = teamHuntTarget?.insideRadiusMeters ?: 20.0,
                nearbyRadiusMeters = teamHuntTarget?.radarRadiusMeters ?: 100.0,
            ),
            simulatedCoordinate = simulatedCoordinate,
        )
    }
    val isLocationStale = simulation == null && userLocation.permission.isGranted &&
        userLocation.currentLocation == null &&
        userLocation.lastKnownLocation != null
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
        compassRelic = compassRelic?.let { compass -> resolvedTreasures.firstOrNull { it.id == compass.id } }
    }
    LaunchedEffect(visibleRelics) {
        selectedRelic = selectedRelic?.takeIf { selected -> visibleRelics.any { it.id == selected.id } }
    }
    LaunchedEffect(requestedTreasureId, resolvedTreasures) {
        val requestedId = requestedTreasureId ?: return@LaunchedEffect
        resolvedTreasures.firstOrNull { it.id == requestedId }?.let { selectedRelic = it }
        onTreasureRequestConsumed()
    }
    LaunchedEffect(teamHuntAllCompleted, teamHuntTarget?.id, activeHuntClaimedMemberIds, currentUserId) {
        if (teamHuntAllCompleted && currentUserId !in activeHuntClaimedMemberIds) detailRelic = teamHuntTarget
    }
    LaunchedEffect(
        teamHuntTarget?.id,
        teamHuntLocationOutput.distanceToTargetMeters,
        teamHuntLocationOutput.validity,
        teamHuntTaskPendingForCurrentUser,
    ) {
        if (teamHuntTaskPendingForCurrentUser &&
            teamHuntLocationOutput.validity == com.comp90018.app.sensors.SensorValidity.VALID &&
            teamHuntLocationOutput.distanceToTargetMeters?.let { it <= HUNT_READY_RADIUS_METERS } == true &&
            !teamHuntArrivalPreferences.getBoolean(teamHuntArrivalPreferenceKey, false)
        ) {
            showTeamHuntArrival = true
        }
    }

    // South Lawn replaces the original viewing-angle challenge with a shared four-fragment
    // search. This branch happens after the common state/effect setup so the Compose call order
    // remains stable as a room starts or ends a hunt.
    if (isSouthLawnFragmentHunt) {
        SouthLawnFragmentHuntScreen(
            fragments = SouthLawnFragments,
            foundFragmentIds = activeHuntFoundFragmentIds,
            userLocation = userLocation,
            deviceHeading = deviceHeading,
            onFindFragment = onFindActiveHuntFragment,
        )
        return
    }

    if (teamHuntActive && teamHuntTarget.id == SOUTH_LAWN_ATLAS_ID &&
        currentUserId in activeHuntClaimedMemberIds
    ) {
        SouthLawnClaimWaitingScreen(
            userLocation = userLocation,
            deviceHeading = deviceHeading,
        )
        return
    }

    huntRevealRelic?.let { relic ->
        if (huntStoryVisible) {
            TreasureStoryPanel(
                relic = relic,
                collecting = false,
                collectionError = null,
                onPutInBackpack = null,
                onBackToReveal = { huntStoryVisible = false },
                onReturnToMap = { returnFromReveal(relic) },
            )
        } else {
            TreasureDiscoveryReveal(
                relic = relic,
                onViewTreasure = { huntStoryVisible = true },
                onReturnToMap = { returnFromReveal(relic) },
            )
        }
        return
    }

    compassRelic?.let { relic ->
        TreasureCompassGate(
            relic = relic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            motion = deviceMotion,
            hapticsEnabled = hapticsEnabled,
            soundEffectsEnabled = soundEffectsEnabled,
            onSignalFound = { complete ->
                if (teamHuntTaskPendingForCurrentUser) {
                    onCompleteActiveHuntTask()
                    complete(null)
                } else {
                    onCollectTreasure(relic.id) { error ->
                        if (error == null) revealedIds = revealedIds + relic.id
                        complete(error)
                    }
                }
            },
            onReveal = {
                compassRelic = null
                huntStoryVisible = false
                huntRevealRelic = relic
            },
            onBack = {
                compassRelic = null
                selectedRelic = null
                detailRelic = null
            },
        )
        return
    }

    memberQuizRelic?.let { relic ->
        MemberHuntQuiz(
            relic = relic,
            // Reuse the same target and effective coordinate used by the team-hunt arrival
            // check. Re-reading only the live GPS fix here can lock the questions after arrival
            // when the map is using a simulated or last-known coordinate.
            locationOutput = teamHuntLocationOutput,
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
            // Keep the Start Hunt gate on the same target-specific calculation that renders
            // the distance.  Otherwise a displayed 0 m could still resolve to UNKNOWN and
            // leave the button disabled.
            locationValidity = locationOutput.validity,
            hapticsEnabled = hapticsEnabled,
            deviceHeading = deviceHeading,
            // Team hunts remain replayable even when this relic is already in the explorer's
            // personal collection; the shared hunt completion is tracked separately.
            isFound = relic.id in foundRelicIds && !teamHuntActive,
            collecting = savingTreasureId == relic.id,
            preciseLocationEnabled = preciseLocationEnabled,
            onCollected = { onComplete ->
                onCollectTreasure(relic.id) { error ->
                    if (error == null && teamHuntActive && relic.id == teamHuntTarget.id && teamHuntAllCompleted) {
                        onClaimCompletedHuntTreasure()
                    }
                    onComplete(error)
                }
            },
            onStartChallenge = relic.challengeConfig
                ?.takeUnless { it.type in LOCAL_HUNT_CHALLENGE_TYPES }
                ?.let { { debugSimulationEnabled = false; challengeRelic = relic } },
            forceReadyToDig = teamHuntAllCompleted && relic.id == teamHuntTarget.id,
            onTeamTaskFinished = if (teamHuntTaskPendingForCurrentUser && teamHuntIsOwner) onCompleteActiveHuntTask else null,
            onBack = {
                detailRelic = null
            },
        )
        return
    }

    // Keep the prompt in the map branch only. Challenge/detail routes return above, so an
    // arrival prompt can never be left layered over a task screen during navigation.
    if (showTeamHuntArrival) {
        TeamHuntArrivalDialog(
            isOwner = teamHuntIsOwner,
            onDismiss = ::acknowledgeTeamHuntArrival,
            onContinue = {
                val target = requireNotNull(teamHuntTarget)
                acknowledgeTeamHuntArrival()
                // The owner always performs the treasure's normal individual challenge. This
                // keeps the GPS/sensor rules and challenge examples identical in solo and team
                // hunts; team mode only adds the shared completion update on success.
                if (teamHuntIsOwner && target.challengeConfig != null) {
                    compassRelic = target
                } else if (teamHuntIsOwner) {
                    detailRelic = target
                } else {
                    memberQuizRelic = target
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = visibleRelics,
            selectedRelic = selectedRelic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            markerPulse = markerPulse,
            discoveredTreasureIds = foundRelicIds,
            flippingRelicId = returningRelicId,
            onFlipFinished = { returningRelicId = null },
            activeHuntTreasureId = activeHuntTreasureId,
            focusSelectedRelic = false,
            perspective = perspective,
            onRelicSelected = {
                selectedRelic = it
            },
            onMapClick = { selectedRelic = null },
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
            } else {
                FindTreasurePrompt(
                    message = when {
                        loading -> "Loading treasures from Firebase…"
                        error != null -> error
                        resolvedTreasures.isEmpty() -> "No enabled treasures are available right now."
                        else -> proximityMessage
                    },
                    loading = loading,
                    onRetry = onRetry.takeIf { error != null },
                    actionLabel = if (huntReadyRelic != null) "Start Hunting" else null,
                    onAction = {
                        huntReadyRelic?.let { relic ->
                            when {
                                teamHuntTaskPendingForCurrentUser && !teamHuntIsOwner -> memberQuizRelic = relic
                                relic.challengeConfig != null -> compassRelic = relic
                                else -> { huntStoryVisible = false; huntRevealRelic = relic }
                            }
                        }
                    },
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
                            selectedRelic = relic
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = Brand)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            when {
                                teamHuntAllCompleted -> "All explorers are ready — dig the treasure!"
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
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }

        PerspectiveSwitch(
            selected = perspective,
            onSelected = { perspective = it },
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )

        if (BuildConfig.DEBUG) {
            DistanceSimulationControl(
                distance = simulation,
                onDistance = { simulation = it },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (huntReadyRelic == null) 108.dp else 176.dp),
            )
        }

        AnimatedContent(
            targetState = selectedRelic,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
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
                        actionLabel = if (locationOutput.distanceToTargetMeters?.let { it <= HUNT_READY_RADIUS_METERS } == true) {
                            "Start Hunting"
                        } else {
                            null
                        },
                        onAction = {
                            when {
                                teamHuntTaskPendingForCurrentUser && !teamHuntIsOwner -> memberQuizRelic = relic
                                relic.challengeConfig != null -> compassRelic = relic
                                else -> { huntStoryVisible = false; huntRevealRelic = relic }
                            }
                        },
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

private data class MemberHuntQuestion(
    val prompt: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
)

private val MEMBER_HUNT_QUESTIONS = mapOf(
    "union_lawn_lost_lake" to listOf(
        MemberHuntQuestion(
            prompt = "What occupied the site of today's Union Lawn before it became a lawn?",
            options = listOf("A botanical garden", "An ornamental lake", "A railway platform", "A sports oval"),
            correctAnswerIndex = 1,
        ),
        MemberHuntQuestion(
            prompt = "What began the filling of the lake in the 1930s?",
            options = listOf(
                "Construction of the New Chemistry Building",
                "Construction of Wilson Hall",
                "Creation of the System Garden",
                "Construction of the Grainger Museum",
            ),
            correctAnswerIndex = 0,
        ),
    ),
    "wilson_hall_rosette" to listOf(
        MemberHuntQuestion(
            prompt = "When was the original Wilson Hall completed?",
            options = listOf("1856", "1882", "1938", "1956"),
            correctAnswerIndex = 1,
        ),
        MemberHuntQuestion(
            prompt = "What was the original Wilson Hall built to host?",
            options = listOf(
                "Examinations and degree ceremonies",
                "Botanical lectures and plant displays",
                "Student accommodation and dining",
                "Musical instrument workshops",
            ),
            correctAnswerIndex = 0,
        ),
    ),
    "old_quad_fossil" to listOf(
        MemberHuntQuestion(
            prompt = "What do the drawings behind Towards a glass monument depict?",
            options = listOf(
                "Mesozoic ferns fossilised in sandstone",
                "Birds native to the Parkville campus",
                "The original University gardens",
                "Early University buildings",
            ),
            correctAnswerIndex = 0,
        ),
        MemberHuntQuestion(
            prompt = "Which artists created the original fern drawings?",
            options = listOf(
                "Tom Nicholson and Geoffrey Wallace",
                "Arthur Bartholomew and Ludwig Becker",
                "Frederick McCoy and Edward La Trobe Bateman",
                "Percy Grainger and Burnett Cross",
            ),
            correctAnswerIndex = 1,
        ),
    ),
    "system_garden_glasshouse" to listOf(
        MemberHuntQuestion(
            prompt = "In which year was the System Garden established?",
            options = listOf("1826", "1856", "1882", "1916"),
            correctAnswerIndex = 1,
        ),
        MemberHuntQuestion(
            prompt = "What was the surviving central tower originally used as?",
            options = listOf(
                "A classroom for botany students",
                "A potting shed for an octagonal conservatory",
                "A water tower for the campus",
                "An entrance gate to the garden",
            ),
            correctAnswerIndex = 1,
        ),
    ),
    "grainger_tone_tool" to listOf(
        MemberHuntQuestion(
            prompt = "Who made the Kangaroo-pouch tone-tool in 1952?",
            options = listOf(
                "Percy Grainger and Burnett Cross",
                "Percy Grainger and Arthur Bartholomew",
                "Burnett Cross and Ludwig Becker",
                "Frederick McCoy and Edward La Trobe Bateman",
            ),
            correctAnswerIndex = 0,
        ),
        MemberHuntQuestion(
            prompt = "Which components were part of the Kangaroo-pouch tone-tool?",
            options = listOf(
                "Glass lenses and water pipes",
                "Paper music rolls and sine-wave oscillators",
                "Piano keys and brass bells",
                "Stone fragments and wooden flutes",
            ),
            correctAnswerIndex = 1,
        ),
    ),
)

/** Member-only team-hunt checkpoint with history questions for each destination. */
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
    val questions = MEMBER_HUNT_QUESTIONS[relic.id].orEmpty()
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Team hunt checkpoint", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
        Text(relic.name, color = Brand, fontWeight = FontWeight.Medium)
        if (!closeEnough) {
            Text("Reach the treasure location to unlock your two questions.", color = Muted)
            locationOutput.distanceToTargetMeters?.let { Text("${it.formatDistance()} away", color = Ink) }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to map") }
        } else if (questions.isEmpty()) {
            Text("No questions are available for this destination yet.", color = Muted)
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to map") }
        } else {
            Text("Question ${questionIndex + 1} of 2", color = Muted)
            Text(questions[questionIndex].prompt, style = MaterialTheme.typography.titleLarge, color = Ink)
            questions[questionIndex].options.forEachIndexed { index, option ->
                OutlinedButton(onClick = {
                    if (index == questions[questionIndex].correctAnswerIndex) {
                        incorrect = false
                        if (questionIndex == questions.lastIndex) onCompleted() else questionIndex += 1
                    } else {
                        incorrect = true
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${'A' + index}. $option",
                        modifier = Modifier.fillMaxWidth(),
                    )
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
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.96f), shadowElevation = 4.dp) {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Rounded.Star, "Open debug tasks", tint = Brand)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            challenges.forEach { relic ->
                DropdownMenuItem(text = { Text(relic.locationName) }, onClick = { expanded = false; onLaunch(relic) })
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
private fun SouthLawnFragmentHuntScreen(
    fragments: List<TeamHuntFragment>,
    foundFragmentIds: List<String>,
    userLocation: LocationOutput,
    deviceHeading: Float,
    onFindFragment: (String) -> Unit,
) {
    var selectedFragmentId by remember { mutableStateOf<String?>(null) }
    val selectedFragment = fragments.firstOrNull { it.id == selectedFragmentId }
    val currentLocation = LocationActionPolicy.actionableCoordinate(userLocation)
    val selectedDistance = selectedFragment?.let { fragment ->
        currentLocation?.let { LocationCalculator.distanceMeters(it, fragment.coordinate) }
    }
    val canCollect = selectedFragment?.let { fragment ->
        LocationActionPolicy.isWithinRadius(
            location = userLocation,
            target = fragment.coordinate,
            radiusMeters = SOUTH_LAWN_FRAGMENT_RADIUS_METERS,
        )
    } == true

    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = emptyList(),
            selectedRelic = null,
            locationOutput = userLocation,
            deviceHeading = deviceHeading,
            huntFragments = fragments,
            foundFragmentIds = foundFragmentIds.toSet(),
            focusSelectedRelic = false,
            onRelicSelected = {},
            onFragmentSelected = { selectedFragmentId = it.id },
            modifier = Modifier.fillMaxSize(),
        )
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("South Lawn Atlas fragments", color = Ink, fontWeight = FontWeight.Bold)
                Text("${foundFragmentIds.size.coerceAtMost(fragments.size)}/${fragments.size} fragments found", color = Brand)
                fragments.forEach { fragment ->
                    val found = fragment.id in foundFragmentIds
                    Text(
                        "${if (found) "✓" else "○"} ${fragment.title}: ${if (found) "is found" else "not found"}",
                        color = if (found) Brand else Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        selectedFragment?.takeIf { it.id !in foundFragmentIds }?.let { fragment ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fragment.title, color = Ink, fontWeight = FontWeight.Bold)
                    Text(selectedDistance.formatDistance() + " away", color = Muted)
                    Button(
                        onClick = { onFindFragment(fragment.id); selectedFragmentId = null },
                        enabled = canCollect,
                        colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    ) { Text("Collect fragment") }
                    if (!canCollect) Text("Move within 8 m to collect this fragment.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Shown after this explorer has claimed the reconstructed Atlas, until their teammate does too. */
@Composable
private fun SouthLawnClaimWaitingScreen(
    userLocation: LocationOutput,
    deviceHeading: Float,
) {
    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = emptyList(),
            selectedRelic = null,
            locationOutput = userLocation,
            deviceHeading = deviceHeading,
            focusSelectedRelic = false,
            onRelicSelected = {},
            modifier = Modifier.fillMaxSize(),
        )
        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Atlas claimed", color = Ink, fontWeight = FontWeight.Bold)
                Text("Waiting for your teammate to claim their Atlas.", color = Muted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun GoogleMapView(
    relics: List<MapRelic>,
    selectedRelic: MapRelic?,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    markerPulse: Float = 1f,
    discoveredTreasureIds: Set<String> = emptySet(),
    activeHuntTreasureId: String? = null,
    flippingRelicId: String? = null,
    onFlipFinished: () -> Unit = {},
    huntFragments: List<TeamHuntFragment> = emptyList(),
    foundFragmentIds: Set<String> = emptySet(),
    focusSelectedRelic: Boolean = true,
    perspective: MapPerspective = MapPerspective.GOD,
    onRelicSelected: (MapRelic) -> Unit,
    onFragmentSelected: (TeamHuntFragment) -> Unit = {},
    onMapClick: () -> Unit = {},
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
    var cameraPerspective by remember { mutableStateOf(perspective) }
    val flip = remember(flippingRelicId) { Animatable(if (flippingRelicId == null) 1f else 0f) }
    LaunchedEffect(flippingRelicId, renderedRelicMarkers) {
        if (flippingRelicId != null && renderedRelicMarkers.containsKey(flippingRelicId)) {
            flip.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
            onFlipFinished()
        }
    }
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
                map.uiSettings.isCompassEnabled = perspective == MapPerspective.GOD
                map.uiSettings.isScrollGesturesEnabled = perspective == MapPerspective.GOD
                map.uiSettings.isRotateGesturesEnabled = perspective == MapPerspective.GOD
                map.isBuildingsEnabled = true
                map.setOnMarkerClickListener { marker ->
                    relics.firstOrNull { it.id == marker.tag }?.let {
                        onRelicSelected(it)
                        true
                    } ?: huntFragments.firstOrNull { "fragment:${it.id}" == marker.tag }?.let {
                        onFragmentSelected(it)
                        true
                    } ?: false
                }
                map.setOnMapClickListener { onMapClick() }
                // Google Maps draws its blue location layer above overlapping treasure markers,
                // which made an arrived explorer unable to tap their active hunt target. Our
                // own marker is rendered below whenever we have a usable reading, so avoid the
                // native layer and keep the target marker tappable.
                map.isMyLocationEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = false
                val selectedRelicId = selectedRelic?.id ?: "__none__"
                val relicSignature = relics.joinToString("|") { relic ->
                    "${relic.id}:${relic.coordinate.latitude}:${relic.coordinate.longitude}:${relic.artworkKey}:${relic.id in discoveredTreasureIds}"
                }
                val fragmentSignature = huntFragments.joinToString("|") { fragment ->
                    "${fragment.id}:${fragment.coordinate.latitude}:${fragment.coordinate.longitude}:${fragment.id in foundFragmentIds}"
                }
                val mapKey = "$selectedRelicId|$activeHuntTreasureId|$relicSignature|$fragmentSignature"
                if (renderedMapKey != mapKey) {
                    map.clear()
                    currentLocationMarker = null
                    renderedRelicMarkers = renderRelics(context, map, relics, selectedRelic, activeHuntTreasureId, markerPulse, discoveredTreasureIds)
                    renderHuntFragments(context, map, huntFragments, foundFragmentIds)
                    renderedMapKey = mapKey
                }
                val pulseBucket = (markerPulse * 20).toInt()
                if (huntFragments.isEmpty() && renderedPulseBucket != pulseBucket) {
                    relics.forEach { relic ->
                        renderedRelicMarkers[relic.id]?.setIcon(questMarkerIcon(
                            context, relic, relic.id in discoveredTreasureIds,
                            selected = relic.id == selectedRelic?.id,
                            active = relic.id == activeHuntTreasureId,
                            pulseScale = markerPulse,
                        ))
                    }
                    renderedPulseBucket = pulseBucket
                }
                flippingRelicId?.let { id ->
                    relics.firstOrNull { it.id == id }?.let { relic ->
                        renderedRelicMarkers[id]?.setIcon(questMarkerIcon(
                            context, relic, discovered = flip.value >= 0.5f,
                            selected = relic.id == selectedRelic?.id,
                            active = relic.id == activeHuntTreasureId,
                            flipScale = abs(cos(Math.PI * flip.value).toFloat()).coerceAtLeast(0.025f),
                        ))
                    }
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
                if (cameraPerspective != perspective) {
                    cameraPerspective = perspective
                    cameraInitialised = false
                }
                if (perspective == MapPerspective.HUNT && displayLocation != null) {
                    moveCameraToHuntView(map, displayLocation, deviceHeading)
                    cameraInitialised = true
                } else if (!cameraInitialised) {
                    if (focusSelectedRelic && selectedRelic != null) {
                        moveCameraToRelic(map, selectedRelic)
                    } else {
                        if (huntFragments.isNotEmpty()) {
                            moveCameraToCampus(map, huntFragments.map { it.coordinate }, displayLocation)
                        } else if (relics.isNotEmpty()) {
                            // Frame the treasure catalogue even if the user's GPS is in another city.
                            moveCameraToTreasureArea(map, relics, activeHuntTreasureId)
                        } else {
                            moveCameraToCampus(map, relics.map { it.coordinate }, displayLocation ?: DEFAULT_CAMPUS_CENTRE)
                        }
                    }
                    cameraInitialised = true
                    cameraSelectedRelicId = if (focusSelectedRelic) selectedRelic?.id else null
                    cameraRelicSignature = relicSignature
                } else if (!focusSelectedRelic && relics.isNotEmpty() && cameraRelicSignature != relicSignature) {
                    moveCameraToTreasureArea(map, relics, activeHuntTreasureId)
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
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 3.dp, color = Brand)
                Text(message, modifier = Modifier.weight(1f), color = if (onRetry == null) Ink else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                onRetry?.let { retry -> Button(onClick = retry) { Text("Retry") } }
            }
            actionLabel?.let { label ->
                DiscoveryHuntButton(label, onAction)
            }
        }
    }
}

@Composable
private fun PerspectiveSwitch(
    selected: MapPerspective,
    onSelected: (MapPerspective) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), shadowElevation = 6.dp, color = Color.White.copy(alpha = 0.96f)) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PerspectiveOption("God view", selected == MapPerspective.GOD) { onSelected(MapPerspective.GOD) }
            PerspectiveOption("Hunt view", selected == MapPerspective.HUNT) { onSelected(MapPerspective.HUNT) }
        }
    }
}

@Composable
private fun PerspectiveOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Brand else Color.Transparent,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            color = if (selected) Color.White else Ink,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun DistanceSimulationControl(distance: Double?, onDistance: (Double?) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.width(240.dp), shape = RoundedCornerShape(18.dp), color = Color(0xFFFFF4DE), shadowElevation = 4.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Test distance: ${distance?.let { String.format(Locale.US, "%.1f m", it) } ?: "GPS"}", Modifier.weight(1f), color = Ink, fontSize = 12.sp)
                Switch(checked = distance != null, onCheckedChange = { onDistance(if (it) 100.0 else null) })
            }
            Slider(value = (distance ?: 100.0).toFloat(), onValueChange = { onDistance(it.toDouble()) }, valueRange = 0f..120f)
        }
    }
}

@Composable
private fun DiscoveryHuntButton(label: String, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "discovery")
    val halo by pulse.animateFloat(0.2f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "discovery_halo")
    val reveal = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = reveal, enter = fadeIn(tween(450)) + slideInVertically { it / 2 }) {
        Box(Modifier.fillMaxWidth().height(66.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                repeat(8) { i ->
                    val x = size.width * (i + 0.5f) / 8f
                    val y = if (i % 2 == 0) 5f else size.height - 5f
                    drawLine(Color(0xFFB58A42).copy(alpha = halo), Offset(x - 4f, y), Offset(x + 4f, y), 2f)
                    drawLine(Color(0xFFB58A42).copy(alpha = halo), Offset(x, y - 4f), Offset(x, y + 4f), 2f)
                }
            }
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF382B40)), shape = RoundedCornerShape(12.dp)) {
                Text("✦  $label  ✦", fontFamily = GothicTreasureFontFamily, fontSize = 20.sp, color = Color(0xFFFFE1A0))
            }
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
private fun TreasureCompassGate(
    relic: MapRelic,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    motion: DeviceMotionSample,
    hapticsEnabled: Boolean,
    soundEffectsEnabled: Boolean,
    onSignalFound: ((String?) -> Unit) -> Unit,
    onReveal: () -> Unit,
    onBack: () -> Unit,
) {
    val compassEntrance = remember(relic.id) { Animatable(0f) }
    val lampEntrance = remember(relic.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var leaving by remember(relic.id) { mutableStateOf(false) }
    var saving by remember(relic.id) { mutableStateOf(false) }
    var saveError by remember(relic.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(relic.id) {
        launch { compassEntrance.animateTo(1f, tween(1050, easing = FastOutSlowInEasing)) }
        delay(380)
        lampEntrance.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
    }
    fun revealAfterSave() {
        if (saving || leaving) return
        saving = true
        saveError = null
        onSignalFound { error ->
            saving = false
            saveError = error
            if (error == null) {
                leaving = true
                scope.launch {
                    launch { lampEntrance.animateTo(0f, tween(420)) }
                    compassEntrance.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
                    onReveal()
                }
            }
        }
    }
    var signalLocked by remember(relic.id) { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    var simulateSensors by remember(relic.id) { mutableStateOf(BuildConfig.DEBUG) }
    var testDistance by remember(relic.id) { mutableStateOf((locationOutput.distanceToTargetMeters ?: 9.0).toFloat().coerceIn(0f, 30f)) }
    var testHeading by remember(relic.id) { mutableStateOf(deviceHeading) }
    var testPitch by remember(relic.id) { mutableStateOf(motion.pitchDegrees) }
    var testRoll by remember(relic.id) { mutableStateOf(motion.rollDegrees) }
    val effectiveMotion = if (BuildConfig.DEBUG && simulateSensors) DeviceMotionSample(0f, maxOf(abs(testPitch), abs(testRoll)), testPitch, testRoll) else motion
    val distance = if (BuildConfig.DEBUG && simulateSensors) testDistance.toDouble() else locationOutput.distanceToTargetMeters
    val targetBearing = locationOutput.targetBearingDegrees
    val heading = if (BuildConfig.DEBUG && simulateSensors) testHeading else deviceHeading
    val turnDegrees = targetBearing?.let { signedBearingDifference(it, heading.toDouble()) }
    val readiness = evaluateHuntReadiness(distance, turnDegrees, effectiveMotion.tiltDegrees)
    val nearTreasure = readiness.nearTreasure
    val facingTreasure = readiness.facingTreasure
    val phoneHorizontal = readiness.phoneHorizontal
    val allReady = readiness.allReady

    val proximityGlow = distance?.let { ((30.0 - it) / 20.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    val headingGlow = turnDegrees?.let { ((180.0 - abs(it)) / 165.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    val levelGlow = ((45f - effectiveMotion.tiltDegrees) / 33f).coerceIn(0f, 1f)
    val stars = oracleStarCount(distance)
    val tone = remember { runCatching { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 55) }.getOrNull() }
    DisposableEffect(tone) { onDispose { tone?.release() } }
    var previousStars by remember(relic.id) { mutableIntStateOf(stars) }
    LaunchedEffect(stars, soundEffectsEnabled) {
        val newlyLit = stars - previousStars
        previousStars = stars
        if (soundEffectsEnabled && newlyLit > 0) {
            repeat(newlyLit) {
                tone?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 85)
                delay(110)
            }
        }
    }

    LaunchedEffect(allReady, signalLocked, hapticsEnabled) {
        if (!allReady) signalLocked = false
        if (allReady && !signalLocked) {
            signalLocked = true
            if (hapticsEnabled) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val instruction = when {
        signalLocked -> "Treasure signal locked — the hidden trial has awakened."
        distance == null || targetBearing == null -> "Waiting for a reliable location signal…"
        !nearTreasure -> "The signal faded. Move back within 10 m."
        !facingTreasure && requireNotNull(turnDegrees) > 0 -> "Turn right ${abs(turnDegrees).formatDegrees()} toward the treasure."
        !facingTreasure -> "Turn left ${abs(requireNotNull(turnDegrees)).formatDegrees()} toward the treasure."
        !phoneHorizontal -> "Lower the phone until it is flat and level."
        else -> "Hold it there — locking onto the treasure…"
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5ECD9)).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to map", tint = Ink)
            }
            Text(
                "✦ FOLLOW THE ORACLE'S SIGNAL ✦",
                modifier = Modifier.weight(1f).padding(end = 48.dp),
                color = Brand,
                fontFamily = GothicTreasureFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
        }
        DivineCompassVisual(
            readiness = readiness,
            turnDegrees = turnDegrees?.toFloat() ?: 0f,
            signalAvailable = turnDegrees != null,
            distanceMeters = distance,
            pitchDegrees = effectiveMotion.pitchDegrees,
            rollDegrees = effectiveMotion.rollDegrees,
            signalLocked = signalLocked,
            modifier = Modifier.fillMaxWidth().height(285.dp).graphicsLayer {
                alpha = compassEntrance.value
                scaleX = 0.35f + compassEntrance.value * 0.65f
                scaleY = scaleX
                rotationZ = (1f - compassEntrance.value) * -24f
                translationY = (1f - compassEntrance.value) * 80.dp.toPx()
            },
        )
        Card(
            Modifier.fillMaxWidth().graphicsLayer {
                alpha = lampEntrance.value
                scaleX = 0.7f + lampEntrance.value * 0.3f
                scaleY = scaleX
                translationY = (1f - lampEntrance.value) * 65.dp.toPx()
            },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (signalLocked) BrandSoft else Color(0xFFFFF1C9)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(instruction, modifier = Modifier.fillMaxWidth(), color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OracleLamp("The Trail", "Draw nearer", distance.formatDistance(), nearTreasure, proximityGlow, Color(0xFFBD903D), Modifier.weight(1f))
                    OracleLamp("The Bearing", "Follow the calling", turnDegrees?.let { "${abs(it).formatDegrees()} / 15°" } ?: "Await signal", facingTreasure, headingGlow, Color(0xFFA44736), Modifier.weight(1f))
                    OracleLamp("The Balance", "Still the vessel", "${effectiveMotion.tiltDegrees.toDouble().formatDegrees()} / 12°", phoneHorizontal, levelGlow, Color(0xFF507052), Modifier.weight(1f))
                }
                AnimatedVisibility(visible = allReady, enter = fadeIn(tween(350)) + slideInVertically { it / 2 }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("All lanterns are alight. The relic awaits.", color = Ink, fontFamily = GothicTreasureFontFamily, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        if (saving) CircularProgressIndicator(Modifier.size(24.dp), color = Brand)
                        else if (!leaving) DiscoveryHuntButton("Hunt", ::revealAfterSave)
                    }
                }
            }
        }
        saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        if (BuildConfig.DEBUG) {
            Surface(color = Color(0xFFE8DDC6), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sensor test controls", Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                        Switch(simulateSensors, { simulateSensors = it })
                    }
                    if (simulateSensors) {
                        SensorTestSlider("Distance · GPS", testDistance, 0f..30f, "m", if (nearTreasure) Color(0xFFB58A42) else Color(0xFF8D8984)) { testDistance = it }
                        SensorTestSlider("Heading · rotation", testHeading, 0f..360f, "°", if (facingTreasure) Color(0xFFA44736) else Color(0xFF8D8984)) { testHeading = it }
                        SensorTestSlider("Pitch · accelerometer", testPitch, -45f..45f, "°", if (phoneHorizontal) Color(0xFF507052) else Color(0xFF8D8984)) { testPitch = it }
                        SensorTestSlider("Roll · accelerometer", testRoll, -45f..45f, "°", if (phoneHorizontal) Color(0xFF507052) else Color(0xFF8D8984)) { testRoll = it }
                    }
                }
            }
        }

    }
}

@Composable
private fun SensorTestSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, color: Color, onChange: (Float) -> Unit) {
    Text("$label  ${String.format(Locale.US, "%.1f", value)}$unit", color = Ink, style = MaterialTheme.typography.labelMedium)
    Slider(value = value, onValueChange = onChange, valueRange = range, colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
}

@Composable
private fun DivineCompassVisual(
    readiness: HuntReadiness,
    turnDegrees: Float,
    signalAvailable: Boolean,
    distanceMeters: Double?,
    pitchDegrees: Float,
    rollDegrees: Float,
    signalLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedTurn by animateFloatAsState(turnDegrees, tween(350), label = "treasure_compass_turn")
    val animatedPitch by animateFloatAsState(pitchDegrees.coerceIn(-30f, 30f), tween(220), label = "treasure_compass_pitch")
    val animatedRoll by animateFloatAsState(rollDegrees.coerceIn(-30f, 30f), tween(220), label = "treasure_compass_roll")
    val orbit = rememberInfiniteTransition(label = "oracle_orbit")
    val orbitDegrees by orbit.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "orbit_degrees")
    val shimmer by orbit.animateFloat(0.25f, 0.9f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "oracle_shimmer")
    val parchment = Color(0xFFF5ECD9)
    val plum = Color(0xFF382B40)
    val grey = Color(0xFF8D8984)
    val gold = if (readiness.nearTreasure) Color(0xFFB58A42) else grey
    val rust = if (readiness.facingTreasure) Color(0xFFA44736) else grey
    val compassInk = if (readiness.facingTreasure) plum else Color(0xFF777570)
    val levelColor = if (readiness.phoneHorizontal) Color(0xFF507052) else grey
    val starCount = oracleStarCount(distanceMeters)
    val starFlash = remember { androidx.compose.animation.core.Animatable(0f) }
    var lastStarCount by remember { mutableIntStateOf(starCount) }
    var flashingStars by remember { mutableStateOf(0 until 0) }
    LaunchedEffect(starCount) {
        if (starCount > lastStarCount) {
            flashingStars = lastStarCount until starCount
            lastStarCount = starCount
            starFlash.snapTo(1f)
            starFlash.animateTo(0f, tween(700))
        } else lastStarCount = starCount
    }
    val distanceProgress = distanceMeters?.let { ((30.0 - it) / 20.0).toFloat().coerceIn(0f, 1f) } ?: 0f
    val levelDegrees = maxOf(abs(pitchDegrees), abs(rollDegrees))
    Box(modifier.padding(4.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(bottom = 42.dp)) {
            val centre = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension * 0.37f
            fun point(angle: Double, length: Float): Offset = centre + Offset(cos(angle).toFloat() * length, sin(angle).toFloat() * length)
            drawCircle(compassInk, radius, centre)
            drawCircle(gold, radius * 0.96f, centre, style = Stroke(3.dp.toPx()))
            drawCircle(parchment, radius * 0.86f, centre)
            drawCircle(gold, radius * 0.76f, centre, style = Stroke(1.dp.toPx()))
            repeat(48) { i ->
                val angle = Math.toRadians(i * 7.5 - 90)
                drawLine(compassInk, point(angle, radius * 0.78f), point(angle, radius * if (i % 4 == 0) 0.86f else 0.82f), if (i % 4 == 0) 3f else 1.5f)
            }
            // Eight flat heraldic points form a compass rose beneath the sensor needle.
            repeat(8) { i ->
                val angle = Math.toRadians(i * 45.0 - 90)
                val rose = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centre.x, centre.y)
                    val left = point(angle - 0.22, radius * 0.23f)
                    val tip = point(angle, radius * if (i % 2 == 0) 0.70f else 0.50f)
                    val right = point(angle + 0.22, radius * 0.23f)
                    lineTo(left.x, left.y); lineTo(tip.x, tip.y); lineTo(right.x, right.y); close()
                }
                drawPath(rose, if (i % 2 == 0) gold.copy(alpha = 0.5f) else compassInk.copy(alpha = 0.18f))
            }
            val trackRadius = radius * 1.12f
            drawCircle(Color(0xFFD2CEC5), trackRadius, centre, style = Stroke(18.dp.toPx()))
            drawArc(gold, -90f, 360f * distanceProgress, false,
                Offset(centre.x - trackRadius, centre.y - trackRadius), Size(trackRadius * 2, trackRadius * 2), style = Stroke(18.dp.toPx()))
            repeat(10) { i ->
                val position = point(Math.toRadians(-72.0 + i * 36.0), trackRadius)
                val lit = i < starCount
                val flash = if (i in flashingStars && lit) starFlash.value else 0f
                if (flash > 0f) {
                    drawCircle(Color(0xFFECC86F).copy(alpha = flash * 0.6f), (10f + (1f - flash) * 16f).dp.toPx(), position, style = Stroke(2.dp.toPx()))
                    repeat(6) { ray ->
                        val angle = Math.toRadians(ray * 60.0)
                        val offset = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                        drawLine(Color(0xFFFFD978).copy(alpha = flash), position + offset * 11.dp.toPx(), position + offset * (16f + (1f - flash) * 8f).dp.toPx(), 2.dp.toPx())
                    }
                }
                val star = androidx.compose.ui.graphics.Path().apply {
                    repeat(10) { vertex ->
                        val angle = Math.toRadians(vertex * 36.0 - 90)
                        val r = (if (vertex % 2 == 0) 6f else 2.7f).dp.toPx() * (1f + flash * 0.4f)
                        val x = position.x + cos(angle).toFloat() * r
                        val y = position.y + sin(angle).toFloat() * r
                        if (vertex == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(star, if (lit) Color(0xFFFFE5A0) else Color(0xFF777570))
            }
            rotate(orbitDegrees, centre) {
                repeat(12) { i ->
                    val angle = Math.toRadians(i * 30.0)
                    val position = point(angle, radius * 1.34f)
                    val extent = if (signalLocked) 6.dp.toPx() else 3.dp.toPx()
                    drawLine(gold.copy(alpha = shimmer), position - Offset(extent, 0f), position + Offset(extent, 0f), 2f)
                    drawLine(gold.copy(alpha = shimmer), position - Offset(0f, extent), position + Offset(0f, extent), 2f)
                }
            }
            if (signalAvailable) rotate(animatedTurn, centre) {
                val needle = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centre.x, centre.y - radius * 0.73f)
                    lineTo(centre.x + radius * 0.12f, centre.y)
                    lineTo(centre.x, centre.y + radius * 0.52f)
                    lineTo(centre.x - radius * 0.12f, centre.y); close()
                }
                drawPath(needle, compassInk)
                val tip = androidx.compose.ui.graphics.Path().apply {
                    moveTo(centre.x, centre.y - radius * 0.73f)
                    lineTo(centre.x + radius * 0.12f, centre.y)
                    lineTo(centre.x - radius * 0.12f, centre.y); close()
                }
                drawPath(tip, rust)
            }
            drawCircle(gold, radius * 0.095f, centre)
            drawCircle(plum, radius * 0.045f, centre)
        }
        // A separate inset instrument keeps the tilt gauge clear of the needle and star trail.
        Column(Modifier.align(Alignment.TopEnd).background(parchment, CircleShape).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(76.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val r = size.minDimension * 0.43f
                drawCircle(compassInk, r, c)
                drawCircle(levelColor, r * 0.91f, c, style = Stroke(2.dp.toPx()))
                drawCircle(parchment, r * 0.78f, c)
                drawCircle(levelColor, r * 0.36f, c, style = Stroke(1.dp.toPx()))
                drawLine(levelColor.copy(alpha = 0.5f), c - Offset(r * 0.65f, 0f), c + Offset(r * 0.65f, 0f), 1.dp.toPx())
                drawLine(levelColor.copy(alpha = 0.5f), c - Offset(0f, r * 0.65f), c + Offset(0f, r * 0.65f), 1.dp.toPx())
                val bubble = c + Offset(animatedRoll / 30f, animatedPitch / 30f) * (r * 0.45f)
                drawCircle(levelColor, r * 0.19f, bubble)
                drawCircle(parchment.copy(alpha = 0.8f), r * 0.055f, bubble - Offset(2.dp.toPx(), 2.dp.toPx()))
            }
            Text("LIBELLA", color = levelColor, fontFamily = GothicTreasureFontFamily, fontSize = 11.sp)
        }
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (signalLocked) "✦ SIGNAL AWAKENED ✦" else "${distanceMeters.formatDistance()} · $starCount / 10 stars", color = plum, fontFamily = GothicTreasureFontFamily, fontSize = 18.sp)
            Text("LEVEL ${levelDegrees.toDouble().formatDegrees()}", color = if (levelDegrees <= HORIZONTAL_TOLERANCE_DEGREES) Color(0xFF507052) else rust, fontSize = 10.sp)
        }
    }
}

internal fun oracleStarCount(distanceMeters: Double?): Int = distanceMeters?.takeIf { it.isFinite() }?.let {
    kotlin.math.floor(((30.0 - it) / 20.0).coerceIn(0.0, 1.0) * 10.0).toInt()
} ?: 0

@Composable
private fun OracleLamp(title: String, hint: String, detail: String, lit: Boolean, proximity: Float, color: Color, modifier: Modifier) {
    val glow by animateFloatAsState(if (lit) 1f else proximity * 0.55f, tween(450), label = "lantern_brightness")
    val flameTransition = rememberInfiniteTransition(label = "living_flame")
    val flicker by flameTransition.animateFloat(0.82f, 1.08f, infiniteRepeatable(tween(620), RepeatMode.Reverse), label = "flame_flicker")
    val sway by flameTransition.animateFloat(-2f, 2f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "lantern_sway")
    val burst = remember { Animatable(0f) }
    LaunchedEffect(lit) {
        if (lit) { burst.snapTo(1f); burst.animateTo(0f, tween(950)) } else burst.snapTo(0f)
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(82.dp).graphicsLayer { rotationZ = if (lit) sway else 0f }) {
            val w = size.width
            val h = size.height
            val c = Offset(w / 2f, h * 0.53f)
            val ink = Color(0xFF382B40)
            val brass = Color(0xFFC69A52)
            if (glow > 0f) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = glow * flicker * 0.32f), Color.Transparent), c, w * 0.48f), w * 0.48f, c)
            if (burst.value > 0f) {
                repeat(8) { i ->
                    val angle = Math.toRadians(i * 45.0)
                    val ray = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                    val radius = w * (0.29f + (1f - burst.value) * 0.19f)
                    drawLine(brass.copy(alpha = burst.value), c + ray * radius, c + ray * (radius + w * 0.06f), 2.dp.toPx())
                }
            }
            // Ring handle, sloping roof, glass chamber, side rails and broad pedestal.
            drawOval(ink, Offset(w * 0.43f, h * 0.05f), Size(w * 0.14f, h * 0.18f), style = Stroke(2.5.dp.toPx()))
            val roof = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.5f, h * 0.19f); lineTo(w * 0.74f, h * 0.34f)
                lineTo(w * 0.26f, h * 0.34f); close()
            }
            drawPath(roof, ink)
            drawLine(brass, Offset(w * 0.3f, h * 0.33f), Offset(w * 0.7f, h * 0.33f), 2.dp.toPx())
            drawRoundRect(androidx.compose.ui.graphics.lerp(Color(0xFFE2D7C6), Color(0xFFFFE4A0), glow), Offset(w * 0.33f, h * 0.36f), Size(w * 0.34f, h * 0.36f), androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
            drawLine(Color.White.copy(alpha = 0.65f), Offset(w * 0.39f, h * 0.4f), Offset(w * 0.39f, h * 0.6f), 2.dp.toPx())
            for (x in listOf(0.3f, 0.7f)) drawLine(ink, Offset(w * x, h * 0.34f), Offset(w * x, h * 0.75f), 3.dp.toPx())
            drawRoundRect(ink, Offset(w * 0.25f, h * 0.73f), Size(w * 0.5f, h * 0.07f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawRoundRect(brass, Offset(w * 0.32f, h * 0.81f), Size(w * 0.36f, h * 0.05f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            drawLine(ink, Offset(w * 0.46f, h * 0.69f), Offset(w * 0.54f, h * 0.69f), 3.dp.toPx())
            if (glow > 0f) {
                val flame = androidx.compose.ui.graphics.Path().apply {
                    moveTo(c.x, h * (0.66f - 0.24f * flicker))
                    cubicTo(w * 0.44f, h * 0.54f, w * 0.37f, h * 0.65f, c.x, h * 0.68f)
                    cubicTo(w * 0.64f, h * 0.64f, w * 0.54f, h * 0.53f, c.x, h * (0.66f - 0.24f * flicker))
                    close()
                }
                drawPath(flame, Color(0xFFE89A36).copy(alpha = glow))
                drawOval(Color(0xFFFFF3CA).copy(alpha = glow), Offset(w * 0.47f, h * 0.57f), Size(w * 0.06f, h * 0.1f))
                if (lit) repeat(3) { i ->
                    val phase = (flicker + i * 0.33f) % 1f
                    drawCircle(brass.copy(alpha = (1f - phase) * 0.7f), 1.5.dp.toPx(), Offset(w * (0.43f + i * 0.07f), h * (0.52f - phase * 0.18f)))
                }
            }
        }
        Text(title, color = if (lit) color else Ink, fontFamily = GothicTreasureFontFamily, fontSize = 17.sp, textAlign = TextAlign.Center)
        Text(if (lit) "FLAME AWAKENED" else hint, color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center)
        Text(detail, color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

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
                                isFound = isFound,
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
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 92.dp),
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
        actionLabel?.let { label ->
            DiscoveryHuntButton(label, onAction)
        }
    }
}

@Composable
private fun TreasureInformationPanel(
    relic: MapRelic,
    isFound: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!isFound) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSoft),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Known legend", color = Brand, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            relic.story.ifBlank { relic.clue.ifBlank { relic.description } },
                            color = Ink,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        } else {
            relic.description.takeIf(String::isNotBlank)?.let { item { Text(it, color = Ink, style = MaterialTheme.typography.bodyLarge, lineHeight = 23.sp) } }
            item { DetailTextRow("Treasure type", relic.treasureTypeLabel.ifBlank { "Treasure" }) }
            item { DetailTextRow("Found at", relic.locationName) }
            relic.story.takeIf(String::isNotBlank)?.let { item { DetailTextRow("The story", it) } }
            relic.buildingStory.takeIf(String::isNotBlank)?.let { item { DetailTextRow("Landmark story", it) } }
            relic.prototypeDesign.takeIf(String::isNotBlank)?.let { item { DetailTextRow("Relic design", it) } }
            relic.coordinateSource.takeIf(String::isNotBlank)?.let { item { DetailTextRow("Location source", it) } }
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
            painter = painterResource(R.drawable.nav_treasure_symbol),
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
    headerArtworkVisible: Boolean = true,
    onHeaderArtworkBounds: (androidx.compose.ui.geometry.Rect) -> Unit = {},
) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFF8F0E4)),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TreasureArtwork(relic, discovered = true, modifier = Modifier.size(82.dp)
                    .onGloballyPositioned { onHeaderArtworkBounds(it.boundsInRoot()) }
                    .graphicsLayer { alpha = if (headerArtworkVisible) 1f else 0f })
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(relic.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    Text(relic.locationName, color = Muted)
                }
            }
        }
        if (onBackToReveal != null) {
            item {
                OutlinedButton(onClick = onBackToReveal, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to reveal")
                }
            }
        }
        if (relic.description.isNotBlank()) {
            item {
                Text(relic.description, color = Ink, style = MaterialTheme.typography.bodyLarge)
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
        if (relic.historicalImageUrl.isNotBlank() || com.comp90018.app.features.treasure.historicalArtworkResource(relic) != null) {
            item {
                Card(
                    Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSoft),
                ) {
                    com.comp90018.app.features.treasure.HistoricalTreasureImage(
                        relic = relic,
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

private data class DeviceMotionSample(
    val accelerationMagnitude: Float,
    val tiltDegrees: Float,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
)

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
                val pitchDegrees = Math.toDegrees(
                    kotlin.math.atan2(-x.toDouble(), kotlin.math.sqrt((y * y + z * z).toDouble())),
                ).toFloat()
                val rollDegrees = Math.toDegrees(kotlin.math.atan2(y.toDouble(), z.toDouble())).toFloat()
                motion = DeviceMotionSample(linearAcceleration, tiltDegrees, pitchDegrees, rollDegrees)
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
    discoveredTreasureIds: Set<String>,
): Map<String, Marker> {
    val markers = mutableMapOf<String, Marker>()
    relics.forEach { relic ->
        val marker = map.addMarker(
            MarkerOptions()
                .position(relic.coordinate.toLatLng())
                .title(relic.name)
                .snippet(relic.locationName)
                .icon(questMarkerIcon(
                    context, relic, relic.id in discoveredTreasureIds,
                    selected = relic.id == selectedRelic?.id,
                    active = relic.id == activeHuntTreasureId,
                    pulseScale = pulseScale,
                ))
                .anchor(0.5f, 0.5f),
        )
        marker?.tag = relic.id
        marker?.let { markers[relic.id] = it }
    }
    return markers
}

private fun renderHuntFragments(
    context: Context,
    map: GoogleMap,
    fragments: List<TeamHuntFragment>,
    foundFragmentIds: Set<String>,
) {
    fragments.forEach { fragment ->
        val found = fragment.id in foundFragmentIds
        map.addMarker(
            MarkerOptions()
                .position(fragment.coordinate.toLatLng())
                .title(fragment.title)
                .snippet(if (found) "Found" else "Not found")
                .icon(fragmentMarkerIcon(context, found))
                .anchor(0.5f, 0.5f)
                .alpha(if (found) 0.42f else 1f),
        )?.tag = "fragment:${fragment.id}"
    }
}

private fun fragmentMarkerIcon(context: Context, found: Boolean): BitmapDescriptor {
    val size = (38f * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (found) android.graphics.Color.rgb(102, 102, 102) else android.graphics.Color.rgb(102, 65, 175)
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.43f, paint)
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        strokeWidth = size * 0.08f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    if (found) {
        canvas.drawLine(size * 0.27f, size * 0.51f, size * 0.44f, size * 0.67f, linePaint)
        canvas.drawLine(size * 0.44f, size * 0.67f, size * 0.74f, size * 0.34f, linePaint)
    } else {
        canvas.drawLine(size * 0.31f, size * 0.36f, size * 0.69f, size * 0.64f, linePaint)
        canvas.drawLine(size * 0.69f, size * 0.36f, size * 0.31f, size * 0.64f, linePaint)
    }
    return BitmapDescriptorFactory.fromBitmap(bitmap)
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

private const val MAX_INITIAL_TREASURE_AREA_SPAN_METERS = 3_000.0

private val treasureMarkerIcons = android.util.LruCache<String, BitmapDescriptor>(96)
private val treasureMarkerArtwork = android.util.LruCache<Int, Bitmap>(8)

private fun questMarkerIcon(
    context: Context,
    relic: MapRelic,
    discovered: Boolean,
    selected: Boolean,
    active: Boolean = false,
    pulseScale: Float = 1f,
    flipScale: Float = 1f,
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val artworkRes = treasureArtworkResource(relic.artworkKey)
    val pulseBucket = (pulseScale * 20).toInt()
    val key = "$artworkRes:$density:$discovered:$selected:$active:$pulseBucket:${(flipScale * 40).toInt()}"
    treasureMarkerIcons.get(key)?.let { return it }
    val scale = pulseBucket / 20f
    val size = (72f * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val center = size / 2f
    canvas.scale(flipScale, 1f, center, center)
    val radius = size * (if (selected || active) 0.40f else 0.33f) * scale
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = if (active) android.graphics.Color.argb(85, 242, 181, 67) else android.graphics.Color.argb(45, 126, 76, 42)
    canvas.drawCircle(center, center, radius * 1.15f, paint)
    paint.color = android.graphics.Color.rgb(255, 247, 232)
    canvas.drawCircle(center, center, radius, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = (if (selected || active) 2.5f else 1.5f) * density
    paint.color = if (active) android.graphics.Color.rgb(242, 181, 67) else android.graphics.Color.rgb(126, 76, 42)
    canvas.drawCircle(center, center, radius, paint)
    val artwork = treasureMarkerArtwork.get(artworkRes) ?: BitmapFactory.decodeResource(context.resources, artworkRes).also {
        treasureMarkerArtwork.put(artworkRes, it)
    }
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        if (!discovered) colorFilter = PorterDuffColorFilter(android.graphics.Color.rgb(90, 59, 40), PorterDuff.Mode.SRC_IN)
    }
    val extent = radius * 1.42f
    val fit = extent / maxOf(artwork.width, artwork.height)
    val width = artwork.width * fit
    val height = artwork.height * fit
    canvas.drawBitmap(artwork, null, android.graphics.RectF(center - width / 2, center - height / 2, center + width / 2, center + height / 2), imagePaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap).also { treasureMarkerIcons.put(key, it) }
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

private fun moveCameraToCampus(map: GoogleMap, coordinates: List<GeoCoordinate>, currentLocation: GeoCoordinate?) {
    val points = buildList {
        addAll(coordinates)
        currentLocation?.let(::add)
    }
    if (points.isEmpty()) return
    val latitudeSpan = (points.maxOfOrNull { it.latitude } ?: 0.0) - (points.minOfOrNull { it.latitude } ?: 0.0)
    val longitudeSpan = (points.maxOfOrNull { it.longitude } ?: 0.0) - (points.minOfOrNull { it.longitude } ?: 0.0)
    if (points.isNotEmpty() && latitudeSpan < 0.0001 && longitudeSpan < 0.0001) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first().toLatLng(), 17f))
        return
    }
    val bounds = LatLngBounds.builder().apply {
        coordinates.forEach { include(it.toLatLng()) }
        currentLocation?.let { include(it.toLatLng()) }
    }.build()
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
}

private fun moveCameraToTreasureArea(
    map: GoogleMap,
    relics: List<MapRelic>,
    activeHuntTreasureId: String?,
) {
    relics.firstOrNull { it.id == activeHuntTreasureId }?.let { relic ->
        moveCameraToRelic(map, relic)
        return
    }

    if (!isCompactTreasureArea(relics)) {
        recommendedInitialRelic(relics)?.let { relic ->
            moveCameraToRelic(map, relic)
            return
        }
    }

    val points = relics.map { it.coordinate }
    if (points.isEmpty()) return
    val latitudeSpan = (points.maxOfOrNull { it.latitude } ?: 0.0) - (points.minOfOrNull { it.latitude } ?: 0.0)
    val longitudeSpan = (points.maxOfOrNull { it.longitude } ?: 0.0) - (points.minOfOrNull { it.longitude } ?: 0.0)
    if (points.isNotEmpty() && latitudeSpan < 0.0001 && longitudeSpan < 0.0001) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first().toLatLng(), 17f))
        return
    }
    val bounds = LatLngBounds.builder().apply {
        relics.forEach { include(it.coordinate.toLatLng()) }
    }.build()
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
}

internal fun recommendedInitialRelic(relics: List<MapRelic>): MapRelic? =
    relics.minWithOrNull(compareBy<MapRelic> { it.sortOrder }.thenBy { it.name })

internal fun isCompactTreasureArea(
    relics: List<MapRelic>,
    maxSpanMeters: Double = MAX_INITIAL_TREASURE_AREA_SPAN_METERS,
): Boolean {
    if (relics.size < 2) return true
    return relics.indices.all { firstIndex ->
        ((firstIndex + 1) until relics.size).all { secondIndex ->
            runCatching {
                LocationCalculator.distanceMeters(
                    relics[firstIndex].coordinate,
                    relics[secondIndex].coordinate,
                ) <= maxSpanMeters
            }.getOrDefault(false)
        }
    }
}

private fun moveCameraToRelic(map: GoogleMap, selectedRelic: MapRelic) {
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedRelic.coordinate.toLatLng(), 17f))
}

private fun moveCameraToHuntView(map: GoogleMap, currentLocation: GeoCoordinate, headingDegrees: Float) {
    val cameraTarget = coordinateAhead(currentLocation, HUNT_CAMERA_LEAD_METERS, headingDegrees.toDouble())
    map.moveCamera(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                .target(cameraTarget.toLatLng())
                .zoom(HUNT_CAMERA_ZOOM)
                .bearing(headingDegrees)
                .tilt(HUNT_CAMERA_TILT)
                .build(),
        ),
    )
}

private fun coordinateAhead(origin: GeoCoordinate, distanceMeters: Double, bearingDegrees: Double): GeoCoordinate {
    val angularDistance = distanceMeters / EARTH_RADIUS_METERS
    val bearing = Math.toRadians(bearingDegrees)
    val latitude = Math.toRadians(origin.latitude)
    val longitude = Math.toRadians(origin.longitude)
    val destinationLatitude = asin(
        sin(latitude) * cos(angularDistance) + cos(latitude) * sin(angularDistance) * cos(bearing),
    )
    val destinationLongitude = longitude + atan2(
        sin(bearing) * sin(angularDistance) * cos(latitude),
        cos(angularDistance) - sin(latitude) * sin(destinationLatitude),
    )
    return GeoCoordinate(Math.toDegrees(destinationLatitude), Math.toDegrees(destinationLongitude))
}

private fun coordinateAtDistance(target: GeoCoordinate, distanceMeters: Double): GeoCoordinate =
    coordinateAhead(target, distanceMeters, 180.0)

private fun GeoCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val HUNT_CAMERA_LEAD_METERS = 80.0
private const val HUNT_CAMERA_ZOOM = 18.5f
private const val HUNT_CAMERA_TILT = 45f

private fun Double?.formatDistance(): String = when {
    this == null -> "unknown"
    this >= 1000.0 -> String.format(Locale.US, "%.2f km", this / 1000.0)
    else -> "${kotlin.math.round(this / 10.0).toInt() * 10} m"
}

private fun Double?.formatDegrees(): String =
    this?.let { String.format(Locale.US, "%.0f°", it) } ?: "unknown"

private fun ProximityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }
