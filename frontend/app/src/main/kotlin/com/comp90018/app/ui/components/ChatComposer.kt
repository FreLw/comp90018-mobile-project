package com.comp90018.app.ui.components

/*
 * Renders the shared message input, send action, emoji tray, and treasure-sticker picker.
 * The caller sends messages; local state only controls which accessory tray is visible.
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.R

private val commonEmojis = listOf("\uD83D\uDE00", "\uD83D\uDE01", "\uD83D\uDE02", "\uD83D\uDE0A", "\uD83D\uDE0D", "\uD83E\uDD70", "\uD83D\uDE0E", "\uD83E\uDD14", "\uD83D\uDE2D", "\uD83D\uDE21", "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4F", "\uD83C\uDF89", "\u2764\uFE0F", "\uD83D\uDD25")

/** Combines a caller-owned draft with local emoji/sticker trays and an optional send button. */
@Composable
fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSend: (() -> Unit)? = null,
    sendEnabled: Boolean = value.isNotBlank(),
    sending: Boolean = false,
    showTreasureAction: Boolean = false,
    ownedTreasureStickerIds: Set<String>? = null,
    onTreasureStickerSelected: ((String) -> Unit)? = null,
) {
    var emojis by remember { mutableStateOf(false) }
    var attachments by remember { mutableStateOf(false) }
    var treasures by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        if (emojis) Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(16.dp), color = BrandSoft) { Column { commonEmojis.chunked(8).forEach { row -> Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { emoji -> IconButton({ onValueChange(value + emoji) }, Modifier.size(38.dp)) { Text(emoji) } } } } } }
        if (treasures && ownedTreasureStickerIds != null && onTreasureStickerSelected != null) {
            TreasureStickerPicker(
                ownedTreasureIds = ownedTreasureStickerIds,
                onSelected = { treasureId ->
                    onTreasureStickerSelected(treasureId)
                    treasures = false
                },
            )
        } else if (treasures) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                    onValueChange(listOf(value.trim(), "[Treasure fragment]").filter { it.isNotBlank() }.joinToString(" "))
                    treasures = false
                },
                shape = RoundedCornerShape(16.dp),
                color = BrandSoft,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.nav_treasure_symbol), null, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Treasure fragment", color = Ink, style = MaterialTheme.typography.titleSmall)
                        Text("Tap to attach it to your message", color = Brand, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (attachments) Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(16.dp), color = BrandSoft) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) { Attachment(Icons.Rounded.Photo, "Photo") } }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value, onValueChange, Modifier.weight(1f), placeholder = { Text("Message") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand, unfocusedBorderColor = BrandSoft, focusedTextColor = Ink))
            onSend?.let {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = it,
                    enabled = sendEnabled && !sending,
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text(if (sending) "Sending…" else "Send")
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ emojis = !emojis; treasures = false }) { Icon(Icons.Rounded.EmojiEmotions, "Choose emoji", tint = Brand) }
            if (showTreasureAction || ownedTreasureStickerIds != null) {
                IconButton({ treasures = !treasures; emojis = false }) {
                    Image(painterResource(R.drawable.nav_treasure_symbol), "Open treasure stickers", modifier = Modifier.size(28.dp))
                }
            }
            IconButton({ attachments = !attachments }) { Icon(Icons.Rounded.PhotoCamera, "Photo", tint = Ink) }
        }
    }
}

/** Shows collectible stickers and enables selection only for treasure IDs owned by the user. */
@Composable
private fun TreasureStickerPicker(
    ownedTreasureIds: Set<String>,
    onSelected: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = BrandSoft,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Treasure stickers", color = Ink, fontWeight = FontWeight.Bold)
            TreasureStickerCatalog.chunked(3).forEach { stickerRow ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stickerRow.forEach { sticker ->
                        val unlocked = sticker.treasureId in ownedTreasureIds
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = unlocked) { onSelected(sticker.treasureId) }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(sticker.drawableRes),
                                    contentDescription = sticker.name,
                                    modifier = Modifier.size(66.dp).alpha(if (unlocked) 1f else 0.35f),
                                )
                                if (!unlocked) {
                                    Surface(shape = CircleShape, color = Ink.copy(alpha = 0.78f)) {
                                        Icon(
                                            Icons.Rounded.Lock,
                                            contentDescription = "Locked until discovered",
                                            tint = BrandSoft,
                                            modifier = Modifier.padding(5.dp).size(16.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = sticker.name,
                                color = if (unlocked) Ink else Ink.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (ownedTreasureIds.none { id -> TreasureStickerCatalog.any { it.treasureId == id } }) {
                Text(
                    "Discover a treasure to unlock its sticker.",
                    color = Brand,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable private fun Attachment(icon: ImageVector, label: String, click: () -> Unit = {}) = Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(click) { Icon(icon, label, tint = Brand) }; Text(label) }
