package com.comp90018.app.features.friends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.comp90018.app.FriendSummary
import com.comp90018.app.features.profile.ProfileAvatar
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FriendAvatar(firestore: FirebaseFirestore, friend: FriendSummary) {
    var avatarUrl by remember(friend.uid) { mutableStateOf("") }
    DisposableEffect(friend.uid, firestore) {
        val listener = firestore.collection("users").document(friend.uid).addSnapshotListener { snapshot, _ ->
            avatarUrl = snapshot?.getString("avatarUrl").orEmpty()
        }
        onDispose { listener.remove() }
    }
    ProfileAvatar(avatarUrl, friend.username, 40.dp)
}
