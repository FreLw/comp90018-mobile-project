package com.comp90018.app.features.rooms

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.*
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.data.rooms.FirebaseTeamRoomRepository
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.treasure.TreasurePrototypeImage
import com.comp90018.app.ui.components.AppTextField
import com.comp90018.app.ui.components.ChatComposer
import com.comp90018.app.ui.components.ConversationMessageRow
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RoomsScreen(
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    profile: UserProfile?,
    viewModel: RoomsViewModel,
    treasures: List<MapRelic>,
    discoveredTreasureIds: Set<String>,
    onOpenTreasureMap: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.activeRoomId != null) {
        TeamRoomChatScreen(
            roomId = requireNotNull(state.activeRoomId),
            user = user,
            firestore = firestore,
            profile = profile,
            treasures = treasures,
            discoveredTreasureIds = discoveredTreasureIds,
            onOpenTreasureMap = onOpenTreasureMap,
            onRoomExited = viewModel::clearErrors,
        )
    } else {
        RoomPlazaScreen(state, viewModel, profile)
    }
}

@Composable
private fun RoomPlazaScreen(state: RoomsUiState, viewModel: RoomsViewModel, profile: UserProfile?) {
    var showCreateRoom by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Room Plaza", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Find a crew and begin a shared hunt.", color = Muted)
                }
                FilledIconButton(onClick = { showCreateRoom = true }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Brand)) {
                    Icon(Icons.Rounded.Add, "Create room")
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Join with an ID", color = Ink, fontWeight = FontWeight.Bold)
                    Text("Room IDs contain six digits only.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.roomIdInput,
                            onValueChange = viewModel::updateRoomId,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("e.g. 428106") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Button(
                            onClick = viewModel::joinRoom,
                            enabled = state.roomIdInput.length == 6 && !state.working,
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                        ) { Text("Join") }
                    }
                }
            }
        }
        (state.actionError ?: state.membershipError ?: state.plazaError)?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        }
        item { Text("Open rooms", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink) }
        when {
            state.plazaLoading -> item { Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) } }
            state.publicRooms.isEmpty() -> item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandSoft)) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Explore, null, tint = Brand, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("The plaza is quiet", color = Ink, fontWeight = FontWeight.Bold)
                        Text("Create the first public room.", color = Muted)
                    }
                }
            }
            else -> items(state.publicRooms, key = { it.id }) { room ->
                PublicRoomCard(
                    room = room,
                    members = state.publicRoomMembers[room.id].orEmpty(),
                    joining = state.working && state.roomIdInput == room.id,
                    onJoin = { viewModel.joinRoom(room.id) },
                )
            }
        }
    }
    if (showCreateRoom) {
        CreateRoomDialog(
            defaultName = profile?.displayName.orEmpty().ifBlank { profile?.username.orEmpty() }.let { name ->
                if (name.isBlank()) "Treasure Crew" else "$name's Crew"
            },
            working = state.working,
            onCreate = { name, isPublic, maxMembers -> viewModel.createRoom(name, isPublic, maxMembers) },
            onDismiss = { if (!state.working) showCreateRoom = false },
        )
    }
}

@Composable
private fun PublicRoomCard(room: TeamRoom, members: List<TeamRoomMember>, joining: Boolean, onJoin: () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(room.name, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("#${room.id}  ·  ${room.memberIds.size}/${room.maxMembers} explorers", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Rounded.Public, "Public room", tint = Brand)
            }
            RoomMemberSlots(room, members, avatarSize = 39.dp)
            Button(onClick = onJoin, modifier = Modifier.fillMaxWidth(), enabled = !joining && room.memberIds.size < room.maxMembers) {
                Text(if (joining) "Joining…" else "Join room")
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(
    defaultName: String,
    working: Boolean,
    onCreate: (String, Boolean, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(defaultName) { mutableStateOf(defaultName.take(40)) }
    var isPublic by remember { mutableStateOf(true) }
    var maxMembers by remember { mutableIntStateOf(6) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create a room", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppTextField("Room name", name, { name = it.take(40) })
                Text("Maximum explorers", color = Ink, fontWeight = FontWeight.Medium)
                RoomCapacityPicker(maxMembers, onSelect = { maxMembers = it })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Show in Room Plaza", color = Ink, fontWeight = FontWeight.Medium)
                        Text("Anyone signed in can discover and join it.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name, isPublic, maxMembers) }, enabled = name.isNotBlank() && !working) {
                Text(if (working) "Creating…" else "Create")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text("Cancel") } },
    )
}

@Composable
private fun RoomCapacityPicker(selected: Int, minimum: Int = 2, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (2..6).forEach { count ->
            FilterChip(
                selected = selected == count,
                onClick = { onSelect(count) },
                enabled = count >= minimum,
                label = { Text(count.toString()) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TeamRoomChatScreen(
    roomId: String,
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    profile: UserProfile?,
    treasures: List<MapRelic>,
    discoveredTreasureIds: Set<String>,
    onOpenTreasureMap: (String) -> Unit,
    onRoomExited: () -> Unit,
) {
    val repository = remember(firestore) { FirebaseTeamRoomRepository(firestore) }
    val viewModel: TeamRoomChatViewModel = viewModel(
        key = "team_room_${roomId}_${user.uid}",
        factory = TeamRoomChatViewModel.factory(repository, roomId, user.uid),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        viewModel.setScreenVisible(true)
        onDispose { viewModel.setScreenVisible(false) }
    }
    val room = state.room
    var showDetails by remember(roomId) { mutableStateOf(false) }
    var viewingMember by remember(roomId) { mutableStateOf<TeamRoomMember?>(null) }
    var directChatId by remember(roomId) { mutableStateOf<String?>(null) }
    var showTreasurePicker by remember(roomId) { mutableStateOf(false) }
    val collectedTreasures = remember(treasures, discoveredTreasureIds) {
        treasures.filter { it.id in discoveredTreasureIds }
    }
    val pinnedTreasure = remember(room?.pinnedTreasureId, treasures) {
        treasures.firstOrNull { it.id == room?.pinnedTreasureId }
    }

    if (directChatId != null) {
        val member = requireNotNull(viewingMember)
        DirectChatScreen(
            firestore = firestore,
            roomId = requireNotNull(directChatId),
            currentUid = user.uid,
            friendUid = member.uid,
            title = member.name,
            currentUsername = profile?.username.orEmpty(),
            currentAvatarUrl = profile?.avatarUrl.orEmpty(),
            onBack = { directChatId = null },
        )
        return
    }
    if (viewingMember != null) {
        RoomMemberProfileScreen(
            firestore = firestore,
            member = requireNotNull(viewingMember),
            currentUser = user,
            currentProfile = profile,
            onBack = { viewingMember = null },
            onOpenChat = { directChatId = it },
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = room != null) { showDetails = true }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(room?.name ?: "Loading room…", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Room #$roomId", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.Info, "Room details", tint = Brand, modifier = Modifier.padding(12.dp).size(24.dp))
        }
        if (room != null) RoomMemberSlots(room, state.members, onMemberClick = { viewingMember = it })
        Spacer(Modifier.height(10.dp))
        Text("TASK BOARD", color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            RoomTreasureBoard(
                treasure = pinnedTreasure,
                hasCollectedTreasures = collectedTreasures.isNotEmpty(),
                onChooseTreasure = { showTreasurePicker = true },
                onOpenMap = { pinnedTreasure?.let { onOpenTreasureMap(it.id) } },
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("ROOM CHAT", color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Card(
            Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            if (state.messages.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                    Text(state.error ?: "Gather your crew and start the conversation.", color = Muted, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.messages, key = { it.id }) { message ->
                        ConversationMessageRow(
                            message = message,
                            currentUid = user.uid,
                            fallbackOtherName = "Explorer",
                            currentName = profile?.displayName.orEmpty().ifBlank { profile?.username.orEmpty() },
                            currentAvatarUrl = profile?.avatarUrl.orEmpty(),
                            hostUid = room?.creatorId,
                            onAvatarClick = state.members.firstOrNull { it.uid == message.senderId }?.let { member ->
                                { viewingMember = member }
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        ChatComposer(
            value = state.input,
            onValueChange = viewModel::updateInput,
            onSend = {
                val name = profile?.displayName.orEmpty().ifBlank { profile?.username.orEmpty().ifBlank { "You" } }.take(30)
                viewModel.send(name, profile?.avatarUrl.orEmpty())
            },
            sendEnabled = state.input.isNotBlank(),
            sending = state.sending,
            showTreasureAction = true,
        )
    }
    if (showDetails && room != null) {
        RoomDetailsDialog(
            room = room,
            members = state.members,
            isOwner = room.creatorId == user.uid,
            saving = state.savingSettings,
            error = state.error,
            onMemberClick = { viewingMember = it; showDetails = false },
            onSave = { name, isPublic, maxMembers ->
                viewModel.updateSettings(name, isPublic, maxMembers) { showDetails = false }
            },
            onLeave = { viewModel.leave(onRoomExited) },
            onDismissRoom = { showDetails = false; viewModel.dismiss(onRoomExited) },
            onDismiss = { if (!state.savingSettings) showDetails = false },
        )
    }
    if (showTreasurePicker) {
        CollectedTreasurePicker(
            treasures = collectedTreasures,
            pinning = state.pinningTreasure,
            error = state.error,
            onChoose = { treasure -> viewModel.pinTreasure(treasure.id) { showTreasurePicker = false } },
            onDismiss = { if (!state.pinningTreasure) showTreasurePicker = false },
        )
    }
}

@Composable
private fun RoomTreasureBoard(
    treasure: MapRelic?,
    hasCollectedTreasures: Boolean,
    onChooseTreasure: () -> Unit,
    onOpenMap: () -> Unit,
) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (treasure == null) {
            Box(Modifier.size(58.dp).background(BrandSoft, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AddLocationAlt, null, tint = Brand)
            }
            Column(Modifier.weight(1f)) {
                Text("Place a collected treasure", color = Ink, fontWeight = FontWeight.Bold)
                Text(
                    if (hasCollectedTreasures) "Pin one from your collection to the room map."
                    else "Discover a treasure before placing it here.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            TextButton(onClick = onChooseTreasure, enabled = hasCollectedTreasures) { Text("Choose") }
        } else {
            Box(Modifier.size(62.dp).background(BrandSoft, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                TreasurePrototypeImage(treasure, discovered = true, modifier = Modifier.size(55.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(treasure.name, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(treasure.locationName, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            IconButton(onClick = onOpenMap) { Icon(Icons.Rounded.Map, "View treasure map", tint = Brand) }
            IconButton(onClick = onChooseTreasure, enabled = hasCollectedTreasures) { Icon(Icons.Rounded.SwapHoriz, "Replace treasure", tint = Brand) }
        }
    }
}

@Composable
private fun CollectedTreasurePicker(
    treasures: List<MapRelic>,
    pinning: Boolean,
    error: String?,
    onChoose: (MapRelic) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Place a treasure", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 420.dp)) {
                items(treasures, key = { it.id }) { treasure ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !pinning) { onChoose(treasure) },
                        color = BrandSoft,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            TreasurePrototypeImage(treasure, discovered = true, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(treasure.name, color = Ink, fontWeight = FontWeight.Bold)
                                Text(treasure.locationName, color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Rounded.PushPin, null, tint = Brand)
                        }
                    }
                }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !pinning) { Text("Cancel") } },
    )
}

@Composable
private fun RoomMemberSlots(
    room: TeamRoom,
    members: List<TeamRoomMember>,
    avatarSize: Dp = 44.dp,
    onMemberClick: ((TeamRoomMember) -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        repeat(6) { index ->
            when {
                index >= room.maxMembers -> LockedRoomSlot(avatarSize)
                index < members.size -> MemberRoomSlot(members[index], members[index].uid == room.creatorId, avatarSize, onMemberClick)
                else -> EmptyRoomSlot(avatarSize)
            }
        }
    }
}

@Composable
private fun MemberRoomSlot(member: TeamRoomMember, isHost: Boolean, size: Dp, onClick: ((TeamRoomMember) -> Unit)?) {
    val clickableModifier = if (onClick == null) Modifier else Modifier.clickable { onClick(member) }
    Box(clickableModifier, contentAlignment = Alignment.BottomEnd) {
        ProfileAvatar(member.avatarUrl, member.name, size)
        if (isHost) {
            Box(
                Modifier.size(size * 0.38f).clip(CircleShape).background(Color(0xFFFFC94A)).border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("★", color = Color(0xFF6D4800), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun LockedRoomSlot(size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(0xFFE7E8EB)), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Lock, "Locked seat", tint = Muted, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
private fun EmptyRoomSlot(size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(BrandSoft).border(1.dp, Brand.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size * 0.48f)) {
            val stroke = Stroke(width = 2.dp.toPx())
            drawCircle(Brand, radius = this.size.minDimension * 0.22f, center = Offset(this.size.width / 2f, this.size.height * 0.3f), style = stroke)
            drawLine(Brand, Offset(this.size.width * 0.2f, this.size.height * 0.82f), Offset(this.size.width * 0.8f, this.size.height * 0.82f), strokeWidth = 2.dp.toPx())
            drawLine(Brand, Offset(this.size.width * 0.2f, this.size.height * 0.82f), Offset(this.size.width * 0.35f, this.size.height * 0.58f), strokeWidth = 2.dp.toPx())
            drawLine(Brand, Offset(this.size.width * 0.8f, this.size.height * 0.82f), Offset(this.size.width * 0.65f, this.size.height * 0.58f), strokeWidth = 2.dp.toPx())
            drawLine(Brand, Offset(this.size.width * 0.73f, this.size.height * 0.3f), Offset(this.size.width * 0.98f, this.size.height * 0.3f), strokeWidth = 2.dp.toPx())
            drawLine(Brand, Offset(this.size.width * 0.855f, this.size.height * 0.175f), Offset(this.size.width * 0.855f, this.size.height * 0.425f), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
private fun RoomDetailsDialog(
    room: TeamRoom,
    members: List<TeamRoomMember>,
    isOwner: Boolean,
    saving: Boolean,
    error: String?,
    onMemberClick: (TeamRoomMember) -> Unit,
    onSave: (String, Boolean, Int) -> Unit,
    onLeave: () -> Unit,
    onDismissRoom: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember(room.id) { mutableStateOf(false) }
    var name by remember(room.id, room.name) { mutableStateOf(room.name) }
    var isPublic by remember(room.id, room.isPublic) { mutableStateOf(room.isPublic) }
    var maxMembers by remember(room.id, room.maxMembers) { mutableIntStateOf(room.maxMembers) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Room details", fontWeight = FontWeight.Bold, color = Ink) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text("ROOM ID", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(room.id, color = Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            context.getSystemService(ClipboardManager::class.java)
                                .setPrimaryClip(ClipData.newPlainText("Room ID", room.id))
                            copied = true
                        }) { Text(if (copied) "Copied" else "Copy", color = Brand) }
                    }
                }
                if (isOwner) {
                    item { AppTextField("Room name", name, { name = it.take(40) }) }
                    item {
                        Text("Maximum explorers", color = Ink, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        RoomCapacityPicker(maxMembers, minimum = members.size.coerceAtLeast(2), onSelect = { maxMembers = it })
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Show in Room Plaza", color = Ink, fontWeight = FontWeight.Medium)
                                Text(if (isPublic) "Public and discoverable" else "Private — ID required", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                        }
                    }
                } else {
                    item {
                        Text(room.name, color = Ink, fontWeight = FontWeight.Bold)
                        Text(if (room.isPublic) "Public room" else "Private room", color = Muted)
                    }
                }
                item {
                    Text("MEMBERS (${members.size}/${room.maxMembers})", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    members.forEach { member ->
                        TextButton(onClick = { onMemberClick(member) }, contentPadding = PaddingValues(0.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                ProfileAvatar(member.avatarUrl, member.name, 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(member.name, color = Ink, fontWeight = FontWeight.Medium)
                                if (member.uid == room.creatorId) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(color = Color(0xFFFFE7A3), shape = RoundedCornerShape(7.dp)) {
                                        Text("HOST", Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = Color(0xFF7A5300), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            if (isOwner) {
                Button(onClick = { onSave(name, isPublic, maxMembers) }, enabled = name.isNotBlank() && !saving) {
                    Text(if (saving) "Saving…" else "Save")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Done", color = Brand) }
            }
        },
        dismissButton = {
            TextButton(onClick = if (isOwner) onDismissRoom else onLeave, enabled = !saving) {
                Text(if (isOwner) "Dismiss room" else "Leave room", color = RelicRed)
            }
        },
    )
}

@Composable
private fun RoomMemberProfileScreen(
    firestore: FirebaseFirestore,
    member: TeamRoomMember,
    currentUser: FirebaseUser,
    currentProfile: UserProfile?,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
) {
    val isCurrentUser = member.uid == currentUser.uid
    val profileRepository = remember(firestore) { FirebaseProfileRepository(firestore) }
    val socialRepository = remember(firestore) { FirebaseSocialRepository(firestore) }
    val viewModel: RoomMemberProfileViewModel = viewModel(
        key = "room_member_${member.uid}_${currentUser.uid}",
        factory = RoomMemberProfileViewModel.factory(profileRepository, socialRepository, currentUser.uid, member.uid, currentProfile?.username.orEmpty()),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val memberProfile = state.profile
    LaunchedEffect(state.directRoomId) {
        state.directRoomId?.let {
            onOpenChat(it)
            viewModel.consumeDirectRoom()
        }
    }
    val username = memberProfile?.username.orEmpty().ifBlank { member.name }
    val displayName = memberProfile?.displayName.orEmpty().ifBlank { username }
    Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text("Explorer profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileAvatar(memberProfile?.avatarUrl.orEmpty().ifBlank { member.avatarUrl }, displayName, 88.dp)
                Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                memberProfile?.bio?.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink, textAlign = TextAlign.Center) }
                if (username != displayName) Text("@$username", color = Muted)
                memberProfile?.gender?.takeIf { it != "unspecified" }?.let { Text(it.replaceFirstChar { char -> char.uppercase() }, color = Muted) }
            }
        }
        Spacer(Modifier.weight(1f))
        state.error?.let { Text(it, color = if (it.startsWith("Friend request")) Brand else MaterialTheme.colorScheme.error) }
        if (!isCurrentUser) when (state.friendship) {
            FriendshipStatus.Friends -> Button(onClick = { viewModel.openChat(username) }, modifier = Modifier.fillMaxWidth(), enabled = !state.working) { Text(if (state.working) "Opening…" else "Chat") }
            FriendshipStatus.None -> Button(onClick = { viewModel.sendFriendRequest(username) }, modifier = Modifier.fillMaxWidth(), enabled = !state.working) { Text(if (state.working) "Sending…" else "Add friend") }
            FriendshipStatus.IncomingPending -> Button(onClick = { viewModel.acceptFriendRequest(username) }, modifier = Modifier.fillMaxWidth(), enabled = !state.working) { Text(if (state.working) "Processing…" else "Accept request") }
            FriendshipStatus.OutgoingPending -> Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) { Text("Friend request sent") }
            null -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Brand)
        }
    }
}
