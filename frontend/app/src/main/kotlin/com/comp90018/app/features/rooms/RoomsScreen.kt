package com.comp90018.app.features.rooms

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.*
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.data.rooms.FirebaseTeamRoomRepository
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.ui.components.AppTextField
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RoomsScreen(user: FirebaseUser, firestore: FirebaseFirestore, profile: UserProfile?) {
    val repository = remember(firestore) { FirebaseTeamRoomRepository(firestore) }
    val viewModel: RoomsViewModel = viewModel(
        key = "rooms_${user.uid}",
        factory = RoomsViewModel.factory(repository, user.uid),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    state.activeRoomId?.let { TeamRoomChatScreen(it, user, firestore, profile, onRoomExited = {}) }
        ?: RoomEntryScreen(state, viewModel)
}

@Composable
private fun RoomEntryScreen(state: RoomsUiState, viewModel: RoomsViewModel) {
    Column(Modifier.fillMaxSize().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(78.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Group, null, tint = Brand, modifier = Modifier.size(36.dp)) }
        Text("You're not in a room yet.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Text("Create a room or join one to team up and hunt for treasure together.", color = Muted, textAlign = TextAlign.Center)
        if (state.joining) AppTextField("Enter room ID", state.roomIdInput, viewModel::updateRoomId)
        (state.actionError ?: state.membershipError)?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        Button(onClick = viewModel::createRoom, modifier = Modifier.fillMaxWidth(), enabled = !state.working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (state.working && !state.joining) "Creating..." else "Create a room") }
        OutlinedButton(onClick = {
            if (!state.joining) viewModel.beginJoin() else viewModel.joinRoom()
        }, modifier = Modifier.fillMaxWidth(), enabled = !state.working, colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand)) { Text(if (state.working && state.joining) "Joining..." else "Join the room") }
    }
}

@Composable
private fun TeamRoomChatScreen(roomId: String, user: FirebaseUser, firestore: FirebaseFirestore, profile: UserProfile?, onRoomExited: () -> Unit) {
    val repository = remember(firestore) { FirebaseTeamRoomRepository(firestore) }
    val viewModel: TeamRoomChatViewModel = viewModel(
        key = "team_room_${roomId}_${user.uid}",
        factory = TeamRoomChatViewModel.factory(repository, roomId, user.uid),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val room = state.room
    val messages = state.messages
    val members = state.members
    val error = state.error
    var showDetails by remember(roomId) { mutableStateOf(false) }
    var viewingMember by remember(roomId) { mutableStateOf<TeamRoomMember?>(null) }
    var directChatId by remember(roomId) { mutableStateOf<String?>(null) }
    if (directChatId != null) {
        DirectChatScreen(firestore, requireNotNull(directChatId), user.uid, viewingMember?.name ?: "Chat", profile?.username.orEmpty(), profile?.avatarUrl.orEmpty(), onBack = { directChatId = null })
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
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { showDetails = true }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Treasure Room", fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.titleLarge)
                Text("${room?.memberIds?.size ?: 1} of 2 explorers", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Info, "Room details", tint = Brand, modifier = Modifier.padding(12.dp).size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            if (messages.isEmpty()) Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) { Text(error ?: "Say hello to your teammate.", color = Muted, textAlign = TextAlign.Center) }
            else LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(messages, key = { it.id }) { TeamMessageRow(it, user.uid, profile) } }
        }
        Spacer(Modifier.height(10.dp))
        AppTextField("Message", state.input, viewModel::updateInput, leadingIcon = Icons.AutoMirrored.Rounded.Chat)
        Button(onClick = {
            val name = profile?.username.orEmpty().ifBlank { profile?.displayName.orEmpty().ifBlank { "You" } }.take(30)
            viewModel.send(name, profile?.avatarUrl.orEmpty())
        }, modifier = Modifier.fillMaxWidth(), enabled = state.input.isNotBlank() && !state.sending, colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
            Icon(Icons.AutoMirrored.Rounded.Send, null); Spacer(Modifier.width(8.dp)); Text("Send")
        }
    }
    if (showDetails) RoomDetailsDialog(
        roomId = roomId,
        members = members,
        isOwner = room?.creatorId == user.uid,
        onMemberClick = { viewingMember = it; showDetails = false },
        onLeave = { viewModel.leave(onRoomExited) },
        onDismissRoom = { viewModel.dismiss(onRoomExited) },
        onDismiss = { showDetails = false },
    )
}

@Composable
private fun TeamMessageRow(message: ChatMessage, currentUid: String, profile: UserProfile?) {
    val mine = message.senderId == currentUid
    val name = message.senderName.ifBlank { if (mine) profile?.displayName.orEmpty().ifBlank { "You" } else "Teammate" }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        if (!mine) { ProfileAvatar(message.senderAvatarUrl, name, 40.dp); Spacer(Modifier.width(8.dp)) }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Text(name, color = Muted, style = MaterialTheme.typography.labelMedium)
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (mine) Brand else BrandSoft)) { Text(message.text, Modifier.padding(14.dp), color = if (mine) Color.White else Ink) }
        }
        if (mine) { Spacer(Modifier.width(8.dp)); ProfileAvatar(message.senderAvatarUrl.ifBlank { profile?.avatarUrl.orEmpty() }, name, 40.dp) }
    }
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
    val directRoomId = state.directRoomId
    LaunchedEffect(directRoomId) {
        if (directRoomId != null) {
            onOpenChat(directRoomId)
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
                if (username != displayName) Text("@$username", color = Muted)
                memberProfile?.gender?.takeIf { it != "unspecified" }?.let { Text(it.replaceFirstChar { char -> char.uppercase() }, color = Muted) }
                memberProfile?.bio?.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink, textAlign = TextAlign.Center) }
            }
        }
        Spacer(Modifier.weight(1f))
        state.error?.let { Text(it, color = if (it.startsWith("Friend request")) Brand else MaterialTheme.colorScheme.error) }
        if (!isCurrentUser) when (state.friendship) {
            FriendshipStatus.Friends -> Button(onClick = { viewModel.openChat(username) }, modifier = Modifier.fillMaxWidth(), enabled = !state.working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (state.working) "Opening..." else "Chat") }
            FriendshipStatus.None -> Button(onClick = { viewModel.sendFriendRequest(username) }, modifier = Modifier.fillMaxWidth(), enabled = !state.working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (state.working) "Sending..." else "Add friend") }
            FriendshipStatus.IncomingPending -> Button(onClick = { viewModel.acceptFriendRequest(username) }, modifier = Modifier.fillMaxWidth(), enabled = !state.working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (state.working) "Processing..." else "Accept request") }
            FriendshipStatus.OutgoingPending -> Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) { Text("Friend request sent") }
            null -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Brand)
        }
    }
}

@Composable
private fun RoomDetailsDialog(
    roomId: String,
    members: List<TeamRoomMember>,
    isOwner: Boolean,
    onMemberClick: (TeamRoomMember) -> Unit,
    onLeave: () -> Unit,
    onDismissRoom: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Room details", fontWeight = FontWeight.Bold, color = Ink) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text("ROOM ID", color = Muted, style = MaterialTheme.typography.labelMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(roomId, color = Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Room ID", roomId))
                        copied = true
                    }) { Text(if (copied) "Copied" else "Copy", color = Brand) }
                }
            }
            Column {
                Text("MEMBERS (${members.size}/2)", color = Muted, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(8.dp))
                members.forEach { member -> TextButton(onClick = { onMemberClick(member) }, contentPadding = PaddingValues(0.dp)) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) { ProfileAvatar(member.avatarUrl, member.name, 36.dp); Spacer(Modifier.width(10.dp)); Text(member.name, color = Ink, fontWeight = FontWeight.Medium) } } }
                if (members.size < 2) Text("Waiting for one more explorer to join.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Brand) } }, dismissButton = {
        TextButton(onClick = if (isOwner) onDismissRoom else onLeave) { Text(if (isOwner) "Dismiss room" else "Leave room", color = RelicRed) }
    })
}
