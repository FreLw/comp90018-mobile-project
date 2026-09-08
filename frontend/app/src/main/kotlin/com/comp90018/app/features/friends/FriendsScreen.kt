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
import androidx.compose.runtime.DisposableEffect
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
import com.comp90018.app.Brand
import com.comp90018.app.FirebaseSocialService
import com.comp90018.app.FriendSummary
import com.comp90018.app.IncomingFriendRequest
import com.comp90018.app.Ink
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.ui.components.EmptyState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FriendsScreen(user: FirebaseUser, firestore: FirebaseFirestore, profile: UserProfile?) {
    var addFriendMode by remember { mutableStateOf(false) }
    var showRequests by remember { mutableStateOf(false) }
    var requests by remember { mutableStateOf<List<IncomingFriendRequest>>(emptyList()) }
    var friends by remember { mutableStateOf<List<FriendSummary>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var roomId by remember { mutableStateOf<String?>(null) }
    var roomTitle by remember { mutableStateOf("") }
    var activeFriend by remember { mutableStateOf<FriendSummary?>(null) }
    var viewingProfile by remember { mutableStateOf(false) }

    LaunchedEffect(user.uid, profile?.username) { FirebaseSocialService.migrateAcceptedFriendships(firestore, user.uid, profile?.username.orEmpty()) }

    if (roomId != null) {
        if (viewingProfile && activeFriend != null) {
            FriendProfileScreen(firestore, requireNotNull(activeFriend), user.uid, onBack = { viewingProfile = false }, onRemoved = { viewingProfile = false; roomId = null; activeFriend = null })
        } else {
            DirectChatScreen(firestore, requireNotNull(roomId), user.uid, roomTitle, profile?.username.orEmpty(), profile?.avatarUrl.orEmpty(), onBack = { roomId = null }, onViewFriend = { viewingProfile = true })
        }
        return
    }
    if (addFriendMode) {
        FriendFinder(user, firestore, profile?.username.orEmpty(), profile?.avatarUrl.orEmpty(), onClose = { addFriendMode = false })
        return
    }

    DisposableEffect(user.uid, firestore) {
        val requestListener = FirebaseSocialService.observeIncomingFriendRequests(firestore, user.uid) { data, error -> requests = data; message = error }
        val friendListener = FirebaseSocialService.observeFriends(firestore, user.uid) { data, error -> friends = data; if (error != null) message = error }
        onDispose { requestListener.remove(); friendListener.remove() }
    }
    Column(Modifier.fillMaxSize().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(onClick = { addFriendMode = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand), shape = RoundedCornerShape(16.dp)) { androidx.compose.material3.Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("Add friend") }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Friends", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            TextButton(onClick = { showRequests = !showRequests }) { Text(if (requests.isEmpty()) "Friend requests" else "Friend requests (${requests.size})") }
        }
        if (showRequests) RequestsList(requests, firestore, user.uid, profile?.username.orEmpty(), onError = { message = it })
        else if (friends.isEmpty()) EmptyState(Icons.Rounded.Group, "No friends yet", "Tap Add friend to search by username.")
        else friends.forEach { friend ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    FriendAvatar(firestore, friend); Spacer(Modifier.width(12.dp)); Text(friend.username, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
                    TextButton(onClick = { FirebaseSocialService.openDirectRoom(firestore, user.uid, friend.uid, friend.username) { id, error -> message = error; if (id != null) { roomTitle = friend.username; roomId = id; activeFriend = friend } } }) { Text("Chat") }
                }
            }
        }
    }
}

@Composable
private fun RequestsList(requests: List<IncomingFriendRequest>, firestore: FirebaseFirestore, uid: String, username: String, onError: (String?) -> Unit) {
    if (requests.isEmpty()) EmptyState(Icons.Rounded.Group, "No friend requests", "New requests will appear here.")
    else requests.forEach { request ->
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(request.fromUsername, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
                TextButton(onClick = { FirebaseSocialService.declineFriendRequest(firestore, request.fromUid, uid, onError) }) { Text("Decline") }
                Button(onClick = { FirebaseSocialService.acceptFriendRequest(firestore, request.fromUid, uid, request.fromUsername, username, onError) }) { Text("Accept") }
            }
        }
    }
}
