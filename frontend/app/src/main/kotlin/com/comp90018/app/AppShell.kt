package com.comp90018.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.comp90018.app.data.rooms.FirebaseTeamRoomRepository
import com.comp90018.app.features.collection.CollectionScreen
import com.comp90018.app.features.friends.FriendsScreen
import com.comp90018.app.features.hunt.HuntScreen
import com.comp90018.app.features.map.MapScreen
import com.comp90018.app.features.profile.ProfileScreen
import com.comp90018.app.features.treasure.DemoRelics
import com.comp90018.app.navigation.AppBottomNavigation
import com.comp90018.app.navigation.AppDestination
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Owns signed-in navigation; profile state belongs to [AppShellViewModel]. */
@Composable
fun AppShell(user: FirebaseUser, firestore: FirebaseFirestore, onLogout: () -> Unit) {
    var destination by remember { mutableStateOf(AppDestination.Map) }
    var activeRelicId by rememberSaveable { mutableStateOf(DemoRelics.first().id) }
    var mapRelicToOpen by rememberSaveable { mutableStateOf<String?>(null) }
    var foundRelicIds by remember { mutableStateOf(setOf("baillieu-compass", "south-lawn-token")) }
    val activeRelic = DemoRelics.first { it.id == activeRelicId }
    val repository = remember(firestore) { FirebaseProfileRepository(firestore) }
    val roomRepository = remember(firestore) { FirebaseTeamRoomRepository(firestore) }
    var activeRoomId by remember { mutableStateOf<String?>(null) }
    DisposableEffect(roomRepository, user.uid) {
        val subscription = roomRepository.observeMembership(user.uid) { roomId, _ -> activeRoomId = roomId }
        onDispose(subscription::cancel)
    }
    val viewModel: AppShellViewModel = viewModel(
        key = "app_shell_${user.uid}",
        factory = AppShellViewModel.factory(repository, user.uid, user.email.orEmpty()),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = Background, bottomBar = { AppBottomNavigation(destination) { destination = it } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                AppDestination.Hunt -> HuntScreen(
                    relic = activeRelic,
                    alreadyFound = activeRelic.id in foundRelicIds,
                    onResumeHunt = {
                        mapRelicToOpen = activeRelic.id
                        destination = AppDestination.Map
                    },
                )
                AppDestination.Collection -> CollectionScreen(foundRelicIds)
                AppDestination.Map -> MapScreen(
                    activeRelic = activeRelic,
                    initiallyOpenRelicId = mapRelicToOpen,
                    onInitialRelicOpened = { mapRelicToOpen = null },
                    onRelicActivated = { activeRelicId = it.id },
                    onRelicFound = { foundRelicIds = foundRelicIds + it.id },
                    onOpenCollection = { destination = AppDestination.Collection },
                )
                AppDestination.Friends -> Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    FriendsScreen(user, firestore, state.profile)
                }
                AppDestination.Profile -> Box(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    ProfileScreen(user.uid, user.email.orEmpty(), activeRoomId, repository, state.profile, state.profileError, onRetry = viewModel::retry, onLogout = onLogout)
                }
            }
        }
    }
}
