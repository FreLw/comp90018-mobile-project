package com.comp90018.app.features.rooms

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TEAM_ROOM_CAPACITY

@Composable
internal fun RoomPlazaScreen(state: RoomsUiState, viewModel: RoomsViewModel, onBack: () -> Unit) {
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(viewModel) {
        viewModel.startBrowsing()
        onDispose { viewModel.stopBrowsing() }
    }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Column {
                Text("Room plaza", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Find fellow explorers", color = Muted)
            }
        }
        Text("Bright: occupied · Gray: waiting for a teammate", color = Muted, style = MaterialTheme.typography.bodySmall)
        when {
            state.browsingLoading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
            state.browsingError != null -> Column { Text(state.browsingError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.stopBrowsing(); viewModel.startBrowsing() }) { Text("Retry") } }
            state.publicRooms.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("No public rooms yet. Create one and invite others to join.", color = Muted, textAlign = TextAlign.Center) }
            else -> LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(state.publicRooms, key = { it.id }) { room ->
                    Card(modifier = Modifier.fillMaxWidth().clickable(enabled = !state.working) { viewModel.clearErrors(); selectedRoomId = room.id },
                        colors = CardDefaults.cardColors(containerColor = BrandSoft.copy(alpha = .35f))) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            RoomDiamond(room)
                            Text(room.name.ifBlank { "Explorer room" }, color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("${room.memberIds.size}/${room.maxMembers} members · ${if (room.memberIds.size >= room.maxMembers) "Full" else "Open"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    selectedRoomId?.let { id ->
        val room = state.publicRooms.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { if (!state.working) selectedRoomId = null },
            title = { Text(room?.name?.ifBlank { "Explorer room" } ?: "Room unavailable") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (room != null) {
                    RoomDiamond(room, Modifier.align(Alignment.CenterHorizontally).size(160.dp))
                    Text(room.description.ifBlank { "Explore together and discover treasures around campus." })
                    Text("Members ${room.memberIds.size}/${room.maxMembers} · No host approval needed", color = Muted)
                    if (room.taskTitle.isNotBlank()) Text("Destination: ${room.taskTitle}")
                } else Text("This room may have closed or now require a Room ID to join.")
                state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(enabled = room != null && room.memberIds.size < room.maxMembers && !state.working,
                onClick = { room?.let(viewModel::joinPublicRoom) }) { Text(if (state.working) "Joining…" else if (room != null && room.memberIds.size >= room.maxMembers) "Room full" else "Join room") } },
            dismissButton = { TextButton(enabled = !state.working, onClick = { selectedRoomId = null }) { Text("Back to plaza") } })
    }
}

@Composable
internal fun RoomDiamond(room: TeamRoom, modifier: Modifier = Modifier.size(128.dp)) {
    val positions = if (room.maxMembers == TEAM_ROOM_CAPACITY) listOf(Alignment.CenterStart, Alignment.CenterEnd)
        else listOf(Alignment.TopCenter, Alignment.CenterEnd, Alignment.BottomCenter, Alignment.CenterStart).take(room.maxMembers)
    Box(modifier.semantics { contentDescription = "${room.name}, ${room.memberIds.size} occupied seats, ${(room.maxMembers-room.memberIds.size).coerceAtLeast(0)} empty seats" }) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val path = Path().apply {
                moveTo(size.width/2, 0f); lineTo(size.width, size.height/2)
                lineTo(size.width/2, size.height); lineTo(0f, size.height/2); close()
            }
            drawPath(path, BrandSoft)
            drawPath(path, Brand.copy(alpha=.45f), style = Stroke(2.dp.toPx()))
            drawCircle(Brand.copy(alpha=.12f), 15.dp.toPx(), Offset(size.width/2, size.height/2))
        }
        positions.forEachIndexed { index, alignment ->
            Box(Modifier.align(alignment).size(24.dp).background(if (index < room.memberIds.size) Brand else Muted.copy(alpha=.25f), androidx.compose.foundation.shape.CircleShape))
        }
        Text("${room.memberIds.size}", color = Brand, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}
