package com.comp90018.app.features.friends

/*
 * Renders the Friends header, Chats/Contacts selector, and list-card layouts.
 * Edit conversation previews, unread indicators, and empty-list presentation in this file.
 */

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.R
import com.comp90018.app.RelicRed
import com.comp90018.app.data.chat.ChatMessageTypes
import com.comp90018.app.data.social.DirectChatSummary
import com.comp90018.app.data.social.FriendSummary
import com.comp90018.app.ui.components.ProfileAvatar
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.ui.components.treasureStickerFor

private enum class FriendsListTab { Chats, Contacts }

/** Main Friends page: header, Chats/Contacts switch, and conversation previews. */
@Composable
internal fun FriendsHomeContent(
    state: FriendsUiState,
    profile: UserProfile?,
    requestCount: Int,
    onAddFriend: () -> Unit,
    onOpenOwnProfile: () -> Unit,
    onToggleRequests: () -> Unit,
    onOpenChat: (FriendSummary) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(FriendsListTab.Chats) }
    val friendsById = remember(state.friends) { state.friends.associateBy(FriendSummary::uid) }
    val recentChatRows = remember(state.recentChats, friendsById) {
        state.recentChats.mapNotNull { chat ->
            friendsById[chat.friendUid]?.let { friend -> friend to chat }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.message?.let { message ->
            item("friends_error") { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        item("friends_header") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.clickable(onClick = onOpenOwnProfile)) {
                    ProfileAvatar(
                        profile?.avatarUrl.orEmpty(),
                        profile?.username.orEmpty().ifBlank { profile?.email.orEmpty() },
                        48.dp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.username.orEmpty().ifBlank { "Explorer" },
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("Ready for the next adventure", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onAddFriend) { Icon(Icons.Rounded.Add, "Add friend", tint = Brand) }
                Box {
                    IconButton(onClick = onToggleRequests) {
                        Icon(Icons.Rounded.Notifications, "Notifications", tint = Ink)
                    }
                    if (requestCount > 0) {
                        Badge(Modifier.align(Alignment.TopEnd), containerColor = RelicRed) {
                            Text(requestCount.toString())
                        }
                    }
                }
            }
        }
        item("friends_tabs") {
            FriendsTabSelector(selectedTab = selectedTab, onSelected = { selectedTab = it })
        }
        when {
            selectedTab == FriendsListTab.Chats && recentChatRows.isEmpty() -> {
                item("no_chats") { NoChatsCard { selectedTab = FriendsListTab.Contacts } }
            }
            selectedTab == FriendsListTab.Contacts && state.friends.isEmpty() -> {
                item("no_contacts") { NoFriendsCard(onAddFriend) }
            }
            selectedTab == FriendsListTab.Chats -> {
                items(recentChatRows, key = { it.first.uid }) { (friend, chat) ->
                    FriendListCard(friend, chat) { onOpenChat(friend) }
                }
            }
            else -> {
                items(state.friends, key = FriendSummary::uid) { friend ->
                    FriendListCard(friend, null) { onOpenChat(friend) }
                }
            }
        }
    }
}

@Composable
private fun FriendsTabSelector(selectedTab: FriendsListTab, onSelected: (FriendsListTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BrandSoft).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FriendsListTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (tab == FriendsListTab.Chats) "Chats" else "Contacts",
                    color = if (selected) Ink else Muted,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** Combines contact identity with conversation preview, timestamp, and unread presentation. */
@Composable
private fun FriendListCard(friend: FriendSummary, chat: DirectChatSummary?, onOpenChat: () -> Unit) {
    val hasUnreadMessages = friend.unreadCount > 0
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpenChat),
        colors = CardDefaults.cardColors(containerColor = if (hasUnreadMessages) BrandSoft else Color.White),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(friend.avatarUrl, friend.displayLabel, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(friend.displayLabel, fontWeight = FontWeight.SemiBold, color = Ink)
                if (chat != null) LastMessagePreview(chat)
                else Text(
                    if (friend.displayName.isNotBlank()) "@${friend.username}" else "Tap to send a message",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (hasUnreadMessages) {
                Badge(containerColor = RelicRed, contentColor = Color.White) {
                    Text(if (friend.unreadCount > 99) "99+" else friend.unreadCount.toString())
                }
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

/** Formats the last-message content shown beneath a conversation title. */
@Composable
private fun LastMessagePreview(chat: DirectChatSummary) {
    val sticker = treasureStickerFor(chat.lastMessageTreasureId)
    when {
        chat.lastMessageType == ChatMessageTypes.TreasureSticker && sticker != null -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(sticker.drawableRes), sticker.name, Modifier.size(28.dp))
                Spacer(Modifier.width(4.dp))
                Text(sticker.name, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        chat.lastMessageType == ChatMessageTypes.Image ->
            Text("[Img]", color = Muted, style = MaterialTheme.typography.bodySmall)
        else -> Text(
            chat.lastMessageText.ifBlank { "Start a conversation" },
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun NoChatsCard(onShowContacts: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onShowContacts),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("No conversations yet", fontWeight = FontWeight.Bold, color = Ink)
            Text("Choose someone from Contacts to start chatting.", color = Muted)
            TextButton(onClick = onShowContacts) { Text("Browse contacts", color = Brand) }
        }
    }
}

@Composable
private fun NoFriendsCard(onAddFriend: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 38.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(72.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.nav_friends_symbol), null, Modifier.size(64.dp))
            }
            Text("No friends yet", fontWeight = FontWeight.Bold, color = Ink)
            Text("Find an explorer and start hunting together.", color = Muted)
            Button(onClick = onAddFriend, colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add friend")
            }
        }
    }
}
