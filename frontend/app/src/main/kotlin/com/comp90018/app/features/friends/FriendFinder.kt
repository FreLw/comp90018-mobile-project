package com.comp90018.app.features.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.FirebaseSocialService
import com.comp90018.app.FriendshipStatus
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.SearchUser
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.ui.components.AppTextField
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FriendFinder(user: FirebaseUser, firestore: FirebaseFirestore, currentUsername: String, currentAvatarUrl: String, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }; var target by remember { mutableStateOf<SearchUser?>(null) }
    var friendship by remember { mutableStateOf<FriendshipStatus?>(null) }; var message by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }; var roomId by remember { mutableStateOf<String?>(null) }
    if (roomId != null) { DirectChatScreen(firestore, requireNotNull(roomId), user.uid, target?.username ?: "Chat", currentUsername, currentAvatarUrl, onBack = { roomId = null }); return }
    if (target == null) {
        Column(Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(onClick = onClose) { Text("Cancel") }; Text("Find friends", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink); Text("Search by username", color = Muted)
            AppTextField("Username", query, { query = it }, leadingIcon = Icons.Rounded.Search)
            Button(onClick = {
                searching = true; message = null
                FirebaseSocialService.findUserByUsername(firestore, query) { found, error ->
                    searching = false; message = error
                    if (error == null && found != null && found.uid != user.uid) { target = found; FirebaseSocialService.getFriendshipStatus(firestore, user.uid, found.uid) { status, statusError -> friendship = status; message = statusError } }
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !searching, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (searching) "Searching…" else "Search") }
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        return
    }
    val found = requireNotNull(target)
    Column(Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { target = null; friendship = null; message = null }) { Text("Search again") }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(88.dp).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) { Text(found.username.take(1).uppercase(), color = Brand, style = MaterialTheme.typography.displaySmall) }
                Text(found.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink); Text(genderLabel(found.gender), color = Muted); found.bio.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink, textAlign = TextAlign.Center) }
            }
        }
        Spacer(Modifier.weight(1f)); message?.let { Text(it, color = if (it.startsWith("Friend request")) Brand else MaterialTheme.colorScheme.error) }
        when (friendship) {
            FriendshipStatus.Friends -> FriendAction("Chat") { done -> FirebaseSocialService.openDirectRoom(firestore, user.uid, found.uid, found.username) { id, error -> done(error); message = error; roomId = id } }
            FriendshipStatus.IncomingPending -> FriendAction("Accept request") { done -> FirebaseSocialService.acceptFriendRequest(firestore, found.uid, user.uid, found.username, currentUsername) { error -> done(error); message = error ?: "Friend request accepted"; if (error == null) friendship = FriendshipStatus.Friends } }
            FriendshipStatus.OutgoingPending -> Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) { Text("Friend request sent") }
            FriendshipStatus.None -> FriendAction("Add friend") { done -> FirebaseSocialService.sendFriendRequest(firestore, user.uid, found.uid, currentUsername, found.username) { error -> done(error); message = error ?: "Friend request sent"; if (error == null) friendship = FriendshipStatus.OutgoingPending } }
            null -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Brand)
        }
    }
}

@Composable
private fun FriendAction(label: String, action: ((String?) -> Unit) -> Unit) {
    var working by remember { mutableStateOf(false) }
    Button(onClick = { working = true; action { working = false } }, modifier = Modifier.fillMaxWidth(), enabled = !working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (working) "Processing…" else label) }
}
