package com.comp90018.app.features.navigation

/*
 * Renders target-focused map guidance, compass/resonance panels, and movable debug controls.
 * Coordinates location, orientation, arrival confirmation, and guiding-thread animation for one target.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.comp90018.app.BuildConfig
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.features.map.debug.DistanceSimulationControl
import com.comp90018.app.features.map.rendering.GoogleMapView
import com.comp90018.app.features.map.LocationActionPolicy
import com.comp90018.app.features.map.rendering.MapPerspective
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.debug.HeadingSimulationControl
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput

/** Standalone target-focused map. It does not own or mutate the existing hunt state machine. */
@Composable
fun RelicNavigationScreen(
    relic: MapRelic,
    userLocation: LocationOutput,
    onStopNavigation: () -> Unit,
    onStartHunting: (String) -> Unit,
) {
    BackHandler(onBack = onStopNavigation)
    val orientationOutput = rememberNavigationOrientationOutput()
    var recenterRequestKey by remember { mutableIntStateOf(0) }
    var distanceSimulation by remember(relic.id) { mutableStateOf(RelicNavigationSimulation()) }
    val simulatedDistance = distanceSimulation.distanceMeters.takeIf { BuildConfig.DEBUG }
    val isSimulating = simulatedDistance != null
    var simulatedHeading by remember(relic.id) { androidx.compose.runtime.mutableStateOf<Double?>(null) }
    val simulatedCoordinate = simulatedDistance?.let { navigationTestCoordinate(relic.coordinate, it) }
    val navigationLocation = LocationActionPolicy.targetOutput(
        location = userLocation,
        target = relic.coordinate,
        config = LocationConfig(
            insideRadiusMeters = NAVIGATION_ARRIVAL_METERS,
            nearbyRadiusMeters = NAVIGATION_ENERGY_RANGE_METERS,
        ),
        simulatedCoordinate = simulatedCoordinate,
    )
    val declination = rememberNavigationDeclination(
        simulatedCoordinate ?: LocationActionPolicy.actionableCoordinate(userLocation),
    )
    val deviceHeading = navigationTrueHeading(
        magneticHeadingDegrees = navigationDeviceHeading(orientationOutput),
        declinationDegrees = declination,
        simulatedTrueHeadingDegrees = simulatedHeading.takeIf { BuildConfig.DEBUG },
    )
    val arrivalSample = if (isSimulating) distanceSimulation.arrivalSample() else RelicArrivalSample(
        hasActionableLocation = LocationActionPolicy.actionableCoordinate(userLocation) != null,
        distanceMeters = navigationLocation.distanceToTargetMeters,
        accuracyMeters = userLocation.accuracyMeters,
        timestampNanos = userLocation.timestampNanos,
    )
    // GPS and synthetic fix identities belong to separate target/session-local evidence streams.
    var arrivalConfirmation by remember(relic.id, isSimulating) {
        mutableStateOf(RelicArrivalConfirmationState())
    }
    val updatedConfirmation = remember(relic.id, isSimulating, arrivalSample, arrivalConfirmation) {
        updateRelicArrivalConfirmation(arrivalConfirmation, arrivalSample)
    }
    // Present the transition immediately so stale arrival cannot keep the button visible.
    val uiState = deriveRelicNavigationUiState(
        sample = arrivalSample,
        confirmation = updatedConfirmation,
        targetBearingDegrees = navigationLocation.targetBearingDegrees,
        deviceHeadingDegrees = deviceHeading,
        locationReadiness = navigationLocationReadiness(userLocation, isSimulating),
    )
    LaunchedEffect(relic.id, isSimulating, arrivalSample) {
        arrivalConfirmation = updatedConfirmation
    }
    var arrivalAnimationComplete by remember(relic.id, isSimulating) { mutableStateOf(false) }
    val arrivalSweep = remember(relic.id) { Animatable(0f) }
    val arrivalScale = remember(relic.id) { Animatable(1f) }
    LaunchedEffect(relic.id, isSimulating, uiState.arrivalConfirmed) {
        arrivalAnimationComplete = false
        arrivalSweep.snapTo(0f)
        arrivalScale.snapTo(1f)
        if (uiState.arrivalConfirmed) {
            coroutineScope {
                launch { arrivalSweep.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
                launch {
                    arrivalScale.animateTo(1.04f, tween(200, easing = FastOutSlowInEasing))
                    arrivalScale.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
                }
            }
            arrivalAnimationComplete = true
        }
    }
    val arrivalPresentation = relicArrivalPresentation(uiState, arrivalAnimationComplete)
    val resonanceProgress by animateFloatAsState(
        targetValue = uiState.resonanceProgress,
        animationSpec = tween(650),
        label = "relic_resonance_progress",
    )
    val trailAnimation = rememberInfiniteTransition(label = "relic_guiding_thread")
    val trailPhase = trailAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = navigationTrailDurationMillis(
                    uiState.targetBearingDegrees,
                    uiState.deviceHeadingDegrees,
                ),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "relic_guiding_thread_phase",
    )
    val mapPresentationHeading = uiState.deviceHeadingDegrees?.toFloat() ?: 0f
    var topPanelsHeightPixels by remember { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val topPanelsHeight = with(LocalDensity.current) { topPanelsHeightPixels.toDp() }
        val maximumTargetHeight = maxHeight * 0.4f
        val maximumResonanceHeight = minOf(maxHeight * 0.5f,
            (maxHeight - topPanelsHeight - 24.dp).coerceAtLeast(48.dp))
        GoogleMapView(
            relics = listOf(relic),
            selectedRelic = relic,
            locationOutput = navigationLocation,
            deviceHeading = mapPresentationHeading,
            activeHuntTreasureId = relic.id,
            focusSelectedRelic = false,
            perspective = MapPerspective.HUNT,
            allowCameraGestures = true,
            allowZoomGestures = false,
            allowRotateGestures = false,
            allowTiltGestures = false,
            recenterRequestKey = recenterRequestKey,
            navigationOnlyUpdates = true,
            navigationTrailTarget = relic.coordinate.takeIf { navigationGuidingThreadAvailable(uiState) },
            navigationTrailStrength = resonanceProgress,
            navigationTrailPhaseState = trailPhase,
            onRelicSelected = {},
            modifier = Modifier.fillMaxSize(),
        )

        Surface(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 5.dp,
        ) {
            IconButton(onClick = { recenterRequestKey += 1 }) {
                Icon(Icons.Rounded.MyLocation, "Recenter map", tint = Brand)
            }
        }

        if (BuildConfig.DEBUG) {
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = topPanelsHeight + 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                DraggableTestControl {
                    HeadingSimulationControl(
                        headingDegrees = simulatedHeading,
                        onHeading = { simulatedHeading = it },
                    )
                }
                DraggableTestControl {
                    DistanceSimulationControl(
                        distance = simulatedDistance,
                        onDistance = { distanceSimulation = distanceSimulation.withDistance(it) },
                        maximumDistance = 300f,
                        initialDistance = RelicNavigationConfig.resonanceRangeMeters,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .onSizeChanged { topPanelsHeightPixels = it.height },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shadowElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onStopNavigation) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Stop navigation", tint = Ink)
                    }
                    Text(
                        "Relic Resonance",
                        modifier = Modifier.weight(1f),
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            RelicNavigationTargetPanel(
                name = relic.name,
                locationName = relic.locationName,
                distanceMeters = uiState.distanceMeters,
                onStopNavigation = onStopNavigation,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)
                    .heightIn(max = maximumTargetHeight),
            )
        }

        RelicNavigationResonancePanel(
            state = uiState,
            presentation = arrivalPresentation,
            arrivalSweep = arrivalSweep.value,
            arrivalScale = arrivalScale.value,
            onBeginHunt = { onStartHunting(relic.id) },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)
                .heightIn(max = maximumResonanceHeight),
        )
    }
}

/** Keeps the development controls movable so they do not obscure the guidance artwork. */
@Composable
private fun DraggableTestControl(content: @Composable () -> Unit) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    Row(
        modifier = Modifier.graphicsLayer {
            translationX = dragOffset.x
            translationY = dragOffset.y
        },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFF4DE),
            shadowElevation = 4.dp,
            modifier = Modifier.pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    dragOffset += amount
                }
            },
        ) {
            Icon(
                Icons.Rounded.DragIndicator,
                contentDescription = "Drag test control",
                tint = Ink,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 12.dp),
            )
        }
        content()
    }
}
