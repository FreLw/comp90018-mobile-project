package com.comp90018.app.features.chat

/*
 * Renders one direct conversation, its message bubbles, header, and composer.
 * Connects the room ViewModel to UI callbacks and collection-backed treasure stickers.
 */

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.*
import com.comp90018.app.data.chat.ChatMessage
import com.comp90018.app.data.chat.ChatMessageTypes
import com.comp90018.app.data.chat.FirebaseChatRepository
import com.comp90018.app.data.treasure.FirebaseTreasureCollectionRepository
import com.comp90018.app.features.profile.ProfileAvatar
import com.comp90018.app.features.treasure.TreasureCollectionViewModel
import com.comp90018.app.ui.components.ChatComposer
import com.comp90018.app.ui.components.formatMessageTimestamp
import com.comp90018.app.ui.components.treasureStickerFor
import com.google.firebase.firestore.FirebaseFirestore

/** Collects room and sticker-ownership state, renders the conversation, and binds composer actions. */
@Composable
fun DirectChatScreen(firestore: FirebaseFirestore, roomId: String, currentUid: String, friendUid: String, title: String, currentUsername: String, currentAvatarUrl: String, onBack: () -> Unit, onViewFriend: (() -> Unit)? = null) {
    val repository = remember(firestore) { FirebaseChatRepository(firestore) }
    val viewModel: DirectChatViewModel = viewModel(
        key = "direct_chat_${roomId}_$currentUid",
        factory = DirectChatViewModel.factory(repository, roomId, currentUid, friendUid, currentUsername, currentAvatarUrl),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val treasureRepository = remember(firestore) { FirebaseTreasureCollectionRepository(firestore) }
    val treasureViewModel: TreasureCollectionViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "direct_chat_treasures_$currentUid",
        factory = TreasureCollectionViewModel.factory(treasureRepository, currentUid),
    )
    val treasureState by treasureViewModel.uiState.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) {
        viewModel.setScreenVisible(true)
        onDispose { viewModel.setScreenVisible(false) }
    }
    LaunchedEffect(currentUsername, currentAvatarUrl) {
        viewModel.updateCurrentUser(currentUsername, currentAvatarUrl)
    }
    Column(Modifier.fillMaxSize().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text(title, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.weight(1f))
            onViewFriend?.let { IconButton(onClick = it) { Icon(Icons.Rounded.Person, "View friend profile", tint = Brand) } }
        }
        Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            if (state.messages.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.error ?: "Start the conversation.", color = Muted) }
            else LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(state.messages, key = { it.id }) { msg -> ChatMessageRow(msg, currentUid, title, currentUsername, currentAvatarUrl) }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(8.dp))
        ChatComposer(
            value = state.input,
            onValueChange = viewModel::updateInput,
            onSend = viewModel::send,
            sendEnabled = state.input.isNotBlank(),
            sending = state.sending,
            ownedTreasureStickerIds = treasureState.discoveredIds,
            onTreasureStickerSelected = viewModel::sendTreasureSticker,
        )
    }
}

/** Chooses message alignment and content presentation for the current sender and message type. */
@Composable private fun ChatMessageRow(msg: ChatMessage, uid: String, title: String, myName: String, myAvatar: String) {
    val mine = msg.senderId == uid; val name = msg.senderName.ifBlank { if (mine) myName.ifBlank { "You" } else title }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
        if (!mine) { ProfileAvatar(msg.senderAvatarUrl, name, 40.dp); Spacer(Modifier.width(8.dp)) }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Text(name, color = Ink, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            formatMessageTimestamp(msg.sentAtMillis)?.let { timestamp ->
                Text(timestamp, color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(3.dp))
            val sticker = treasureStickerFor(msg.treasureId)
            if (msg.messageType == ChatMessageTypes.TreasureSticker && sticker != null) {
                Image(
                    painter = painterResource(sticker.drawableRes),
                    contentDescription = sticker.name,
                    modifier = Modifier.size(112.dp),
                )
            } else {
                Surface(color = if (mine) Brand else BrandSoft, shape = RoundedCornerShape(18.dp)) {
                    Text(
                        text = if (msg.messageType == ChatMessageTypes.Image || msg.imageUrl.isNotBlank()) "[Img]" else msg.text,
                        modifier = Modifier.padding(14.dp),
                        color = if (mine) Color.White else Ink,
                    )
                }
            }
        }
        if (mine) { Spacer(Modifier.width(8.dp)); ProfileAvatar(msg.senderAvatarUrl.ifBlank { myAvatar }, name, 40.dp) }
    }
}
