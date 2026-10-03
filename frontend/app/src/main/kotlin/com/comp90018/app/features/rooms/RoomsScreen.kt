package com.comp90018.app.features.rooms

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.*
import com.comp90018.app.R
import com.comp90018.app.data.chat.ChatMessage
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.data.rooms.FirebaseTeamRoomRepository
import com.comp90018.app.data.rooms.TeamRoom
import com.comp90018.app.data.rooms.TeamRoomMember
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.data.social.FriendshipStatus
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.ui.components.AppTextField
import com.comp90018.app.ui.components.ChatComposer
import com.comp90018.app.ui.components.formatMessageTimestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RoomsScreen(
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    profile: UserProfile?,
    viewModel: RoomsViewModel,
    treasures: List<MapRelic>,
    treasuresLoading: Boolean,
    treasuresError: String?,
    onStartHunt: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mockActiveRoomId by remember { mutableStateOf<String?>(null) }
    when {
        mockActiveRoomId != null -> MockTeamRoomScreen(
            roomId = requireNotNull(mockActiveRoomId),
            profile = profile,
            onDismiss = { mockActiveRoomId = null },
        )
        state.activeRoomId != null -> TeamRoomChatScreen(
            requireNotNull(state.activeRoomId),
            user,
            firestore,
            profile,
            onRoomExited = viewModel::clearErrors,
            treasures = treasures,
            treasuresLoading = treasuresLoading,
            treasuresError = treasuresError,
            onStartHunt = onStartHunt,
        )
        else -> RoomEntryScreen(
            state = state,
            viewModel = viewModel,
            onJoin = {
                if (state.roomIdInput.trim().equals(DEMO_ROOM_ID, ignoreCase = true)) {
                    mockActiveRoomId = DEMO_ROOM_ID
                } else {
                    viewModel.joinRoom()
                }
            },
        )
    }
}

@Composable
private fun RoomEntryScreen(state: RoomsUiState, viewModel: RoomsViewModel, onJoin: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(86.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.nav_rooms_symbol), null, modifier = Modifier.size(76.dp))
        }
        Text("You haven't joined a room yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Text("Join an existing room or create a new one to hunt for treasure together.", color = Muted, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onJoin, enabled = state.roomIdInput.isNotBlank() && !state.working) {
                Icon(Icons.Rounded.ChevronRight, "Find room", tint = Brand, modifier = Modifier.size(30.dp))
            }
            Box(Modifier.weight(1f)) {
                AppTextField("Enter room ID", state.roomIdInput, viewModel::updateRoomId)
            }
        }
        Text("Demo room ID: $DEMO_ROOM_ID", color = Muted, style = MaterialTheme.typography.bodySmall)
        (state.actionError ?: state.membershipError)?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        Button(onClick = viewModel::createRoom, modifier = Modifier.fillMaxWidth(), enabled = !state.working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (state.working && !state.joining) "Creating..." else "Create a room") }
    }
}

private const val DEMO_ROOM_ID = "CAMPUS-2026"

@Composable
private fun TeamRoomChatScreen(
    roomId: String,
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    profile: UserProfile?,
    onRoomExited: () -> Unit,
    treasures: List<MapRelic>,
    treasuresLoading: Boolean,
    treasuresError: String?,
    onStartHunt: (String) -> Unit,
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
    val messages = state.messages
    val members = state.members
    val error = state.error
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
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        if (room?.taskStatus == "hunting") {
            ActiveHuntHeader(
                room = room,
                currentUserId = user.uid,
                updating = state.updatingTask,
                onContinue = { onStartHunt(room.taskId) },
                onTerminate = viewModel::terminateHunt,
                onClaimAtlas = viewModel::claimCompletedHuntTreasure,
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
        Row(
            Modifier.fillMaxWidth().clickable { showDetails = true }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (members.isEmpty()) {
                ProfileAvatar(profile?.avatarUrl.orEmpty(), profile?.username.orEmpty().ifBlank { "You" }, 46.dp)
            } else {
                members.take(4).forEachIndexed { index, member ->
                    if (index > 0) Spacer(Modifier.width(8.dp))
                    ProfileAvatar(member.avatarUrl, member.name, 46.dp)
                }
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
        ChatComposer(
            value = state.input,
            onValueChange = viewModel::updateInput,
            onSend = {
                val name = profile?.username.orEmpty().ifBlank { profile?.displayName.orEmpty().ifBlank { "You" } }.take(30)
                viewModel.send(name, profile?.avatarUrl.orEmpty())
            },
            sendEnabled = state.input.isNotBlank(),
            sending = state.sending,
            showTreasureAction = true,
        )
    }
    if (choosingDestination) DestinationPickerDialog(
        treasures = treasures,
        loading = treasuresLoading,
        error = treasuresError,
        onSelected = { relic ->
            viewModel.selectDestination(relic.id, relic.name)
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
        onLeave = { viewModel.leave(onRoomExited) },
        onDismissRoom = {
            showDetails = false
            viewModel.dismiss(onRoomExited)
        },
        onDismiss = { showDetails = false },
    )
}

@Composable
private fun ActiveHuntHeader(
    room: TeamRoom,
    currentUserId: String,
    updating: Boolean,
    onContinue: () -> Unit,
    onTerminate: () -> Unit,
    onClaimAtlas: () -> Unit,
) {
    val isFragmentHunt = room.taskId == "south_lawn_atlas"
    val allFragmentsFound = listOf(
        "south_lawn_north_west", "south_lawn_north_east",
        "south_lawn_south_west", "south_lawn_south_east",
    ).all { it in room.foundFragmentIds }
    val completed = currentUserId in room.taskCompletedMemberIds
    val allCompleted = room.memberIds.isNotEmpty() && room.memberIds.all { it in room.taskCompletedMemberIds }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = BrandSoft)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Targeting ${room.taskTitle}", color = Ink, fontWeight = FontWeight.Bold)
            Text(
                when {
                    isFragmentHunt && currentUserId in room.taskClaimedMemberIds -> "Your Atlas is claimed. Waiting for your teammate to claim theirs."
                    currentUserId in room.taskClaimedMemberIds -> "Your treasure is claimed. Waiting for your teammate to claim theirs."
                    isFragmentHunt && allFragmentsFound -> "All four fragments are found. Turn to the map to reconstruct the Atlas."
                    isFragmentHunt -> "Find all four Atlas fragments together. Found fragments are shared with your teammate."
                    allCompleted -> "Both explorers are ready. Turn to the map to dig the treasure."
                    completed -> "Waiting for your teammate to finish. You can help them."
                    else -> "Go and hunt for the treasure."
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (isFragmentHunt) SouthLawnFragmentStatus(room.foundFragmentIds)
            if (allCompleted && currentUserId !in room.taskClaimedMemberIds) {
                Button(
                    onClick = onClaimAtlas,
                    enabled = !updating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) { Text(if (updating) "Claiming..." else "Claim Treasure") }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                Text(if (isFragmentHunt && currentUserId in room.taskClaimedMemberIds) "View hunt status" else if (isFragmentHunt && allFragmentsFound) "Turn to map and reconstruct" else if (isFragmentHunt) "Turn to map to find fragments" else if (allCompleted) "Turn to map and dig" else "Turn to map to continue your hunt")
            }
            if (room.creatorId == currentUserId) {
                OutlinedButton(onClick = onTerminate, enabled = !updating, modifier = Modifier.fillMaxWidth()) {
                    Text(if (updating) "Terminating..." else "Terminate Hunt", color = RelicRed)
                }
            }
        }
    }
}

@Composable
private fun SouthLawnFragmentStatus(foundFragmentIds: List<String>) {
    val fragments = listOf(
        "south_lawn_north_west" to "North-west fragment",
        "south_lawn_north_east" to "North-east fragment",
        "south_lawn_south_west" to "South-west fragment",
        "south_lawn_south_east" to "South-east fragment",
    )
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("ROOM TASK STATUS", color = Muted, style = MaterialTheme.typography.labelMedium)
        fragments.forEach { (id, label) ->
            val found = id in foundFragmentIds
            Text(
                "${if (found) "✓" else "○"} $label: ${if (found) "is found" else "not found"}",
                color = if (found) Brand else Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RoomHuntControls(
    room: TeamRoom?,
    treasures: List<MapRelic>,
    isOwner: Boolean,
    memberCount: Int,
    treasuresLoading: Boolean,
    treasuresError: String?,
    updating: Boolean,
    onChooseDestination: () -> Unit,
    onStartHunt: () -> Unit,
) {
    val hasDestination = !room?.taskId.isNullOrBlank()
    val hasTwoMembers = memberCount == 2
    val destinationName = room?.taskTitle?.takeIf { it.isNotBlank() } ?: "Choose destination"
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onChooseDestination,
                enabled = !updating,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Rounded.LocationOn, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(destinationName, maxLines = 1)
            }
            if (isOwner) {
                Button(
                    onClick = onStartHunt,
                    enabled = hasTwoMembers && hasDestination && !updating,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(if (updating) "Starting..." else "Start Hunt")
                }
            }
        }
        if (!isOwner || !hasTwoMembers || !hasDestination) {
            Text(
                when {
                    !isOwner -> "Only the room owner can start a hunt."
                    !hasTwoMembers -> "Start Hunt unlocks when 2 explorers are in the room."
                    treasuresLoading -> "Loading the six treasure destinations…"
                    treasuresError != null -> "Treasure destinations are unavailable right now."
                    else -> "Choose one of the six treasures to unlock Start Hunt."
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        } else if (room.taskStatus == "hunting") {
            Text("Hunt in progress: $destinationName", color = Brand, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DestinationPickerDialog(
    treasures: List<MapRelic>,
    loading: Boolean,
    error: String?,
    onSelected: (MapRelic) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose destination", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brand) }
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                treasures.isEmpty() -> Text("No enabled treasures are available yet. Add the six treasure records in Firestore, then try again.", color = Muted)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(treasures.take(6), key = { it.id }) { relic ->
                        OutlinedButton(onClick = { onSelected(relic) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(relic.name, color = Ink, fontWeight = FontWeight.Medium)
                                Text(relic.locationName, color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Brand) } },
    )
}

@Composable
private fun MockTeamRoomScreen(roomId: String, profile: UserProfile?, onDismiss: () -> Unit) {
    var input by remember(roomId) { mutableStateOf("") }
    var messages by remember(roomId) { mutableStateOf(listOf("ava: Welcome! I found our first clue.")) }
    var showDetails by remember(roomId) { mutableStateOf(false) }
    val ownName = profile?.username.orEmpty().ifBlank { "You" }
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { showDetails = true }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(profile?.avatarUrl.orEmpty(), ownName, 46.dp)
            Spacer(Modifier.width(8.dp))
            ProfileAvatar("", "ava", 46.dp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Info, "Room details", tint = Brand, modifier = Modifier.padding(12.dp).size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages) { message -> Text(message, color = Ink) }
            }
        }
        Spacer(Modifier.height(10.dp))
        ChatComposer(
            value = input,
            onValueChange = { input = it },
            onSend = {
                val message = input.trim()
                if (message.isNotBlank()) {
                    messages = messages + "$ownName: $message"
                    input = ""
                }
            },
            sendEnabled = input.isNotBlank(),
            showTreasureAction = true,
        )
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Room details", color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Room ID: $roomId", color = Ink)
                    Text("This is a local demo room. No backend data was changed.", color = Muted)
                }
            },
            confirmButton = { TextButton(onClick = { showDetails = false }) { Text("Done", color = Brand) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss room", color = RelicRed) } },
        )
    }
}

@Composable
private fun TeamMessageRow(message: ChatMessage, currentUid: String, profile: UserProfile?) {
    val mine = message.senderId == currentUid
    val name = message.senderName.ifBlank { if (mine) profile?.displayName.orEmpty().ifBlank { "You" } else "Teammate" }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        if (!mine) { ProfileAvatar(message.senderAvatarUrl, name, 40.dp); Spacer(Modifier.width(8.dp)) }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Text(name, color = Muted, style = MaterialTheme.typography.labelMedium)
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (mine) Brand else BrandSoft)) {
                Text(message.text, Modifier.padding(14.dp), color = if (mine) Color.White else Ink)
            }
            formatMessageTimestamp(message.sentAtMillis)?.let { timestamp ->
                Text(timestamp, color = Muted, style = MaterialTheme.typography.labelSmall)
            }
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
                memberProfile?.bio?.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink, textAlign = TextAlign.Center) }
                if (username != displayName) Text("@$username", color = Muted)
                memberProfile?.gender?.takeIf { it != "unspecified" }?.let { Text(it.replaceFirstChar { char -> char.uppercase() }, color = Muted) }
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
    room: TeamRoom?,
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
            room?.takeIf { it.taskStatus == "hunting" }?.let { activeHunt ->
                Column {
                    Text("HUNT STATUS", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(activeHunt.taskTitle, color = Ink, fontWeight = FontWeight.Medium)
                    if (activeHunt.taskId == "south_lawn_atlas") {
                        SouthLawnFragmentStatus(activeHunt.foundFragmentIds)
                        members.forEach { member ->
                            val claimed = member.uid in activeHunt.taskClaimedMemberIds
                            Text(
                                "${if (claimed) "✓" else "○"} ${member.name}: ${if (claimed) "Atlas claimed" else "Atlas not claimed"}",
                                color = if (claimed) Brand else Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    members.forEach { member ->
                        val done = member.uid in activeHunt.taskCompletedMemberIds
                        Text("${if (done) "✓" else "○"} ${member.name}: ${if (done) "Task complete" else "Still hunting"}", color = if (done) Brand else Muted)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Brand) } }, dismissButton = {
        TextButton(onClick = if (isOwner) onDismissRoom else onLeave) { Text(if (isOwner) "Dismiss room" else "Leave room", color = RelicRed) }
    })
}
