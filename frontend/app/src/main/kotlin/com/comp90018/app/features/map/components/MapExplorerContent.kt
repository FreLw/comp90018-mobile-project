package com.comp90018.app.features.map.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.comp90018.app.BuildConfig
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.map.HUNT_READY_RADIUS_METERS
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.canReplayTreasure
import com.comp90018.app.features.map.debug.DebugChallengeLauncher
import com.comp90018.app.features.map.debug.DistanceSimulationControl
import com.comp90018.app.features.map.detail.TreasureInformationPanel
import com.comp90018.app.features.map.detail.TreasurePeekHeader
import com.comp90018.app.features.map.rendering.GoogleMapView
import com.comp90018.app.features.map.rendering.MapPerspective
import com.comp90018.app.features.map.route.MapNavigationState
import com.comp90018.app.sensors.location.LocationOutput

/** Local presentation state; the screen owns the original remember keys. */
internal class MapExplorerState(
    val treasureSheetExpanded: MutableState<Boolean>,
    val perspective: MutableState<MapPerspective>,
    val simulation: MutableState<Double?>,
    val returningRelicId: MutableState<String?>,
)

/** Values and actions rendered by the ordinary map, without changing hunt routing. */
internal data class MapExplorerContext(
    val visibleRelics: List<MapRelic>,
    val teammateLocations: List<TeamHuntLocation>,
    val huntMemberNames: Map<String, String>,
    val activeHuntOwnerId: String?,
    val locationOutput: LocationOutput,
    val deviceHeading: Float,
    val markerPulse: Float,
    val foundRelicIds: Set<String>,
    val activeHuntTreasureId: String?,
    val userLocation: LocationOutput,
    val isLocationStale: Boolean,
    val loading: Boolean,
    val error: String?,
    val resolvedTreasures: List<MapRelic>,
    val teamHuntAlreadyClaimed: Boolean,
    val teamHuntCanClaim: Boolean,
    val teamHuntActive: Boolean,
    val teamHuntTaskPendingForCurrentUser: Boolean,
    val teamHuntIsOwner: Boolean,
    val proximityMessage: String,
    val huntReadyRelic: MapRelic?,
    val teamHuntTarget: MapRelic?,
    val debugTasks: List<MapRelic>,
    val activeHuntSessionId: String?,
    val preciseLocationEnabled: Boolean,
    val onEnableLocation: () -> Unit,
    val onRetry: () -> Unit,
    val onDebugCompleteActiveHuntTask: ((String?) -> Unit) -> Unit,
    val openHunt: (MapRelic) -> Unit,
)

@Composable
internal fun MapExplorerContent(navigation: MapNavigationState, state: MapExplorerState, context: MapExplorerContext) {
    var selectedRelic by navigation.selectedRelic
    var debugTaskRelic by navigation.debugTaskRelic
    var debugTaskSession by navigation.debugTaskSession
    var treasureSheetExpanded by state.treasureSheetExpanded
    var perspective by state.perspective
    var simulation by state.simulation
    var returningRelicId by state.returningRelicId
    val visibleRelics = context.visibleRelics
    val teammateLocations = context.teammateLocations
    val huntMemberNames = context.huntMemberNames
    val activeHuntOwnerId = context.activeHuntOwnerId
    val locationOutput = context.locationOutput
    val deviceHeading = context.deviceHeading
    val markerPulse = context.markerPulse
    val foundRelicIds = context.foundRelicIds
    val activeHuntTreasureId = context.activeHuntTreasureId
    val userLocation = context.userLocation
    val isLocationStale = context.isLocationStale
    val loading = context.loading
    val error = context.error
    val resolvedTreasures = context.resolvedTreasures
    val teamHuntAlreadyClaimed = context.teamHuntAlreadyClaimed
    val teamHuntCanClaim = context.teamHuntCanClaim
    val teamHuntActive = context.teamHuntActive
    val teamHuntTaskPendingForCurrentUser = context.teamHuntTaskPendingForCurrentUser
    val teamHuntIsOwner = context.teamHuntIsOwner
    val proximityMessage = context.proximityMessage
    val huntReadyRelic = context.huntReadyRelic
    val teamHuntTarget = context.teamHuntTarget
    val debugTasks = context.debugTasks
    val activeHuntSessionId = context.activeHuntSessionId
    val preciseLocationEnabled = context.preciseLocationEnabled
    val onEnableLocation = context.onEnableLocation
    val onRetry = context.onRetry
    val onDebugCompleteActiveHuntTask = context.onDebugCompleteActiveHuntTask
    val openHunt = context.openHunt

    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = visibleRelics,
            teammateLocations = teammateLocations,
            huntMemberNames = huntMemberNames,
            huntOwnerId = activeHuntOwnerId,
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
                        teamHuntAlreadyClaimed -> "Your treasure is claimed. Waiting for the other explorers."
                        teamHuntCanClaim -> "Every explorer is ready. Claim your treasure!"
                        teamHuntActive && !teamHuntTaskPendingForCurrentUser -> "Your task is complete. Waiting for the other explorers to finish."
                        else -> proximityMessage
                    },
                    loading = loading,
                    onRetry = onRetry.takeIf { error != null },
                    actionLabel = when {
                        teamHuntCanClaim -> "Claim treasure"
                        teamHuntActive && !teamHuntTaskPendingForCurrentUser -> null
                        huntReadyRelic != null -> "Start Hunting"
                        else -> null
                    },
                    onAction = {
                        (if (teamHuntCanClaim) teamHuntTarget else huntReadyRelic)?.let { openHunt(it) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                )
            }
        }

        if (BuildConfig.DEBUG) {
            DebugChallengeLauncher(
                relics = debugTasks,
                onLaunch = { relic ->
                    debugTaskSession += 1
                    debugTaskRelic = relic
                },
                teamTaskLabel = when {
                    !teamHuntActive -> "No active team task"
                    !teamHuntTaskPendingForCurrentUser -> "My team task is complete"
                    teamHuntIsOwner -> "Complete my owner task"
                    else -> "Complete my member task"
                },
                canCompleteTeamTask = teamHuntTaskPendingForCurrentUser,
                huntSessionKey = activeHuntSessionId,
                onCompleteTeamTask = onDebugCompleteActiveHuntTask,
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
                .padding(top = 18.dp),
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "map_treasure_header",
        ) { relic ->
            if (relic != null) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.97f)),
                ) {
                    TreasurePeekHeader(
                        relic = relic,
                        distance = locationOutput.distanceToTargetMeters.formatDistance(),
                        isFound = relic.id in foundRelicIds,
                        expanded = treasureSheetExpanded,
                        onChevron = { treasureSheetExpanded = !treasureSheetExpanded },
                        onSheetDrag = { expanded -> treasureSheetExpanded = expanded },
                        actionLabel = when {
                            teamHuntCanClaim -> "Claim treasure"
                            !teamHuntActive && relic.id in foundRelicIds -> null
                            teamHuntActive && !teamHuntTaskPendingForCurrentUser -> null
                            locationOutput.distanceToTargetMeters?.let { it <= HUNT_READY_RADIUS_METERS } == true -> "Start Hunting"
                            else -> null
                        },
                        onAction = { openHunt(relic) },
                    )
                    if (treasureSheetExpanded) {
                        TreasureInformationPanel(
                            relic = relic,
                            isFound = relic.id in foundRelicIds,
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            replayEnabled = canReplayTreasure(locationOutput.distanceToTargetMeters,
                                locationOutput.validity, preciseLocationEnabled, relic.compassGateConfig.huntReadyRadiusMeters),
                            replayRadiusMeters = relic.compassGateConfig.huntReadyRadiusMeters,
                            onRestartHunt = if (!teamHuntActive) ({
                                if (canReplayTreasure(locationOutput.distanceToTargetMeters,
                                        locationOutput.validity, preciseLocationEnabled, relic.compassGateConfig.huntReadyRadiusMeters)) {
                                    selectedRelic = null
                                    openHunt(relic)
                                }
                            }) else null,
                        )
                    }
                }
            }
        }

    }
}
