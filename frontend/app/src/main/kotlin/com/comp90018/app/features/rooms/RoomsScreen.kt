package com.comp90018.app.features.rooms

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.comp90018.app.features.map.MapRelic
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.rooms.chat.TeamRoomChatScreen
import com.comp90018.app.features.rooms.demo.DEMO_ROOM_ID
import com.comp90018.app.features.rooms.demo.MockTeamRoomScreen
import com.comp90018.app.features.rooms.entry.RoomEntryScreen
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Chooses room entry, browsing, preview, or active chat based on membership and local navigation. */
@Composable
fun RoomsScreen(
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    profile: UserProfile?,
    viewModel: RoomsViewModel,
    treasures: List<MapRelic>,
    treasuresLoading: Boolean,
    treasuresError: String?,
    onStartHunt: (String) -> Unit,
    onClaimTreasure: (TeamRoomChatViewModel) -> Unit = { it.claimCompletedHuntTreasure() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var mockActiveRoomId by remember { mutableStateOf<String?>(null) }
    when {
        mockActiveRoomId != null -> MockTeamRoomScreen(
            roomId = requireNotNull(mockActiveRoomId),
            profile = profile,
            onDismiss = { mockActiveRoomId = null },
        )
        state.activeRoomId != null -> TeamRoomChatScreen(
            requireNotNull(state.activeRoomId),
            user,
            firestore,
            profile,
            onRoomExited = viewModel::clearErrors,
            treasures = treasures,
            treasuresLoading = treasuresLoading,
            treasuresError = treasuresError,
            onStartHunt = onStartHunt,
            onClaimTreasure = onClaimTreasure,
        )
        else -> RoomEntryScreen(
            state = state,
            viewModel = viewModel,
            onJoin = {
                if (state.roomIdInput.trim().equals(DEMO_ROOM_ID, ignoreCase = true)) {
                    mockActiveRoomId = DEMO_ROOM_ID
                } else {
                    viewModel.joinRoom()
                }
            },
        )
    }
}
