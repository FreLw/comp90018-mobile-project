package com.comp90018.app.features.map.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.TreasureStoryPanel
import com.comp90018.app.features.map.canReplayTreasure
import com.comp90018.app.features.map.components.formatDistance
import com.comp90018.app.features.map.rendering.GoogleMapView
import com.comp90018.app.sensors.location.LocationOutput
import kotlinx.coroutines.delay

/** Coordinates detail/search/dig/found/story presentation and its collection callbacks. */
@Composable
internal fun TreasureDetailScreen(
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
    onRestartHunt: (() -> Unit)? = null,
    forceReadyToDig: Boolean = false,
    onTeamTaskFinished: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    var stage by remember(relic.id, forceReadyToDig) { mutableStateOf(if (forceReadyToDig) TreasureHuntStage.READY_TO_DIG else TreasureHuntStage.DETAILS) }
    var searchStep by remember(relic.id) { mutableIntStateOf(0) }
    var detailsVisible by remember(relic.id) { mutableStateOf(false) }
    var collectionActionError by remember(relic.id) { mutableStateOf<String?>(null) }
    var collectionInProgress by remember(relic.id) { mutableStateOf(false) }
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
            collecting = collecting || collectionInProgress,
            collectionError = collectionActionError,
            onPutInBackpack = {
                collectionInProgress = true
                collectionActionError = null
                onCollected { error ->
                    collectionInProgress = false
                    collectionActionError = error
                    if (error == null) onBack()
                }
            },
            onReturnToMap = onBack,
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
                                actionLabel = if (!isFound && !forceReadyToDig) "Start Hunting" else null,
                                onAction = { onStartChallenge?.invoke() },
                                onChevron = onBack,
                            )
                            TreasureInformationPanel(
                                relic = relic,
                                isFound = isFound,
                                modifier = Modifier.weight(1f),
                                replayEnabled = canReplayTreasure(locationOutput.distanceToTargetMeters,
                                    locationValidity, preciseLocationEnabled, relic.compassGateConfig.huntReadyRadiusMeters),
                                replayRadiusMeters = relic.compassGateConfig.huntReadyRadiusMeters,
                                onRestartHunt = onRestartHunt,
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
                            onBack = onBack,
                        )
                        TreasureHuntStage.FOUND, TreasureHuntStage.STORY -> Unit
                    }
                }
            }
        }
    }
}

private enum class TreasureHuntStage { DETAILS, SEARCHING, READY_TO_DIG, FOUND, STORY }
