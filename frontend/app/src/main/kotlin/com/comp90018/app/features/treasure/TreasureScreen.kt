package com.comp90018.app.features.treasure

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicGold
import com.comp90018.app.R
import com.comp90018.app.TeamRoom
import com.comp90018.app.data.rooms.TeamRoomRepository
import com.comp90018.app.features.map.sampleMapRelics

private enum class TreasureSection { Tasks, Collection }

private enum class CollectionFilter(val label: String) {
    All("All"),
    Found("Found"),
    Locked("Locked"),
}

private data class TreasureTask(
    val title: String,
    val detail: String,
    val progress: Float,
    val icon: ImageVector,
)

/** Frontend treasure hub. Room data is observed read-only; collection progress is a preview. */
@Composable
fun TreasureScreen(activeRoomId: String?, repository: TeamRoomRepository) {
    var room by remember(activeRoomId) { mutableStateOf<TeamRoom?>(null) }
    var roomError by remember(activeRoomId) { mutableStateOf<String?>(null) }
    var section by remember { mutableStateOf(TreasureSection.Tasks) }
    var collectionFilter by remember { mutableStateOf(CollectionFilter.All) }

    DisposableEffect(activeRoomId, repository) {
        if (activeRoomId.isNullOrBlank()) {
            room = null
            roomError = null
            return@DisposableEffect onDispose { }
        }
        val subscription = repository.observeRoom(activeRoomId) { updatedRoom, error ->
            room = updatedRoom
            roomError = error
        }
        onDispose { subscription.cancel() }
    }

    Column(
        Modifier.fillMaxSize().padding(top = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TreasureTabs(selected = section, onSelected = { section = it })
        when (section) {
            TreasureSection.Tasks -> TasksContent(room, activeRoomId, roomError)
            TreasureSection.Collection -> CollectionContent(collectionFilter) { collectionFilter = it }
        }
    }
}

@Composable
private fun TreasureTabs(selected: TreasureSection, onSelected: (TreasureSection) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BrandSoft,
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TreasureTab(
                label = "Tasks",
                icon = Icons.Rounded.TaskAlt,
                selected = selected == TreasureSection.Tasks,
                onClick = { onSelected(TreasureSection.Tasks) },
                modifier = Modifier.weight(1f),
            )
            TreasureTab(
                label = "Collection",
                icon = Icons.Rounded.Inventory2,
                selected = selected == TreasureSection.Collection,
                onClick = { onSelected(TreasureSection.Collection) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TreasureTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Text(label, color = if (selected) Color.White else Ink, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TasksContent(room: TeamRoom?, activeRoomId: String?, roomError: String?) {
    val tasks = listOf(
        TreasureTask("Visit a treasure", "Open the map and reach any marked location.", 0.65f, Icons.Rounded.Explore),
        TreasureTask("Hunt together", "Explore with both members of your room.", if (room?.memberIds?.size == 2) 1f else 0.5f, Icons.Rounded.Groups),
        TreasureTask("Recover a fragment", "Finish a location search to unlock a collection piece.", 0f, Icons.Rounded.Stars),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ActiveQuestCard(room, activeRoomId) }
        roomError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        item {
            ProgressOverview(
                title = "Today's progress",
                detail = "1 of 3 goals in progress",
                progress = 1f / 3f,
            )
        }
        item { Text("Adventure tasks", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) }
        items(tasks) { task -> TaskCard(task) }
    }
}

@Composable
private fun ActiveQuestCard(room: TeamRoom?, activeRoomId: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Brand),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).background(Color.White.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Route, null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("ACTIVE QUEST", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
                    Text(
                        room?.taskTitle?.takeIf { it.isNotBlank() } ?: "Campus fragment hunt",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            Text(
                if (activeRoomId.isNullOrBlank()) "Join or create a room to share this quest with another explorer."
                else "Room ${activeRoomId.take(8)} · ${room?.memberIds?.size ?: 1}/2 explorers",
                color = Color.White.copy(alpha = 0.88f),
            )
        }
    }
}

@Composable
private fun ProgressOverview(title: String, detail: String, progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold)
                Text("${(progress * 100).toInt()}%", color = Brand, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Brand,
                trackColor = BrandSoft,
            )
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TaskCard(task: TreasureTask) {
    val complete = task.progress >= 1f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(if (complete) Brand else BrandSoft, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (complete) Icons.Rounded.CheckCircle else task.icon, null, tint = if (complete) Color.White else Brand)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(task.title, color = Ink, fontWeight = FontWeight.Bold)
                Text(task.detail, color = Muted, style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = if (complete) Brand else RelicGold,
                    trackColor = BrandSoft,
                )
            }
        }
    }
}

@Composable
private fun CollectionContent(filter: CollectionFilter, onFilterChanged: (CollectionFilter) -> Unit) {
    val foundIds = setOf(sampleMapRelics.first().id)
    val visibleRelics = sampleMapRelics.filter { relic ->
        when (filter) {
            CollectionFilter.All -> true
            CollectionFilter.Found -> relic.id in foundIds
            CollectionFilter.Locked -> relic.id !in foundIds
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            CollectionSummary(found = foundIds.size, total = sampleMapRelics.size)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionFilter.entries.forEach { option ->
                    CollectionFilterButton(
                        label = option.label,
                        selected = option == filter,
                        onClick = { onFilterChanged(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        items(visibleRelics, key = { it.id }) { relic ->
            val found = relic.id in foundIds
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).background(if (found) BrandSoft else Color(0xFFF0F2F1), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (found) {
                            Image(painterResource(R.drawable.nav_treasure_game), null, modifier = Modifier.size(48.dp))
                        } else {
                            Icon(Icons.Rounded.Lock, null, tint = Muted.copy(alpha = 0.55f), modifier = Modifier.size(30.dp))
                        }
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (found) relic.name else "Undiscovered fragment", color = Ink, fontWeight = FontWeight.Bold)
                        Text(if (found) relic.locationName else "Reach this location to reveal it", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (found) relic.description else "Complete its map task and add the fragment to your collection.",
                            color = Muted,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 2,
                        )
                    }
                    Icon(
                        if (found) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                        if (found) "Found" else "Locked",
                        tint = if (found) Brand else Muted.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionSummary(found: Int, total: Int) {
    val progress = found.toFloat() / total.coerceAtLeast(1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF6DF)),
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).background(RelicGold.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.nav_treasure_game), null, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Fragment collection", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("$found of $total treasures recovered", color = Muted)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = RelicGold,
                    trackColor = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CollectionFilterButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) BrandSoft else Color.White,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, BrandSoft),
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 10.dp),
            color = if (selected) Brand else Muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
