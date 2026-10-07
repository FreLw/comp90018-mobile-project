package com.comp90018.app.features.friends

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.data.social.FriendRequestStatus
import com.comp90018.app.data.social.FriendSummary
import com.comp90018.app.data.social.IncomingFriendRequest
import com.comp90018.app.features.chat.DirectChatScreen
import com.comp90018.app.features.profile.UserProfile
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
                title = chatTarget.friend.displayLabel,
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
            FriendsPage.Requests -> FriendRequestsContent(
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
                FriendRequestProfileContent(
                    firestore = firestore,
                    request = request,
                    onBack = { viewingRequest = null },
                )
            }
            FriendsPage.Main -> FriendsHomeContent(
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
