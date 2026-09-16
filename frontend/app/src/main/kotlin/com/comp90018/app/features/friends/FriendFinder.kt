package com.comp90018.app.features.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.ui.components.AppTextField
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FriendFinder(user: FirebaseUser, firestore: FirebaseFirestore, currentUsername: String, currentAvatarUrl: String, onClose: () -> Unit) {
    val repository = remember(firestore) { FirebaseSocialRepository(firestore) }
    val viewModel: FriendFinderViewModel = viewModel(key = "finder_" + user.uid, factory = FriendFinderViewModel.factory(repository, user.uid, currentUsername))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(currentUsername) { viewModel.updateCurrentUsername(currentUsername) }
    val target = state.target
    var viewingProfile by remember(target?.uid) { mutableStateOf(false) }
    var showInvite by remember { mutableStateOf(false) }
    var invitation by remember { mutableStateOf("Hi! Want to explore campus together?") }
    var blocked by remember(target?.uid) { mutableStateOf(false) }

    state.roomId?.let { roomId ->
        DirectChatScreen(firestore, roomId, user.uid, target?.username ?: "Chat", currentUsername, currentAvatarUrl, onBack = viewModel::closeChat)
        return
    }

    if (target == null || blocked) {
        Column(Modifier.fillMaxSize().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null); Text("Back to friends") }
            Text("Find friends", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            Text("Search by username. Try “liang” for the demo account.", color = Muted)
            AppTextField("Username", state.query, viewModel::updateQuery, leadingIcon = Icons.Rounded.Search)
            Button(onClick = viewModel::search, modifier = Modifier.fillMaxWidth(), enabled = !state.searching, colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                Text(if (state.searching) "Searching…" else "Search")
            }
            if (blocked) Text("This account is blocked for this demo session.", color = MaterialTheme.colorScheme.error)
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        return
    }

    if (viewingProfile) {
        FinderProfile(target, state.friendship, state.working, state.message, onBack = { viewingProfile = false }, onAdd = { showInvite = true }, onAccept = viewModel::acceptFriendRequest, onChat = viewModel::openChat, onBlock = { blocked = true; viewingProfile = false })
    } else {
        Column(Modifier.fillMaxSize().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = viewModel::searchAgain) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null); Text("Search again") }
            Text("Search result", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    DemoAvatar(target, Modifier.clickable { viewingProfile = true })
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f).clickable { viewingProfile = true }) {
                        Text(target.displayName.ifBlank { target.username }, fontWeight = FontWeight.Bold, color = Ink)
                        Text("@" + target.username, color = Muted)
                        if (target.uid == "demo-liang") Text("Level 12 · Room 2048", color = Brand, style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { showInvite = true }, enabled = state.friendship == FriendshipStatus.None) { Icon(Icons.Rounded.Add, "Add friend", tint = Brand) }
                }
            }
            Text("Tap the avatar to view this explorer’s profile.", color = Muted, style = MaterialTheme.typography.bodySmall)
            state.message?.let { Text(it, color = if (it.startsWith("Friend request")) Brand else MaterialTheme.colorScheme.error) }
        }
    }

    if (showInvite) InviteDialog(target.username, invitation, { invitation = it.take(120) }, { showInvite = false }, { showInvite = false; viewModel.sendFriendRequest() })
}

@Composable
private fun FinderProfile(target: SearchUser, friendship: FriendshipStatus?, working: Boolean, message: String?, onBack: () -> Unit, onAdd: () -> Unit, onAccept: () -> Unit, onChat: () -> Unit, onBlock: () -> Unit) {
    val isLiang = target.uid == "demo-liang"
    Column(Modifier.fillMaxSize().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null); Text("Back") }
        DemoAvatar(target, Modifier.size(104.dp))
        Text(target.displayName.ifBlank { target.username }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Text("@" + target.username, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { InfoPill(if (isLiang) "Level 12" else "Level 5"); InfoPill(if (isLiang) "Room 2048" else "No room") }
        target.bio.takeIf(String::isNotBlank)?.let { Text(it, color = Ink, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 18.dp)) }
        if (isLiang) Text("Faculty of Engineering and IT", color = Muted)
        Spacer(Modifier.weight(1f))
        message?.let { Text(it, color = if (it.startsWith("Friend request")) Brand else MaterialTheme.colorScheme.error) }
        when (friendship) {
            FriendshipStatus.Friends -> FriendAction("Message", working, onChat)
            FriendshipStatus.IncomingPending -> FriendAction("Accept friend", working, onAccept)
            FriendshipStatus.OutgoingPending -> Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) { Text("Friend request sent") }
            FriendshipStatus.None -> FriendAction("Add friend", working, onAdd)
            null -> CircularProgressIndicator(color = Brand)
        }
        OutlinedButton(onClick = onBlock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Rounded.Block, null); Spacer(Modifier.width(8.dp)); Text("Block")
        }
    }
}

@Composable
private fun DemoAvatar(target: SearchUser, modifier: Modifier = Modifier) {
    Box(modifier.size(64.dp).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) {
        Text(target.username.take(1).uppercase(), color = Brand, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoPill(text: String) {
    Box(Modifier.background(BrandSoft, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) { Text(text, color = Brand, fontWeight = FontWeight.Bold) }
}

@Composable
private fun InviteDialog(username: String, text: String, onTextChange: (String) -> Unit, onDismiss: () -> Unit, onSend: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Invite @" + username, fontWeight = FontWeight.Bold) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Add a message to your friend request.", color = Muted)
            AppTextField("Invitation message", text, onTextChange, singleLine = false, maxLines = 4)
            Text(text.length.toString() + "/120", color = Muted, modifier = Modifier.align(Alignment.End))
        }
    }, confirmButton = { Button(onClick = onSend, enabled = text.isNotBlank()) { Text("Send request") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun FriendAction(label: String, working: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = !working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (working) "Processing…" else label) }
}
