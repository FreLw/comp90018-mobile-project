package com.comp90018.app.features.friends

/*
 * Renders incoming/outgoing request history and incoming-request profile previews.
 * Request-card layout lives here; accept/decline actions are supplied by the parent feature.
 */

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.data.social.FriendRequestStatus
import com.comp90018.app.data.social.IncomingFriendRequest
import com.comp90018.app.data.social.OutgoingFriendRequest
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.ui.components.EmptyState
import com.google.firebase.firestore.FirebaseFirestore

/** Incoming and outgoing request history shown from the Friends notification icon. */
@Composable
internal fun FriendRequestsContent(
    requests: List<IncomingFriendRequest>,
    outgoingRequests: List<OutgoingFriendRequest>,
    onBack: () -> Unit,
    onAccept: (IncomingFriendRequest) -> Unit,
    onDecline: (IncomingFriendRequest) -> Unit,
    onOpenRequest: (IncomingFriendRequest) -> Unit,
    onSendMessage: (OutgoingFriendRequest) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        if (requests.isEmpty() && outgoingRequests.isEmpty()) {
            EmptyState(Icons.Rounded.Group, "No notifications", "Friend activity will appear here.")
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(requests, key = { "request_${it.fromUid}" }) { request ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onOpenRequest(request) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar("", request.fromUsername, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "${request.fromUsername} sent you a friend request",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink,
                                )
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

/** Shows the recipient and current status of a previously sent friend request. */
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

/** Read-only profile preview opened from an incoming friend request. */
@Composable
internal fun FriendRequestProfileContent(
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

    Column(
        Modifier.fillMaxSize().padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
        }
        ProfileAvatar(avatarUrl, username, 96.dp)
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("$username sent you a friend request", color = Ink, fontWeight = FontWeight.Bold)
                Text(request.message.ifBlank { "Friend request" }, color = Muted)
            }
        }
    }
}
