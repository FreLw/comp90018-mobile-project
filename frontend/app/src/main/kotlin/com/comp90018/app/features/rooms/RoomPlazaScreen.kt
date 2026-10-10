package com.comp90018.app.features.rooms

/*
 * Renders public-room browsing and the decorative room seals.
 * RoomDiamond draws the flat compass/laurel emblem and occupied-seat accents used by room cards.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
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

/** Displays joinable public rooms and connects browse/join actions to RoomsViewModel. */
@Composable
internal fun RoomPlazaScreen(state: RoomsUiState, viewModel: RoomsViewModel, onBack: () -> Unit) {
    val previewRooms = remember { roomPreviewFixtures() }
    var previewEnabled by remember { mutableStateOf(previewRooms.isNotEmpty()) }
    val rooms = (if (previewEnabled) previewRooms else state.publicRooms)
        .sortedBy { it.memberIds.size >= it.maxMembers }
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
                Text("Room plaza", color = Ink, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Find fellow explorers", color = Muted)
            }
        }
        if (previewRooms.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Preview 10 sample rooms", color = Ink, style = MaterialTheme.typography.titleSmall)
                    Text("Sample rooms are for preview only.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = previewEnabled, onCheckedChange = {
                    previewEnabled = it
                    selectedRoomId = null
                })
            }
        }
        Text("Open rooms first · Ash-gray rooms are full", color = Muted, style = MaterialTheme.typography.bodySmall)
        when {
            !previewEnabled && state.browsingLoading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
            !previewEnabled && state.browsingError != null -> Column { Text(state.browsingError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.stopBrowsing(); viewModel.startBrowsing() }) { Text("Retry") } }
            rooms.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("No public rooms yet. Create one and invite others to join.", color = Muted, textAlign = TextAlign.Center) }
            else -> LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(rooms, key = { it.id }) { room ->
                    val full = room.memberIds.size >= room.maxMembers
                    Column(
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            .clickable(enabled = !full && !state.working) {
                                viewModel.clearErrors()
                                selectedRoomId = room.id
                            }.padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        RoomDiamond(room, Modifier.weight(1f).aspectRatio(1f))
                        Text(
                            room.name.ifBlank { "Explorer room" },
                            color = if (full) Color(0xFF8D8983) else Ink,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                        )
                        Text(
                            "${room.memberIds.size}/${room.maxMembers} · ${if (full) "Full" else "Enter room"}",
                            color = if (full) Color(0xFF9B9690) else Brand,
                            fontFamily = FontFamily.Serif,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
    selectedRoomId?.let { id ->
        val room = rooms.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { if (!state.working) selectedRoomId = null },
            title = { Text(room?.name?.ifBlank { "Explorer room" } ?: "Room unavailable",
                fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (room != null) {
                    RoomDiamond(room, Modifier.align(Alignment.CenterHorizontally).size(160.dp))
                    Text(room.description.ifBlank { "Explore together and discover treasures around campus." })
                    Text(if (previewEnabled) "Members ${room.memberIds.size}/${room.maxMembers} · Preview only"
                        else "Members ${room.memberIds.size}/${room.maxMembers} · No host approval needed", color = Muted)
                    if (room.taskTitle.isNotBlank()) Text("Destination: ${room.taskTitle}")
                } else Text("This room may have closed or now require a Room ID to join.")
                state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(enabled = !previewEnabled && room != null && room.memberIds.size < room.maxMembers && !state.working,
                onClick = { room?.let(viewModel::joinPublicRoom) }) { Text(if (previewEnabled) "Preview only" else if (state.working) "Joining…" else if (room != null && room.memberIds.size >= room.maxMembers) "Room full" else "Join room") } },
            dismissButton = { TextButton(enabled = !state.working, onClick = { selectedRoomId = null }) { Text("Back to plaza") } })
    }
}

/** A flat compass seal with laurel sprigs and an engraved Roman numeral. */
@Composable
internal fun RoomDiamond(room: TeamRoom, modifier: Modifier = Modifier.size(128.dp)) {
    val full = room.memberIds.size >= room.maxMembers
    val metal = if (full) Color(0xFF9A9690) else Brand
    val accent = if (full) Color(0xFFB9B5AE) else Color(0xFFB88A4B)
    val wash = if (full) Color(0xFFE4E0D9) else BrandSoft
    Box(modifier.semantics {
        contentDescription = "${room.name}, ${room.memberIds.size} occupied seats, ${(room.maxMembers - room.memberIds.size).coerceAtLeast(0)} empty seats, ${if (full) "full" else "open"}"
    }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val unit = size.minDimension
            val cx = size.width / 2
            val cy = size.height / 2
            val radius = unit * .32f
            drawCircle(wash.copy(alpha = .65f), radius, Offset(cx, cy))
            drawCircle(metal, radius, Offset(cx, cy), style = Stroke(1.2.dp.toPx()))
            drawCircle(accent, radius * .88f, Offset(cx, cy), style = Stroke(.7.dp.toPx()))
            // Four small compass tips retain the silhouette of the original diamond.
            repeat(4) { index ->
                val angle = index * Math.PI / 2
                fun point(r: Float, delta: Double = 0.0) = Offset(
                    cx + (cos(angle + delta) * r).toFloat(),
                    cy + (sin(angle + delta) * r).toFloat(),
                )
                val tip = point(unit * .43f)
                val left = point(radius * .98f, .12)
                val right = point(radius * .98f, -.12)
                val path = Path().apply {
                    moveTo(tip.x, tip.y); lineTo(left.x, left.y)
                    lineTo(right.x, right.y); close()
                }
                drawPath(path, accent)
            }
            // Paired laurel branches: fine stems with flat, alternating leaves.
            for (side in listOf(-1f, 1f)) {
                val branchX = cx + side * unit * .39f
                drawLine(accent, Offset(branchX, cy + unit * .22f),
                    Offset(branchX, cy - unit * .18f), strokeWidth = 1.dp.toPx())
                repeat(5) { index ->
                    val y = cy + unit * .17f - index * unit * .075f
                    val leaf = Path().apply {
                        moveTo(branchX, y)
                        quadraticTo(branchX + side * unit * .12f, y - unit * .015f,
                            branchX + side * unit * .075f, y - unit * .075f)
                        quadraticTo(branchX, y - unit * .06f, branchX, y)
                        close()
                    }
                    drawPath(leaf, accent.copy(alpha = .85f))
                }
            }
            // Occupied seats appear as tiny brass studs on the inner seal.
            repeat(room.maxMembers.coerceIn(0, 8)) { index ->
                val x = cx + (index - (room.maxMembers.coerceAtMost(8) - 1) / 2f) * unit * .10f
                drawCircle(if (index < room.memberIds.size) metal else accent.copy(alpha = .28f),
                    unit * .018f, Offset(x, cy + radius * .60f))
            }
        }
        Text(
            decorativeRoomNumber(room.memberIds.size),
            color = metal,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.offset(y = (-4).dp),
        )
    }
}

private fun decorativeRoomNumber(number: Int): String = when (number) {
    0 -> "–"
    1 -> "I"
    2 -> "II"
    3 -> "III"
    4 -> "IV"
    5 -> "V"
    6 -> "VI"
    7 -> "VII"
    8 -> "VIII"
    else -> number.toString()
}
