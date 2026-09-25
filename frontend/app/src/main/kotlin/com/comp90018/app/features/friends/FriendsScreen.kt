package com.comp90018.app.features.friends

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.FriendSummary
import com.comp90018.app.FriendRequestStatus
import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.OutgoingFriendRequest
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.R
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.ui.components.EmptyState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

private enum class FriendsPage { Main, Requests, RequestDetail, Finder }

/** Renders friend state and delegates social actions to [FriendsViewModel]. */
@Composable
fun FriendsScreen(user: FirebaseUser, firestore: FirebaseFirestore, profile: UserProfile?, onOpenOwnProfile: () -> Unit) {
    val repository = remember(firestore) { FirebaseSocialRepository(firestore) }
    val viewModel: FriendsViewModel = viewModel(
        key = user.uid,
        factory = FriendsViewModel.factory(repository, user.uid, profile?.username.orEmpty()),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(profile?.username) { viewModel.updateCurrentUsername(profile?.username.orEmpty()) }
    val chatTarget = state.chatTarget
    var addFriendMode by remember { mutableStateOf(false) }
    var viewingProfile by remember(chatTarget) { mutableStateOf(false) }
    var viewingRequest by remember { mutableStateOf<IncomingFriendRequest?>(null) }

    val acceptRequest: (IncomingFriendRequest) -> Unit = viewModel::accept
    val declineRequest: (IncomingFriendRequest) -> Unit = viewModel::decline

    if (chatTarget != null) {
        if (viewingProfile) {
            FriendProfileScreen(
                firestore = firestore,
                friend = chatTarget.friend,
                onBack = { viewingProfile = false },
                onRemoved = { viewingProfile = false; viewModel.closeChat() },
                onRemoveFriend = viewModel::removeFriend,
            )
        } else {
            DirectChatScreen(
                firestore = firestore,
                roomId = chatTarget.roomId,
                currentUid = user.uid,
                friendUid = chatTarget.friend.uid,
                title = chatTarget.friend.username,
                currentUsername = profile?.username.orEmpty(),
                currentAvatarUrl = profile?.avatarUrl.orEmpty(),
                onBack = viewModel::closeChat,
                onViewFriend = { viewingProfile = true },
            )
        }
        return
    }
    val page = when {
        viewingRequest != null -> FriendsPage.RequestDetail
        addFriendMode -> FriendsPage.Finder
        state.showRequests -> FriendsPage.Requests
        else -> FriendsPage.Main
    }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState == FriendsPage.RequestDetail || targetState == FriendsPage.Finder) {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            } else if (targetState != FriendsPage.Main) {
                (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
            }
        },
        label = "friends_pages",
    ) { visiblePage ->
        when (visiblePage) {
            FriendsPage.Finder -> FriendFinder(
                user = user,
                firestore = firestore,
                currentUsername = profile?.username.orEmpty(),
                currentAvatarUrl = profile?.avatarUrl.orEmpty(),
                knownFriendIds = state.friends.map { it.uid }.toSet(),
                outgoingRequestIds = state.outgoingRequests
                    .filter { it.status == FriendRequestStatus.Pending }
                    .map { it.toUid }
                    .toSet(),
                onClose = { addFriendMode = false },
            )
            FriendsPage.Requests -> FriendRequestsScreen(
                    requests = state.requests,
                    outgoingRequests = state.outgoingRequests,
                    onBack = viewModel::toggleRequests,
                    onAccept = acceptRequest,
                    onDecline = declineRequest,
                    onOpenRequest = { viewingRequest = it },
                    onSendMessage = { request ->
                        viewModel.openChat(FriendSummary(request.toUid, request.toUsername))
                    },
                )
            FriendsPage.RequestDetail -> viewingRequest?.let { request ->
                FriendRequestProfileScreen(
                    firestore = firestore,
                    request = request,
                    onBack = { viewingRequest = null },
                )
            }
            FriendsPage.Main -> FriendsContent(
                    state = state,
                    profile = profile,
                    requestCount = if (profile?.settings?.notifications == false) {
                        0
                    } else {
                        state.requests.size
                    },
                    onAddFriend = { addFriendMode = true },
                    onOpenOwnProfile = onOpenOwnProfile,
                    onToggleRequests = viewModel::toggleRequests,
                    onOpenChat = viewModel::openChat,
                )
        }
    }
}

@Composable
private fun FriendsContent(
    state: FriendsUiState,
    profile: UserProfile?,
    requestCount: Int,
    onAddFriend: () -> Unit,
    onOpenOwnProfile: () -> Unit,
    onToggleRequests: () -> Unit,
    onOpenChat: (FriendSummary) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(onClick = onOpenOwnProfile)) {
                ProfileAvatar(profile?.avatarUrl.orEmpty(), profile?.username.orEmpty().ifBlank { profile?.email.orEmpty() }, 48.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile?.username.orEmpty().ifBlank { "Explorer" }, fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.titleMedium)
                Text("Ready for the next adventure", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onAddFriend) { androidx.compose.material3.Icon(Icons.Rounded.Add, "Add friend", tint = Brand) }
            Box {
                IconButton(onClick = onToggleRequests) { androidx.compose.material3.Icon(Icons.Rounded.Notifications, "Notifications", tint = Ink) }
                if (requestCount > 0) Badge(Modifier.align(Alignment.TopEnd), containerColor = RelicRed) { Text(requestCount.toString()) }
            }
        }
        if (state.friends.isEmpty()) NoFriendsCard(onAddFriend)
        else state.friends.forEach { friend ->
            val hasUnreadMessages = friend.unreadCount > 0
            Card(
                Modifier.fillMaxWidth().clickable { onOpenChat(friend) },
                colors = CardDefaults.cardColors(containerColor = if (hasUnreadMessages) BrandSoft else Color.White),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar("", friend.username, 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(friend.username, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
                        if (hasUnreadMessages) {
                            Badge(containerColor = RelicRed, contentColor = Color.White) {
                                Text(if (friend.unreadCount > 99) "99+" else friend.unreadCount.toString())
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoFriendsCard(onAddFriend: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(72.dp).background(BrandSoft, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.nav_friends_symbol), null, modifier = Modifier.size(64.dp))
            }
            Text("No friends yet", fontWeight = FontWeight.Bold, color = Ink)
            Text("Find an explorer and start hunting together.", color = com.comp90018.app.Muted)
            Button(onClick = onAddFriend, colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                androidx.compose.material3.Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add friend")
            }
        }
    }
}

@Composable
private fun FriendRequestsScreen(
    requests: List<IncomingFriendRequest>,
    outgoingRequests: List<OutgoingFriendRequest>,
    onBack: () -> Unit,
    onAccept: (IncomingFriendRequest) -> Unit,
    onDecline: (IncomingFriendRequest) -> Unit,
    onOpenRequest: (IncomingFriendRequest) -> Unit,
    onSendMessage: (OutgoingFriendRequest) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        }
        if (requests.isEmpty() && outgoingRequests.isEmpty()) {
            EmptyState(Icons.Rounded.Group, "No notifications", "Friend activity will appear here.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(requests, key = { "request_${it.fromUid}" }) { request ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenRequest(request) }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar("", request.fromUsername, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${request.fromUsername} sent you a friend request", fontWeight = FontWeight.SemiBold, color = Ink)
                                Text(
                                    request.message.ifBlank { "Friend request" },
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            TextButton(onClick = { onDecline(request) }) { Text("Decline", color = RelicRed) }
                            TextButton(onClick = { onAccept(request) }) { Text("Accept", color = Brand) }
                        }
                    }
                }
                items(outgoingRequests, key = { "outgoing_${it.toUid}" }) { request ->
                    OutgoingFriendRequestCard(request, onSendMessage)
                }
            }
        }
    }
}

@Composable
private fun OutgoingFriendRequestCard(
    request: OutgoingFriendRequest,
    onSendMessage: (OutgoingFriendRequest) -> Unit,
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar("", request.toUsername, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    when (request.status) {
                        FriendRequestStatus.Accepted -> "${request.toUsername} accepted your friend request"
                        FriendRequestStatus.Declined -> "${request.toUsername} declined your friend request"
                        FriendRequestStatus.Pending -> "Request sent to ${request.toUsername}"
                    },
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    when (request.status) {
                        FriendRequestStatus.Accepted -> "You are now friends."
                        FriendRequestStatus.Declined -> "This request was not accepted."
                        FriendRequestStatus.Pending -> "Awaiting approval"
                    },
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (request.message.isNotBlank()) {
                    Text("“${request.message}”", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (request.status == FriendRequestStatus.Accepted) {
                TextButton(onClick = { onSendMessage(request) }) { Text("Send message", color = Brand) }
            }
        }
    }
}

@Composable
private fun FriendRequestProfileScreen(
    firestore: FirebaseFirestore,
    request: IncomingFriendRequest,
    onBack: () -> Unit,
) {
    val repository = remember(firestore) { FirebaseProfileRepository(firestore) }
    var loadedProfile by remember(request.fromUid) { mutableStateOf<UserProfile?>(null) }
    DisposableEffect(repository, request.fromUid) {
        val subscription = repository.observeProfile(request.fromUid) { profile, _ -> loadedProfile = profile }
        onDispose { subscription.cancel() }
    }
    val username = loadedProfile?.username.orEmpty().ifBlank { request.fromUsername }
    val avatarUrl = loadedProfile?.avatarUrl.orEmpty()

    Column(Modifier.fillMaxSize().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
        }
        ProfileAvatar(avatarUrl, username, 96.dp)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$username sent you a friend request", color = Ink, fontWeight = FontWeight.Bold)
                Text(request.message.ifBlank { "Friend request" }, color = Muted)
            }
        }
    }
}
