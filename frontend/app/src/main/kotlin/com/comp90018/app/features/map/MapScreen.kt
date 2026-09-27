package com.comp90018.app.features.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.AndroidLocationSensor
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.location.ProximityState
import com.comp90018.app.sensors.HorizontalState
import com.comp90018.app.sensors.orientation.AndroidOrientationSensor
import com.comp90018.app.sensors.orientation.OrientationOutput
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.R
import com.comp90018.app.RelicRed
import com.comp90018.app.features.treasure.RemoteTreasureImage
import com.comp90018.app.features.treasure.TreasurePrototypeImage
import com.comp90018.app.features.treasure.localTreasureArtworkResId
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private enum class MapPerspective { GOD, HUNT }

internal enum class ProximitySimulation(val label: String, val distanceMeters: Double?) {
    OFF("Test distance", null),
    HUNDRED_METRES("Test: 100 m", 99.0),
    FIFTY_METRES("Test: 50 m", 49.0),
    TEN_METRES("Test: 10 m", 9.0),
}

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
    onCollectTreasure: (String, (String?) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val runningInEmulator = remember { isProbablyEmulator() }
    val resolvedTreasures = remember(treasures) {
        treasures.map(NonFinalChallengeCatalog::attachChallenge)
    }
    var selectedRelic by remember { mutableStateOf<MapRelic?>(null) }
    var detailRelic by remember { mutableStateOf<MapRelic?>(null) }
    var directHuntRelicId by remember { mutableStateOf<String?>(null) }
    var perspective by remember { mutableStateOf(MapPerspective.GOD) }
    var simulation by remember { mutableStateOf(ProximitySimulation.OFF) }
    val foundRelicIds = discoveredTreasureIds
    val locationSensor = remember(context, preciseLocationEnabled) {
        AndroidLocationSensor(
            context = context.applicationContext,
            config = LocationConfig(
                insideRadiusMeters = HUNT_READY_RADIUS_METERS,
                nearbyRadiusMeters = RADAR_SCAN_RADIUS_METERS,
            ),
            preciseLocationEnabled = preciseLocationEnabled,
        )
    }
    val orientationSensor = remember(context) {
        AndroidOrientationSensor(context.applicationContext)
    }
    val liveLocationOutput by locationSensor.output.collectAsStateWithLifecycle()
    val orientationOutput by orientationSensor.output.collectAsStateWithLifecycle()
    val deviceHeading = orientationOutput.direction.headingDegrees?.toFloat()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        locationSensor.refreshPermissionState()
        if (grants.values.any { it }) locationSensor.start()
    }
    LaunchedEffect(runningInEmulator) {
        if (!runningInEmulator) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }
    DisposableEffect(lifecycle, locationSensor, orientationSensor) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    locationSensor.refreshPermissionState()
                    locationSensor.start()
                    orientationSensor.start()
                }
                Lifecycle.Event.ON_STOP -> {
                    locationSensor.stop()
                    orientationSensor.stop()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            locationSensor.refreshPermissionState()
            locationSensor.start()
            orientationSensor.start()
        }
        onDispose {
            lifecycle.removeObserver(observer)
            locationSensor.stop()
            orientationSensor.stop()
        }
    }
    val undiscoveredTreasures = remember(resolvedTreasures, foundRelicIds) {
        resolvedTreasures.filterNot { it.id in foundRelicIds }
    }
    val defaultCoordinate = if (runningInEmulator) {
        DEFAULT_CAMPUS_CENTRE
    } else {
        liveLocationOutput.currentLocation ?: DEFAULT_CAMPUS_CENTRE
    }
    val simulationTarget = remember(undiscoveredTreasures) {
        undiscoveredTreasures.minByOrNull { LocationCalculator.distanceMeters(DEFAULT_CAMPUS_CENTRE, it.coordinate) }
    }
    val currentCoordinate = simulation.distanceMeters?.let { distance ->
        simulationTarget?.coordinate?.let { target -> coordinateAtDistance(target, distance) }
    } ?: defaultCoordinate
    val proximityTreasures = remember(undiscoveredTreasures, simulation, simulationTarget) {
        if (simulation == ProximitySimulation.OFF || simulationTarget == null) undiscoveredTreasures
        else listOf(simulationTarget)
    }
    val treasureDistances = remember(proximityTreasures, currentCoordinate) {
        proximityTreasures
            .map { relic -> relic to LocationCalculator.distanceMeters(currentCoordinate, relic.coordinate) }
            .sortedBy { it.second }
    }
    val nearestTreasure = treasureDistances.firstOrNull()
    val visibleRelics = remember(treasureDistances) {
        treasureDistances.filter { (_, distance) -> distance <= REVEAL_RADIUS_METERS }.map { it.first }
    }
    val proximityMessage = remember(nearestTreasure, undiscoveredTreasures) {
        when {
            undiscoveredTreasures.isEmpty() -> "Every campus relic has been recovered — legendary work, explorer!"
            nearestTreasure == null -> "The trail has gone quiet — follow the hint and venture closer!"
            else -> treasureProximityMessage(nearestTreasure.second, currentCoordinate, nearestTreasure.first.coordinate)
        }
    }
    val huntReadyRelic = nearestTreasure?.takeIf { (_, distance) -> distance <= HUNT_READY_RADIUS_METERS }?.first
    val activeRelic = detailRelic ?: selectedRelic
    val locationOutput = remember(liveLocationOutput, currentCoordinate, activeRelic) {
        LocationCalculator.buildOutput(
            currentLocation = currentCoordinate,
            targetLocation = activeRelic?.coordinate,
            timestampNanos = liveLocationOutput.timestampNanos,
            config = LocationConfig(
                insideRadiusMeters = HUNT_READY_RADIUS_METERS,
                nearbyRadiusMeters = RADAR_SCAN_RADIUS_METERS,
            ),
            permission = liveLocationOutput.permission,
            availability = liveLocationOutput.availability,
            accuracyMeters = liveLocationOutput.accuracyMeters,
        )
    }
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
    }

    LaunchedEffect(visibleRelics) {
        selectedRelic = selectedRelic?.takeIf { selected -> visibleRelics.any { it.id == selected.id } }
    }

    detailRelic?.let { relic ->
        TreasureDetailScreen(
            relic = relic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            orientationOutput = orientationOutput,
            isFound = relic.id in foundRelicIds,
            collecting = savingTreasureId == relic.id,
            startInCompass = directHuntRelicId == relic.id,
            hapticsEnabled = hapticsEnabled,
            onCollected = { onComplete -> onCollectTreasure(relic.id, onComplete) },
            onBack = {
                detailRelic = null
                directHuntRelicId = null
            },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = visibleRelics,
            selectedRelic = selectedRelic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading ?: 0f,
            markerPulse = if (selectedRelic == null) markerPulse else 1f,
            focusSelectedRelic = false,
            perspective = perspective,
            onRelicSelected = {
                selectedRelic = it
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (selectedRelic == null) {
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
                        directHuntRelicId = relic.id
                        detailRelic = relic
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 18.dp),
            )
        }

        PerspectiveSwitch(
            selected = perspective,
            onSelected = { perspective = it },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        )

        SimulationButton(
            simulation = simulation,
            onClick = { simulation = simulation.next() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (huntReadyRelic == null) 112.dp else 190.dp,
                ),
        )

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
                    )
                }
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
    focusSelectedRelic: Boolean = true,
    perspective: MapPerspective = MapPerspective.GOD,
    onRelicSelected: (MapRelic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }
    var renderedMapKey by remember { mutableStateOf("__unrendered__") }
    var cameraInitialised by remember { mutableStateOf(false) }
    var cameraCentredOnLocation by remember { mutableStateOf(false) }
    var cameraSelectedRelicId by remember { mutableStateOf<String?>(null) }
    var cameraRelicSignature by remember { mutableStateOf("") }
    var currentLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var renderedRelicMarkers by remember { mutableStateOf<Map<String, Marker>>(emptyMap()) }
    var renderedPulseBucket by remember { mutableIntStateOf(-1) }
    var mapStyleConfigured by remember { mutableStateOf(false) }
    var renderedPerspective by remember { mutableStateOf<MapPerspective?>(null) }
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
                    } ?: false
                }
                map.isMyLocationEnabled = false
                val selectedRelicId = selectedRelic?.id ?: "__none__"
                val relicSignature = relics.joinToString("|") { relic ->
                    "${relic.id}:${relic.coordinate.latitude}:${relic.coordinate.longitude}"
                }
                val mapKey = "$selectedRelicId|$relicSignature"
                if (renderedMapKey != mapKey) {
                    map.clear()
                    currentLocationMarker = null
                    renderedRelicMarkers = renderRelics(context, map, relics, selectedRelic, markerPulse)
                    renderedMapKey = mapKey
                }
                val pulseBucket = (markerPulse * 20).toInt()
                if (renderedPulseBucket != pulseBucket) {
                    val pulseIcon = questMarkerIcon(context, selected = false, pulseScale = markerPulse)
                    renderedRelicMarkers.forEach { (relicId, marker) ->
                        if (relicId != selectedRelic?.id) marker.setIcon(pulseIcon)
                    }
                    renderedPulseBucket = pulseBucket
                }
                currentLocationMarker = renderCurrentLocation(
                    context = context,
                    map = map,
                    currentLocationMarker = currentLocationMarker,
                    currentLocation = locationOutput.currentLocation,
                    headingDegrees = deviceHeading,
                )
                if (perspective == MapPerspective.HUNT && locationOutput.currentLocation != null) {
                    moveCameraToHuntView(map, locationOutput.currentLocation, deviceHeading)
                    cameraInitialised = true
                    cameraCentredOnLocation = true
                    cameraSelectedRelicId = null
                    cameraRelicSignature = relicSignature
                } else if (renderedPerspective == MapPerspective.HUNT) {
                    moveCameraToCampus(map, relics, locationOutput.currentLocation)
                    cameraInitialised = true
                    cameraCentredOnLocation = locationOutput.currentLocation != null
                    cameraSelectedRelicId = if (focusSelectedRelic) selectedRelic?.id else null
                    cameraRelicSignature = relicSignature
                } else if (!cameraInitialised) {
                    if (focusSelectedRelic && selectedRelic != null) {
                        moveCameraToRelic(map, selectedRelic)
                    } else {
                        moveCameraToCampus(map, relics, locationOutput.currentLocation)
                    }
                    cameraInitialised = true
                    cameraCentredOnLocation = locationOutput.currentLocation != null
                    cameraSelectedRelicId = if (focusSelectedRelic) selectedRelic?.id else null
                    cameraRelicSignature = relicSignature
                } else if (!cameraCentredOnLocation && locationOutput.currentLocation != null) {
                    moveCameraToCampus(map, relics, locationOutput.currentLocation)
                    cameraCentredOnLocation = true
                } else if (!focusSelectedRelic && relics.isNotEmpty() && cameraRelicSignature != relicSignature) {
                    moveCameraToCampus(map, relics, locationOutput.currentLocation)
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
                renderedPerspective = perspective
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
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(label, fontWeight = FontWeight.Bold)
                }
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 6.dp,
        color = Color.White.copy(alpha = 0.96f),
    ) {
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
private fun SimulationButton(
    simulation: ProximitySimulation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.96f)),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                simulation.label,
                color = Brand,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium,
            )
            Text("Tap to advance", color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TreasureDetailScreen(
    relic: MapRelic,
    locationOutput: LocationOutput,
    deviceHeading: Float?,
    orientationOutput: OrientationOutput,
    isFound: Boolean,
    collecting: Boolean,
    startInCompass: Boolean,
    hapticsEnabled: Boolean,
    onCollected: ((String?) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    var stage by remember(relic.id, isFound, startInCompass) {
        mutableStateOf(
            when {
                startInCompass && !isFound -> TreasureHuntStage.COMPASS
                else -> TreasureHuntStage.DETAILS
            },
        )
    }
    var detailsVisible by remember(relic.id) { mutableStateOf(false) }
    var collectionActionError by remember(relic.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(relic.id) {
        detailsVisible = true
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
    if (stage == TreasureHuntStage.MINI_GAME) {
        MiniGamePlaceholderPanel(
            relic = relic,
            onBack = { stage = TreasureHuntStage.COMPASS },
            onCompleted = { stage = TreasureHuntStage.DIGGING },
        )
        return
    }
    if (stage == TreasureHuntStage.DIGGING) {
        TreasureDiggingPanel(
            relic = relic,
            onFinished = { stage = TreasureHuntStage.FOUND },
        )
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        GoogleMapView(
            relics = listOf(relic),
            selectedRelic = relic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading ?: 0f,
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
                                onStartHunt = {
                                    stage = if (isFound) TreasureHuntStage.STORY else TreasureHuntStage.COMPASS
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TreasureHuntStage.COMPASS -> TreasureCompassPanel(
                            relic = relic,
                            locationOutput = locationOutput,
                            deviceHeading = deviceHeading,
                            orientationOutput = orientationOutput,
                            hapticsEnabled = hapticsEnabled,
                            onSignalFound = { stage = TreasureHuntStage.MINI_GAME },
                            onBack = { stage = TreasureHuntStage.DETAILS },
                        )
                        TreasureHuntStage.MINI_GAME,
                        TreasureHuntStage.DIGGING,
                        TreasureHuntStage.FOUND,
                        TreasureHuntStage.STORY -> Unit
                    }
                }
            }
        }
    }
}

private enum class TreasureHuntStage { DETAILS, COMPASS, MINI_GAME, DIGGING, FOUND, STORY }

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
        if (expanded) {
            IconButton(onClick = onChevron) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    "Collapse treasure details",
                    tint = Brand,
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            TextButton(onClick = onChevron) {
                Text("Learn more", color = Brand, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Brand)
            }
        }
    }
}

@Composable
private fun TreasureInformationPanel(
    relic: MapRelic,
    locationOutput: LocationOutput,
    isFound: Boolean,
    onStartHunt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val distance = locationOutput.distanceToTargetMeters
    val huntReady = distance != null && distance <= HUNT_READY_RADIUS_METERS
    val huntStatus = when {
        isFound -> "Discovered"
        locationOutput.proximity == ProximityState.NEARBY || locationOutput.proximity == ProximityState.INSIDE -> "Nearby"
        else -> "Locked"
    }
    val statusColor = when (huntStatus) {
        "Discovered" -> Color(0xFF3F7D4A)
        "Nearby" -> Color(0xFFD07B2D)
        else -> Color(0xFF76665B)
    }
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onStartHunt,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = isFound || huntReady,
            ) {
                Image(painterResource(R.drawable.map_arrived_symbol), null, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isFound) "View Treasure" else "Start Hunt")
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
                if (!isFound) {
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1C9)),
                        ) {
                            Text(
                                if (huntReady) "You are within 10 m. Start the hunt and follow the compass signal."
                                else "Move within 10 m to unlock the hunt. The exact treasure story stays hidden until discovery.",
                                modifier = Modifier.padding(16.dp),
                                color = Ink,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                relic.description.takeIf { it.isNotBlank() }?.let { description ->
                    item { Text(description, color = Ink, style = MaterialTheme.typography.bodyLarge, lineHeight = 23.sp) }
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
                if (isFound) {
                    relic.buildingStory.takeIf { it.isNotBlank() }?.let { story ->
                        item { DetailTextRow("Landmark story", story) }
                    }
                    relic.prototypeDesign.takeIf { it.isNotBlank() }?.let { design ->
                        item { DetailTextRow("Relic design", design) }
                    }
                    relic.coordinateSource.takeIf { it.isNotBlank() }?.let { source ->
                        item { DetailTextRow("Location source", source) }
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
private fun TreasureCompassPanel(
    relic: MapRelic,
    locationOutput: LocationOutput,
    deviceHeading: Float?,
    orientationOutput: OrientationOutput,
    hapticsEnabled: Boolean,
    onSignalFound: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var signalLocked by remember(relic.id) { mutableStateOf(false) }
    val distance = locationOutput.distanceToTargetMeters
    val targetBearing = locationOutput.targetBearingDegrees
    val turnDegrees = if (targetBearing != null && deviceHeading != null) {
        signedBearingDifference(targetBearing, deviceHeading.toDouble())
    } else {
        null
    }
    val turn = turnDegrees ?: 0.0
    val readiness = evaluateHuntReadiness(
        distanceMeters = distance,
        turnDegrees = turnDegrees,
        horizontalState = orientationOutput.attitude.horizontalState,
    )
    LaunchedEffect(readiness.allReady, signalLocked, hapticsEnabled) {
        if (readiness.allReady && !signalLocked) {
            signalLocked = true
            if (hapticsEnabled) vibrateTreasureLock(context)
        }
    }
    val instruction = when {
        signalLocked -> "Treasure signal locked! The hidden challenge has awakened."
        distance == null || targetBearing == null -> "Waiting for a reliable location signal…"
        !readiness.nearTreasure -> "The signal faded. Move back within 10 m."
        deviceHeading == null -> "Waking the compass… hold your phone steady."
        !readiness.facingTreasure && turn > 0 -> "Turn right ${abs(turn).formatDegrees()} toward the treasure."
        !readiness.facingTreasure -> "Turn left ${abs(turn).formatDegrees()} toward the treasure."
        orientationOutput.attitude.horizontalState == HorizontalState.UNKNOWN -> "Calibrating the spirit level…"
        !readiness.phoneHorizontal -> "Lower the phone until it is flat and level."
        readiness.allReady -> "Hold it there — locking onto the treasure…"
        turn > 0 -> "Turn right ${abs(turn).formatDegrees()}"
        else -> "Turn left ${abs(turn).formatDegrees()}"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Follow the treasure signal",
                color = Ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            CompassSignalVisual(
                relic = relic,
                turnDegrees = turnDegrees?.toFloat() ?: 0f,
                signalAvailable = turnDegrees != null,
                pitchDegrees = orientationOutput.attitude.pitchDegrees?.toFloat(),
                rollDegrees = orientationOutput.attitude.rollDegrees?.toFloat(),
                signalLocked = signalLocked,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (signalLocked || readiness.allReady) BrandSoft else Color(0xFFFFF1C9),
                ),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        instruction,
                        modifier = Modifier.fillMaxWidth(),
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    HuntConditionRow(
                        label = "Near the treasure",
                        detail = "Distance ${distance.formatDistance()} · need 10 m or less",
                        satisfied = readiness.nearTreasure,
                    )
                    HuntConditionRow(
                        label = "Facing the treasure",
                        detail = turnDegrees?.let { "${abs(it).formatDegrees()} from the signal" } ?: "Waiting for heading sensor",
                        satisfied = readiness.facingTreasure,
                    )
                    HuntConditionRow(
                        label = "Phone flat and level",
                        detail = formatPhoneTilt(orientationOutput),
                        satisfied = readiness.phoneHorizontal,
                    )
                }
            }
        }
        if (signalLocked) {
            item {
                Button(
                    onClick = onSignalFound,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Image(painterResource(R.drawable.nav_treasure_symbol), null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play mini-game", fontWeight = FontWeight.Bold)
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
private fun CompassSignalVisual(
    relic: MapRelic,
    turnDegrees: Float,
    signalAvailable: Boolean,
    pitchDegrees: Float?,
    rollDegrees: Float?,
    signalLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedTurn by animateFloatAsState(turnDegrees, tween(350), label = "treasure_compass_turn")
    val animatedPitch by animateFloatAsState(
        (pitchDegrees ?: 0f).coerceIn(-30f, 30f),
        tween(220),
        label = "treasure_compass_pitch",
    )
    val animatedRoll by animateFloatAsState(
        (rollDegrees ?: 0f).coerceIn(-30f, 30f),
        tween(220),
        label = "treasure_compass_roll",
    )
    val density = LocalDensity.current.density
    Card(
        modifier = modifier
            .height(260.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .graphicsLayer {
                rotationX = 9f + animatedPitch * 0.42f
                rotationY = -animatedRoll * 0.42f
                cameraDistance = 24f * density
                shadowElevation = 18f * density
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4B3024)),
    ) {
        Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val centre = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.42f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6A4934), Color(0xFF2E1B13)),
                        center = centre - Offset(radius * 0.22f, radius * 0.25f),
                        radius = radius * 1.25f,
                    ),
                    radius = radius,
                    center = centre,
                )
                drawCircle(Color.Black.copy(alpha = 0.32f), radius = radius * 0.98f, center = centre + Offset(0f, 10f), style = Stroke(width = 12f))
                drawCircle(
                    if (signalLocked) Color(0xFFFFD76A) else Color(0xFFE9B34F),
                    radius = radius,
                    center = centre,
                    style = Stroke(width = if (signalLocked) 10f else 7f),
                )
                drawCircle(Color(0xFFE9B34F).copy(alpha = 0.22f), radius = radius * 0.68f, center = centre, style = Stroke(width = 3f))
                repeat(12) { index ->
                    val angle = Math.toRadians(index * 30.0 - 90.0)
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
                if (signalAvailable) {
                    val arrowAngle = Math.toRadians(animatedTurn.toDouble() - 90.0)
                    val tip = Offset(
                        centre.x + kotlin.math.cos(arrowAngle).toFloat() * radius * 0.76f,
                        centre.y + kotlin.math.sin(arrowAngle).toFloat() * radius * 0.76f,
                    )
                    val tail = Offset(
                        centre.x - kotlin.math.cos(arrowAngle).toFloat() * radius * 0.34f,
                        centre.y - kotlin.math.sin(arrowAngle).toFloat() * radius * 0.34f,
                    )
                    drawLine(Color(0xFFE9B34F), centre, tail, strokeWidth = 9f)
                    drawLine(Color(0xFFF25B45), centre, tip, strokeWidth = 14f)
                    drawCircle(Color.White, radius = 12f, center = centre)
                    drawCircle(Color(0xFF4B3024), radius = 6f, center = centre)
                }
            }
            TreasurePrototypeImage(
                relic = relic,
                discovered = false,
                modifier = Modifier.size(68.dp),
            )
            Text("N", modifier = Modifier.align(Alignment.TopCenter), color = Color.White, fontWeight = FontWeight.Bold)
            if (signalLocked) {
                Text(
                    "SIGNAL LOCKED",
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = Color(0xFFFFD76A),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun HuntConditionRow(label: String, detail: String, satisfied: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (satisfied) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Brand, modifier = Modifier.size(25.dp))
        } else {
            Text("○", color = Muted, fontSize = 28.sp, lineHeight = 28.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MiniGamePlaceholderPanel(
    relic: MapRelic,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF8F0E4)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "Treasure challenge",
            modifier = Modifier.fillMaxWidth(),
            color = Ink,
            fontFamily = GothicTreasureFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
        )
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.nav_treasure_symbol),
                    contentDescription = null,
                    modifier = Modifier.size(104.dp),
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    relic.name,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Mini-game prototype",
                    color = Brand,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "The full challenge is coming soon. For now, use the button below to simulate winning the mini-game.",
                    color = Muted,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Button(
            onClick = onCompleted,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Complete mini-game", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Back to compass", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TreasureDiggingPanel(relic: MapRelic, onFinished: () -> Unit) {
    val revealProgress = remember(relic.id) { Animatable(0f) }
    val density = LocalDensity.current.density
    LaunchedEffect(relic.id) {
        revealProgress.snapTo(0f)
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2_500, easing = FastOutSlowInEasing),
        )
        delay(650L)
        onFinished()
    }
    val progress = revealProgress.value
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFF0CE), Color(0xFFD69A5B), Color(0xFF6A3F27)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val groundY = size.height * 0.61f
            drawRect(Color(0xFFA8663A), topLeft = Offset(0f, groundY), size = Size(size.width, size.height - groundY))
            drawRect(Color(0xFF7A472D), topLeft = Offset(0f, groundY + size.height * 0.12f), size = Size(size.width, size.height * 0.27f))
            val holeWidth = size.width * (0.22f + progress * 0.34f)
            val holeHeight = size.height * (0.035f + progress * 0.055f)
            drawOval(
                color = Color(0xFF321C16).copy(alpha = 0.45f + progress * 0.45f),
                topLeft = Offset(size.width / 2f - holeWidth / 2f, groundY - holeHeight / 2f),
                size = Size(holeWidth, holeHeight),
            )
            val burst = (progress * 1.8f).coerceIn(0f, 1f)
            repeat(14) { index ->
                val angle = Math.toRadians(205.0 + index * 10.5)
                val travel = size.minDimension * (0.10f + (index % 4) * 0.025f) * burst
                val particle = Offset(
                    size.width / 2f + kotlin.math.cos(angle).toFloat() * travel,
                    groundY + kotlin.math.sin(angle).toFloat() * travel - burst * size.height * 0.08f,
                )
                drawCircle(
                    color = Color(0xFF8B512F).copy(alpha = (1f - burst * 0.72f).coerceIn(0f, 1f)),
                    radius = 7f + (index % 3) * 4f,
                    center = particle,
                )
            }
            repeat(9) { index ->
                val angle = Math.toRadians(index * 40.0)
                val sparkleDistance = size.minDimension * 0.22f * progress
                val sparkle = Offset(
                    size.width / 2f + kotlin.math.cos(angle).toFloat() * sparkleDistance,
                    groundY - size.height * 0.18f + kotlin.math.sin(angle).toFloat() * sparkleDistance * 0.45f,
                )
                drawCircle(
                    color = Color(0xFFFFE28A).copy(alpha = progress),
                    radius = if (index % 2 == 0) 7f else 4f,
                    center = sparkle,
                )
            }
        }
        Column(
            Modifier.align(Alignment.TopCenter).padding(top = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (progress < 0.92f) "Digging through history…" else "Treasure unearthed!",
                color = Ink,
                fontFamily = GothicTreasureFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
            )
            Text("The ground is giving up its secret", color = Ink.copy(alpha = 0.72f))
        }
        Text(
            "⛏",
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationX = -95f * density
                    translationY = (55f - progress * 28f) * density
                    rotationZ = -55f + progress * 105f
                },
            fontSize = 72.sp,
        )
        TreasurePrototypeImage(
            relic = relic,
            discovered = true,
            modifier = Modifier
                .align(Alignment.Center)
                .size(180.dp)
                .graphicsLayer {
                    translationY = (155f * (1f - progress) - 15f * progress) * density
                    scaleX = 0.55f + progress * 0.45f
                    scaleY = 0.55f + progress * 0.45f
                    alpha = (progress * 1.5f).coerceIn(0f, 1f)
                    rotationZ = (1f - progress) * -8f
                },
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
private fun TreasureStoryPanel(
    relic: MapRelic,
    collecting: Boolean,
    collectionError: String?,
    onPutInBackpack: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFF8F0E4)),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
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
                        fallbackDrawableRes = relic.localTreasureArtworkResId(),
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
        item {
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
                .icon(if (relic.id == selectedRelic?.id) selectedIcon else availableIcon)
                .anchor(0.5f, 0.5f),
        )
        marker?.tag = relic.id
        marker?.let { markers[relic.id] = it }
    }
    return markers
}

private fun renderCurrentLocation(
    context: Context,
    map: GoogleMap,
    currentLocationMarker: Marker?,
    currentLocation: GeoCoordinate?,
    headingDegrees: Float,
): Marker? {
    if (currentLocation == null) {
        currentLocationMarker?.remove()
        return null
    }
    val position = currentLocation.toLatLng()
    if (currentLocationMarker != null) {
        currentLocationMarker.position = position
        currentLocationMarker.rotation = headingDegrees
        return currentLocationMarker
    }
    return map.addMarker(
        MarkerOptions()
            .position(position)
            .title("You")
            .snippet("Your current position")
            .icon(currentLocationIcon(context))
            .anchor(0.5f, 0.72f)
            .flat(true)
            .rotation(headingDegrees),
    )
}

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
    if (points.isEmpty()) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CAMPUS_CENTRE.toLatLng(), 16f))
        return
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

private fun moveCameraToHuntView(map: GoogleMap, currentLocation: GeoCoordinate, headingDegrees: Float) {
    val cameraTarget = coordinateAhead(
        origin = currentLocation,
        distanceMeters = HUNT_CAMERA_LEAD_METERS,
        bearingDegrees = headingDegrees.toDouble(),
    )
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

private fun coordinateAhead(
    origin: GeoCoordinate,
    distanceMeters: Double,
    bearingDegrees: Double,
): GeoCoordinate {
    val angularDistance = distanceMeters / EARTH_RADIUS_METERS
    val bearing = Math.toRadians(bearingDegrees)
    val latitude = Math.toRadians(origin.latitude)
    val longitude = Math.toRadians(origin.longitude)
    val destinationLatitude = asin(
        sin(latitude) * cos(angularDistance) +
            cos(latitude) * sin(angularDistance) * cos(bearing),
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

private val DEFAULT_CAMPUS_CENTRE = GeoCoordinate(-37.7986, 144.9602)
private const val RADAR_SCAN_RADIUS_METERS = 100.0
private const val REVEAL_RADIUS_METERS = 50.0
private const val HUNT_READY_RADIUS_METERS = 10.0
private const val COMPASS_ALIGNMENT_TOLERANCE_DEGREES = 15.0
private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val HUNT_CAMERA_LEAD_METERS = 80.0
private const val HUNT_CAMERA_ZOOM = 18.5f
private const val HUNT_CAMERA_TILT = 45f

internal enum class RadarSignalRange { OUT_OF_RANGE, NEARBY_HIDDEN, REVEALED, HUNT_READY }

internal fun radarSignalForDistance(distanceMeters: Double): RadarSignalRange = when {
    distanceMeters <= HUNT_READY_RADIUS_METERS -> RadarSignalRange.HUNT_READY
    distanceMeters <= REVEAL_RADIUS_METERS -> RadarSignalRange.REVEALED
    distanceMeters <= RADAR_SCAN_RADIUS_METERS -> RadarSignalRange.NEARBY_HIDDEN
    else -> RadarSignalRange.OUT_OF_RANGE
}

private fun treasureProximityMessage(
    distanceMeters: Double,
    currentLocation: GeoCoordinate,
    treasureLocation: GeoCoordinate,
): String = when (radarSignalForDistance(distanceMeters)) {
    RadarSignalRange.HUNT_READY ->
        "The treasure is right before your eyes — steady your compass and begin the hunt!"
    RadarSignalRange.REVEALED -> {
        val direction = bearingToCompassDirection(LocationCalculator.bearingDegrees(currentLocation, treasureLocation))
        "Hot trail! A treasure is within 50 m, lurking to the $direction."
    }
    RadarSignalRange.NEARBY_HIDDEN ->
        "Your relic-sense is tingling… a treasure is hiding nearby!"
    RadarSignalRange.OUT_OF_RANGE ->
        "The trail has gone quiet — no treasure within 100 m. Follow the hint and venture closer!"
}

internal fun bearingToCompassDirection(bearingDegrees: Double): String {
    val directions = arrayOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")
    val normalized = LocationCalculator.normalizeDegrees(bearingDegrees)
    return directions[((normalized + 22.5) / 45.0).toInt() % directions.size]
}

private fun ProximitySimulation.next(): ProximitySimulation = when (this) {
    ProximitySimulation.OFF -> ProximitySimulation.HUNDRED_METRES
    ProximitySimulation.HUNDRED_METRES -> ProximitySimulation.FIFTY_METRES
    ProximitySimulation.FIFTY_METRES -> ProximitySimulation.TEN_METRES
    ProximitySimulation.TEN_METRES -> ProximitySimulation.OFF
}

private fun isProbablyEmulator(): Boolean =
    Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.lowercase(Locale.US).contains("emulator") ||
        Build.MODEL.lowercase(Locale.US).let {
            "google_sdk" in it || "sdk_gphone" in it || "emulator" in it || "android sdk built for" in it
        } ||
        Build.MANUFACTURER.lowercase(Locale.US).contains("genymotion") ||
        Build.PRODUCT.lowercase(Locale.US).let { it.startsWith("sdk") || "emulator" in it } ||
        Build.HARDWARE.lowercase(Locale.US).let { "goldfish" in it || "ranchu" in it } ||
        (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))

internal data class HuntReadiness(
    val nearTreasure: Boolean,
    val facingTreasure: Boolean,
    val phoneHorizontal: Boolean,
) {
    val allReady: Boolean get() = nearTreasure && facingTreasure && phoneHorizontal
}

internal fun evaluateHuntReadiness(
    distanceMeters: Double?,
    turnDegrees: Double?,
    horizontalState: HorizontalState,
): HuntReadiness = HuntReadiness(
    nearTreasure = distanceMeters != null && distanceMeters <= HUNT_READY_RADIUS_METERS,
    facingTreasure = turnDegrees != null && abs(turnDegrees) <= COMPASS_ALIGNMENT_TOLERANCE_DEGREES,
    phoneHorizontal = horizontalState == HorizontalState.HORIZONTAL,
)

private fun formatPhoneTilt(orientationOutput: OrientationOutput): String {
    val pitch = orientationOutput.attitude.pitchDegrees
    val roll = orientationOutput.attitude.rollDegrees
    return if (pitch == null || roll == null) {
        "Waiting for level sensor"
    } else {
        String.format(Locale.US, "Pitch %.0f° · Roll %.0f° · keep within ±12°", pitch, roll)
    }
}

@Suppress("DEPRECATION")
private fun vibrateTreasureLock(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return
    if (!vibrator.hasVibrator()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 90L, 70L, 180L), -1))
    } else {
        vibrator.vibrate(longArrayOf(0L, 90L, 70L, 180L), -1)
    }
}

internal fun signedBearingDifference(targetBearingDegrees: Double, deviceHeadingDegrees: Double): Double =
    ((targetBearingDegrees - deviceHeadingDegrees + 540.0) % 360.0) - 180.0

private fun Double?.formatDistance(): String = when {
    this == null -> "unknown"
    this >= 1000.0 -> String.format(Locale.US, "%.2f km", this / 1000.0)
    else -> "${kotlin.math.round(this / 10.0).toInt() * 10} m"
}

private fun Double?.formatDegrees(): String =
    this?.let { String.format(Locale.US, "%.0f°", it) } ?: "unknown"

private fun ProximityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }
