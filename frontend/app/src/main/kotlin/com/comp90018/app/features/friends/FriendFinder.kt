package com.comp90018.app.features.friends

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
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
import com.comp90018.app.*
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.ui.components.AppTextField
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun FriendFinder(
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    currentUsername: String,
    currentAvatarUrl: String,
    knownFriendIds: Set<String>,
    outgoingRequestIds: Set<String>,
    onClose: () -> Unit,
) {
    val repository = remember(firestore) { FirebaseSocialRepository(firestore) }
    val viewModel: FriendFinderViewModel = viewModel(
        key = "finder_${user.uid}",
        factory = FriendFinderViewModel.factory(repository, user.uid, currentUsername),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(currentUsername) { viewModel.updateCurrentUsername(currentUsername) }
    LaunchedEffect(state.query) {
        if (state.query.trim().isNotEmpty()) {
            delay(300)
            viewModel.search()
        }
    }

    val roomId = state.roomId
    if (roomId != null) {
        val chatTarget = requireNotNull(state.target)
        DirectChatScreen(firestore, roomId, user.uid, chatTarget.uid, chatTarget.username, currentUsername, currentAvatarUrl, onBack = viewModel::closeChat)
        return
    }
    val normalizedQuery = state.query.trim().lowercase()
    val candidates = state.results
        .filter { it.username.contains(normalizedQuery) || it.displayName.lowercase().contains(normalizedQuery) }
        .filter { it.uid != user.uid }

    val page = when {
        state.target == null -> FinderPage.Search
        state.requestMode -> FinderPage.Request
        else -> FinderPage.Profile
    }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (targetState == FinderPage.Search) {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            } else {
                (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            }
        },
        label = "friend_finder_pages",
    ) { visiblePage ->
        when (visiblePage) {
            FinderPage.Search -> Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                AppTextField("Search by username", state.query, viewModel::updateQuery, leadingIcon = Icons.Rounded.Search)
                Text("Suggestions appear as you type", color = Muted, style = MaterialTheme.typography.bodySmall)
                state.message?.let { Text(it, color = if (it == "Friend request sent") Brand else MaterialTheme.colorScheme.error) }
                when {
                    state.searching && candidates.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = Brand)
                    normalizedQuery.isBlank() -> Spacer(Modifier.height(1.dp))
                    candidates.isEmpty() -> Text("No matching explorers yet.", color = Muted)
                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(candidates, key = { it.uid }) { candidate ->
                            CandidateRow(
                                candidate = candidate,
                                isFriend = candidate.uid in knownFriendIds,
                                requestSent = candidate.uid in outgoingRequestIds || candidate.uid in state.sentUserIds,
                                onViewProfile = { viewModel.selectUser(candidate, knownFriend = candidate.uid in knownFriendIds) },
                                onAdd = { viewModel.selectUser(candidate, knownFriend = false, requestMode = true) },
                            )
                        }
                    }
                }
            }
            FinderPage.Profile -> state.target?.let { FriendCandidateProfile(it, state, viewModel) }
            FinderPage.Request -> state.target?.let {
                FriendRequestComposer(it, state, viewModel)
            }
        }
    }
}

private enum class FinderPage { Search, Profile, Request }

@Composable
private fun CandidateRow(candidate: SearchUser, isFriend: Boolean, requestSent: Boolean, onViewProfile: () -> Unit, onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onViewProfile), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(candidate.avatarUrl, candidate.username, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(candidate.username, color = Ink, fontWeight = FontWeight.Bold)
                candidate.displayName.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (isFriend) {
                Text("Added", color = Brand, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            } else if (requestSent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = Brand, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sent", color = Brand, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Rounded.Add, "Request friendship", tint = Brand)
                }
            }
        }
    }
}

@Composable
private fun FriendCandidateProfile(target: SearchUser, state: FriendFinderUiState, viewModel: FriendFinderViewModel) {
    Column(
        Modifier.fillMaxSize().padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconButton(onClick = viewModel::searchAgain, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
        }
        ProfileAvatar(target.avatarUrl, target.username, 96.dp)
        Text(target.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Text(
                target.bio.ifBlank { "This explorer has not added an introduction yet." },
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                color = Ink,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        state.message?.let { Text(it, color = if (it.startsWith("Friend request")) Brand else MaterialTheme.colorScheme.error) }
        when (state.friendship) {
            FriendshipStatus.Friends -> FriendAction("Chat", state.working, viewModel::openChat)
            FriendshipStatus.IncomingPending -> FriendAction("Accept request", state.working, viewModel::acceptFriendRequest)
            FriendshipStatus.OutgoingPending -> Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) { Text("Friend request sent") }
            FriendshipStatus.None -> FriendAction("Add friend", state.working, viewModel::startRequest)
            null -> CircularProgressIndicator(color = Brand)
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CandidateDetail("Gender", genderLabel(target.gender))
                target.department.takeIf { it.isNotBlank() }?.let { CandidateDetail("Department", it) }
                target.major.takeIf { it.isNotBlank() }?.let { CandidateDetail("Major", it) }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FriendRequestComposer(
    target: SearchUser,
    state: FriendFinderUiState,
    viewModel: FriendFinderViewModel,
) {
    var requestMessage by remember(target.uid) { mutableStateOf("Hi, would you like to team up?") }
    Column(
        Modifier.fillMaxSize().padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = viewModel::searchAgain) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
        }
        Text(
            "Send a friend request to ${target.username}",
            color = Ink,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
        )
        AppTextField("Add a message", requestMessage, { requestMessage = it.take(120) })
        Text("${requestMessage.length}/120", color = Muted, style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = {
                viewModel.sendFriendRequest(requestMessage.trim(), target)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = requestMessage.isNotBlank() && !state.working && state.friendship != FriendshipStatus.OutgoingPending,
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
        ) {
            Text(
                when {
                    state.working -> "Sending…"
                    state.friendship == FriendshipStatus.OutgoingPending -> "Request sent"
                    else -> "Send request"
                },
            )
        }
        state.message?.let { Text(it, color = if (it == "Friend request sent") Brand else MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun CandidateDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = Ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FriendAction(label: String, working: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = !working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
        Text(if (working) "Processing…" else label)
    }
}
