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
import com.comp90018.app.*
import com.comp90018.app.features.profile.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun FriendProfileScreen(firestore: FirebaseFirestore, friend: FriendSummary, currentUid: String, onBack: () -> Unit, onRemoved: () -> Unit) {
    var profile by remember(friend.uid) { mutableStateOf<UserProfile?>(null) }; var error by remember { mutableStateOf<String?>(null) }; var removing by remember { mutableStateOf(false) }
    DisposableEffect(friend.uid, firestore) {
        val listener = firestore.collection("users").document(friend.uid).addSnapshotListener { doc, issue ->
            if (issue != null) error = issue.localizedMessage else if (doc?.exists() == true) profile = UserProfile(friend.uid, doc.getString("email").orEmpty(), doc.getString("username") ?: friend.username, doc.getString("displayName").orEmpty(), doc.getString("gender") ?: "unspecified", doc.getString("bio").orEmpty(), doc.getString("avatarUrl").orEmpty())
        }; onDispose { listener.remove() }
    }
    Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }; Text("Friend profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink) }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(profile?.username ?: friend.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            profile?.email?.takeIf { it.isNotBlank() }?.let { Detail("Email", it) }; profile?.gender?.let { Detail("Gender", it) }; profile?.bio?.takeIf { it.isNotBlank() }?.let { Detail("About", it) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } }
        Button(onClick = { removing = true; FirebaseSocialService.removeFriend(firestore, currentUid, friend.uid) { issue -> removing = false; if (issue == null) onRemoved() else error = issue } }, modifier = Modifier.fillMaxWidth(), enabled = !removing, colors = ButtonDefaults.buttonColors(containerColor = RelicRed)) { Text(if (removing) "Removing…" else "Remove friend") }
    }
}
@Composable private fun Detail(label: String, value: String) { Column { Text(label, style = MaterialTheme.typography.labelMedium, color = Muted); Text(value, color = Ink) } }
