package com.comp90018.app.features.map

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comp90018.app.Background
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.treasure.DemoRelics
import com.comp90018.app.features.treasure.RelicItem
import kotlinx.coroutines.delay

@Composable
fun MapScreen(
    activeRelic: RelicItem,
    initiallyOpenRelicId: String?,
    onInitialRelicOpened: () -> Unit,
    onRelicActivated: (RelicItem) -> Unit,
    onRelicFound: (RelicItem) -> Unit,
    onOpenCollection: () -> Unit,
) {
    var selectedMarkerId by rememberSaveable { mutableStateOf<String?>(activeRelic.id) }
    var detailRelicId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailRelic = DemoRelics.firstOrNull { it.id == detailRelicId }

    LaunchedEffect(initiallyOpenRelicId) {
        if (initiallyOpenRelicId != null) {
            detailRelicId = initiallyOpenRelicId
            selectedMarkerId = initiallyOpenRelicId
            onInitialRelicOpened()
        }
    }

    AnimatedContent(targetState = detailRelic, label = "map-detail") { relic ->
        if (relic == null) {
            MapOverview(
                selectedId = selectedMarkerId,
                onSelected = { selectedMarkerId = it.id },
                onOpen = {
                    onRelicActivated(it)
                    detailRelicId = it.id
                },
            )
        } else {
            RelicDetailFlow(
                relic = relic,
                onBack = { detailRelicId = null },
                onRelicFound = onRelicFound,
                onOpenCollection = onOpenCollection,
            )
        }
    }
}

@Composable
private fun MapOverview(
    selectedId: String?,
    onSelected: (RelicItem) -> Unit,
    onOpen: (RelicItem) -> Unit,
) {
    val selected = DemoRelics.firstOrNull { it.id == selectedId }
    Box(Modifier.fillMaxSize()) {
        CampusMap(DemoRelics, selectedId, onSelected, Modifier.fillMaxSize())
        Surface(
            modifier = Modifier.padding(start = 18.dp, top = 18.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White.copy(alpha = .94f),
            shadowElevation = 4.dp,
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text("Explore campus", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                Text("${DemoRelics.size} relics nearby", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
        if (selected != null) {
            RelicPreviewCard(
                relic = selected,
                onOpen = { onOpen(selected) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun RelicPreviewCard(relic: RelicItem, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RelicArtwork(relic, Modifier.size(78.dp), compact = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(relic.shortName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, Modifier.size(15.dp), tint = relic.accent)
                    Text(relic.distance, style = MaterialTheme.typography.labelMedium, color = relic.accent)
                }
                Text(relic.address, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(relic.description, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Open relic details", tint = Brand, modifier = Modifier.size(30.dp))
            }
        }
    }
}

private enum class HuntStage { Details, Searching, Ready, Success }

@Composable
private fun RelicDetailFlow(
    relic: RelicItem,
    onBack: () -> Unit,
    onRelicFound: (RelicItem) -> Unit,
    onOpenCollection: () -> Unit,
) {
    var stage by remember(relic.id) { mutableStateOf(HuntStage.Details) }
    var sensorHint by remember(relic.id) { mutableStateOf("Move a little closer") }
    BackHandler {
        if (stage == HuntStage.Details || stage == HuntStage.Success) onBack() else stage = HuntStage.Details
    }

    LaunchedEffect(stage) {
        if (stage == HuntStage.Searching) {
            sensorHint = "Move a little closer"
            delay(700)
            sensorHint = "Turn slightly to your left"
            delay(700)
            sensorHint = "Hold your phone steady"
            delay(600)
            sensorHint = "Relic signal locked"
            stage = HuntStage.Ready
        }
    }

    when (stage) {
        HuntStage.Success -> RelicSuccessScreen(relic, onOpenCollection, onBack)
        HuntStage.Details -> RelicInformationScreen(relic, onBack, onStartSearch = { stage = HuntStage.Searching })
        HuntStage.Searching, HuntStage.Ready -> RelicSearchingScreen(
            relic = relic,
            ready = stage == HuntStage.Ready,
            hint = sensorHint,
            onBack = { stage = HuntStage.Details },
            onCollect = {
                onRelicFound(relic)
                stage = HuntStage.Success
            },
        )
    }
}

@Composable
private fun RelicInformationScreen(relic: RelicItem, onBack: () -> Unit, onStartSearch: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background)) {
        DetailHeader(relic.shortName, onBack)
        CampusMap(
            relics = DemoRelics,
            selectedId = relic.id,
            onRelicSelected = {},
            modifier = Modifier.fillMaxWidth().height(245.dp),
            focusRelic = relic,
        )
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RelicArtwork(relic, Modifier.size(106.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(relic.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text(relic.distance, style = MaterialTheme.typography.labelLarge, color = relic.accent, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Group, null, Modifier.size(18.dp), tint = Muted)
                            Text("  ${relic.foundBy} explorers found it", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onStartSearch,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) {
                    Icon(Icons.Rounded.Search, null)
                    Text("  You're nearby - start searching", fontWeight = FontWeight.Bold)
                }
            }
            item { RelicInformationCard(relic) }
        }
    }
}

@Composable
private fun RelicSearchingScreen(relic: RelicItem, ready: Boolean, hint: String, onBack: () -> Unit, onCollect: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background)) {
        DetailHeader(if (ready) "Signal found" else "Searching...", onBack)
        CampusMap(
            relics = DemoRelics,
            selectedId = relic.id,
            onRelicSelected = {},
            modifier = Modifier.fillMaxWidth().weight(1f),
            focusRelic = relic,
        )
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SearchSignal(ready = ready, onCollect = onCollect)
                Text(
                    if (ready) "Tap the signal to collect ${relic.shortName}" else hint,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (ready) Brand else Ink,
                )
                Text(
                    if (ready) "Sensor conditions met - relic ready" else "Demo sensor check in progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun SearchSignal(ready: Boolean, onCollect: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "search-pulse")
    val pulse by transition.animateFloat(
        initialValue = .90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "signal-scale",
    )
    IconButton(
        onClick = { if (ready) onCollect() },
        enabled = ready,
        modifier = Modifier
            .size(84.dp)
            .scale(if (ready) 1f else pulse)
            .background(if (ready) Brand else BrandSoft, CircleShape),
    ) {
        Icon(
            if (ready) Icons.Rounded.LockOpen else Icons.Rounded.Radar,
            contentDescription = if (ready) "Collect relic" else "Searching for relic",
            tint = if (ready) Color.White else Brand,
            modifier = Modifier.size(42.dp),
        )
    }
}

@Composable
private fun RelicSuccessScreen(relic: RelicItem, onOpenCollection: () -> Unit, onBackToMap: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFE4F1E8), Background))).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(92.dp).background(Brand, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CheckCircle, null, Modifier.size(58.dp), tint = Color.White)
        }
        Spacer(Modifier.height(20.dp))
        Text("Relic found!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("A new campus story is now yours.", style = MaterialTheme.typography.bodyLarge, color = Muted)
        Spacer(Modifier.height(28.dp))
        RelicArtwork(relic, Modifier.fillMaxWidth().height(230.dp))
        Spacer(Modifier.height(18.dp))
        Text(relic.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Text("Added to your collection", style = MaterialTheme.typography.bodyMedium, color = Brand, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onOpenCollection, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Rounded.CollectionsBookmark, null)
            Text("  View collection", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onBackToMap) { Text("Back to map") }
    }
}

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RelicInformationCard(relic: RelicItem) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            InformationSection("Location", relic.address, Icons.Rounded.LocationOn)
            HorizontalDivider(color = BrandSoft)
            InformationSection("The story", relic.story, Icons.Rounded.AccountBalance)
            HorizontalDivider(color = BrandSoft)
            Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Recently discovered by", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    relic.discoverers.forEach { name ->
                        Box(Modifier.size(38.dp).background(relic.accent.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(name.take(1), color = relic.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("+" + (relic.foundBy - relic.discoverers.size).coerceAtLeast(0) + " more", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun InformationSection(title: String, body: String, icon: ImageVector) {
    Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(42.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Brand) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}

@Composable
fun RelicArtwork(relic: RelicItem, modifier: Modifier = Modifier, compact: Boolean = false) {
    val icon = when (relic.id) {
        "old-quad-seal" -> Icons.Rounded.Shield
        "baillieu-compass" -> Icons.Rounded.Navigation
        "union-house-key" -> Icons.Rounded.Key
        else -> Icons.Rounded.WbSunny
    }
    Box(
        modifier.background(
            Brush.linearGradient(listOf(relic.accent.copy(alpha = .95f), relic.accent.copy(alpha = .55f))),
            RoundedCornerShape(if (compact) 18.dp else 26.dp),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = .13f), size.minDimension * .40f, Offset(size.width * .17f, size.height * .12f))
            drawCircle(Color.White.copy(alpha = .10f), size.minDimension * .30f, Offset(size.width * .90f, size.height * .86f))
        }
        Icon(icon, contentDescription = "Illustration of ${relic.shortName}", tint = Color.White, modifier = Modifier.size(if (compact) 44.dp else 74.dp))
    }
}
