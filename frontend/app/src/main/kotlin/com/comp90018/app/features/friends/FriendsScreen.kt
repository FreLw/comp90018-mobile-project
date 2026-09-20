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
import androidx.compose.material.icons.automirrored.rounded.Send
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
import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.R
import com.comp90018.app.SearchUser
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.ui.components.ChatComposer
import com.comp90018.app.ui.components.EmptyState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

private enum class FriendsPage { Main, Requests, RequestDetail, Finder }

private enum class FriendNoticeStatus { Accepted, Declined, Pending }

private data class FriendNotice(
    val user: SearchUser,
    val status: FriendNoticeStatus,
    val requestMessage: String,
)

private val initialFriendNotices = listOf(
    FriendNotice(mockFriendDirectory.first { it.uid == "mock_ava" }, FriendNoticeStatus.Accepted, "Hi, would you like to team up?"),
    FriendNotice(mockFriendDirectory.first { it.uid == "mock_noah" }, FriendNoticeStatus.Declined, "Let's explore the campus together."),
    FriendNotice(mockFriendDirectory.first { it.uid == "mock_emma" }, FriendNoticeStatus.Pending, "Want to hunt for the Old Quad treasure?"),
)

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
    var simulatedRequests by remember { mutableStateOf(mockIncomingRequests) }
    var simulatedFriends by remember {
        mutableStateOf(listOf(FriendSummary("mock_ava", "ava")))
    }
    var friendNotices by remember { mutableStateOf(initialFriendNotices) }
    var mockChatFriend by remember { mutableStateOf<FriendSummary?>(null) }
    var viewingRequest by remember { mutableStateOf<IncomingFriendRequest?>(null) }

    val acceptRequest: (IncomingFriendRequest) -> Unit = { request ->
        if (request.fromUid.startsWith("mock_")) {
            val candidate = mockFriendDirectory.firstOrNull { it.uid == request.fromUid }
            if (candidate != null && simulatedFriends.none { it.uid == candidate.uid }) {
                simulatedFriends = simulatedFriends + FriendSummary(candidate.uid, candidate.username)
            }
            simulatedRequests = simulatedRequests - request
        } else viewModel.accept(request)
    }
    val declineRequest: (IncomingFriendRequest) -> Unit = { request ->
        if (request.fromUid.startsWith("mock_")) simulatedRequests = simulatedRequests - request
        else viewModel.decline(request)
    }

    mockChatFriend?.let { friend ->
        MockDirectChatScreen(friend, onBack = { mockChatFriend = null })
        return
    }

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
    val mergedState = state.copy(friends = (state.friends + simulatedFriends).distinctBy { it.uid })
    val mergedRequests = state.requests + simulatedRequests
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
                knownFriendIds = mergedState.friends.map { it.uid }.toSet(),
                onRequestSent = { candidate, message ->
                    friendNotices = friendNotices
                        .filterNot { it.user.uid == candidate.uid } +
                        FriendNotice(candidate, FriendNoticeStatus.Pending, message)
                },
                onClose = { addFriendMode = false },
            )
            FriendsPage.Requests -> FriendRequestsScreen(
                    requests = mergedRequests,
                    notices = friendNotices,
                    onBack = viewModel::toggleRequests,
                    onAccept = acceptRequest,
                    onDecline = declineRequest,
                    onOpenRequest = { viewingRequest = it },
                    onSendMessage = { notice ->
                        mockChatFriend = FriendSummary(notice.user.uid, notice.user.username)
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
                    state = mergedState,
                    profile = profile,
                    requestCount = mergedRequests.size + friendNotices.size,
                    onAddFriend = { addFriendMode = true },
                    onOpenOwnProfile = onOpenOwnProfile,
                    onToggleRequests = viewModel::toggleRequests,
                    onOpenChat = { friend ->
                        if (friend.uid.startsWith("mock_")) mockChatFriend = friend else viewModel.openChat(friend)
                    },
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
    notices: List<FriendNotice>,
    onBack: () -> Unit,
    onAccept: (IncomingFriendRequest) -> Unit,
    onDecline: (IncomingFriendRequest) -> Unit,
    onOpenRequest: (IncomingFriendRequest) -> Unit,
    onSendMessage: (FriendNotice) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        }
        if (requests.isEmpty() && notices.isEmpty()) {
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
                                Text("Hi, would you like to team up?", color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onDecline(request) }) { Text("Decline", color = RelicRed) }
                            TextButton(onClick = { onAccept(request) }) { Text("Accept", color = Brand) }
                        }
                    }
                }
                items(notices, key = { "notice_${it.user.uid}" }) { notice ->
                    FriendNoticeCard(notice, onSendMessage)
                }
            }
        }
    }
}

@Composable
private fun FriendNoticeCard(notice: FriendNotice, onSendMessage: (FriendNotice) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(notice.user.avatarUrl, notice.user.username, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    when (notice.status) {
                        FriendNoticeStatus.Accepted -> "${notice.user.username} accepted your friend request"
                        FriendNoticeStatus.Declined -> "${notice.user.username} declined your friend request"
                        FriendNoticeStatus.Pending -> "Request sent to ${notice.user.username}"
                    },
                    color = Ink,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    when (notice.status) {
                        FriendNoticeStatus.Accepted -> "You are now friends."
                        FriendNoticeStatus.Declined -> "This request was not accepted."
                        FriendNoticeStatus.Pending -> "Awaiting approval"
                    },
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (notice.status == FriendNoticeStatus.Pending && notice.requestMessage.isNotBlank()) {
                    Text("“${notice.requestMessage}”", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (notice.status == FriendNoticeStatus.Accepted) {
                TextButton(onClick = { onSendMessage(notice) }) { Text("Send message", color = Brand) }
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
    val mockProfile = remember(request.fromUid) { mockFriendDirectory.firstOrNull { it.uid == request.fromUid } }
    DisposableEffect(repository, request.fromUid) {
        if (request.fromUid.startsWith("mock_")) return@DisposableEffect onDispose { }
        val subscription = repository.observeProfile(request.fromUid) { profile, _ -> loadedProfile = profile }
        onDispose { subscription.cancel() }
    }
    val username = loadedProfile?.username.orEmpty().ifBlank { mockProfile?.username ?: request.fromUsername }
    val avatarUrl = loadedProfile?.avatarUrl.orEmpty().ifBlank { mockProfile?.avatarUrl.orEmpty() }

    Column(Modifier.fillMaxSize().padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
        }
        ProfileAvatar(avatarUrl, username, 96.dp)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$username sent you a friend request", color = Ink, fontWeight = FontWeight.Bold)
                Text("Hi, would you like to team up?", color = Muted)
            }
        }
    }
}

@Composable
private fun MockDirectChatScreen(friend: FriendSummary, onBack: () -> Unit) {
    var input by remember(friend.uid) { mutableStateOf("") }
    var messages by remember(friend.uid) { mutableStateOf(listOf("${friend.username}: Hi! Ready for a treasure hunt?")) }
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            ProfileAvatar("", friend.username, 42.dp)
            Spacer(Modifier.width(10.dp))
            Text(friend.username, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
        Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                messages.forEach { message -> Text(message, color = Ink) }
            }
        }
        ChatComposer(input, { input = it })
        Button(
            onClick = {
                val text = input.trim()
                if (text.isNotBlank()) {
                    messages = messages + "You: $text"
                    input = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = input.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
        ) {
            androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.Send, null)
            Spacer(Modifier.width(8.dp))
            Text("Send")
        }
    }
}
