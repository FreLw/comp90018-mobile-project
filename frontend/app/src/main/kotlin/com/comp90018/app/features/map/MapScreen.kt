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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

/** Dedicated map feature boundary; location rendering belongs here. */
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val locationSensor = remember(context) { AndroidLocationSensor(context) }
    val locationOutput by locationSensor.output.collectAsState()
    var selectedRelic by remember { mutableStateOf(sampleMapRelics.first()) }
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
        locationSensor.setTargetLocation(selectedRelic.coordinate)
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) locationSensor.start() else locationSensor.stop()
    }

    DisposableEffect(Unit) {
        onDispose { locationSensor.stop() }
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

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapStatusCard(selectedRelic, locationOutput)
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.GpsFixed, contentDescription = null)
                    Text("Enable location")
                }
            }
        }
    }
}

@Composable
private fun GoogleMapView(
    relics: List<MapRelic>,
    selectedRelic: MapRelic,
    locationOutput: LocationOutput,
    hasLocationPermission: Boolean,
    onRelicSelected: (MapRelic) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply { onCreate(Bundle()) }
    }
    MapLifecycle(mapView)

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { view ->
            view.getMapAsync { map ->
                map.uiSettings.isZoomControlsEnabled = true
                map.uiSettings.isCompassEnabled = true
                map.isBuildingsEnabled = true
                map.setOnMarkerClickListener { marker ->
                    relics.firstOrNull { it.id == marker.tag }?.let {
                        onRelicSelected(it)
                        true
                    } ?: false
                }
                enableMyLocationIfAllowed(map, hasLocationPermission)
                renderRelics(map, relics, selectedRelic)
                renderCurrentLocation(map, locationOutput.currentLocation)
                moveCameraToCampus(map, relics, locationOutput.currentLocation)
            }
        },
    )
}

@Composable
private fun MapStatusCard(selectedRelic: MapRelic, output: LocationOutput) {
    ElevatedCard(shape = RoundedCornerShape(10.dp)) {
        Column(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.94f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null)
                Column {
                    Text(selectedRelic.name, fontWeight = FontWeight.Bold)
                    Text(selectedRelic.locationName, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Distance: ${output.distanceToTargetMeters.formatMeters()}")
            Text("Proximity: ${output.proximity.label()}")
            Text("Target bearing: ${output.targetBearingDegrees.formatDegrees()}")
            Text(
                text = "Permission: ${output.permission.name.lowercase()} | GPS: ${output.availability.label()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
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

private fun renderRelics(map: GoogleMap, relics: List<MapRelic>, selectedRelic: MapRelic) {
    map.clear()
    relics.forEach { relic ->
        val marker = map.addMarker(
            MarkerOptions()
                .position(relic.coordinate.toLatLng())
                .title(relic.name)
                .snippet(relic.locationName)
                .icon(
                    BitmapDescriptorFactory.defaultMarker(
                        if (relic.id == selectedRelic.id) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_ORANGE,
                    ),
                ),
        )
        marker?.tag = relic.id
    }
}

private fun renderCurrentLocation(map: GoogleMap, currentLocation: GeoCoordinate?) {
    if (currentLocation == null) return
    map.addMarker(
        MarkerOptions()
            .position(currentLocation.toLatLng())
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

private fun Double?.formatMeters(): String =
    this?.let { String.format(Locale.US, "%.0f m", it) } ?: "unknown"

private fun Double?.formatDegrees(): String =
    this?.let { String.format(Locale.US, "%.0f°", it) } ?: "unknown"

private fun ProximityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }

private fun LocationAvailabilityState.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }
