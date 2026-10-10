package com.comp90018.app.features.map.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.GothicTreasureFontFamily
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.features.map.HUNT_READY_RADIUS_METERS
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.map.components.DiscoveryHuntButton
import com.comp90018.app.features.map.components.formatGateDistance
import com.comp90018.app.features.treasure.TreasurePrototypeImage

/** Displays the selected treasure summary and expandable detail affordance on the map. */
@Composable
internal fun TreasurePeekHeader(
    relic: MapRelic,
    distance: String,
    isFound: Boolean,
    expanded: Boolean,
    onChevron: () -> Unit,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onSheetDrag: ((Boolean) -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxWidth().then(
            if (onSheetDrag != null) Modifier.pointerInput(onSheetDrag) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, amount -> change.consume(); dragged += amount },
                    onDragEnd = {
                        if (dragged < -24.dp.toPx()) onSheetDrag(true)
                        else if (dragged > 24.dp.toPx()) onSheetDrag(false)
                    },
                )
            } else Modifier
        ).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 92.dp),
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
                    if (isFound) relic.name else "Undiscovered treasure",
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
        actionLabel?.let { label ->
            DiscoveryHuntButton(label, onAction)
        }
    }
}

/** Renders treasure facts and the actions available from the detail stage. */
@Composable
internal fun TreasureInformationPanel(
    relic: MapRelic,
    isFound: Boolean,
    modifier: Modifier = Modifier,
    replayEnabled: Boolean = false,
    replayRadiusMeters: Double = HUNT_READY_RADIUS_METERS,
    onRestartHunt: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isFound && onRestartHunt != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRestartHunt,
                        enabled = replayEnabled,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Brand,
                            disabledContainerColor = Color(0xFFE0DDD7),
                            disabledContentColor = Color(0xFF8B8781),
                        ),
                    ) { Text("Start Hunting Again", fontWeight = FontWeight.Bold) }
                    Text(
                        if (replayEnabled) "Experience this treasure hunt again. Your collected treasure stays in your backpack."
                        else "Move within ${replayRadiusMeters.formatGateDistance()} and enable precise location to hunt again.",
                        color = Muted, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (!isFound) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSoft),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Adventure awaits!", color = Brand, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Keep exploring and find this treasure to uncover its story. You’re getting closer!",
                            color = Ink,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        } else {
            relic.description.takeIf(String::isNotBlank)?.let { item { Text(it, color = Ink, style = MaterialTheme.typography.bodyLarge, lineHeight = 23.sp) } }
            item { DetailTextRow("Treasure type", relic.treasureTypeLabel.ifBlank { "Treasure" }) }
            item { DetailTextRow("Found at", relic.locationName) }
            relic.story.takeIf(String::isNotBlank)?.let { item { DetailTextRow("The story", it) } }
            relic.buildingStory.takeIf(String::isNotBlank)?.let { item { DetailTextRow("Landmark story", it) } }
            relic.prototypeDesign.takeIf(String::isNotBlank)?.let { item { DetailTextRow("Relic design", it) } }
            relic.coordinateSource.takeIf(String::isNotBlank)?.let { item { DetailTextRow("Location source", it) } }
        }
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

/** Presents found treasure artwork and the action that opens its story. */
@Composable
internal fun TreasureFoundPanel(relic: MapRelic, onViewStory: () -> Unit) {
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
private fun DetailTextRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
        Text(title, color = Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
    androidx.compose.material3.HorizontalDivider(color = BrandSoft)
}
