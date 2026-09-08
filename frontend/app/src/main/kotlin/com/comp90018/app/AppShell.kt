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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.comp90018.app.features.friends.FriendsScreen
import com.comp90018.app.features.map.MapScreen
import com.comp90018.app.features.profile.ProfileScreen
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.features.rooms.RoomsScreen
import com.comp90018.app.navigation.AppBottomNavigation
import com.comp90018.app.navigation.AppDestination
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Owns signed-in navigation and the current user's Firestore profile subscription. */
@Composable
fun AppShell(user: FirebaseUser, firestore: FirebaseFirestore, onLogout: () -> Unit) {
    var destination by remember { mutableStateOf(AppDestination.Friends) }
    var profile by remember(user.uid) { mutableStateOf<UserProfile?>(null) }
    var profileError by remember(user.uid) { mutableStateOf<String?>(null) }
    var retryKey by remember(user.uid) { mutableStateOf(0) }
    var creatingProfile by remember(user.uid) { mutableStateOf(false) }

    DisposableEffect(user.uid, firestore, retryKey) {
        val reference = firestore.collection("users").document(user.uid)
        val registration = reference.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                profileError = exception.localizedMessage ?: "Unable to load your Firestore profile"
            } else if (snapshot != null && snapshot.exists()) {
                profileError = null
                val username = snapshot.getString("username").orEmpty()
                profile = UserProfile(user.uid, snapshot.getString("email") ?: user.email.orEmpty(), username, snapshot.getString("displayName").orEmpty(), snapshot.getString("gender") ?: "unspecified", snapshot.getString("bio").orEmpty(), snapshot.getString("avatarUrl").orEmpty())
                if (username.isBlank() && !creatingProfile) {
                    creatingProfile = true
                    createProfile(user, firestore) { creatingProfile = false; profileError = it }
                }
            } else if (!creatingProfile) {
                creatingProfile = true
                createProfile(user, firestore) { creatingProfile = false; profileError = it }
            }
        }
        onDispose { registration.remove() }
    }

    Scaffold(containerColor = Background, bottomBar = { AppBottomNavigation(destination) { destination = it } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            when (destination) {
                AppDestination.Treasure -> HomeScreen()
                AppDestination.Rooms -> RoomsScreen(user, firestore, profile)
                AppDestination.Map -> MapScreen()
                AppDestination.Friends -> FriendsScreen(user, firestore, profile)
                AppDestination.Profile -> ProfileScreen(user, firestore, profile, profileError, onRetry = { retryKey += 1 }, onLogout = onLogout)
            }
        }
    }
}

private fun createProfile(user: FirebaseUser, firestore: FirebaseFirestore, complete: (String?) -> Unit) {
    FirebaseAuthService.ensureProfile(user, firestore, complete)
}

@Composable
private fun HomeScreen() = Box(Modifier.fillMaxSize())
