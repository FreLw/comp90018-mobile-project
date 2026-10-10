package com.comp90018.app.features.map

import android.content.Context
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.haptics.TreasureHapticAttempt
import com.comp90018.app.features.haptics.TreasureHapticController
import com.comp90018.app.features.haptics.TreasureHapticSave
import com.comp90018.app.features.haptics.TreasureHapticTarget
import com.comp90018.app.features.map.components.MapExplorerContent
import com.comp90018.app.features.map.components.MapExplorerContext
import com.comp90018.app.features.map.components.MapExplorerState
import com.comp90018.app.features.map.rendering.MapPerspective
import com.comp90018.app.features.map.rendering.coordinateAtDistance
import com.comp90018.app.features.map.route.MapHuntContent
import com.comp90018.app.features.map.route.MapHuntContext
import com.comp90018.app.features.map.route.rememberMapNavigationState
import com.comp90018.app.features.map.sensors.rememberDeviceHeading
import com.comp90018.app.features.map.team.TeamHuntArrivalDialog
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput

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
    hapticController: TreasureHapticController? = null,
    soundEffectsEnabled: Boolean = true,
    onEnableLocation: () -> Unit,
    onCollectTreasure: (String, (String?) -> Unit) -> Unit,
    activeHuntTreasureId: String? = null,
    activeHuntOwnerId: String? = null,
    activeHuntSessionId: String? = null,
    activeHuntMemberIds: List<String> = emptyList(),
    activeHuntCompletedMemberIds: List<String> = emptyList(),
    activeHuntFoundFragmentIds: List<String> = emptyList(),
    activeHuntClaimedMemberIds: List<String> = emptyList(),
    currentUserId: String = "",
    teammateLocations: List<TeamHuntLocation> = emptyList(),
    huntMemberNames: Map<String, String> = emptyMap(),
    onCompleteActiveHuntTask: ((String?) -> Unit) -> Unit = { complete -> complete("No active team hunt") },
    onDebugCompleteActiveHuntTask: ((String?) -> Unit) -> Unit = { complete -> complete("No active team hunt") },
    onFindActiveHuntFragment: (String) -> Unit = {},
    onDebugUnlockHuntFragments: (List<String>, (String?) -> Unit) -> Unit = { _, complete -> complete("No active team hunt") },
    onClaimCompletedHuntTreasure: (TreasureHapticAttempt?, (String?) -> Unit) -> Unit = { _, complete -> complete("No active team hunt") },
    requestedTreasureId: String? = null,
    onTreasureRequestConsumed: () -> Unit = {},
    requestedHuntTreasureId: String? = null,
    onHuntRequestConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val teamHuntTarget = activeHuntTreasureId?.let { id -> treasures.firstOrNull { it.id == id } }
    val teamHuntActive = teamHuntTarget != null && activeHuntOwnerId != null
    val fragmentHuntConfig = teamHuntTarget?.fragmentHuntConfig
    val huntFragments = fragmentHuntConfig?.fragments.orEmpty()
    val teamHuntIsOwner = activeHuntOwnerId == currentUserId
    val teamHuntAllCompleted = teamHuntActive && activeHuntMemberIds.size in 2..4 &&
        activeHuntMemberIds.all { it in activeHuntCompletedMemberIds }
    val teamHuntCanClaim = TeamHuntTaskEligibility.canClaim(
        teamHuntActive, activeHuntMemberIds, activeHuntCompletedMemberIds,
        activeHuntClaimedMemberIds, currentUserId,
    ) && (teamHuntTarget?.id != SOUTH_LAWN_ATLAS_ID ||
        (fragmentHuntConfig != null && huntFragments.all { it.id in activeHuntFoundFragmentIds }))
    val teamHuntAlreadyClaimed = teamHuntActive && currentUserId in activeHuntClaimedMemberIds
    val teamHuntTaskPendingForCurrentUser = TeamHuntTaskEligibility.isPendingFor(
        huntActive = teamHuntActive,
        memberIds = activeHuntMemberIds,
        completedMemberIds = activeHuntCompletedMemberIds,
        currentUserId = currentUserId,
    )
    val isSouthLawnFragmentHunt = teamHuntActive && teamHuntTarget.id == SOUTH_LAWN_ATLAS_ID &&
        (fragmentHuntConfig == null || huntFragments.any { it.id !in activeHuntFoundFragmentIds })
    // An active team hunt locks the solo catalogue to its shared target.
    val resolvedTreasures = if (teamHuntActive) listOf(requireNotNull(teamHuntTarget)) else treasures
    // These selections represent the map-owned pages. A hunt-session change resets its transient routes.
    val navigation = rememberMapNavigationState(activeHuntSessionId, currentUserId)
    var selectedRelic by navigation.selectedRelic
    val treasureSheetExpandedState = remember(selectedRelic?.id) { mutableStateOf(false) }
    var treasureSheetExpanded by treasureSheetExpandedState
    BackHandler(enabled = selectedRelic != null && treasureSheetExpanded) { treasureSheetExpanded = false }
    var detailRelic by navigation.detailRelic
    var challengeRelic by navigation.challengeRelic
    var compassRelic by navigation.compassRelic
    var huntRevealRelic by navigation.huntRevealRelic
    // A successful final claim can end the room session while this animation is running.
    var atlasAssemblyRelic by navigation.atlasAssemblyRelic
    var huntStoryVisible by navigation.huntStoryVisible
    var memberQuizRelic by navigation.memberQuizRelic
    val perspectiveState = remember { mutableStateOf(MapPerspective.GOD) }
    var perspective by perspectiveState
    val simulationState = remember { mutableStateOf<Double?>(null) }
    var simulation by simulationState
    // This acknowledgement must survive leaving/re-entering MapScreen (and app restarts).
    // Otherwise a location update can repeatedly reopen the arrival dialog after the explorer
    // has already chosen either action. It is scoped to the explorer and hunt session.
    val teamHuntArrivalPreferences = remember(context) {
        context.getSharedPreferences("team_hunt_arrival_prompts", Context.MODE_PRIVATE)
    }
    val teamHuntArrivalPreferenceKey = remember(currentUserId, activeHuntSessionId) {
        "arrival_${TeamHuntTaskEligibility.promptKey(currentUserId, activeHuntSessionId.orEmpty())}"
    }
    val claimPromptPreferences = remember(context) {
        context.getSharedPreferences("team_hunt_claim_prompts", Context.MODE_PRIVATE)
    }
    val claimPromptKey = TeamHuntTaskEligibility.promptKey(currentUserId, activeHuntSessionId.orEmpty())
    val claimPrompt = remember(claimPromptKey, claimPromptPreferences) {
        TeamHuntClaimPrompt(
            { claimPromptPreferences.getBoolean(claimPromptKey, false) },
            { claimPromptPreferences.edit().putBoolean(claimPromptKey, true).apply() },
        )
    }
    var showTeamHuntClaimPrompt by remember(claimPromptKey) { mutableStateOf(false) }
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
    var debugSimulationEnabled by navigation.debugSimulationEnabled
    var debugTaskRelic by navigation.debugTaskRelic
    var debugTaskSession by navigation.debugTaskSession
    val debugTasks = remember { debugChallengeRelics() }
    val revealCoordinator = remember(activeHuntSessionId) { PostChallengeRevealCoordinator() }
    var revealedIds by remember(currentUserId) { mutableStateOf(emptySet<String>()) }
    val returningRelicIdState = remember { mutableStateOf<String?>(null) }
    var returningRelicId by returningRelicIdState
    val foundRelicIds = discoveredTreasureIds + revealedIds
    fun openHunt(relic: MapRelic, simulateChallenge: Boolean = false) {
        debugSimulationEnabled = simulateChallenge
        when (huntEntry(teamHuntActive, teamHuntTaskPendingForCurrentUser, teamHuntIsOwner, teamHuntCanClaim)) {
            HuntEntry.CLAIM -> {
                if (relic.id == SOUTH_LAWN_ATLAS_ID) atlasAssemblyRelic = relic
                else detailRelic = relic
            }
            HuntEntry.WAITING -> selectedRelic = relic
            HuntEntry.MEMBER_QUIZ -> memberQuizRelic = relic
            HuntEntry.COMPASS -> compassRelic = relic
        }
    }
    LaunchedEffect(requestedHuntTreasureId, treasures) {
        val requestedId = requestedHuntTreasureId ?: return@LaunchedEffect
        val requestedRelic = treasures.firstOrNull { it.id == requestedId } ?: return@LaunchedEffect
        onHuntRequestConsumed()
        openHunt(requestedRelic)
    }
    val mapActivity = LocalActivity.current
    DisposableEffect(hapticController) {
        onDispose {
            if (mapActivity?.isChangingConfigurations != true) hapticController?.abandonOwner("map")
        }
    }
    fun collectWithHaptics(treasureId: String, onComplete: (String?) -> Unit) {
        TreasureHapticSave.collect(
            treasureId, treasureId in foundRelicIds || teamHuntActive, hapticController, onCollectTreasure, onComplete,
        )
    }
    fun returnFromReveal(relic: MapRelic) {
        returningRelicId = relic.id
        huntRevealRelic = null
        selectedRelic = null
    }
    val deviceHeading = rememberDeviceHeading()
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
    val hapticLifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(userLocation, huntCandidates, foundRelicIds, activeHuntTreasureId,
        compassRelic?.id, challengeRelic?.id, hapticsEnabled, hapticLifecycleState, hapticController) {
        if (!hapticsEnabled || !hapticLifecycleState.isAtLeast(Lifecycle.State.RESUMED)) return@LaunchedEffect
        TreasureHapticTarget.nearest(
            huntCandidates, foundRelicIds,
            activeHuntTreasureId ?: compassRelic?.id ?: challengeRelic?.id,
            userLocation, SystemClock.elapsedRealtimeNanos(),
        )?.let { (id, distance) -> hapticController?.nearby(id, distance) }
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
    val activeRelic = compassRelic ?: challengeRelic ?: detailRelic ?: selectedRelic
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
        memberQuizRelic = memberQuizRelic?.let { quiz -> resolvedTreasures.firstOrNull { it.id == quiz.id } }
    }
    LaunchedEffect(visibleRelics) {
        selectedRelic = selectedRelic?.takeIf { selected -> visibleRelics.any { it.id == selected.id } }
    }
    LaunchedEffect(requestedTreasureId, resolvedTreasures) {
        val requestedId = requestedTreasureId ?: return@LaunchedEffect
        resolvedTreasures.firstOrNull { it.id == requestedId }?.let { selectedRelic = it }
        onTreasureRequestConsumed()
    }
    LaunchedEffect(teamHuntCanClaim, claimPromptKey) {
        if (!activeHuntSessionId.isNullOrBlank() && claimPrompt.shouldShow(teamHuntCanClaim)
        ) {
            showTeamHuntClaimPrompt = true
        } else if (!teamHuntCanClaim) {
            showTeamHuntClaimPrompt = false
        }
    }
    LaunchedEffect(
        teamHuntTarget?.id,
        activeHuntSessionId,
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

    if (MapHuntContent(navigation, MapHuntContext(
        teamHuntTarget = teamHuntTarget,
        teamHuntActive = teamHuntActive,
        isSouthLawnFragmentHunt = isSouthLawnFragmentHunt,
        teamHuntIsOwner = teamHuntIsOwner,
        teamHuntCanClaim = teamHuntCanClaim,
        teamHuntAlreadyClaimed = teamHuntAlreadyClaimed,
        teamHuntTaskPendingForCurrentUser = teamHuntTaskPendingForCurrentUser,
        teamHuntAllCompleted = teamHuntAllCompleted,
        fragmentHuntConfig = fragmentHuntConfig,
        huntFragments = huntFragments,
        activeHuntFoundFragmentIds = activeHuntFoundFragmentIds,
        activeHuntClaimedMemberIds = activeHuntClaimedMemberIds,
        hapticsEnabled = hapticsEnabled,
        activeHuntOwnerId = activeHuntOwnerId,
        activeHuntSessionId = activeHuntSessionId,
        currentUserId = currentUserId,
        teammateLocations = teammateLocations,
        huntMemberNames = huntMemberNames,
        userLocation = userLocation,
        locationOutput = locationOutput,
        teamHuntLocationOutput = teamHuntLocationOutput,
        deviceHeading = deviceHeading,
        soundEffectsEnabled = soundEffectsEnabled,
        preciseLocationEnabled = preciseLocationEnabled,
        foundRelicIds = foundRelicIds,
        savingTreasureId = savingTreasureId,
        debugTasks = debugTasks,
        hapticController = hapticController,
        revealCoordinator = revealCoordinator,
        onCollectTreasure = onCollectTreasure,
        onClaimCompletedHuntTreasure = onClaimCompletedHuntTreasure,
        onCompleteActiveHuntTask = onCompleteActiveHuntTask,
        onDebugCompleteActiveHuntTask = onDebugCompleteActiveHuntTask,
        onFindActiveHuntFragment = onFindActiveHuntFragment,
        onDebugUnlockHuntFragments = onDebugUnlockHuntFragments,
        openHunt = { openHunt(it) },
        returnFromReveal = ::returnFromReveal,
        collectWithHaptics = ::collectWithHaptics,
    ))) return

    // Keep the prompt in the map branch only. Challenge/detail routes return above, so an
    // arrival prompt can never be left layered over a task screen during navigation.
    if (showTeamHuntClaimPrompt && teamHuntCanClaim) {
        // Acknowledge only when the prompt actually reaches the map branch. Effects can run
        // behind a task screen; those must not consume the once-per-session notification.
        LaunchedEffect(claimPromptKey) {
            claimPrompt.onShown()
        }
        AlertDialog(
            onDismissRequest = { showTeamHuntClaimPrompt = false },
            title = { Text("Treasure unlocked") },
            text = { Text("Every explorer has completed their task. You can now claim your treasure.") },
            confirmButton = {
                Button(onClick = {
                    showTeamHuntClaimPrompt = false
                    teamHuntTarget?.let { openHunt(it) }
                }) { Text("Claim treasure") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTeamHuntClaimPrompt = false }) { Text("Later") }
            },
        )
    }
    if (showTeamHuntArrival && teamHuntTaskPendingForCurrentUser) {
        TeamHuntArrivalDialog(
            isOwner = teamHuntIsOwner,
            onDismiss = ::acknowledgeTeamHuntArrival,
            onContinue = {
                val target = requireNotNull(teamHuntTarget)
                acknowledgeTeamHuntArrival()
                // The owner always performs the treasure's normal individual challenge. This
                // keeps the GPS/sensor rules and challenge examples identical in solo and team
                // hunts; team mode only adds the shared completion update on success.
                openHunt(target)
            },
        )
    }

    MapExplorerContent(
        navigation = navigation,
        state = MapExplorerState(treasureSheetExpandedState, perspectiveState, simulationState, returningRelicIdState),
        context = MapExplorerContext(
            visibleRelics = visibleRelics,
            teammateLocations = teammateLocations,
            huntMemberNames = huntMemberNames,
            activeHuntOwnerId = activeHuntOwnerId,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            markerPulse = markerPulse,
            foundRelicIds = foundRelicIds,
            activeHuntTreasureId = activeHuntTreasureId,
            userLocation = userLocation,
            isLocationStale = isLocationStale,
            loading = loading,
            error = error,
            resolvedTreasures = resolvedTreasures,
            teamHuntAlreadyClaimed = teamHuntAlreadyClaimed,
            teamHuntCanClaim = teamHuntCanClaim,
            teamHuntActive = teamHuntActive,
            teamHuntTaskPendingForCurrentUser = teamHuntTaskPendingForCurrentUser,
            teamHuntIsOwner = teamHuntIsOwner,
            proximityMessage = proximityMessage,
            huntReadyRelic = huntReadyRelic,
            teamHuntTarget = teamHuntTarget,
            debugTasks = debugTasks,
            activeHuntSessionId = activeHuntSessionId,
            preciseLocationEnabled = preciseLocationEnabled,
            onEnableLocation = onEnableLocation,
            onRetry = onRetry,
            onDebugCompleteActiveHuntTask = onDebugCompleteActiveHuntTask,
            openHunt = { openHunt(it) },
        ),
    )

}
