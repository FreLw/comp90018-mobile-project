package com.comp90018.app.features.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.delay
import java.util.Locale

/** Dedicated map feature boundary; location rendering belongs here. */
@Composable
fun MapScreen() {
    var selectedRelic by remember { mutableStateOf<MapRelic?>(null) }
    var detailRelic by remember { mutableStateOf<MapRelic?>(null) }
    val deviceHeading = rememberDeviceHeading()
    val activeRelic = detailRelic ?: selectedRelic
    val locationOutput = remember(activeRelic) { stopOneLocationOutput(activeRelic) }

    detailRelic?.let { relic ->
        TreasureDetailScreen(
            relic = relic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            onBack = {
                detailRelic = null
                selectedRelic = null
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
            onRelicSelected = {
                selectedRelic = it
                detailRelic = it
            },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedContent(
            targetState = detailRelic == null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            },
            label = "map_prompt",
        ) { visible ->
            if (visible) {
                FindTreasurePrompt(Modifier.fillMaxWidth())
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
    MapLifecycle(mapView)

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map ->
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
                    renderRelics(context, map, relics, selectedRelic)
                    renderedSelectedRelicId = selectedRelicId
                }
                currentLocationMarker = renderCurrentLocation(
                    context = context,
                    map = map,
                    currentLocationMarker = currentLocationMarker,
                    currentLocation = locationOutput.currentLocation,
                    headingDegrees = deviceHeading,
                )
                if (!cameraInitialised) {
                    moveCameraToCampus(map, relics, locationOutput.currentLocation)
                    cameraInitialised = true
                    cameraSelectedRelicId = selectedRelic?.id
                } else if (selectedRelic != null && cameraSelectedRelicId != selectedRelic.id) {
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
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.nav_map_game), null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(9.dp))
            Text("Tap any treasure on the map to view its details.", color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TreasureDetailScreen(
    relic: MapRelic,
    locationOutput: LocationOutput,
    deviceHeading: Float,
    onBack: () -> Unit,
) {
    var navigating by remember(relic.id) { mutableStateOf(false) }
    var expanded by remember(relic.id) { mutableStateOf(false) }
    var stage by remember(relic.id) { mutableStateOf(TreasureHuntStage.DETAILS) }
    var searchStep by remember(relic.id) { mutableIntStateOf(0) }

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

    val handleBack: () -> Unit = {
        when {
            stage == TreasureHuntStage.DETAILS && expanded -> expanded = false
            stage == TreasureHuntStage.DETAILS -> onBack()
            stage == TreasureHuntStage.STORY -> stage = TreasureHuntStage.FOUND
            else -> stage = TreasureHuntStage.DETAILS
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        GoogleMapView(
            relics = listOf(relic),
            selectedRelic = relic,
            locationOutput = locationOutput,
            deviceHeading = deviceHeading,
            onRelicSelected = {},
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.96f),
            shadowElevation = 4.dp,
        ) {
            IconButton(onClick = handleBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Ink) }
        }

        val collapsed = stage == TreasureHuntStage.DETAILS && !expanded
        val expandedHeight = (maxHeight - 260.dp).coerceAtLeast(420.dp)
        val sheetHeight by animateDpAsState(
            targetValue = if (collapsed) 120.dp else expandedHeight,
            animationSpec = tween(420),
            label = "treasure_sheet_height",
        )
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(sheetHeight),
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
                            showLearnMore = !expanded,
                            onLearnMore = { expanded = true },
                        )
                        if (expanded) {
                            TreasureInformationPanel(
                                relic = relic,
                                locationOutput = locationOutput,
                                navigating = navigating,
                                onNavigate = { navigating = true },
                                onArrived = { stage = TreasureHuntStage.SEARCHING },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    TreasureHuntStage.SEARCHING -> TreasureSearchPanel(
                        searchStep = searchStep,
                        readyToDig = false,
                        onDig = {},
                    )
                    TreasureHuntStage.READY_TO_DIG -> TreasureSearchPanel(
                        searchStep = searchStep,
                        readyToDig = true,
                        onDig = { stage = TreasureHuntStage.FOUND },
                    )
                    TreasureHuntStage.FOUND -> TreasureFoundPanel(
                        relic = relic,
                        onViewStory = { stage = TreasureHuntStage.STORY },
                    )
                    TreasureHuntStage.STORY -> TreasureStoryPanel(relic)
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
    showLearnMore: Boolean,
    onLearnMore: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(76.dp).background(BrandSoft, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.nav_treasure_game),
                contentDescription = "Treasure",
                modifier = Modifier.size(66.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                relic.name,
                color = Ink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${relic.locationName} · $distance",
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showLearnMore) {
            androidx.compose.material3.TextButton(onClick = onLearnMore) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Brand, modifier = Modifier.size(28.dp))
                    Text("Learn more", color = Brand, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun TreasureInformationPanel(
    relic: MapRelic,
    locationOutput: LocationOutput,
    navigating: Boolean,
    onNavigate: () -> Unit,
    onArrived: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Text(
            relic.description,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            color = Ink,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = onArrived, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Image(painterResource(R.drawable.map_quest_selected), null, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(6.dp))
                Text("I've arrived")
            }
            OutlinedButton(onClick = onNavigate, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Image(painterResource(R.drawable.nav_map_game), null, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (navigating) "Navigating…" else "Navigate")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        ) {
            item { DetailTextRow("Past logs", "No discoveries recorded yet") }
            item { DetailTextRow("Attributes", "Outdoor location · Search radius ${relic.insideRadiusMeters.toInt()} m") }
            item { DetailTextRow("Target bearing", locationOutput.targetBearingDegrees.formatDegrees()) }
            if (navigating) item {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSoft),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Live navigation", color = Brand, fontWeight = FontWeight.Bold)
                        Text("${locationOutput.proximity.label()} · Stop 1 demo location", color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun TreasureSearchPanel(
    searchStep: Int,
    readyToDig: Boolean,
    onDig: () -> Unit,
) {
    val hint = when {
        readyToDig -> "Treasure signal locked"
        searchStep == 0 -> "Move a little closer"
        searchStep == 1 -> "Face left"
        else -> "Hold your phone steady"
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RadarAnimation(Modifier.size(176.dp))
        Text(
            if (readyToDig) "Treasure found" else "Searching…",
            color = Ink,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
        )
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSoft),
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.nav_map_game), null, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Sensor guidance", color = Brand, fontWeight = FontWeight.Bold)
                        Text(hint, color = Ink, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text(
                    if (readyToDig) "You are close enough to uncover this treasure."
                    else "Distance, direction and motion sensors are narrowing the search area.",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (readyToDig) {
            Button(
                onClick = onDig,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Image(painterResource(R.drawable.nav_treasure_game), null, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(8.dp))
                Text("Dig for treasure", fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Keep moving slowly while the signal updates.", color = Muted)
        }
    }
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
private fun TreasureFoundPanel(relic: MapRelic, onViewStory: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(painterResource(R.drawable.nav_treasure_game), null, modifier = Modifier.size(132.dp))
        Spacer(Modifier.height(18.dp))
        Text(
            "Congratulations! You found a new treasure!",
            color = Ink,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(relic.name, color = Brand, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onViewStory,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("View treasure story", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TreasureStoryPanel(relic: MapRelic) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.nav_treasure_game), null, modifier = Modifier.size(72.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Treasure story", color = Brand, fontWeight = FontWeight.Bold)
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
        item { DetailTextRow("Collection status", "Added to your campus collection") }
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

private fun renderRelics(context: Context, map: GoogleMap, relics: List<MapRelic>, selectedRelic: MapRelic?) {
    val availableIcon = questMarkerIcon(context, selected = false)
    val selectedIcon = questMarkerIcon(context, selected = true)
    relics.forEach { relic ->
        val marker = map.addMarker(
            MarkerOptions()
                .position(relic.coordinate.toLatLng())
                .title(relic.name)
                .snippet(relic.locationName)
                .icon(if (relic.id == selectedRelic?.id) selectedIcon else availableIcon)
                .anchor(0.5f, 1f),
        )
        marker?.tag = relic.id
    }
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
            .anchor(0.5f, 0.5f)
            .flat(true)
            .rotation(headingDegrees),
    )
}

private fun questMarkerIcon(context: Context, selected: Boolean): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val width = ((if (selected) 28 else 18) * density).toInt().coerceAtLeast(1)
    val height = ((if (selected) 42 else 27) * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(214, 48, 38)
        style = Paint.Style.FILL
    }
    val left = width * 0.34f
    val right = width * 0.66f
    canvas.drawRoundRect(left, height * 0.04f, right, height * 0.63f, width * 0.16f, width * 0.16f, paint)
    canvas.drawCircle(width * 0.5f, height * 0.83f, width * 0.16f, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun currentLocationIcon(context: Context): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val size = (44 * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val center = size / 2f
    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(74, 36, 23)
        style = Paint.Style.FILL
    }
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(217, 84, 53)
        style = Paint.Style.FILL
    }
    val arrow = Path().apply {
        moveTo(center, size * 0.04f)
        lineTo(size * 0.70f, size * 0.53f)
        lineTo(center, size * 0.43f)
        lineTo(size * 0.30f, size * 0.53f)
        close()
    }
    canvas.drawPath(arrow, arrowPaint)
    canvas.drawCircle(center, size * 0.65f, size * 0.20f, haloPaint)
    canvas.drawCircle(center, size * 0.65f, size * 0.13f, dotPaint)
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
