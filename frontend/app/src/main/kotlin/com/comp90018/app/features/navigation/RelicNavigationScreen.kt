package com.comp90018.app.features.navigation

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.comp90018.app.BuildConfig
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.map.DistanceSimulationControl
import com.comp90018.app.features.map.GoogleMapView
import com.comp90018.app.features.map.LocationActionPolicy
import com.comp90018.app.features.map.MapPerspective
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.HeadingSimulationControl
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput
import kotlin.math.roundToInt
import kotlin.math.abs

/** Standalone target-focused map. It does not own or mutate the existing hunt state machine. */
@Composable
fun RelicNavigationScreen(
    relic: MapRelic,
    userLocation: LocationOutput,
    onStopNavigation: () -> Unit,
    onStartHunting: (String) -> Unit,
) {
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
    val updatedConfirmation = remember(arrivalSample, arrivalConfirmation) {
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
    val energyProgress by animateFloatAsState(
        targetValue = uiState.resonanceProgress,
        animationSpec = tween(650),
        label = "relic_energy_progress",
    )
    val trailAnimation = rememberInfiniteTransition(label = "relic_energy_trail")
    val trailPhaseRaw by trailAnimation.animateFloat(
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
        label = "relic_energy_trail_phase",
    )
    val trailPhase by remember { derivedStateOf { (trailPhaseRaw * 24).roundToInt() / 24f } }
    val mapPresentationHeading = uiState.deviceHeadingDegrees?.toFloat() ?: 0f
    Box(Modifier.fillMaxSize()) {
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
            navigationTrailTarget = relic.coordinate.takeIf { navigationGuidingThreadAvailable(uiState) },
            navigationTrailStrength = energyProgress,
            navigationTrailPhase = trailPhase,
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
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 132.dp),
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
                        initialDistance = 250.0,
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shadowElevation = 4.dp,
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
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(relic.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(relic.locationName, color = Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        navigationDistanceLabel(uiState.distanceMeters),
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = onStopNavigation) {
                        Text("Stop")
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RosetteResonanceGauge(
                        progress = uiState.resonanceProgress,
                        stage = uiState.resonanceStage,
                        stateDescription = arrivalPresentation.stateDescription,
                        arrivalSweep = arrivalSweep.value,
                        scale = arrivalScale.value,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Relic Resonance", color = Ink, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(arrivalPresentation.stateDescription, color = Muted,
                            style = MaterialTheme.typography.bodySmall)
                        Text(
                            when (uiState.directionHint) {
                                NavigationDirectionHint.UNAVAILABLE -> "Finding direction…"
                                NavigationDirectionHint.ALIGNED -> "Trail aligned"
                                NavigationDirectionHint.TURN_LEFT -> "Turn left · ${abs(requireNotNull(uiState.headingErrorDegrees)).roundToInt()}°"
                                NavigationDirectionHint.TURN_RIGHT -> "Turn right · ${abs(requireNotNull(uiState.headingErrorDegrees)).roundToInt()}°"
                            },
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (arrivalPresentation.showBeginHunt) {
                        Spacer(Modifier.width(10.dp))
                        Button(onClick = { onStartHunting(relic.id) }) {
                            Text("Begin Hunt")
                        }
                    }
                }
            }
        }
    }
}

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
