package com.comp90018.app.features.map.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.comp90018.app.BuildConfig
import com.comp90018.app.data.rooms.TeamHuntLocation
import com.comp90018.app.features.haptics.TreasureHapticAttempt
import com.comp90018.app.features.haptics.TreasureHapticController
import com.comp90018.app.features.haptics.TreasureHapticSave
import com.comp90018.app.features.map.FragmentHuntConfig
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.PostChallengeRevealCoordinator
import com.comp90018.app.features.map.PostChallengeRevealScreen
import com.comp90018.app.features.map.SOUTH_LAWN_ATLAS_ID
import com.comp90018.app.features.map.SouthLawnAssemblyScreen
import com.comp90018.app.features.map.TeamHuntFragment
import com.comp90018.app.features.map.TreasureDiscoveryReveal
import com.comp90018.app.features.map.TreasureStoryPanel
import com.comp90018.app.features.map.canReplayTreasure
import com.comp90018.app.features.map.compass.TreasureCompassGate
import com.comp90018.app.features.map.debug.DebugChallengeLauncher
import com.comp90018.app.features.map.detail.TreasureDetailScreen
import com.comp90018.app.features.map.saveRelicDiscovery
import com.comp90018.app.features.map.team.MemberHuntQuiz
import com.comp90018.app.features.map.team.SouthLawnClaimWaitingScreen
import com.comp90018.app.features.map.team.SouthLawnFragmentHuntScreen
import com.comp90018.app.features.map.team.TeamHuntStatusScreen
import com.comp90018.app.features.map.validatedChallengeConfig
import com.comp90018.app.features.treasurechallenge.TreasureChallengeRoute
import com.comp90018.app.sensors.location.LocationOutput

/** Immutable inputs and existing actions for full-screen hunt routes. */
internal data class MapHuntContext(
    val teamHuntTarget: MapRelic?,
    val teamHuntActive: Boolean,
    val isSouthLawnFragmentHunt: Boolean,
    val teamHuntIsOwner: Boolean,
    val teamHuntCanClaim: Boolean,
    val teamHuntAlreadyClaimed: Boolean,
    val teamHuntTaskPendingForCurrentUser: Boolean,
    val teamHuntAllCompleted: Boolean,
    val fragmentHuntConfig: FragmentHuntConfig?,
    val huntFragments: List<TeamHuntFragment>,
    val activeHuntFoundFragmentIds: List<String>,
    val activeHuntClaimedMemberIds: List<String>,
    val hapticsEnabled: Boolean,
    val activeHuntOwnerId: String?,
    val activeHuntSessionId: String?,
    val currentUserId: String,
    val teammateLocations: List<TeamHuntLocation>,
    val huntMemberNames: Map<String, String>,
    val userLocation: LocationOutput,
    val locationOutput: LocationOutput,
    val teamHuntLocationOutput: LocationOutput,
    val deviceHeading: Float,
    val soundEffectsEnabled: Boolean,
    val preciseLocationEnabled: Boolean,
    val foundRelicIds: Set<String>,
    val savingTreasureId: String?,
    val debugTasks: List<MapRelic>,
    val hapticController: TreasureHapticController?,
    val revealCoordinator: PostChallengeRevealCoordinator,
    val onCollectTreasure: (String, (String?) -> Unit) -> Unit,
    val onClaimCompletedHuntTreasure: (TreasureHapticAttempt?, (String?) -> Unit) -> Unit,
    val onCompleteActiveHuntTask: ((String?) -> Unit) -> Unit,
    val onDebugCompleteActiveHuntTask: ((String?) -> Unit) -> Unit,
    val onFindActiveHuntFragment: (String) -> Unit,
    val onDebugUnlockHuntFragments: (List<String>, (String?) -> Unit) -> Unit,
    val openHunt: (MapRelic) -> Unit,
    val returnFromReveal: (MapRelic) -> Unit,
    val collectWithHaptics: (String, (String?) -> Unit) -> Unit,
)

/** Renders the first active hunt page; true means the ordinary map must stay hidden. */
@Composable
internal fun MapHuntContent(navigation: MapNavigationState, context: MapHuntContext): Boolean {
    var selectedRelic by navigation.selectedRelic
    var detailRelic by navigation.detailRelic
    var challengeRelic by navigation.challengeRelic
    var compassRelic by navigation.compassRelic
    var huntRevealRelic by navigation.huntRevealRelic
    var atlasAssemblyRelic by navigation.atlasAssemblyRelic
    var huntStoryVisible by navigation.huntStoryVisible
    var memberQuizRelic by navigation.memberQuizRelic
    var debugSimulationEnabled by navigation.debugSimulationEnabled
    var debugTaskRelic by navigation.debugTaskRelic
    var debugTaskSession by navigation.debugTaskSession
    val teamHuntTarget = context.teamHuntTarget
    val teamHuntActive = context.teamHuntActive && teamHuntTarget != null
    val isSouthLawnFragmentHunt = context.isSouthLawnFragmentHunt
    val teamHuntIsOwner = context.teamHuntIsOwner
    val teamHuntCanClaim = context.teamHuntCanClaim
    val teamHuntAlreadyClaimed = context.teamHuntAlreadyClaimed
    val teamHuntTaskPendingForCurrentUser = context.teamHuntTaskPendingForCurrentUser
    val teamHuntAllCompleted = context.teamHuntAllCompleted && teamHuntTarget != null
    val fragmentHuntConfig = context.fragmentHuntConfig
    val huntFragments = context.huntFragments
    val activeHuntFoundFragmentIds = context.activeHuntFoundFragmentIds
    val activeHuntClaimedMemberIds = context.activeHuntClaimedMemberIds
    val hapticsEnabled = context.hapticsEnabled
    val activeHuntOwnerId = context.activeHuntOwnerId
    val activeHuntSessionId = context.activeHuntSessionId
    val currentUserId = context.currentUserId
    val teammateLocations = context.teammateLocations
    val huntMemberNames = context.huntMemberNames
    val userLocation = context.userLocation
    val locationOutput = context.locationOutput
    val teamHuntLocationOutput = context.teamHuntLocationOutput
    val deviceHeading = context.deviceHeading
    val soundEffectsEnabled = context.soundEffectsEnabled
    val preciseLocationEnabled = context.preciseLocationEnabled
    val foundRelicIds = context.foundRelicIds
    val savingTreasureId = context.savingTreasureId
    val debugTasks = context.debugTasks
    val hapticController = context.hapticController
    val revealCoordinator = context.revealCoordinator
    val onCollectTreasure = context.onCollectTreasure
    val onClaimCompletedHuntTreasure = context.onClaimCompletedHuntTreasure
    val onCompleteActiveHuntTask = context.onCompleteActiveHuntTask
    val onDebugCompleteActiveHuntTask = context.onDebugCompleteActiveHuntTask
    val onFindActiveHuntFragment = context.onFindActiveHuntFragment
    val onDebugUnlockHuntFragments = context.onDebugUnlockHuntFragments
    val openHunt = context.openHunt
    val returnFromReveal = context.returnFromReveal
    fun collectWithHaptics(id: String, complete: (String?) -> Unit) = context.collectWithHaptics(id, complete)

    // Render a full-screen hunt route before the normal map; the first matching branch owns this frame.
    atlasAssemblyRelic?.let { relic ->
        SouthLawnAssemblyScreen(
            onClaim = { complete ->
                TreasureHapticSave.collectTeam(
                    relic.id, hapticController, onCollectTreasure, onClaimCompletedHuntTreasure, complete,
                )
            },
            onContinue = {
                atlasAssemblyRelic = null
                huntRevealRelic = relic
                huntStoryVisible = false
                selectedRelic = null
                detailRelic = null
            },
            onBack = { atlasAssemblyRelic = null; selectedRelic = null; detailRelic = null },
        )
        return true
    }

    // South Lawn replaces the original viewing-angle challenge with a shared four-fragment
    // search. This branch happens after the common state/effect setup so the Compose call order
    // remains stable as a room starts or ends a hunt.
    if (isSouthLawnFragmentHunt && debugTaskRelic == null) {
        Box(Modifier.fillMaxSize()) {
            SouthLawnFragmentHuntScreen(
                teammateLocations = teammateLocations,
                huntMemberNames = huntMemberNames,
                huntOwnerId = activeHuntOwnerId,
                fragments = huntFragments,
                collectionRadiusMeters = fragmentHuntConfig?.collectionRadiusMeters,
                foundFragmentIds = activeHuntFoundFragmentIds,
                userLocation = userLocation,
                deviceHeading = deviceHeading,
                onFindFragment = onFindActiveHuntFragment,
                onDebugUnlockFragments = onDebugUnlockHuntFragments,
            )
            if (BuildConfig.DEBUG) {
                DebugChallengeLauncher(
                    relics = debugTasks,
                    onLaunch = { relic -> debugTaskSession += 1; debugTaskRelic = relic },
                    teamTaskLabel = "Complete shared fragment task",
                    canCompleteTeamTask = fragmentHuntConfig != null && teamHuntTaskPendingForCurrentUser,
                    huntSessionKey = activeHuntSessionId,
                    onCompleteTeamTask = onDebugCompleteActiveHuntTask,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
        }
        return true
    }

    if (teamHuntActive && teamHuntTarget.id == SOUTH_LAWN_ATLAS_ID &&
        currentUserId in activeHuntClaimedMemberIds && huntRevealRelic == null
    ) {
        SouthLawnClaimWaitingScreen(
            teammateLocations = teammateLocations,
            huntMemberNames = huntMemberNames,
            huntOwnerId = activeHuntOwnerId,
            userLocation = userLocation,
            deviceHeading = deviceHeading,
        )
        return true
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
        return true
    }

    compassRelic?.let { relic ->
        TreasureCompassGate(
            relic = relic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            soundEffectsEnabled = soundEffectsEnabled,
            debugSimulationEnabled = debugSimulationEnabled,
            // Passing the compass only opens the physical task. Completion and persistence
            // stay in the challenge/claim callbacks, so this cannot unlock a Room treasure.
            onContinue = { compassRelic = null; detailRelic = null; challengeRelic = relic },
            onBack = {
                hapticController?.abandonTreasure("map", relic.id)
                compassRelic = null
                detailRelic = null
                selectedRelic = null
                debugSimulationEnabled = false
            },
        )
        return true
    }

    memberQuizRelic?.let { relic ->
        MemberHuntQuiz(
            relic = relic,
            // Reuse the same target and effective coordinate used by the team-hunt arrival
            // check. Re-reading only the live GPS fix here can lock the questions after arrival
            // when the map is using a simulated or last-known coordinate.
            locationOutput = teamHuntLocationOutput,
            quizSessionKey = "${currentUserId}:${activeHuntSessionId}:${relic.id}",
            onCompleted = onCompleteActiveHuntTask,
            onFinished = { memberQuizRelic = null },
            onBack = { memberQuizRelic = null },
        )
        return true
    }

    revealCoordinator.session?.let { session ->
        PostChallengeRevealScreen(session) {
            revealCoordinator.clear()
            detailRelic = null
            selectedRelic = null
        }
        return true
    }

    if (BuildConfig.DEBUG) debugTaskRelic?.let { relic ->
        BackHandler { debugTaskRelic = null }
        TreasureChallengeRoute(
            config = requireNotNull(relic.validatedChallengeConfig()),
            treasureId = relic.id,
            radarRadiusMeters = relic.radarRadiusMeters,
            preciseLocationEnabled = true,
            debugSimulationEnabled = true,
            challengeSessionId = "debug-task-$debugTaskSession",
            // UI previews complete locally, without changing collection or Room progress.
            onChallengeCompleted = { complete -> complete(null) },
            onChallengeSatisfied = { completionId -> hapticController?.challengeCompleted(relic.id, completionId) },
            revealRelic = relic,
            onDiscoveryReady = { capturedPhotoUri, discoverySave, photoArrival ->
                revealCoordinator.openOnCompletion(relic, capturedPhotoUri, discoverySave, photoArrival)
                debugTaskRelic = null
            },
            onBack = { debugTaskRelic = null },
        )
        return true
    }

    challengeRelic?.let { relic ->
        relic.validatedChallengeConfig()?.let { config ->
            TreasureChallengeRoute(
                config = config,
                treasureId = relic.id,
                radarRadiusMeters = relic.radarRadiusMeters,
                preciseLocationEnabled = preciseLocationEnabled,
                // The debug launcher starts simulation immediately; each task also has a Test panel toggle.
                debugSimulationEnabled = debugSimulationEnabled,
                challengeSessionId = activeHuntSessionId.orEmpty(),
                onChallengeCompleted = { onComplete ->
                    if (teamHuntActive) {
                        // A Room owner's challenge is that owner's shared hunt task, not a solo
                        // discovery.  Wait for both Room members before exposing the dig action.
                        if (teamHuntTaskPendingForCurrentUser && teamHuntIsOwner) {
                            onCompleteActiveHuntTask { error ->
                                if (error == null) {
                                    challengeRelic = null
                                    debugSimulationEnabled = false
                                } else onComplete(error)
                            }
                        } else {
                            challengeRelic = null
                            debugSimulationEnabled = false
                        }
                    } else {
                        saveRelicDiscovery(relic, ::collectWithHaptics, onComplete)
                    }
                },
                onChallengeSatisfied = { completionId -> hapticController?.challengeCompleted(relic.id, completionId) },
                revealRelic = relic,
                onDiscoveryReady = { capturedPhotoUri, discoverySave, photoArrival ->
                    if (!teamHuntActive) {
                        revealCoordinator.openOnCompletion(relic, capturedPhotoUri, discoverySave, photoArrival) {
                            discoverySave.retry { done -> saveRelicDiscovery(relic, ::collectWithHaptics, done) }
                        }
                        challengeRelic = null
                        debugSimulationEnabled = false
                    }
                },
                onBack = {
                    hapticController?.abandonTreasure("map", relic.id)
                    challengeRelic = null; debugSimulationEnabled = false
                },
            )
            return true
        }
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Challenge unavailable", style = MaterialTheme.typography.headlineSmall)
            Text("This treasure has missing or invalid challenge configuration. Please ask the team to update the catalogue.")
            Button(onClick = { challengeRelic = null }) { Text("Back to map") }
        }
        return true
    }

    detailRelic?.let { relic ->
        if (teamHuntActive && !teamHuntCanClaim) {
            TeamHuntStatusScreen(
                message = when {
                    teamHuntAlreadyClaimed -> "You have claimed your treasure. Waiting for the other explorers to claim theirs."
                    !teamHuntTaskPendingForCurrentUser -> "Your task is complete. Waiting for the other explorers to finish."
                    else -> "Complete your task first. The treasure unlocks when every explorer is ready."
                },
                onStartTask = if (teamHuntTaskPendingForCurrentUser) {
                    { detailRelic = null; openHunt(relic) }
                } else null,
                onBack = { detailRelic = null; selectedRelic = null },
            )
            return true
        }
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
                if (teamHuntActive && relic.id == teamHuntTarget.id && teamHuntCanClaim) {
                    TreasureHapticSave.collectTeam(
                        relic.id, hapticController, onCollectTreasure, onClaimCompletedHuntTreasure, onComplete,
                    )
                } else if (teamHuntActive) {
                    onComplete("Every explorer must complete the task before you can claim this treasure.")
                } else {
                    collectWithHaptics(relic.id, onComplete)
                }
            },
            onStartChallenge = { openHunt(relic) },
            onRestartHunt = if (!teamHuntActive) ({
                if (canReplayTreasure(locationOutput.distanceToTargetMeters,
                        locationOutput.validity, preciseLocationEnabled, relic.compassGateConfig.huntReadyRadiusMeters)) {
                    detailRelic = null
                    selectedRelic = null
                    openHunt(relic)
                }
            }) else null,
            forceReadyToDig = teamHuntAllCompleted && relic.id == teamHuntTarget.id,
            onTeamTaskFinished = null,
            onBack = {
                hapticController?.abandonTreasure("map", relic.id)
                detailRelic = null
            },
        )
        return true
    }

    return false
}
