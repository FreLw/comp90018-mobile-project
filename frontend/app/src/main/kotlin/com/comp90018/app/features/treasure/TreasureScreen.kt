package com.comp90018.app.features.treasure

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator

/** Treasure lore browser. The live hunt flow belongs to Map. */
@Composable
fun TreasureScreen(
    treasures: List<MapRelic>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    discoveredTreasureIds: Set<String>,
    currentLocation: GeoCoordinate?,
    onOpenMap: (String) -> Unit,
    onNavigate: ((String) -> Unit)? = null,
) {
    var selectedTreasure by remember { mutableStateOf<MapRelic?>(null) }

    selectedTreasure?.let { treasure ->
        val distance = currentLocation?.let { LocationCalculator.distanceMeters(it, treasure.coordinate) }
        TreasureRouteDetail(
            treasure = treasure,
            discovered = treasure.id in discoveredTreasureIds,
            distanceMeters = distance,
            onBack = { selectedTreasure = null },
            onOpenMap = { onOpenMap(treasure.id) },
            onNavigate = onNavigate?.let { navigate -> { navigate(treasure.id) } },
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(top = 18.dp)) {
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
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(24.dp), CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (loading) CircularProgressIndicator(color = Brand)
            Text(message, color = if (actionLabel == null) Muted else MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            actionLabel?.let { Button(onClick = onAction) { Text(it) } }
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
                    Text(
                        if (discovered) "Discovered" else "Undiscovered",
                        color = if (discovered) Color(0xFF39794A) else Brand,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    listOfNotNull(treasure.locationName, distanceMeters?.formatTreasureDistance()).joinToString(" · "),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (discovered) treasure.description.ifBlank { treasure.story } else undiscoveredLegend(treasure),
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
    onOpenMap: () -> Unit,
    onNavigate: (() -> Unit)?,
) {
    val context = LocalContext.current
    val navigationEntry = treasureNavigationEntry(discovered, distanceMeters, onNavigate != null)
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
            Card(Modifier.fillMaxWidth(), RoundedCornerShape(28.dp), CardDefaults.cardColors(containerColor = BrandSoft)) {
                Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                    TreasurePrototypeImage(treasure, discovered, Modifier.size(205.dp))
                }
            }
        }
        if (!discovered) {
            item {
                Card(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CardDefaults.cardColors(containerColor = Color.White)) {
                    Text(undiscoveredLegend(treasure), Modifier.padding(20.dp), color = Ink, style = MaterialTheme.typography.bodyLarge)
                }
            }
            navigationEntry.primaryLabel?.let { label -> item {
                Button(onClick = { onNavigate?.invoke() }, modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Rounded.Navigation, null)
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontWeight = FontWeight.Bold)
                }
            } }
            navigationEntry.secondaryMapLabel?.let { label -> item {
                OutlinedButton(onClick = onOpenMap, modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(18.dp)) {
                    Icon(Icons.Rounded.Map, null)
                    Spacer(Modifier.width(8.dp))
                    Text(label)
                }
            } }
        } else {
            treasure.description.takeIf(String::isNotBlank)?.let { item { DetailSection("About the treasure", it) } }
            treasure.story.takeIf(String::isNotBlank)?.let { item { DetailSection("The story", it) } }
            treasure.clue.takeIf(String::isNotBlank)?.let { item { DetailSection("Original clue", it) } }
            item { DetailSection("Found at", treasure.locationName) }
            treasure.treasureTypeLabel.takeIf(String::isNotBlank)?.let { item { DetailSection("Treasure type", it) } }
            treasure.buildingStory.takeIf(String::isNotBlank)?.let { item { DetailSection("About the landmark", it) } }
            treasure.prototypeDesign.takeIf(String::isNotBlank)?.let { item { DetailSection("Relic design", it) } }
            if (treasure.historicalImageUrl.isNotBlank() || historicalArtworkResource(treasure) != null) {
                item {
                    Card(Modifier.fillMaxWidth().height(220.dp), RoundedCornerShape(22.dp)) {
                        HistoricalTreasureImage(
                            relic = treasure,
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                        )
                    }
                }
            }
            treasure.historicalImageCredit.takeIf(String::isNotBlank)?.let { item { DetailSection("Image credit", it) } }
            treasure.coordinateSource.takeIf(String::isNotBlank)?.let { item { DetailSection("Coordinate source", it) } }
            if (treasure.sourceTitle.isNotBlank() || treasure.sourceUrl.isNotBlank()) {
                item {
                    DetailSection(
                        "Historical source",
                        treasure.sourceTitle.ifBlank { treasure.sourceUrl },
                        treasure.sourceUrl.takeIf(String::isNotBlank)?.let { url ->
                            { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, body: String, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth().then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Brand, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(body, color = Ink, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun undiscoveredLegend(treasure: MapRelic): String {
    val legend = treasure.story.ifBlank { treasure.clue.ifBlank { treasure.description } }.trim().trimEnd('.')
    return "No explorer has uncovered this treasure yet, but an old campus legend whispers that ${legend.replaceFirstChar { it.lowercase() }}."
}
