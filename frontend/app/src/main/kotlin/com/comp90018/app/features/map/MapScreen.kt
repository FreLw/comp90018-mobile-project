package com.comp90018.app.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Straighten
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.comp90018.app.sensors.location.AndroidLocationSensor
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationAvailabilityState
import com.comp90018.app.sensors.location.LocationOutput
import com.comp90018.app.sensors.location.ProximityState
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

/** Dedicated map feature boundary; location rendering belongs here. */
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val locationSensor = remember(context) { AndroidLocationSensor(context) }
    val locationOutput by locationSensor.output.collectAsState()
    var selectedRelic by remember { mutableStateOf<MapRelic?>(null) }
    var detailRelic by remember { mutableStateOf<MapRelic?>(null) }
    var permissionRefreshKey by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRefreshKey++
        locationSensor.refreshPermissionState()
        locationSensor.start()
    }

    val hasLocationPermission = remember(permissionRefreshKey) {
        context.hasLocationPermission()
    }

    LaunchedEffect(selectedRelic) {
        selectedRelic?.let { locationSensor.setTargetLocation(it.coordinate) }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) locationSensor.start() else locationSensor.stop()
    }

    DisposableEffect(Unit) {
        onDispose { locationSensor.stop() }
    }

    detailRelic?.let { relic ->
        TreasureDetailScreen(
            relic = relic,
            locationOutput = locationOutput,
            hasLocationPermission = hasLocationPermission,
            onBack = { detailRelic = null },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        GoogleMapView(
            relics = sampleMapRelics,
            selectedRelic = selectedRelic,
            locationOutput = locationOutput,
            hasLocationPermission = hasLocationPermission,
            onRelicSelected = { selectedRelic = it },
            modifier = Modifier.fillMaxSize(),
        )

        selectedRelic?.let { relic ->
            MapStatusCard(
                selectedRelic = relic,
                output = locationOutput,
                onOpenDetails = { detailRelic = relic },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 16.dp),
            )
        } ?: FindTreasurePrompt(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )

        if (!hasLocationPermission) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                },
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            ) {
                Icon(Icons.Rounded.GpsFixed, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enable location")
            }
        }
    }
}

@Composable
private fun GoogleMapView(
    relics: List<MapRelic>,
    selectedRelic: MapRelic?,
    locationOutput: LocationOutput,
    hasLocationPermission: Boolean,
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
                enableMyLocationIfAllowed(map, hasLocationPermission)
                val selectedRelicId = selectedRelic?.id ?: "__none__"
                if (renderedSelectedRelicId != selectedRelicId) {
                    map.clear()
                    currentLocationMarker = null
                    renderRelics(map, relics, selectedRelic)
                    renderedSelectedRelicId = selectedRelicId
                }
                currentLocationMarker = renderCurrentLocation(map, currentLocationMarker, locationOutput.currentLocation)
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
private fun MapStatusCard(
    selectedRelic: MapRelic,
    output: LocationOutput,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.97f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(68.dp).background(BrandSoft, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, "Treasure logo placeholder", tint = Brand, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(selectedRelic.name, fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = Brand, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(selectedRelic.locationName, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text(selectedRelic.description, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TreasureMetric(Icons.Rounded.Straighten, output.distanceToTargetMeters.formatDistance())
                    TreasureMetric(Icons.Rounded.Explore, output.proximity.label())
                }
            }
            IconButton(onClick = onOpenDetails) {
                Icon(Icons.Rounded.ChevronRight, "Open treasure details", tint = Brand, modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun FindTreasurePrompt(modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.96f))) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Explore, null, tint = Brand)
            Spacer(Modifier.width(9.dp))
            Text("Find a treasure!", color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TreasureMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = Brand, modifier = Modifier.size(16.dp))
        Text(value, color = Ink, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun TreasureDetailScreen(
    relic: MapRelic,
    locationOutput: LocationOutput,
    hasLocationPermission: Boolean,
    onBack: () -> Unit,
) {
    var navigating by remember(relic.id) { mutableStateOf(false) }
    var searching by remember(relic.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(285.dp)) {
            GoogleMapView(
                relics = listOf(relic),
                selectedRelic = relic,
                locationOutput = locationOutput,
                hasLocationPermission = hasLocationPermission,
                onRelicSelected = {},
                modifier = Modifier.fillMaxSize(),
            )
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.96f),
                shadowElevation = 4.dp,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Ink) }
            }
        }
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .padding(start = 18.dp)
                    .offset(y = (-34).dp)
                    .size(68.dp)
                    .background(BrandSoft, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    "Treasure logo placeholder",
                    tint = Brand,
                    modifier = Modifier.size(38.dp),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 12.dp),
            ) {
                Text(relic.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                Text(relic.locationName, color = Muted)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(onClick = { searching = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.Explore, null)
                Spacer(Modifier.width(6.dp))
                Text(if (searching) "Searching…" else "I've arrived")
            }
            OutlinedButton(onClick = { navigating = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Icon(if (navigating) Icons.Rounded.GpsFixed else Icons.Rounded.NearMe, null)
                Spacer(Modifier.width(6.dp))
                Text(if (navigating) "Navigating…" else "Navigate")
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        ) {
            item { DetailRow(Icons.Rounded.Info, "Description", relic.description) }
            item { DetailRow(Icons.Rounded.Route, "Past logs", "No discoveries recorded yet") }
            item { DetailRow(Icons.Rounded.Explore, "Attributes", "Outdoor location · Search radius ${relic.insideRadiusMeters.toInt()} m") }
            item { DetailRow(Icons.Rounded.Straighten, "Distance", locationOutput.distanceToTargetMeters.formatDistance()) }
            item { DetailRow(Icons.Rounded.NearMe, "Target bearing", locationOutput.targetBearingDegrees.formatDegrees()) }
            if (navigating || searching) item {
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSoft),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (searching) "Searching nearby" else "Live navigation", color = Brand, fontWeight = FontWeight.Bold)
                        Text("${locationOutput.proximity.label()} · GPS ${locationOutput.availability.label()}", color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Brand, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
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

private fun renderRelics(map: GoogleMap, relics: List<MapRelic>, selectedRelic: MapRelic?) {
    relics.forEach { relic ->
        val marker = map.addMarker(
            MarkerOptions()
                .position(relic.coordinate.toLatLng())
                .title(relic.name)
                .snippet(relic.locationName)
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        if (relic.id == selectedRelic?.id) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_ORANGE,
                    ),
                ),
        )
        marker?.tag = relic.id
    }
}

private fun renderCurrentLocation(
    map: GoogleMap,
    currentLocationMarker: Marker?,
    currentLocation: GeoCoordinate?,
): Marker? {
    if (currentLocation == null) return currentLocationMarker
    val position = currentLocation.toLatLng()
    if (currentLocationMarker != null) {
        currentLocationMarker.position = position
        return currentLocationMarker
    }
    return map.addMarker(
        MarkerOptions()
            .position(position)
            .title("Current location")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)),
    )
}

private fun moveCameraToCampus(map: GoogleMap, relics: List<MapRelic>, currentLocation: GeoCoordinate?) {
    val bounds = LatLngBounds.builder().apply {
        relics.forEach { include(it.coordinate.toLatLng()) }
        currentLocation?.let { include(it.toLatLng()) }
    }.build()
    map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
}

private fun moveCameraToRelic(map: GoogleMap, selectedRelic: MapRelic) {
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedRelic.coordinate.toLatLng(), 17f))
}

@SuppressLint("MissingPermission")
private fun enableMyLocationIfAllowed(map: GoogleMap, allowed: Boolean) {
    map.isMyLocationEnabled = allowed
}

private fun GeoCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun android.content.Context.hasLocationPermission(): Boolean {
    val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private fun Double?.formatDistance(): String = when {
    this == null -> "unknown"
    this >= 1000.0 -> String.format(Locale.US, "%.2f km", this / 1000.0)
    else -> "${kotlin.math.round(this / 10.0).toInt() * 10} m"
}

private fun Double?.formatDegrees(): String =
    this?.let { String.format(Locale.US, "%.0f°", it) } ?: "unknown"

private fun ProximityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }

private fun LocationAvailabilityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }
