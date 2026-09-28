package com.comp90018.app.features.treasure

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.app.*
import com.comp90018.app.features.map.DEFAULT_CAMPUS_CENTRE
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.REVEAL_RADIUS_METERS
import com.comp90018.app.features.map.isProbablyEmulator
import com.comp90018.app.sensors.location.AndroidLocationSensor
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import com.comp90018.app.sensors.location.LocationPermissionState
import java.util.Locale

/** Treasure catalogue and lore details. Hunting itself lives exclusively in Map. */
@Composable
fun TreasureScreen(
    treasures: List<MapRelic>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onStartNearbyHunt: (String) -> Unit,
    discoveredTreasureIds: Set<String>,
) {
    var selectedTreasure by remember { mutableStateOf<MapRelic?>(null) }
    val currentLocation = rememberTreasurePageLocation()

    selectedTreasure?.let { treasure ->
        val distance = currentLocation?.let { LocationCalculator.distanceMeters(it, treasure.coordinate) }
        TreasureRouteDetail(
            treasure = treasure,
            discovered = treasure.id in discoveredTreasureIds,
            distanceMeters = distance,
            onBack = { selectedTreasure = null },
            onStartHunt = { onStartNearbyHunt(treasure.id) },
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            loading && treasures.isEmpty() -> CatalogStateCard("Loading treasures from Firebase…", loading = true)
            error != null && treasures.isEmpty() -> CatalogStateCard(error, actionLabel = "Try again", onAction = onRetry)
            treasures.isEmpty() -> CatalogStateCard("No enabled treasures are available right now.")
            else -> HuntRoute(
                treasures = treasures,
                foundIds = discoveredTreasureIds,
                currentLocation = currentLocation,
                catalogError = error,
                onOpenTreasure = { selectedTreasure = it },
            )
        }
    }
}

@Composable
private fun CatalogStateCard(
    message: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (loading) CircularProgressIndicator(color = Brand)
            Text(message, color = if (actionLabel == null) Muted else MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            actionLabel?.let { label -> Button(onClick = onAction) { Text(label) } }
        }
    }
}

@Composable
private fun HuntRoute(
    treasures: List<MapRelic>,
    foundIds: Set<String>,
    currentLocation: GeoCoordinate?,
    catalogError: String?,
    onOpenTreasure: (MapRelic) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        catalogError?.let { warning -> item { Text(warning, color = MaterialTheme.colorScheme.error) } }
        item {
            Text("Hunt route", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Open a relic to read what is known about it.", color = Muted)
        }
        items(treasures, key = { it.id }) { treasure ->
            val distance = currentLocation?.let { LocationCalculator.distanceMeters(it, treasure.coordinate) }
            TreasureRouteCard(
                treasure = treasure,
                discovered = treasure.id in foundIds,
                distanceMeters = distance,
                onClick = { onOpenTreasure(treasure) },
            )
        }
    }
}

@Composable
private fun TreasureRouteCard(
    treasure: MapRelic,
    discovered: Boolean,
    distanceMeters: Double?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(70.dp).background(BrandSoft, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                TreasurePrototypeImage(treasure, discovered, Modifier.size(61.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(treasure.name, modifier = Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(if (discovered) "Discovered" else "Undiscovered", color = if (discovered) Color(0xFF39794A) else Brand, style = MaterialTheme.typography.labelSmall)
                }
                Text(
                    listOfNotNull(treasure.locationName, distanceMeters?.formatDistance()).joinToString(" · "),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (discovered) treasure.description.ifBlank { treasure.story }
                    else undiscoveredLegend(treasure),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, "Open treasure details", tint = Brand)
        }
    }
}

@Composable
private fun TreasureRouteDetail(
    treasure: MapRelic,
    discovered: Boolean,
    distanceMeters: Double?,
    onBack: () -> Unit,
    onStartHunt: () -> Unit,
) {
    val context = LocalContext.current
    val nearbyAndHidden = !discovered && distanceMeters != null && distanceMeters <= REVEAL_RADIUS_METERS
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text(treasure.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    Text(if (discovered) "Discovered treasure" else "Undiscovered legend", color = Muted)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = BrandSoft)) {
                Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                    TreasurePrototypeImage(treasure, discovered, Modifier.size(205.dp))
                }
            }
        }
        if (!discovered) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text(
                        undiscoveredLegend(treasure),
                        modifier = Modifier.padding(20.dp),
                        color = Ink,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (nearbyAndHidden) {
                item {
                    Button(onClick = onStartHunt, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Rounded.Map, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Find on Map · ${distanceMeters.formatDistance()}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            treasure.description.takeIf { it.isNotBlank() }?.let { item { DetailSection("About the treasure", it) } }
            treasure.story.takeIf { it.isNotBlank() }?.let { item { DetailSection("The story", it) } }
            treasure.clue.takeIf { it.isNotBlank() }?.let { item { DetailSection("Original clue", it) } }
            item { DetailSection("Found at", treasure.locationName) }
            treasure.treasureTypeLabel.takeIf { it.isNotBlank() }?.let { item { DetailSection("Treasure type", it) } }
            treasure.buildingStory.takeIf { it.isNotBlank() }?.let { item { DetailSection("About the landmark", it) } }
            treasure.prototypeDesign.takeIf { it.isNotBlank() }?.let { item { DetailSection("Relic design", it) } }
            if (treasure.historicalImageUrl.isNotBlank()) {
                item {
                    Card(
                        Modifier.fillMaxWidth().height(220.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        RemoteTreasureImage(
                            imageUrl = treasure.historicalImageUrl,
                            contentDescription = "Historical reference for ${treasure.name}",
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            fallbackDrawableRes = treasure.localTreasureArtworkResId(),
                        )
                    }
                }
            }
            treasure.historicalImageCredit.takeIf { it.isNotBlank() }?.let { item { DetailSection("Image credit", it) } }
            treasure.coordinateSource.takeIf { it.isNotBlank() }?.let { item { DetailSection("Location source", it) } }
            treasure.sourceTitle.takeIf { it.isNotBlank() }?.let { item { DetailSection("Historical source", it) } }
            treasure.sourceUrl.takeIf { it.startsWith("https://") }?.let { sourceUrl ->
                item {
                    OutlinedButton(
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl))) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) { Text("Open historical source") }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Brand, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(body, color = Ink, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun undiscoveredLegend(treasure: MapRelic): String {
    val legend = treasure.description.ifBlank { treasure.story.ifBlank { treasure.clue.ifBlank { "its story remains hidden somewhere on campus" } } }
    return "No explorer has uncovered this treasure yet, but the old campus legend says $legend"
}

@Composable
private fun rememberTreasurePageLocation(): GeoCoordinate? {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val emulator = remember { isProbablyEmulator() }
    val sensor = remember(context) { AndroidLocationSensor(context.applicationContext) }
    val output by sensor.output.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        sensor.refreshPermissionState()
        if (grants.values.any { it }) sensor.start()
    }
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission && !emulator) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    DisposableEffect(lifecycle, sensor) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { sensor.refreshPermissionState(); sensor.start() }
                Lifecycle.Event.ON_STOP -> sensor.stop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) sensor.start()
        onDispose { lifecycle.removeObserver(observer); sensor.stop() }
    }
    return when {
        output.permission == LocationPermissionState.GRANTED -> output.currentLocation
        emulator -> DEFAULT_CAMPUS_CENTRE
        else -> null
    }
}

private fun Double.formatDistance(): String = when {
    this < 1_000.0 -> "${this.coerceAtLeast(0.0).toInt()} m"
    else -> String.format(Locale.US, "%.1f km", this / 1_000.0)
}
