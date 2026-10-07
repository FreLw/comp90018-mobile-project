package com.comp90018.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.SideEffect
import com.comp90018.app.features.haptics.TreasureHapticSessionBinding
import com.comp90018.app.features.haptics.TreasureHapticSessionViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.data.rooms.FirebaseTeamRoomRepository
import com.comp90018.app.data.rooms.FirebaseTeamHuntLocationRepository
import com.comp90018.app.data.social.FirebaseSocialRepository
import com.comp90018.app.data.treasure.FirebaseTreasureCollectionRepository
import com.comp90018.app.data.treasure.FirebaseTreasureRepository
import com.comp90018.app.features.friends.FriendsScreen
import com.comp90018.app.features.friends.UnreadMessagesViewModel
import com.comp90018.app.features.map.MapScreen
import com.comp90018.app.features.map.UserLocationViewModel
import com.comp90018.app.features.map.rememberTeamHuntLocations
import com.comp90018.app.features.map.LocationActionPolicy
import com.comp90018.app.features.navigation.RelicNavigationScreen
import com.comp90018.app.features.profile.ProfileScreen
import com.comp90018.app.features.rooms.RoomsScreen
import com.comp90018.app.features.rooms.RoomsViewModel
import com.comp90018.app.features.rooms.TeamRoomChatViewModel
import com.comp90018.app.features.rooms.UnreadRoomMessagesViewModel
import com.comp90018.app.features.treasure.TreasureScreen
import com.comp90018.app.features.treasure.TreasureCatalogViewModel
import com.comp90018.app.features.treasure.TreasureCollectionViewModel
import com.comp90018.app.navigation.AppBottomNavigation
import com.comp90018.app.navigation.AppDestination
import com.comp90018.app.ui.components.SensorErrorHost
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Owns signed-in navigation; profile state belongs to [AppShellViewModel]. */
@Composable
fun AppShell(user: FirebaseUser, firestore: FirebaseFirestore, onLogout: () -> Unit) {
    var destination by remember { mutableStateOf(AppDestination.Friends) }
    var activeHuntTreasureId by remember { mutableStateOf<String?>(null) }
    var requestedMapTreasureId by remember { mutableStateOf<String?>(null) }
    var navigationTreasureId by remember { mutableStateOf<String?>(null) }
    var requestedHuntTreasureId by remember { mutableStateOf<String?>(null) }
    val profileRepository = remember(firestore) { FirebaseProfileRepository(firestore) }
    val appShellViewModel: AppShellViewModel = viewModel(
        key = "app_shell_${user.uid}",
        factory = AppShellViewModel.factory(profileRepository, user.uid, user.email.orEmpty()),
    )
    val appState by appShellViewModel.uiState.collectAsStateWithLifecycle()
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
    // Keep the active hunt visible on Map even after the user leaves the Room tab.
    val activeRoomHuntViewModel: TeamRoomChatViewModel? = roomsState.activeRoomId?.let { roomId ->
        viewModel(
            key = "active_room_hunt_${roomId}_${user.uid}",
            factory = TeamRoomChatViewModel.factory(teamRoomRepository, roomId, user.uid),
        )
    }
    val activeRoomHuntState by (activeRoomHuntViewModel?.uiState
        ?: remember { kotlinx.coroutines.flow.MutableStateFlow(com.comp90018.app.features.rooms.TeamRoomChatUiState()) })
        .collectAsStateWithLifecycle()
    // The local ID only bridges the short period between starting a hunt and the
    // Firestore listener receiving it. Once the room reports a non-hunting
    // state (including termination), it must not keep the map hunt alive.
    LaunchedEffect(activeRoomHuntState.room?.taskStatus) {
        if (activeRoomHuntState.room?.taskStatus != null && activeRoomHuntState.room?.taskStatus != "hunting") {
            activeHuntTreasureId = null
        }
    }
    val treasureRepository = remember(firestore) { FirebaseTreasureRepository(firestore) }
    val treasureCatalogViewModel: TreasureCatalogViewModel = viewModel(
        key = "treasure_catalog",
        factory = TreasureCatalogViewModel.factory(treasureRepository),
    )
    val treasureCatalogState by treasureCatalogViewModel.uiState.collectAsStateWithLifecycle()
    val treasureCollectionRepository = remember(firestore) { FirebaseTreasureCollectionRepository(firestore) }
    val treasureCollectionViewModel: TreasureCollectionViewModel = viewModel(
        key = "treasure_collection_${user.uid}",
        factory = TreasureCollectionViewModel.factory(treasureCollectionRepository, user.uid),
    )
    val treasureCollectionState by treasureCollectionViewModel.uiState.collectAsStateWithLifecycle()
    val settings = appState.profile?.settings
    val notificationsEnabled = settings?.notifications ?: true
    val preciseLocationEnabled = settings?.preciseLocation ?: true
    val context = LocalContext.current
    val userLocationViewModel: UserLocationViewModel = viewModel(
        key = "user_location_$preciseLocationEnabled",
        factory = UserLocationViewModel.factory(context, preciseLocationEnabled),
    )
    val userLocation by userLocationViewModel.output.collectAsStateWithLifecycle()
    val sharedHuntRoom = activeRoomHuntState.room?.takeIf { it.id == roomsState.activeRoomId }
    val huntLocationRepository = remember(firestore) { FirebaseTeamHuntLocationRepository(firestore) }
    val teammateLocations = rememberTeamHuntLocations(huntLocationRepository, sharedHuntRoom, user.uid, userLocation)
    val treasurePageLocation = LocationActionPolicy.actionableCoordinate(userLocation)
    val hapticSession: TreasureHapticSessionViewModel = viewModel(
        key = "treasure_haptics_${user.uid}",
        factory = TreasureHapticSessionViewModel.factory(context),
    )
    // Unknown/failed profile settings must not enable automatic treasure feedback.
    val automaticHapticsPreference = settings?.haptics.takeIf { appState.profileError == null }
    TreasureHapticSessionBinding(hapticSession, automaticHapticsPreference)
    SideEffect { hapticSession.teamClaims.observeRoom(activeRoomHuntState.room) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, userLocationViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                userLocationViewModel.refreshWhenForegrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (navigationTreasureId == null) {
                AppBottomNavigation(
                    selected = destination,
                    unreadFriendMessages = if (notificationsEnabled) unreadFriendMessages else 0,
                    unreadRoomMessages = if (notificationsEnabled) unreadRoomMessages else 0,
                    soundEffectsEnabled = settings?.soundEffects ?: true,
                    hapticsEnabled = settings?.haptics ?: true,
                ) { destination = it }
            }
        },
        snackbarHost = { SensorErrorHost() },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .then(
                if (destination == AppDestination.Map || navigationTreasureId != null) Modifier
                else Modifier.padding(horizontal = 20.dp),
            )
        Box(contentModifier) {
            val navigationTreasure = navigationTreasureId?.let { id ->
                treasureCatalogState.treasures.firstOrNull { it.id == id }
            }
            if (navigationTreasure != null) {
                RelicNavigationScreen(
                    relic = navigationTreasure,
                    userLocation = userLocation,
                    onStopNavigation = { navigationTreasureId = null },
                    onStartHunting = { treasureId ->
                        navigationTreasureId = null
                        requestedHuntTreasureId = treasureId
                        destination = AppDestination.Map
                    },
                )
                return@Box
            }
            when (destination) {
                AppDestination.Treasure -> TreasureScreen(
                    treasures = treasureCatalogState.treasures,
                    loading = treasureCatalogState.loading,
                    error = treasureCatalogState.error,
                    onRetry = treasureCatalogViewModel::retry,
                    discoveredTreasureIds = treasureCollectionState.discoveredIds,
                    currentLocation = treasurePageLocation,
                    onNavigate = { treasureId -> navigationTreasureId = treasureId },
                    onOpenMap = { treasureId ->
                        requestedMapTreasureId = treasureId
                        destination = AppDestination.Map
                    },
                )
                AppDestination.Rooms -> RoomsScreen(
                    user = user,
                    firestore = firestore,
                    profile = appState.profile,
                    viewModel = roomsViewModel,
                    treasures = treasureCatalogState.treasures,
                    treasuresLoading = treasureCatalogState.loading,
                    treasuresError = treasureCatalogState.error,
                    onClaimTreasure = { model -> hapticSession.teamClaims.claim(model, user.uid) },
                    onStartHunt = { treasureId ->
                        activeHuntTreasureId = treasureId
                        destination = AppDestination.Map
                    },
                )
                AppDestination.Map -> MapScreen(
                    treasures = treasureCatalogState.treasures,
                    loading = treasureCatalogState.loading,
                    error = treasureCatalogState.error,
                    onRetry = treasureCatalogViewModel::retry,
                    discoveredTreasureIds = treasureCollectionState.discoveredIds,
                    savingTreasureId = treasureCollectionState.savingTreasureId,
                    preciseLocationEnabled = preciseLocationEnabled,
                    hapticsEnabled = automaticHapticsPreference == true,
                    soundEffectsEnabled = settings?.soundEffects ?: true,
                    userLocation = userLocation,
                    hapticController = hapticSession.controller,
                    onEnableLocation = userLocationViewModel::retryAfterPermissionGranted,
                    onCollectTreasure = treasureCollectionViewModel::addDiscoveredTreasure,
                    activeHuntTreasureId = activeHuntTreasureId
                        ?: activeRoomHuntState.room?.takeIf { it.taskStatus == "hunting" }?.taskId,
                    activeHuntOwnerId = activeRoomHuntState.room?.takeIf { it.taskStatus == "hunting" }?.creatorId,
                    activeHuntSessionId = activeRoomHuntState.room?.takeIf { it.taskStatus == "hunting" }?.huntSessionId,
                    activeHuntMemberIds = activeRoomHuntState.room?.memberIds.orEmpty(),
                    activeHuntCompletedMemberIds = activeRoomHuntState.room?.taskCompletedMemberIds.orEmpty(),
                    activeHuntFoundFragmentIds = activeRoomHuntState.room?.foundFragmentIds.orEmpty(),
                    activeHuntClaimedMemberIds = activeRoomHuntState.room?.taskClaimedMemberIds.orEmpty(),
                    currentUserId = user.uid,
                    teammateLocations = teammateLocations,
                    huntMemberNames = activeRoomHuntState.members.associate { it.uid to it.name },
                    onCompleteActiveHuntTask = { complete ->
                        activeRoomHuntViewModel?.completeHuntTaskWithConfirmation(complete)
                            ?: complete("No active team hunt")
                    },
                    onFindActiveHuntFragment = activeRoomHuntViewModel?.let { it::findHuntFragment } ?: {},
                    onClaimCompletedHuntTreasure = { attempt, complete ->
                        activeRoomHuntViewModel?.let { model ->
                            hapticSession.teamClaims.claim(model, user.uid, attempt, complete)
                        } ?: complete("No active team hunt")
                    },
                    requestedTreasureId = requestedMapTreasureId,
                    onTreasureRequestConsumed = { requestedMapTreasureId = null },
                    requestedHuntTreasureId = requestedHuntTreasureId,
                    onHuntRequestConsumed = { requestedHuntTreasureId = null },
                )
                AppDestination.Friends -> FriendsScreen(user, firestore, appState.profile, onOpenOwnProfile = { destination = AppDestination.Profile })
                AppDestination.Profile -> ProfileScreen(
                    userUid = user.uid,
                    userEmail = user.email.orEmpty(),
                    repository = profileRepository,
                    profile = appState.profile,
                    profileError = appState.profileError,
                    currentRoomId = roomsState.activeRoomId,
                    onRetry = appShellViewModel::retry,
                    onLogout = { hapticSession.endSession(); onLogout() },
                )
            }
        }
    }
}
