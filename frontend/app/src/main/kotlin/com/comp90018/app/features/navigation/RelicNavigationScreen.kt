package com.comp90018.app.features.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.comp90018.app.features.map.rememberDeviceHeading
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput
import kotlin.math.roundToInt

/** Standalone target-focused map. It does not own or mutate the existing hunt state machine. */
@Composable
fun RelicNavigationScreen(
    relic: MapRelic,
    userLocation: LocationOutput,
    onStopNavigation: () -> Unit,
) {
    val deviceHeading = rememberDeviceHeading()
    var recenterRequestKey by remember { mutableIntStateOf(0) }
    var simulatedDistance by remember(relic.id) { androidx.compose.runtime.mutableStateOf<Double?>(null) }
    var simulatedHeading by remember(relic.id) { androidx.compose.runtime.mutableStateOf<Double?>(null) }
    val effectiveHeading = simulatedHeading?.toFloat() ?: deviceHeading
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
    val distanceMeters = navigationLocation.distanceToTargetMeters
    val energyProgress by animateFloatAsState(
        targetValue = navigationEnergyProgress(distanceMeters),
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
                    navigationLocation.targetBearingDegrees,
                    effectiveHeading.toDouble(),
                ),
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "relic_energy_trail_phase",
    )
    val trailPhase by remember { derivedStateOf { (trailPhaseRaw * 24).roundToInt() / 24f } }
    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = listOf(relic),
            selectedRelic = relic,
            locationOutput = navigationLocation,
            deviceHeading = effectiveHeading,
            activeHuntTreasureId = relic.id,
            focusSelectedRelic = false,
            perspective = MapPerspective.HUNT,
            allowCameraGestures = true,
            allowZoomGestures = false,
            allowRotateGestures = false,
            allowTiltGestures = false,
            recenterRequestKey = recenterRequestKey,
            navigationTrailTarget = relic.coordinate,
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
                        onDistance = { simulatedDistance = it },
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
                    "Navigation",
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
                        navigationDistanceLabel(distanceMeters),
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
                    RelicEnergyBattery(energyProgress, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${(energyProgress * 100f).toInt()}%",
                        color = energyColor(energyProgress),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
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

@Composable
private fun RelicEnergyBattery(progress: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    val color by animateColorAsState(energyColor(progress), tween(650), label = "relic_energy_color")
    Row(
        modifier = modifier.semantics {
            progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f).height(26.dp).border(2.dp, Ink.copy(alpha = 0.62f), shape)
                .padding(3.dp).clip(RoundedCornerShape(3.dp)),
        ) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(color),
            )
        }
        Box(Modifier.width(5.dp).height(13.dp).background(Ink.copy(alpha = 0.62f), RoundedCornerShape(2.dp)))
    }
}

private fun energyColor(progress: Float): Color {
    val red = Color(0xFFD94B3D)
    val yellow = Color(0xFFF2B543)
    val green = Color(0xFF3D9A5B)
    val value = progress.coerceIn(0f, 1f)
    return if (value <= 0.5f) lerp(red, yellow, value * 2f) else lerp(yellow, green, (value - 0.5f) * 2f)
}
