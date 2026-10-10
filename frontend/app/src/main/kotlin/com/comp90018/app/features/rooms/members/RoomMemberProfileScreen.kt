package com.comp90018.app.features.rooms.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.data.rooms.TeamRoomMember
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.data.social.FriendshipStatus
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.rooms.RoomMemberProfileViewModel
import com.comp90018.app.ui.components.ProfileAvatar
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Shows teammate details and available friendship/direct-chat actions. */
@Composable
internal fun RoomMemberProfileScreen(
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
