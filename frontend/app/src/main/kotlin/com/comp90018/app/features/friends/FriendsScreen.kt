package com.comp90018.app.features.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.Brand
import com.comp90018.app.FriendSummary
import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.Ink
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.ui.components.EmptyState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Renders friend state and delegates social actions to [FriendsViewModel]. */
@Composable
fun FriendsScreen(user: FirebaseUser, firestore: FirebaseFirestore, profile: UserProfile?) {
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
                title = chatTarget.friend.username,
                currentUsername = profile?.username.orEmpty(),
                currentAvatarUrl = profile?.avatarUrl.orEmpty(),
                onBack = viewModel::closeChat,
                onViewFriend = { viewingProfile = true },
            )
        }
        return
    }
    if (addFriendMode) {
        FriendFinder(user, firestore, profile?.username.orEmpty(), profile?.avatarUrl.orEmpty(), onClose = { addFriendMode = false })
        return
    }

    FriendsContent(
        state = state,
        onAddFriend = { addFriendMode = true },
        onToggleRequests = viewModel::toggleRequests,
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        onOpenChat = viewModel::openChat,
    )
}

@Composable
private fun FriendsContent(state: FriendsUiState, onAddFriend: () -> Unit, onToggleRequests: () -> Unit, onAccept: (IncomingFriendRequest) -> Unit, onDecline: (IncomingFriendRequest) -> Unit, onOpenChat: (FriendSummary) -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = onAddFriend, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand), shape = RoundedCornerShape(16.dp)) { androidx.compose.material3.Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Add friend") }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Friends", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            TextButton(onClick = onToggleRequests) { Text(if (state.requests.isEmpty()) "Friend requests" else "Friend requests (${state.requests.size})") }
        }
        if (state.showRequests) RequestsList(state.requests, onAccept, onDecline)
        else if (state.friends.isEmpty()) EmptyState(Icons.Rounded.Group, "No friends yet", "Tap Add friend to search by username.")
        else state.friends.forEach { friend ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(friend.username, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
                    TextButton(onClick = { onOpenChat(friend) }) { Text("Chat") }
                }
            }
        }
    }
}

@Composable
private fun RequestsList(requests: List<IncomingFriendRequest>, onAccept: (IncomingFriendRequest) -> Unit, onDecline: (IncomingFriendRequest) -> Unit) {
    if (requests.isEmpty()) EmptyState(Icons.Rounded.Group, "No friend requests", "New requests will appear here.")
    else requests.forEach { request ->
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(request.fromUsername, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
                TextButton(onClick = { onDecline(request) }) { Text("Decline") }
                Button(onClick = { onAccept(request) }) { Text("Accept") }
            }
        }
    }
}
