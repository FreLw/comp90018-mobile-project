package com.comp90018.app.features.map

import android.content.Context
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationAvailabilityState
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationConfig
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.location.LocationPermissionState
import com.comp90018.app.sensors.location.ProximityState
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.R
import com.comp90018.app.RelicRed
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
fun MapScreen() {
    var selectedRelic by remember { mutableStateOf<MapRelic?>(null) }
    var detailRelic by remember { mutableStateOf<MapRelic?>(null) }
    val foundRelicIds = LocalTreasureCollection.foundIds
    val deviceHeading = rememberDeviceHeading()
    val activeRelic = detailRelic ?: selectedRelic
    val locationOutput = remember(activeRelic) { stopOneLocationOutput(activeRelic) }
    val markerTransition = rememberInfiniteTransition(label = "map_quest_marker")
    val markerPulse by markerTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "map_quest_marker_pulse",
    )

    detailRelic?.let { relic ->
        TreasureDetailScreen(
            relic = relic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            isFound = relic.id in foundRelicIds,
            onCollected = { LocalTreasureCollection.add(relic.id) },
            onBack = {
                detailRelic = null
            },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = sampleMapRelics,
            selectedRelic = selectedRelic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            markerPulse = if (selectedRelic == null) markerPulse else 1f,
            focusSelectedRelic = false,
            onRelicSelected = {
                selectedRelic = it
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (selectedRelic == null) {
            FindTreasurePrompt(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
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
private fun GoogleMapView(
    relics: List<MapRelic>,
    selectedRelic: MapRelic?,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    markerPulse: Float = 1f,
    focusSelectedRelic: Boolean = true,
    onRelicSelected: (MapRelic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }
    var renderedSelectedRelicId by remember { mutableStateOf("__unrendered__") }
    var cameraInitialised by remember { mutableStateOf(false) }
    var cameraSelectedRelicId by remember { mutableStateOf<String?>(null) }
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
                map.isMyLocationEnabled = false
                val selectedRelicId = selectedRelic?.id ?: "__none__"
                if (renderedSelectedRelicId != selectedRelicId) {
                    map.clear()
                    currentLocationMarker = null
                    renderedRelicMarkers = renderRelics(context, map, relics, selectedRelic, markerPulse)
                    renderedSelectedRelicId = selectedRelicId
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
                if (!cameraInitialised) {
                    if (focusSelectedRelic && selectedRelic != null) {
                        moveCameraToRelic(map, selectedRelic)
                    } else {
                        moveCameraToCampus(map, relics, locationOutput.currentLocation)
                    }
                    cameraInitialised = true
                    cameraSelectedRelicId = if (focusSelectedRelic) selectedRelic?.id else null
                } else if (focusSelectedRelic && selectedRelic != null && cameraSelectedRelicId != selectedRelic.id) {
                    moveCameraToRelic(map, selectedRelic)
                    cameraSelectedRelicId = selectedRelic.id
                }
            }
        },
    )
}

@Composable
private fun FindTreasurePrompt(modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
        Text(
            "Psst… tap a treasure and see what’s hiding nearby!",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            color = Ink,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TreasureDetailScreen(
    relic: MapRelic,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    isFound: Boolean,
    onCollected: () -> Unit,
    onBack: () -> Unit,
) {
    var navigating by remember(relic.id) { mutableStateOf(false) }
    var stage by remember(relic.id) { mutableStateOf(TreasureHuntStage.DETAILS) }
    var searchStep by remember(relic.id) { mutableIntStateOf(0) }
    var detailsVisible by remember(relic.id) { mutableStateOf(false) }

    LaunchedEffect(relic.id) {
        detailsVisible = true
    }

    LaunchedEffect(stage) {
        if (stage == TreasureHuntStage.SEARCHING) {
            searchStep = 0
            delay(1_500)
            searchStep = 1
            delay(1_500)
            searchStep = 2
            delay(1_500)
            stage = TreasureHuntStage.READY_TO_DIG
        }
    }

    if (stage == TreasureHuntStage.FOUND) {
        TreasureFoundPanel(relic = relic, onViewStory = { stage = TreasureHuntStage.STORY })
        return
    }
    if (stage == TreasureHuntStage.STORY) {
        TreasureStoryPanel(
            relic = relic,
            onPutInBackpack = {
                onCollected()
                stage = TreasureHuntStage.DETAILS
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
                                onNavigate = { navigating = true },
                                onArrived = { stage = TreasureHuntStage.SEARCHING },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TreasureHuntStage.SEARCHING -> TreasureSearchPanel(
                            searchStep = searchStep,
                            readyToDig = false,
                            deviceHeading = deviceHeading,
                            onDig = {},
                            onBack = { stage = TreasureHuntStage.DETAILS },
                        )
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
    onNavigate: () -> Unit,
    onArrived: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Button(onClick = onArrived, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
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
                            title = "Hunt type",
                            value = "${relic.huntType} · ${relic.requiredPlayers} ${if (relic.requiredPlayers == 1) "explorer" else "explorers"}",
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
                item { DetailTextRow("Challenge level", relic.difficulty) }
                item { DetailTextRow("Quest condition", relic.taskHint) }
                if (navigating) item {
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandSoft),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("The trail is awake", color = Brand, style = MaterialTheme.typography.titleMedium)
                            Text("Follow the map glow toward ${relic.locationName}; the relic will call louder as you approach.", color = Ink)
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
    val acceleration = listOf(1.28f, 0.46f, 0.12f)[phase]
    val levelTilt = listOf(12.4f, 4.8f, 1.2f)[phase]
    val simulatedHeading = listOf(28f, 74f, 118f)[phase] + (deviceHeading * 0.02f)
    val command = when {
        readyToDig -> "Signal captured — the relic is beneath your feet."
        phase == 0 -> "Freeze the trail — stop and steady your stance."
        phase == 1 -> "Balance the relic lens — hold your phone level."
        else -> "Sweep the horizon — rotate slowly until the signal blooms."
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                command,
                color = Ink,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            if (readyToDig) {
                AnimatedTreasureChest(Modifier.size(150.dp))
            } else {
                RelicScannerVisual(
                    acceleration = acceleration,
                    levelTilt = levelTilt,
                    heading = simulatedHeading,
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
                Text(
                    if (readyToDig) "The hidden lock has opened. Dig when your team is ready."
                    else "Calm motion, centre the level spark, then turn with the compass glow.",
                    modifier = Modifier.padding(16.dp),
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (readyToDig) {
            item {
                Button(
                    onClick = onDig,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Image(painterResource(R.drawable.nav_treasure_symbol), null, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Dig for treasure", fontWeight = FontWeight.Bold)
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
    Image(
        painter = painterResource(if (discovered) relic.imageRes else R.drawable.treasure_unknown),
        contentDescription = if (discovered) relic.name else "Unknown treasure",
        modifier = modifier,
    )
}

@Composable
private fun TreasureSilhouette(relic: MapRelic, discovered: Boolean, modifier: Modifier = Modifier) {
    if (discovered) {
        TreasureArtwork(relic, discovered = true, modifier = modifier)
    } else {
        Image(
            painter = painterResource(relic.imageRes),
            contentDescription = "Locked ${relic.name} silhouette",
            modifier = modifier,
            colorFilter = ColorFilter.tint(Color(0xFF5A4032)),
        )
    }
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
private fun TreasureStoryPanel(relic: MapRelic, onPutInBackpack: () -> Unit) {
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
        item { DetailTextRow("Found at", relic.locationName) }
        item { DetailTextRow("Discovery", relic.description) }
        item {
            Button(
                onClick = onPutInBackpack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Image(painterResource(R.drawable.nav_treasure_symbol), null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Put in backpack", fontWeight = FontWeight.Bold)
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
    if (currentLocation == null) return currentLocationMarker
    val position = currentLocation.toLatLng()
    if (currentLocationMarker != null) {
        currentLocationMarker.position = position
        currentLocationMarker.rotation = headingDegrees
        return currentLocationMarker
    }
    return map.addMarker(
        MarkerOptions()
            .position(position)
            .title("You · Melbourne University Stop 1")
            .snippet("Demo starting location")
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

private val MELBOURNE_UNIVERSITY_STOP_ONE = GeoCoordinate(-37.7986, 144.9602)

private fun stopOneLocationOutput(relic: MapRelic?): LocationOutput = LocationCalculator.buildOutput(
    currentLocation = MELBOURNE_UNIVERSITY_STOP_ONE,
    targetLocation = relic?.coordinate,
    config = LocationConfig(
        insideRadiusMeters = relic?.insideRadiusMeters ?: 20.0,
        nearbyRadiusMeters = relic?.nearbyRadiusMeters ?: 120.0,
    ),
    permission = LocationPermissionState.GRANTED,
    availability = LocationAvailabilityState.AVAILABLE,
    accuracyMeters = 5.0,
)

private fun Double?.formatDistance(): String = when {
    this == null -> "unknown"
    this >= 1000.0 -> String.format(Locale.US, "%.2f km", this / 1000.0)
    else -> "${kotlin.math.round(this / 10.0).toInt() * 10} m"
}

private fun Double?.formatDegrees(): String =
    this?.let { String.format(Locale.US, "%.0f°", it) } ?: "unknown"

private fun ProximityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }
