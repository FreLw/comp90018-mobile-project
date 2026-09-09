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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.FriendshipStatus
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.ui.components.AppTextField
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FriendFinder(user: FirebaseUser, firestore: FirebaseFirestore, currentUsername: String, currentAvatarUrl: String, onClose: () -> Unit) {
    val repository = remember(firestore) { FirebaseSocialRepository(firestore) }
    val viewModel: FriendFinderViewModel = viewModel(
        key = "finder_${user.uid}",
        factory = FriendFinderViewModel.factory(repository, user.uid, currentUsername),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(currentUsername) { viewModel.updateCurrentUsername(currentUsername) }
    val target = state.target

    val roomId = state.roomId
    if (roomId != null) {
        DirectChatScreen(firestore, roomId, user.uid, target?.username ?: "Chat", currentUsername, currentAvatarUrl, onBack = viewModel::closeChat)
        return
    }
    if (target == null) {
        Column(Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            TextButton(onClick = onClose) { Text("Cancel") }
            Text("Find friends", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
            Text("Search by username", color = Muted)
            AppTextField("Username", state.query, viewModel::updateQuery, leadingIcon = Icons.Rounded.Search)
            Button(onClick = viewModel::search, modifier = Modifier.fillMaxWidth(), enabled = !state.searching, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (state.searching) "Searching…" else "Search") }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        return
    }
    Column(Modifier.fillMaxSize().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = viewModel::searchAgain) { Text("Search again") }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(88.dp).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) { Text(target.username.take(1).uppercase(), color = Brand, style = MaterialTheme.typography.displaySmall) }
                Text(target.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                Text(genderLabel(target.gender), color = Muted)
                target.bio.takeIf { it.isNotBlank() }?.let { Text(it, color = Ink, textAlign = TextAlign.Center) }
            }
        }
        Spacer(Modifier.weight(1f))
        state.message?.let { Text(it, color = if (it.startsWith("Friend request")) Brand else MaterialTheme.colorScheme.error) }
        when (state.friendship) {
            FriendshipStatus.Friends -> FriendAction("Chat", state.working, viewModel::openChat)
            FriendshipStatus.IncomingPending -> FriendAction("Accept request", state.working, viewModel::acceptFriendRequest)
            FriendshipStatus.OutgoingPending -> Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) { Text("Friend request sent") }
            FriendshipStatus.None -> FriendAction("Add friend", state.working, viewModel::sendFriendRequest)
            null -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Brand)
        }
    }
}

@Composable
private fun FriendAction(label: String, working: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = !working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (working) "Processing…" else label) }
}
