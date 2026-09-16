package com.comp90018.app.features.hunt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.treasure.RelicItem

private val CompletedGold = Color(0xFFD39B2D)
private val LockedGray = Color(0xFFD7D9DD)

private data class QuestRelic(val name: String, val complete: Boolean, val active: Boolean = false)

@Composable
fun HuntScreen(relic: RelicItem, alreadyFound: Boolean, onResumeHunt: () -> Unit) {
    val mainRelics = listOf(
        QuestRelic("Compass", true),
        QuestRelic("Garden Token", true),
        QuestRelic(if (relic.shortName == "Founder's Seal") "Founder Seal" else relic.shortName, alreadyFound, active = !alreadyFound),
        QuestRelic("Clock Key", false),
    )
    val sideQuests = listOf(
        "Library Secrets" to listOf(QuestRelic("Margin Note", true), QuestRelic("Reading Lamp", false), QuestRelic("Archive Seal", false)),
        "Hidden Gardens" to listOf(QuestRelic("Stone Leaf", true), QuestRelic("Pond Star", true), QuestRelic("Glass Flower", false)),
    )
    var mainExpanded by remember { mutableStateOf(false) }
    var expandedSide by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Main quest", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
            QuestCard(
                title = "The Lost Campus Chronicle",
                subtitle = "2 of 4 relics collected",
                expanded = mainExpanded,
                onToggle = { mainExpanded = !mainExpanded },
            ) {
                RelicRoute(mainRelics, onActiveRelic = onResumeHunt)
            }
        }
        item { Text("Side quests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(top = 8.dp)) }
        sideQuests.forEach { (title, nodes) ->
            item(key = title) {
                QuestCard(
                    title = title,
                    subtitle = nodes.count(QuestRelic::complete).toString() + " of " + nodes.size + " relics collected",
                    expanded = expandedSide == title,
                    onToggle = { expandedSide = if (expandedSide == title) null else title },
                ) { RelicRoute(nodes, onActiveRelic = {}) }
            }
        }
        item { Text("Team mission", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink, modifier = Modifier.padding(top = 8.dp)) }
        item { TeamPuzzleCard() }
    }
}

@Composable
private fun QuestCard(title: String, subtitle: String, expanded: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggle), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(CompletedGold.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Map, null, tint = CompletedGold) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = Ink); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
                Icon(Icons.Rounded.ChevronRight, if (expanded) "Collapse" else "Expand", tint = Muted, modifier = Modifier.rotate(if (expanded) 90f else 0f))
            }
            if (expanded) {
                HorizontalDivider(color = LockedGray)
                content()
            }
        }
    }
}

@Composable
private fun RelicRoute(relics: List<QuestRelic>, onActiveRelic: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Relics to collect", color = Muted, style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            relics.forEachIndexed { index, relic ->
                Column(Modifier.weight(1f).clickable(enabled = relic.active) { onActiveRelic() }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (index > 0) Box(Modifier.weight(1f).height(3.dp).background(if (relics[index - 1].complete && relic.complete) CompletedGold else LockedGray))
                        Box(Modifier.size(34.dp).background(if (relic.complete) CompletedGold else LockedGray, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(if (relic.complete) Icons.Rounded.Star else Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        if (index < relics.lastIndex) Box(Modifier.weight(1f).height(3.dp).background(if (relic.complete && relics[index + 1].complete) CompletedGold else LockedGray))
                    }
                    Text(relic.name, color = if (relic.complete) CompletedGold else Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 7.dp))
                    if (relic.active) Text("Continue", color = CompletedGold, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TeamPuzzleCard() {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(CompletedGold.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Groups, null, tint = CompletedGold) }
                Spacer(Modifier.width(12.dp))
                Column { Text("Assemble the Founders’ Map", fontWeight = FontWeight.Bold, color = Ink); Text("Work with your room to collect all puzzle pieces", color = Muted, style = MaterialTheme.typography.bodySmall) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { index ->
                    Box(Modifier.weight(1f).aspectRatio(1f).background(if (index < 3) CompletedGold else LockedGray, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                        Icon(if (index < 3) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text("3 of 6 puzzle pieces collected", color = CompletedGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
    }
}
