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
import androidx.compose.material.icons.rounded.Lock
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
            Column {
                Text("房间广场", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("寻找同行的探索者", color = Muted)
            }
        }
        Text("亮点：已有成员  ·  灰点：空位  ·  锁：不可用", color = Muted, style = MaterialTheme.typography.bodySmall)
        when {
            state.browsingLoading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
            state.browsingError != null -> Column { Text(state.browsingError, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { viewModel.stopBrowsing(); viewModel.startBrowsing() }) { Text("重试") } }
            state.publicRooms.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Text("广场还没有公开房间，创建一个邀请大家加入吧。", color = Muted, textAlign = TextAlign.Center) }
            else -> LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(state.publicRooms, key = { it.id }) { room ->
                    Card(modifier = Modifier.fillMaxWidth().clickable(enabled = !state.working) { viewModel.clearErrors(); selectedRoomId = room.id },
                        colors = CardDefaults.cardColors(containerColor = BrandSoft.copy(alpha = .35f))) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            RoomDiamond(room)
                            Text(room.name.ifBlank { "探索者房间" }, color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("${room.memberIds.size}/${room.maxMembers} 人 · ${if (room.memberIds.size >= room.maxMembers) "已满" else "可加入"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
    selectedRoomId?.let { id ->
        val room = state.publicRooms.firstOrNull { it.id == id }
        AlertDialog(onDismissRequest = { if (!state.working) selectedRoomId = null },
            title = { Text(room?.name?.ifBlank { "探索者房间" } ?: "房间已不可用") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (room != null) {
                    RoomDiamond(room, Modifier.align(Alignment.CenterHorizontally).size(160.dp))
                    Text(room.description.ifBlank { "一起出发，寻找校园里的宝藏。" })
                    Text("成员 ${room.memberIds.size}/${room.maxMembers} · 无需房主审核", color = Muted)
                    if (room.taskTitle.isNotBlank()) Text("目的地：${room.taskTitle}")
                } else Text("房间可能已关闭，或已改为仅 Room ID 加入。")
                state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { Button(enabled = room != null && room.memberIds.size < room.maxMembers && !state.working,
                onClick = { room?.let(viewModel::joinPublicRoom) }) { Text(if (state.working) "加入中…" else if (room != null && room.memberIds.size >= room.maxMembers) "房间已满" else "加入房间") } },
            dismissButton = { TextButton(enabled = !state.working, onClick = { selectedRoomId = null }) { Text("返回广场") } })
    }
}

@Composable
internal fun RoomDiamond(room: TeamRoom, modifier: Modifier = Modifier.size(128.dp)) {
    val positions = listOf(Alignment.TopCenter, Alignment.CenterEnd, Alignment.BottomCenter, Alignment.CenterStart)
    Box(modifier.semantics { contentDescription = "${room.name}，${room.memberIds.size} 个座位有人，${room.maxMembers-room.memberIds.size} 个空位，${4-room.maxMembers} 个锁定座位" }) {
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
            Box(Modifier.align(alignment).size(24.dp).background(if (index < room.memberIds.size) Brand else Muted.copy(alpha=.25f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                if (index >= room.maxMembers) Icon(Icons.Rounded.Lock, null, tint = Muted, modifier = Modifier.size(14.dp))
            }
        }
        Text("${room.memberIds.size}", color = Brand, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
    }
}
