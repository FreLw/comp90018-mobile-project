package com.comp90018.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.ChatMessage
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.profile.ProfileAvatar

/** Shared message layout for direct and team-room conversations. */
@Composable
fun ConversationMessageRow(
    message: ChatMessage,
    currentUid: String,
    fallbackOtherName: String,
    currentName: String,
    currentAvatarUrl: String,
    hostUid: String? = null,
    onAvatarClick: (() -> Unit)? = null,
) {
    val mine = message.senderId == currentUid
    val name = message.senderName.ifBlank {
        if (mine) currentName.ifBlank { "You" } else fallbackOtherName.ifBlank { "Explorer" }
    }
    val avatarUrl = message.senderAvatarUrl.ifBlank { if (mine) currentAvatarUrl else "" }
    val rowAlignment = if (mine) Alignment.End else Alignment.Start
    val horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start

    Column(Modifier.fillMaxWidth(), horizontalAlignment = rowAlignment) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!mine) {
                MessageAvatar(avatarUrl, name, onAvatarClick)
                Spacer(Modifier.width(8.dp))
            }
            Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Muted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                    if (hostUid == message.senderId) {
                        Spacer(Modifier.width(6.dp))
                        HostBadge()
                    }
                }
                formatMessageTimestamp(message.sentAtMillis)?.let { timestamp ->
                    Text(timestamp, color = Muted.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (mine) {
                Spacer(Modifier.width(8.dp))
                MessageAvatar(avatarUrl, name, onAvatarClick)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = horizontalArrangement,
        ) {
            if (!mine) Spacer(Modifier.width(48.dp))
            Surface(
                color = if (mine) Brand else BrandSoft,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(
                    text = message.text.ifBlank { if (message.imageUrl.isNotBlank()) "Photo attachment" else "…" },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    color = if (mine) Color.White else Ink,
                )
            }
            if (mine) Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun MessageAvatar(source: String, name: String, onClick: (() -> Unit)?) {
    if (onClick == null) {
        ProfileAvatar(source, name, 40.dp)
    } else {
        androidx.compose.foundation.layout.Box(Modifier.clickable(onClick = onClick)) {
            ProfileAvatar(source, name, 40.dp)
        }
    }
}

@Composable
private fun HostBadge() {
    Surface(color = Color(0xFFFFE7A3), shape = RoundedCornerShape(7.dp)) {
        Text(
            "HOST",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color(0xFF7A5300),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
