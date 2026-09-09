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
import com.comp90018.app.features.friends.FriendsScreen
import com.comp90018.app.features.map.MapScreen
import com.comp90018.app.features.profile.ProfileScreen
import com.comp90018.app.features.rooms.RoomsScreen
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

    Scaffold(containerColor = Background, bottomBar = { AppBottomNavigation(destination) { destination = it } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (destination) {
                AppDestination.Treasure -> HomeScreen()
                AppDestination.Rooms -> RoomsScreen(user, firestore, state.profile)
                AppDestination.Map -> MapScreen()
                AppDestination.Friends -> FriendsScreen(user, firestore, state.profile)
                AppDestination.Profile -> ProfileScreen(user.uid, user.email.orEmpty(), repository, state.profile, state.profileError, onRetry = viewModel::retry, onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun HomeScreen() = Box(Modifier.fillMaxSize())
