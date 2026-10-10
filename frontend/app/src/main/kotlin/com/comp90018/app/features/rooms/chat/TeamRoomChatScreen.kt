package com.comp90018.app.features.rooms.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.data.chat.ChatMessage
import com.comp90018.app.data.chat.ChatMessageTypes
import com.comp90018.app.data.rooms.FirebaseTeamRoomRepository
import com.comp90018.app.data.rooms.TEAM_ROOM_CAPACITY
import com.comp90018.app.data.rooms.TeamRoomMember
import com.comp90018.app.data.treasure.FirebaseTreasureCollectionRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.rooms.TeamRoomChatViewModel
import com.comp90018.app.features.rooms.hunt.ActiveHuntHeader
import com.comp90018.app.features.rooms.hunt.DestinationPickerDialog
import com.comp90018.app.features.rooms.hunt.RoomHuntControls
import com.comp90018.app.features.rooms.hunt.roomDestinationName
import com.comp90018.app.features.rooms.members.RoomMemberProfileScreen
import com.comp90018.app.features.rooms.settings.RoomDetailsDialog
import com.comp90018.app.features.rooms.settings.RoomSettingsForm
import com.comp90018.app.features.treasure.TreasureCollectionViewModel
import com.comp90018.app.ui.components.ChatComposer
import com.comp90018.app.ui.components.ProfileAvatar
import com.comp90018.app.ui.components.formatMessageTimestamp
import com.comp90018.app.ui.components.treasureStickerFor
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Builds the room header, message list, composer, and cooperative-hunt controls. */
@Composable
internal fun TeamRoomChatScreen(
    roomId: String,
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    profile: UserProfile?,
    onRoomExited: () -> Unit,
    treasures: List<MapRelic>,
    treasuresLoading: Boolean,
    treasuresError: String?,
    onStartHunt: (String) -> Unit,
    onClaimTreasure: (TeamRoomChatViewModel) -> Unit = { it.claimCompletedHuntTreasure() },
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
    // Resolve existing selections from the catalogue too: older rooms stored the relic title.
    val room = state.room?.let { storedRoom ->
        treasures.firstOrNull { it.id == storedRoom.taskId }?.let { relic ->
            storedRoom.copy(taskTitle = relic.roomDestinationName())
        } ?: storedRoom
    }
    val messages = state.messages
    val members = state.members
    val error = state.error
    val treasureRepository = remember(firestore) { FirebaseTreasureCollectionRepository(firestore) }
    val treasureViewModel: TreasureCollectionViewModel = viewModel(
        key = "room_treasures_${user.uid}",
        factory = TreasureCollectionViewModel.factory(treasureRepository, user.uid),
    )
    val treasureState by treasureViewModel.uiState.collectAsStateWithLifecycle()
    var confirmDismiss by remember(roomId) { mutableStateOf(false) }
    var editingSettings by remember(roomId) { mutableStateOf(false) }
    var showDetails by remember(roomId) { mutableStateOf(false) }
    var choosingDestination by remember(roomId) { mutableStateOf(false) }
    var viewingMember by remember(roomId) { mutableStateOf<TeamRoomMember?>(null) }
    var directChatId by remember(roomId) { mutableStateOf<String?>(null) }
    if (directChatId != null) {
        val chatMember = requireNotNull(viewingMember)
        DirectChatScreen(firestore, requireNotNull(directChatId), user.uid, chatMember.uid, chatMember.name, profile?.username.orEmpty(), profile?.avatarUrl.orEmpty(), onBack = { directChatId = null })
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
    if (editingSettings && room != null) {
        RoomSettingsForm(title = "Room settings", initialName = room.name,
            initialDescription = room.description, initialIdOnly = room.idOnly,
            working = state.updatingTask, error = state.error, onCancel = { editingSettings = false },
            onSave = { name, description, idOnly -> viewModel.updateSettings(name, room.maxMembers, description, idOnly) { editingSettings = false } })
        return
    }
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        Text(room?.name?.takeIf { it.isNotBlank() } ?: if (room == null) "Loading room…" else "Room ${room.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            val orderedIds = room?.let { listOf(it.creatorId) + it.memberIds.filter { id -> id != it.creatorId } }.orEmpty()
            repeat(room?.maxMembers ?: TEAM_ROOM_CAPACITY) { index ->
                Box(Modifier.padding(end = 8.dp), contentAlignment = Alignment.BottomEnd) {
                    val memberId = orderedIds.getOrNull(index)
                    val member = members.firstOrNull { it.uid == memberId }
                    if (memberId != null) {
                        Box(Modifier.clickable(enabled = member != null) { viewingMember = member }) {
                            ProfileAvatar(member?.avatarUrl ?: if (memberId == user.uid) profile?.avatarUrl.orEmpty() else "",
                                member?.name ?: if (memberId == user.uid) "You" else "Explorer", 46.dp)
                        }
                        if (index == 0) Surface(shape = CircleShape, color = BrandSoft) {
                            Icon(Icons.Rounded.Star, "Room owner", tint = Brand, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Box(Modifier.size(46.dp).border(1.dp, BrandSoft, CircleShape).background(BrandSoft.copy(alpha = 0.35f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, "Waiting for teammate", tint = Muted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDetails = true }) { Icon(Icons.Rounded.Menu, "Room settings", tint = Brand) }
        }

        if (room?.taskStatus == "hunting") {
            ActiveHuntHeader(
                room = room,
                currentUserId = user.uid,
                updating = state.updatingTask,
                onContinue = { onStartHunt(room.taskId) },
                onTerminate = viewModel::terminateHunt,
                onClaimAtlas = { onClaimTreasure(viewModel) },
            )
        } else {
            RoomHuntControls(
                room = room,
                treasures = treasures,
                isOwner = room?.creatorId == user.uid,
                // `memberIds` comes from the live room snapshot. Profile records may
                // arrive later, so they must not determine whether the room is full.
                memberCount = room?.memberIds?.size ?: 0,
                treasuresLoading = treasuresLoading,
                treasuresError = treasuresError,
                updating = state.updatingTask,
                onChooseDestination = { choosingDestination = true },
                onStartHunt = { viewModel.startHunt { onStartHunt(requireNotNull(room).taskId) } },
            )
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            if (messages.isEmpty()) Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) { Text(error ?: "Say hello to your teammate.", color = Muted.copy(alpha = 0.45f), textAlign = TextAlign.Center) }
            else LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(messages, key = { it.id }) { TeamMessageRow(it, user.uid, profile) } }
        }
        Spacer(Modifier.height(10.dp))
        ChatComposer(
            value = state.input,
            onValueChange = viewModel::updateInput,
            onSend = {
                val name = profile?.username.orEmpty().ifBlank { profile?.displayName.orEmpty().ifBlank { "You" } }.take(30)
                viewModel.send(name, profile?.avatarUrl.orEmpty())
            },
            sendEnabled = state.input.isNotBlank(),
            sending = state.sending,
            ownedTreasureStickerIds = treasureState.discoveredIds,
            onTreasureStickerSelected = { viewModel.sendTreasureSticker(it, profile?.username.orEmpty(), profile?.avatarUrl.orEmpty()) },
        )
    }
    if (choosingDestination) DestinationPickerDialog(
        treasures = treasures,
        loading = treasuresLoading,
        error = treasuresError,
        onSelected = { relic ->
            viewModel.selectDestination(relic.id, relic.roomDestinationName())
            choosingDestination = false
        },
        onDismiss = { choosingDestination = false },
    )
    if (showDetails) RoomDetailsDialog(
        roomId = roomId,
        room = room,
        members = members,
        isOwner = room?.creatorId == user.uid,
        onMemberClick = { viewingMember = it; showDetails = false },
        onEditSettings = { showDetails = false; editingSettings = true },
        onRemoveMember = { member, complete -> viewModel.removeMember(member.uid, complete) },
        working = state.updatingTask,
        error = state.error,
        onLeave = { viewModel.leave(onRoomExited) },
        onDismissRoom = {
            showDetails = false
            confirmDismiss = true
        },
        onDismiss = { showDetails = false },
    )
    if (confirmDismiss) AlertDialog(
        onDismissRequest = { confirmDismiss = false },
        title = { Text("Dismiss room?") },
        text = { Text("Are you sure you want to close this room? You may lose your chat history.") },
        confirmButton = { TextButton(enabled = !state.leaving, onClick = { viewModel.dismiss(onRoomExited) }) { Text(if (state.leaving) "Closing..." else "Dismiss room", color = RelicRed) } },
        dismissButton = { TextButton(onClick = { confirmDismiss = false }) { Text("Cancel") } },
    )

}

/** Aligns and formats a team message according to its sender and content type. */
@Composable
private fun TeamMessageRow(message: ChatMessage, currentUid: String, profile: UserProfile?) {
    val mine = message.senderId == currentUid
    val name = message.senderName.ifBlank { if (mine) profile?.displayName.orEmpty().ifBlank { "You" } else "Teammate" }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        if (!mine) { ProfileAvatar(message.senderAvatarUrl, name, 40.dp); Spacer(Modifier.width(8.dp)) }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Text(name, color = Muted, style = MaterialTheme.typography.labelMedium)
            val sticker = treasureStickerFor(message.treasureId)
            if (message.messageType == ChatMessageTypes.TreasureSticker && sticker != null) {
                Image(painterResource(sticker.drawableRes), sticker.name, Modifier.size(112.dp))
            } else {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (mine) Brand else BrandSoft)) {
                    SelectionContainer {
                        Text(if (message.imageUrl.isNotBlank()) "[Img]" else message.text, Modifier.padding(14.dp), color = if (mine) Color.White else Ink)
                    }
                }
            }
            formatMessageTimestamp(message.sentAtMillis)?.let { timestamp ->
                Text(timestamp, color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (mine) { Spacer(Modifier.width(8.dp)); ProfileAvatar(message.senderAvatarUrl.ifBlank { profile?.avatarUrl.orEmpty() }, name, 40.dp) }
    }
}
