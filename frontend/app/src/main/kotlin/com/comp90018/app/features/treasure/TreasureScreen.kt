package com.comp90018.app.features.treasure

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
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicGold
import com.comp90018.app.RelicRed
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationCalculator
import java.util.Locale

private enum class TreasureSection { Tasks, Collection }

private enum class CollectionFilter(val label: String) {
    All("All"),
    Discovered("Discovered"),
    Undiscovered("Missing"),
}

private val stopOne = GeoCoordinate(-37.7986, 144.9602)

/** Treasure-focused frontend hub backed by the shared Firebase catalogue. */
@Composable
fun TreasureScreen(
    treasures: List<MapRelic>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onOpenMap: () -> Unit,
    discoveredTreasureIds: Set<String>,
    collectionLoading: Boolean,
    collectionError: String?,
    onRetryCollection: () -> Unit,
) {
    var section by remember { mutableStateOf(TreasureSection.Tasks) }
    var collectionFilter by remember { mutableStateOf(CollectionFilter.All) }

    Column(
        Modifier.fillMaxSize().padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TreasureTabs(selected = section, onSelected = { section = it })
        when {
            loading && treasures.isEmpty() -> CatalogStateCard("Loading treasures from Firebase…", loading = true)
            error != null && treasures.isEmpty() -> CatalogStateCard(error, actionLabel = "Try again", onAction = onRetry)
            treasures.isEmpty() -> CatalogStateCard("No enabled treasures are available right now.")
            else -> when (section) {
                TreasureSection.Tasks -> TasksContent(treasures, discoveredTreasureIds, error, onOpenMap)
                TreasureSection.Collection -> CollectionContent(
                    treasures = treasures,
                    foundIds = discoveredTreasureIds,
                    loading = collectionLoading,
                    error = collectionError,
                    filter = collectionFilter,
                    onRetry = onRetryCollection,
                    onFilterChanged = { collectionFilter = it },
                )
            }
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
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
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
private fun TreasureTabs(selected: TreasureSection, onSelected: (TreasureSection) -> Unit) {
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), color = BrandSoft) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TreasureTab("Tasks", Icons.Rounded.TaskAlt, selected == TreasureSection.Tasks, { onSelected(TreasureSection.Tasks) }, Modifier.weight(1f))
            TreasureTab("Collection", Icons.Rounded.Inventory2, selected == TreasureSection.Collection, { onSelected(TreasureSection.Collection) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TreasureTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Brand else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (selected) Color.White else Muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(7.dp))
            Text(label, color = if (selected) Color.White else Ink)
        }
    }
}

@Composable
private fun TasksContent(treasures: List<MapRelic>, foundIds: Set<String>, catalogError: String?, onOpenMap: () -> Unit) {
    val activeRelic = remember(treasures, foundIds) { treasures.firstOrNull { it.id !in foundIds } }
    val otherRelics = treasures.filter { it.id != activeRelic?.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        catalogError?.let { warning -> item { Text(warning, color = MaterialTheme.colorScheme.error) } }
        if (activeRelic != null) {
            item { CurrentHuntCard(activeRelic, onOpenMap) }
        } else {
            item { CollectionCompleteCard() }
        }
        item { Text("Hunt route", color = Ink, style = MaterialTheme.typography.titleLarge) }
        lazyItems(otherRelics, key = { it.id }) { relic ->
            AvailableHuntCard(relic, relic.id in foundIds, onOpenMap)
        }
    }
}

@Composable
private fun CurrentHuntCard(relic: MapRelic, onResume: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Brand),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("CURRENT HUNT", color = Color.White.copy(alpha = 0.76f), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Surface(shape = CircleShape, color = RelicGold) {
                    Text("TRAVEL", modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp), color = Ink, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(88.dp).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    RelicImage(relic, discovered = false, modifier = Modifier.size(76.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(relic.name, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFFFC65A), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(relic.locationName, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Inventory2, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(relic.treasureTypeLabel, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            HuntStageTrack(currentStage = 0)
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.11f)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("NEXT CLUE", color = Color(0xFFFFC65A), style = MaterialTheme.typography.labelSmall)
                    Text(relic.clue, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE0A0), contentColor = Ink),
            ) {
                Text("Resume Hunt")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.ChevronRight, null)
            }
        }
    }
}

@Composable
private fun HuntStageTrack(currentStage: Int) {
    val stages = listOf("Travel", "Arrive", "Scan", "Unlock")
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            stages.forEachIndexed { index, _ ->
                Box(
                    Modifier.size(24.dp).background(if (index <= currentStage) RelicGold else Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index < currentStage) Icon(Icons.Rounded.Check, null, tint = Ink, modifier = Modifier.size(15.dp))
                    else Text((index + 1).toString(), color = if (index <= currentStage) Ink else Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
                }
                if (index < stages.lastIndex) {
                    Box(Modifier.weight(1f).height(3.dp).background(if (index < currentStage) RelicGold else Color.White.copy(alpha = 0.18f)))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            stages.forEach { stage ->
                Text(stage, modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AvailableHuntCard(relic: MapRelic, discovered: Boolean, onClick: () -> Unit) {
    val distance = LocationCalculator.distanceMeters(stopOne, relic.coordinate)
    val status = when {
        discovered -> "Discovered"
        distance <= relic.nearbyRadiusMeters -> "Nearby"
        else -> "Available"
    }
    val statusColor = when (status) {
        "Discovered" -> Color(0xFF39794A)
        "Nearby" -> RelicGold
        else -> Brand
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(70.dp).background(BrandSoft, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                RelicImage(relic, discovered, Modifier.size(61.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(relic.name, modifier = Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(status, color = statusColor, style = MaterialTheme.typography.labelSmall)
                }
                Text("${relic.locationName} · ${distance.formatDistance()}", color = Muted, style = MaterialTheme.typography.bodySmall)
                relic.treasureTypeLabel.takeIf { it.isNotBlank() }?.let { type ->
                    Text(type, color = Brand, style = MaterialTheme.typography.labelMedium)
                }
                Text(relic.clue, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, "View on map", tint = Brand)
        }
    }
}

@Composable
private fun CollectionContent(
    treasures: List<MapRelic>,
    foundIds: Set<String>,
    loading: Boolean,
    error: String?,
    filter: CollectionFilter,
    onRetry: () -> Unit,
    onFilterChanged: (CollectionFilter) -> Unit,
) {
    val visibleRelics = treasures.filter { relic ->
        when (filter) {
            CollectionFilter.All -> true
            CollectionFilter.Discovered -> relic.id in foundIds
            CollectionFilter.Undiscovered -> relic.id !in foundIds
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CatalogStateCard("Loading your collection…", loading = true)
            }
        }
        error?.let { message ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                CatalogStateCard(message, actionLabel = "Try again", onAction = onRetry)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { CollectionSummary(foundIds.count { id -> treasures.any { it.id == id } }, treasures.size) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionFilter.entries.forEach { option ->
                    CollectionFilterButton(option.label, option == filter, { onFilterChanged(option) }, Modifier.weight(1f))
                }
            }
        }
        gridItems(visibleRelics, key = { it.id }) { relic ->
            CollectionRelicCard(relic, relic.id in foundIds)
        }
    }
}

@Composable
private fun CollectionRelicCard(relic: MapRelic, discovered: Boolean) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.fillMaxWidth().height(124.dp).background(if (discovered) Color(0xFFFFF2D8) else BrandSoft, RoundedCornerShape(17.dp)),
                contentAlignment = Alignment.Center,
            ) {
                RelicImage(relic, discovered, Modifier.size(108.dp))
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp),
                    shape = CircleShape,
                    color = if (discovered) Color(0xFF39794A) else Ink.copy(alpha = 0.76f),
                ) {
                    Icon(if (discovered) Icons.Rounded.CheckCircle else Icons.Rounded.Lock, if (discovered) "Discovered" else "Locked", tint = Color.White, modifier = Modifier.padding(5.dp).size(16.dp))
                }
            }
            Text(if (discovered) relic.name else "Unknown relic", color = Ink, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(relic.locationName, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            relic.treasureTypeLabel.takeIf { it.isNotBlank() }?.let { type ->
                Text(type, color = if (discovered) Brand else Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun RelicImage(relic: MapRelic, discovered: Boolean, modifier: Modifier = Modifier) {
    TreasurePrototypeImage(relic = relic, discovered = discovered, modifier = modifier)
}

@Composable
private fun CollectionSummary(found: Int, total: Int) {
    val progress = found.toFloat() / total.coerceAtLeast(1)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1D2))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Relic collection", color = Ink, style = MaterialTheme.typography.titleLarge)
                    Text("$found of $total campus relics discovered", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("${(progress * 100).toInt()}%", color = RelicRed, style = MaterialTheme.typography.titleLarge)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                color = RelicGold,
                trackColor = Color.White,
            )
            Text("Every recovered relic unlocks a campus story.", color = Brand, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CollectionFilterButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), color = if (selected) BrandSoft else Color.White) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            color = if (selected) Brand else Muted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CollectionCompleteCard() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F0DF))) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Every campus relic is safe", color = Ink, style = MaterialTheme.typography.titleLarge)
            Text("Your collection is complete. Revisit a story or help a teammate finish their route.", color = Muted)
        }
    }
}

private fun Double.formatDistance(): String = when {
    this >= 1000.0 -> String.format(Locale.US, "%.2f km", this / 1000.0)
    else -> "${kotlin.math.round(this / 10.0).toInt() * 10} m"
}
