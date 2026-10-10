package com.comp90018.app.features.rooms.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.RelicRed
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.ui.components.ChatComposer
import com.comp90018.app.ui.components.ProfileAvatar

internal const val DEMO_ROOM_ID = "CAMPUS-2026"

/** Displays development room-preview content without joining a live team room. */
@Composable
internal fun MockTeamRoomScreen(roomId: String, profile: UserProfile?, onDismiss: () -> Unit) {
    var input by remember(roomId) { mutableStateOf("") }
    var messages by remember(roomId) { mutableStateOf(listOf("ava: Welcome! I found our first clue.")) }
    var showDetails by remember(roomId) { mutableStateOf(false) }
    val ownName = profile?.username.orEmpty().ifBlank { "You" }
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { showDetails = true }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(profile?.avatarUrl.orEmpty(), ownName, 46.dp)
            Spacer(Modifier.width(8.dp))
            ProfileAvatar("", "ava", 46.dp)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Info, "Room details", tint = Brand, modifier = Modifier.padding(12.dp).size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages) { message ->
                    SelectionContainer { Text(message, color = Ink) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        ChatComposer(
            value = input,
            onValueChange = { input = it },
            onSend = {
                val message = input.trim()
                if (message.isNotBlank()) {
                    messages = messages + "$ownName: $message"
                    input = ""
                }
            },
            sendEnabled = input.isNotBlank(),
            showTreasureAction = true,
        )
    }
    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Room details", color = Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Room ID: $roomId", color = Ink)
                    Text("This is a local demo room. No backend data was changed.", color = Muted)
                }
            },
            confirmButton = { TextButton(onClick = { showDetails = false }) { Text("Done", color = Brand) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss room", color = RelicRed) } },
        )
    }
}
