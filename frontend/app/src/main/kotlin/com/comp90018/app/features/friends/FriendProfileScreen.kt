package com.comp90018.app.features.friends

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.FriendSummary
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.data.profile.FirebaseProfileRepository
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FriendProfileScreen(
    firestore: FirebaseFirestore,
    friend: FriendSummary,
    onBack: () -> Unit,
    onRemoved: () -> Unit,
    onRemoveFriend: (String, (String?) -> Unit) -> Unit,
) {
    val repository = remember(firestore) { FirebaseProfileRepository(firestore) }
    val viewModel: FriendProfileViewModel = viewModel(
        key = "friend_profile_${friend.uid}",
        factory = FriendProfileViewModel.factory(repository, friend.uid),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }; Text("Friend profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink) }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(profile?.username ?: friend.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            profile?.email?.takeIf { it.isNotBlank() }?.let { Detail("Email", it) }; profile?.gender?.let { Detail("Gender", it) }; profile?.bio?.takeIf { it.isNotBlank() }?.let { Detail("About", it) }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }
        Button(onClick = { viewModel.remove(onRemoveFriend, friend.uid, onRemoved) }, modifier = Modifier.fillMaxWidth(), enabled = !state.removing, colors = ButtonDefaults.buttonColors(containerColor = RelicRed)) { Text(if (state.removing) "Removing..." else "Remove friend") }
    }
}

@Composable private fun Detail(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = Muted); Text(value, color = Ink) } }
