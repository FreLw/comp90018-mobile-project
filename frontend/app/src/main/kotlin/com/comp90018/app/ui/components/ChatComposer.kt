package com.comp90018.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.R

private val commonEmojis = listOf("\uD83D\uDE00", "\uD83D\uDE01", "\uD83D\uDE02", "\uD83D\uDE0A", "\uD83D\uDE0D", "\uD83E\uDD70", "\uD83D\uDE0E", "\uD83E\uDD14", "\uD83D\uDE2D", "\uD83D\uDE21", "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4F", "\uD83C\uDF89", "\u2764\uFE0F", "\uD83D\uDD25")

@Composable
fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSend: (() -> Unit)? = null,
    sendEnabled: Boolean = value.isNotBlank(),
    sending: Boolean = false,
    showTreasureAction: Boolean = false,
) {
    var emojis by remember { mutableStateOf(false) }
    var attachments by remember { mutableStateOf(false) }
    var treasures by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        if (emojis) Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(16.dp), color = BrandSoft) { Column { commonEmojis.chunked(8).forEach { row -> Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { emoji -> IconButton({ onValueChange(value + emoji) }, Modifier.size(38.dp)) { Text(emoji) } } } } } }
        if (treasures) {
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
            if (showTreasureAction) {
                IconButton({ treasures = !treasures; emojis = false }) {
                    Image(painterResource(R.drawable.nav_treasure_symbol), "Send a treasure fragment", modifier = Modifier.size(28.dp))
                }
            }
            IconButton({ attachments = !attachments }) { Icon(Icons.Rounded.Add, "More options", tint = Ink) }
        }
    }
}
@Composable private fun Attachment(icon: ImageVector, label: String, click: () -> Unit = {}) = Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(click) { Icon(icon, label, tint = Brand) }; Text(label) }
