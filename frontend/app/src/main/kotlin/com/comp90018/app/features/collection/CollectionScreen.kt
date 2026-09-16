package com.comp90018.app.features.collection

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comp90018.app.*
import com.comp90018.app.features.map.RelicArtwork
import com.comp90018.app.features.treasure.DemoRelics
import com.comp90018.app.features.treasure.RelicItem

private data class FragmentQuest(val title: String, val hint: String, val total: Int)

@Composable
fun CollectionScreen(foundRelicIds: Set<String>) {
    val relics = DemoRelics.filter { it.id in foundRelicIds }
    val quests = listOf(
        FragmentQuest("Clocktower Cipher", "Find fragments around the old clocktower", 5),
        FragmentQuest("Library Whispers", "Search the quiet corners of Baillieu", 4),
        FragmentQuest("Garden Constellation", "Collect the symbols hidden near the ponds", 6),
    )
    var progress by rememberSaveable { mutableStateOf(listOf(2, 1, 0)) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Fragment quests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink) }
        items(quests.indices.toList(), key = { it }) { index ->
            FragmentQuestCard(quests[index], progress[index]) {
                val updated = progress.toMutableList()
                updated[index] = (updated[index] + 1).coerceAtMost(quests[index].total)
                progress = updated
            }
        }
        item {
            Text("Found relics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 8.dp))
            Text(relics.size.toString() + " relics discovered", color = Muted)
        }
        items(relics, key = RelicItem::id) { relic -> RelicCollectionCard(relic) }
        if (relics.isEmpty()) item { Text("Complete your first hunt to add a relic here.", style = MaterialTheme.typography.bodyLarge, color = Muted) }
    }
}

@Composable
private fun FragmentQuestCard(quest: FragmentQuest, found: Int, onCollect: () -> Unit) {
    val complete = found == quest.total
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = if (complete) BrandSoft else Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (complete) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome, null, tint = Brand)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(quest.title, fontWeight = FontWeight.Bold, color = Ink); Text(quest.hint, color = Muted, style = MaterialTheme.typography.bodySmall) }
                Text(found.toString() + "/" + quest.total, color = Brand, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { found.toFloat() / quest.total }, modifier = Modifier.fillMaxWidth(), color = Brand, trackColor = BrandSoft)
            OutlinedButton(onClick = onCollect, enabled = !complete, modifier = Modifier.fillMaxWidth()) { Text(if (complete) "Fragments complete" else "Collect demo fragment") }
        }
    }
}

@Composable
private fun RelicCollectionCard(relic: RelicItem) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RelicArtwork(relic, Modifier.size(88.dp), compact = true)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(relic.shortName, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink); Icon(Icons.Rounded.CheckCircle, "Found", Modifier.size(22.dp), tint = Brand) }
                Text(relic.description, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Muted)
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.LocationOn, null, Modifier.size(16.dp), tint = relic.accent); Text("  " + relic.distance, style = MaterialTheme.typography.labelMedium, color = relic.accent, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
