package com.comp90018.app.features.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.*
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.ui.components.AppTextField
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DirectChatScreen(firestore: FirebaseFirestore, roomId: String, currentUid: String, title: String, currentUsername: String, currentAvatarUrl: String, onBack: () -> Unit, onViewFriend: (() -> Unit)? = null) {
    var messages by remember(roomId) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var input by remember(roomId) { mutableStateOf("") }
    var error by remember(roomId) { mutableStateOf<String?>(null) }
    DisposableEffect(roomId, firestore) {
        val listener = FirebaseSocialService.observeMessages(firestore, roomId) { data, issue -> messages = data; error = issue }
        onDispose { listener.remove() }
    }
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text(title, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.weight(1f))
            onViewFriend?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Person, "View friend profile", tint = Brand) } }
        }
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            if (messages.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error ?: "Start the conversation.", color = Muted) }
            else LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(messages, key = { it.id }) { msg -> ChatMessageRow(msg, currentUid, title, currentUsername, currentAvatarUrl) }
            }
        }
        AppTextField("Message", input, { input = it }, leadingIcon = Icons.AutoMirrored.Rounded.Chat)
        Button(onClick = { val text = input; input = ""; FirebaseSocialService.sendMessage(firestore, roomId, currentUid, currentUsername.ifBlank { "You" }, currentAvatarUrl, text) { error = it } }, modifier = Modifier.fillMaxWidth(), enabled = input.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, null); Text("Send") }
    }
}

@Composable private fun ChatMessageRow(msg: ChatMessage, uid: String, title: String, myName: String, myAvatar: String) {
    val mine = msg.senderId == uid; val name = msg.senderName.ifBlank { if (mine) myName.ifBlank { "You" } else title }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        if (!mine) { ProfileAvatar(msg.senderAvatarUrl, name, 40.dp); Spacer(Modifier.width(8.dp)) }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Text(name, color = Muted, style = MaterialTheme.typography.labelMedium)
            Surface(color = if (mine) Brand else BrandSoft, shape = RoundedCornerShape(18.dp)) { Text(msg.text, Modifier.padding(14.dp), color = if (mine) Color.White else Ink) }
        }
        if (mine) { Spacer(Modifier.width(8.dp)); ProfileAvatar(msg.senderAvatarUrl.ifBlank { myAvatar }, name, 40.dp) }
    }
}
