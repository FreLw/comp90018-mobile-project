package com.comp90018.app.features.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.*
import com.comp90018.app.data.rooms.FirebaseTeamRoomRepository
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.rooms.RoomsScreen
import com.comp90018.app.features.rooms.RoomsViewModel
import com.comp90018.app.ui.components.AppTextField
import com.comp90018.app.ui.components.ChatComposer
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

private data class DemoFriend(val username: String, val name: String, val level: Int, val roomId: String?, val bio: String, val lastMessage: String)
private val DemoApplicants = listOf(
    DemoFriend("zoey", "Zoey Wang", 9, null, "Design student, photographer, and relic hunter.", "Hey! Are you hunting today?"),
    DemoFriend("mia", "Mia Lin", 6, "3091", "New to campus and collecting library relics.", "I found another clue near the library."),
    DemoFriend("noah", "Noah Kim", 11, "4812", "History student building a complete relic archive.", "Thanks for sharing the map pin!"),
)

@Composable
fun FriendsScreen(user: FirebaseUser, firestore: FirebaseFirestore, profile: UserProfile?) {
    val socialRepository = remember(firestore) { FirebaseSocialRepository(firestore) }
    val friendsViewModel: FriendsViewModel = viewModel(key = user.uid, factory = FriendsViewModel.factory(socialRepository, user.uid, profile?.username.orEmpty()))
    val state by friendsViewModel.uiState.collectAsStateWithLifecycle()
    val roomRepository = remember(firestore) { FirebaseTeamRoomRepository(firestore) }
    val roomsViewModel: RoomsViewModel = viewModel(key = "rooms_" + user.uid, factory = RoomsViewModel.factory(roomRepository, user.uid))
    val roomState by roomsViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(profile?.username) { friendsViewModel.updateCurrentUsername(profile?.username.orEmpty()) }

    var addFriendMode by remember { mutableStateOf(false) }
    var requestsPage by remember { mutableStateOf(false) }
    var roomPage by remember { mutableStateOf(false) }
    var profileTarget by remember { mutableStateOf<DemoFriend?>(null) }
    var demoChatTarget by remember { mutableStateOf<DemoFriend?>(null) }
    val pendingDemo = remember { mutableStateListOf<DemoFriend>().also { it.addAll(DemoApplicants) } }
    val acceptedDemo = remember { mutableStateListOf<DemoFriend>() }

    state.chatTarget?.let { chatTarget ->
        DirectChatScreen(firestore, chatTarget.roomId, user.uid, chatTarget.friend.username, profile?.username.orEmpty(), profile?.avatarUrl.orEmpty(), onBack = friendsViewModel::closeChat)
        return
    }
    if (roomPage) { RoomsScreen(user, firestore, profile, onBack = { roomPage = false }); return }
    if (addFriendMode) { FriendFinder(user, firestore, profile?.username.orEmpty(), profile?.avatarUrl.orEmpty(), onClose = { addFriendMode = false }); return }
    demoChatTarget?.let { friend -> DemoChatScreen(friend, profile?.username.orEmpty()) { demoChatTarget = null }; return }
    profileTarget?.let { friend ->
        DemoFriendProfile(
            friend = friend,
            pending = friend in pendingDemo,
            accepted = friend in acceptedDemo,
            onBack = { profileTarget = null },
            onAccept = { pendingDemo.remove(friend); if (friend !in acceptedDemo) acceptedDemo.add(friend); profileTarget = null },
            onDecline = { pendingDemo.remove(friend); profileTarget = null },
            onMessage = { profileTarget = null; demoChatTarget = friend },
        )
        return
    }
    if (requestsPage) {
        FriendRequestsPage(
            demoRequests = pendingDemo,
            realRequests = state.requests,
            onBack = { requestsPage = false },
            onViewDemo = { profileTarget = it },
            onAcceptDemo = { pendingDemo.remove(it); if (it !in acceptedDemo) acceptedDemo.add(it) },
            onDeclineDemo = { pendingDemo.remove(it) },
            onAcceptReal = friendsViewModel::accept,
            onDeclineReal = friendsViewModel::decline,
        )
        return
    }

    FriendsHome(
        profile = profile,
        email = user.email.orEmpty(),
        roomId = roomState.activeRoomId,
        roomWorking = roomState.working,
        roomError = roomState.actionError ?: roomState.membershipError,
        demoFriends = acceptedDemo,
        realFriends = state.friends,
        requestCount = pendingDemo.size + state.requests.size,
        onRequests = { requestsPage = true },
        onAddFriend = { addFriendMode = true },
        onOpenRoom = { roomPage = true },
        onCreateRoom = roomsViewModel::createRoom,
        onJoinRoom = { roomsViewModel.beginJoin(); roomPage = true },
        onDemoChat = { demoChatTarget = it },
        onRealChat = friendsViewModel::openChat,
    )
}

@Composable
private fun FriendsHome(
    profile: UserProfile?, email: String, roomId: String?, roomWorking: Boolean, roomError: String?,
    demoFriends: List<DemoFriend>, realFriends: List<FriendSummary>, requestCount: Int,
    onRequests: () -> Unit, onAddFriend: () -> Unit, onOpenRoom: () -> Unit, onCreateRoom: () -> Unit, onJoinRoom: () -> Unit,
    onDemoChat: (DemoFriend) -> Unit, onRealChat: (FriendSummary) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val normalized = search.trim().lowercase()
    val shownDemo = demoFriends.filter { normalized.isBlank() || it.name.lowercase().contains(normalized) || it.username.contains(normalized) || it.lastMessage.lowercase().contains(normalized) }
    val shownReal = realFriends.filter { normalized.isBlank() || it.username.lowercase().contains(normalized) }
    val username = profile?.username.orEmpty().ifBlank { email.substringBefore("@") }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatar(profile?.avatarUrl.orEmpty(), username, 52.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(username, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Text("Online · Exploring campus", color = Brand, style = MaterialTheme.typography.bodySmall) }
                Box {
                    IconButton(onClick = onRequests) { Icon(Icons.Rounded.Notifications, "Friend requests", tint = Ink) }
                    if (requestCount > 0) Box(Modifier.align(Alignment.TopEnd).size(19.dp).background(Brand, CircleShape), contentAlignment = Alignment.Center) { Text(requestCount.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall) }
                }
                IconButton(onClick = onAddFriend) { Icon(Icons.Rounded.Add, "Add friend", tint = Ink, modifier = Modifier.size(30.dp)) }
            }
        }
        item { Spacer(Modifier.height(18.dp)); AppTextField("Search chats", search, { search = it }, leadingIcon = Icons.Rounded.Search); Spacer(Modifier.height(16.dp)) }
        item {
            CompactRoomRow(roomId, roomWorking, roomError, onOpenRoom, onCreateRoom, onJoinRoom)
            Spacer(Modifier.height(12.dp))
        }
        if (shownDemo.isEmpty() && shownReal.isEmpty()) item { Text(if (normalized.isBlank()) "No conversations yet" else "No matching chats", color = Muted, modifier = Modifier.padding(vertical = 28.dp)) }
        items(shownDemo, key = DemoFriend::username) { friend ->
            ConversationRow(friend.name, friend.lastMessage, friend.username.take(1), onClick = { onDemoChat(friend) })
        }
        items(shownReal, key = FriendSummary::uid) { friend ->
            ConversationRow(friend.username, "Tap to continue your conversation", friend.username.take(1), onClick = { onRealChat(friend) })
        }
    }
}

@Composable
private fun CompactRoomRow(roomId: String?, working: Boolean, error: String?, onOpen: () -> Unit, onCreate: () -> Unit, onJoin: () -> Unit) {
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(enabled = roomId != null, onClick = onOpen),
            color = BrandSoft,
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(Brand, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Groups, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(if (roomId == null) "No room yet" else "Current room", fontWeight = FontWeight.Bold, color = Ink); Text(roomId?.let { "Room ID  " + numericRoomCode(it) } ?: "Join or create a room with another explorer", color = Muted, style = MaterialTheme.typography.bodySmall) }
                if (roomId != null) Icon(Icons.Rounded.ChevronRight, "Open room", tint = Brand)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        if (roomId == null) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCreate, enabled = !working, modifier = Modifier.weight(1f)) { Text(if (working) "Creating…" else "Create room") }
            TextButton(onClick = onJoin, enabled = !working, modifier = Modifier.weight(1f)) { Text("Join room") }
        }
    }
}

@Composable
private fun ConversationRow(name: String, preview: String, letter: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterAvatar(letter.uppercase())
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, color = Ink); Text(preview, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
        HorizontalDivider(color = Color(0xFFE5E7E8))
    }
}

@Composable
private fun FriendRequestsPage(
    demoRequests: SnapshotStateList<DemoFriend>, realRequests: List<IncomingFriendRequest>, onBack: () -> Unit,
    onViewDemo: (DemoFriend) -> Unit, onAcceptDemo: (DemoFriend) -> Unit, onDeclineDemo: (DemoFriend) -> Unit,
    onAcceptReal: (IncomingFriendRequest) -> Unit, onDeclineReal: (IncomingFriendRequest) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }; Text("Friend requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink) }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)) {
            items(demoRequests.toList(), key = DemoFriend::username) { friend ->
                RequestRow(friend.name, "@" + friend.username, friend.username.take(1), onView = { onViewDemo(friend) }, onDecline = { onDeclineDemo(friend) }, onAccept = { onAcceptDemo(friend) })
            }
            items(realRequests, key = IncomingFriendRequest::fromUid) { request ->
                RequestRow(request.fromUsername, "Wants to be friends", request.fromUsername.take(1), onView = {}, onDecline = { onDeclineReal(request) }, onAccept = { onAcceptReal(request) })
            }
            if (demoRequests.isEmpty() && realRequests.isEmpty()) item { Text("No new friend requests", color = Muted, modifier = Modifier.padding(vertical = 32.dp)) }
        }
    }
}

@Composable
private fun RequestRow(name: String, subtitle: String, letter: String, onView: () -> Unit, onDecline: () -> Unit, onAccept: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onView).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterAvatar(letter.uppercase()); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(name, fontWeight = FontWeight.Bold, color = Ink); Text(subtitle, color = Muted) }; TextButton(onClick = onView) { Text("View") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) { Text("Decline") }; Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accept") } }
        HorizontalDivider(Modifier.padding(top = 14.dp), color = Color(0xFFE5E7E8))
    }
}

@Composable
private fun LetterAvatar(letter: String, modifier: Modifier = Modifier) { Box(modifier.size(52.dp).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) { Text(letter, color = Brand, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) } }

@Composable
private fun DemoFriendProfile(friend: DemoFriend, pending: Boolean, accepted: Boolean, onBack: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit, onMessage: () -> Unit) {
    var blocked by remember(friend.username) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        LetterAvatar(friend.username.take(1).uppercase(), Modifier.size(108.dp)); Text(friend.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink); Text("@" + friend.username, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { InfoBadge("Level " + friend.level); InfoBadge(friend.roomId?.let { "Room " + it } ?: "No room") }
        Text(friend.bio, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 18.dp)); Spacer(Modifier.weight(1f))
        if (blocked) Text(friend.name + " is blocked for this demo session.", color = MaterialTheme.colorScheme.error)
        else if (accepted) Button(onClick = onMessage, modifier = Modifier.fillMaxWidth()) { Text("Message") }
        else if (pending) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) { Text("Decline") }; Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accept friend") } }
        else Button(onClick = onAccept, modifier = Modifier.fillMaxWidth()) { Text("Add friend") }
        OutlinedButton(onClick = { blocked = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Rounded.Block, null); Spacer(Modifier.width(8.dp)); Text("Block") }
    }
}

@Composable
private fun InfoBadge(text: String) { Box(Modifier.background(BrandSoft, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) { Text(text, color = Brand, fontWeight = FontWeight.Bold) } }

@Composable
private fun DemoChatScreen(friend: DemoFriend, currentUsername: String, onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val messages = remember(friend.username) { mutableStateListOf(friend.lastMessage) }
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }; LetterAvatar(friend.username.take(1).uppercase(), Modifier.size(38.dp)); Spacer(Modifier.width(10.dp)); Text(friend.name, fontWeight = FontWeight.Bold, color = Ink) }
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Row { LetterAvatar(friend.username.take(1).uppercase(), Modifier.size(36.dp)); Spacer(Modifier.width(8.dp)); Surface(color = BrandSoft, shape = RoundedCornerShape(18.dp)) { Text(messages.first(), Modifier.padding(14.dp), color = Ink) } } }
                items(messages.drop(1)) { message -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Surface(color = Brand, shape = RoundedCornerShape(18.dp)) { Text(message, Modifier.padding(14.dp), color = Color.White) } } }
            }
        }
        ChatComposer(input, { input = it })
        Button(onClick = { messages.add(input); input = "" }, modifier = Modifier.fillMaxWidth(), enabled = input.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, null); Spacer(Modifier.width(8.dp)); Text("Send as " + currentUsername.ifBlank { "You" }) }
    }
}
