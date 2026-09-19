package com.comp90018.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.data.rooms.FirebaseTeamRoomRepository
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.features.friends.FriendsScreen
import com.comp90018.app.features.friends.UnreadMessagesViewModel
import com.comp90018.app.features.map.MapScreen
import com.comp90018.app.features.profile.ProfileScreen
import com.comp90018.app.features.rooms.RoomsScreen
import com.comp90018.app.features.rooms.RoomsViewModel
import com.comp90018.app.features.rooms.UnreadRoomMessagesViewModel
import com.comp90018.app.features.treasure.TreasureScreen
import com.comp90018.app.navigation.AppBottomNavigation
import com.comp90018.app.navigation.AppDestination
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Owns signed-in navigation; profile state belongs to [AppShellViewModel]. */
@Composable
fun AppShell(user: FirebaseUser, firestore: FirebaseFirestore, onLogout: () -> Unit) {
    var destination by remember { mutableStateOf(AppDestination.Friends) }
    val repository = remember(firestore) { FirebaseProfileRepository(firestore) }
    val viewModel: AppShellViewModel = viewModel(
        key = "app_shell_${user.uid}",
        factory = AppShellViewModel.factory(repository, user.uid, user.email.orEmpty()),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val socialRepository = remember(firestore) { FirebaseSocialRepository(firestore) }
    val unreadMessagesViewModel: UnreadMessagesViewModel = viewModel(
        key = "unread_messages_${user.uid}",
        factory = UnreadMessagesViewModel.factory(socialRepository, user.uid),
    )
    val unreadFriendMessages by unreadMessagesViewModel.unreadCount.collectAsStateWithLifecycle()
    val teamRoomRepository = remember(firestore) { FirebaseTeamRoomRepository(firestore) }
    val unreadRoomMessagesViewModel: UnreadRoomMessagesViewModel = viewModel(
        key = "unread_room_messages_${user.uid}",
        factory = UnreadRoomMessagesViewModel.factory(teamRoomRepository, user.uid),
    )
    val unreadRoomMessages by unreadRoomMessagesViewModel.unreadCount.collectAsStateWithLifecycle()
    val roomsViewModel: RoomsViewModel = viewModel(
        key = "rooms_${user.uid}",
        factory = RoomsViewModel.factory(teamRoomRepository, user.uid),
    )
    val roomsState by roomsViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = Background, bottomBar = {
        AppBottomNavigation(destination, unreadFriendMessages, unreadRoomMessages) { destination = it }
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (destination) {
                AppDestination.Treasure -> TreasureScreen(roomsState.activeRoomId, teamRoomRepository)
                AppDestination.Rooms -> RoomsScreen(user, firestore, state.profile, roomsViewModel)
                AppDestination.Map -> MapScreen()
                AppDestination.Friends -> FriendsScreen(user, firestore, state.profile, onOpenOwnProfile = { destination = AppDestination.Profile })
                AppDestination.Profile -> ProfileScreen(
                    userUid = user.uid,
                    userEmail = user.email.orEmpty(),
                    repository = repository,
                    profile = state.profile,
                    profileError = state.profileError,
                    currentRoomId = roomsState.activeRoomId,
                    onRetry = viewModel::retry,
                    onLogout = onLogout,
                )
            }
        }
    }
}
