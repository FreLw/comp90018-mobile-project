package com.comp90018.app.features.rooms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.comp90018.app.AppTextField
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.FirebaseTeamRoomService
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.profile.UserProfile
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RoomsScreen(user: FirebaseUser, firestore: FirebaseFirestore, profile: UserProfile?) {
    var joining by remember { mutableStateOf(false) }
    var roomId by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(78.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(Icons.Rounded.Group, null, tint = Brand, modifier = Modifier.size(36.dp))
        }
        Text("You’re not in a room yet.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Text("Create a room or join one to team up and hunt for treasure together.", color = Muted, textAlign = TextAlign.Center)
        if (joining) AppTextField("Enter room ID", roomId, { roomId = it })
        message?.let { Text(it, color = if (it.startsWith("Room")) Brand else MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        Button(onClick = {
            working = true; message = null
            FirebaseTeamRoomService.createRoom(firestore, user.uid) { id, error ->
                working = false; message = error ?: "Room created. Share this room ID: $id"
            }
        }, modifier = Modifier.fillMaxWidth(), enabled = !working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (working && !joining) "Creating…" else "Create a room") }
        OutlinedButton(onClick = {
            if (!joining) { joining = true; message = null }
            else { working = true; message = null; FirebaseTeamRoomService.joinRoom(firestore, roomId, user.uid) { error -> working = false; message = error ?: "Room joined successfully." } }
        }, modifier = Modifier.fillMaxWidth(), enabled = !working, colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand)) { Text(if (working && joining) "Joining…" else "Join the room") }
    }
}
