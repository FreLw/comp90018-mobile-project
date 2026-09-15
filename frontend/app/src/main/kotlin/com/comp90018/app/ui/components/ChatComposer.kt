package com.comp90018.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink

private val commonEmojis = listOf("\uD83D\uDE00", "\uD83D\uDE01", "\uD83D\uDE02", "\uD83D\uDE0A", "\uD83D\uDE0D", "\uD83E\uDD70", "\uD83D\uDE0E", "\uD83E\uDD14", "\uD83D\uDE2D", "\uD83D\uDE21", "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4F", "\uD83C\uDF89", "\u2764\uFE0F", "\uD83D\uDD25")

@Composable fun ChatComposer(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var emojis by remember { mutableStateOf(false) }; var attachments by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        if (emojis) Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(16.dp), color = BrandSoft) { Column { commonEmojis.chunked(8).forEach { row -> Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { emoji -> IconButton({ onValueChange(value + emoji) }, Modifier.size(38.dp)) { Text(emoji) } } } } } }
        if (attachments) Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(16.dp), color = BrandSoft) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) { Attachment(Icons.Rounded.Inventory2, "Treasure") } }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value, onValueChange, Modifier.weight(1f), placeholder = { Text("Message") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand, unfocusedBorderColor = BrandSoft, focusedTextColor = Ink))
            IconButton({ emojis = !emojis }) { Icon(Icons.Rounded.EmojiEmotions, "Choose emoji", tint = Brand) }
            IconButton({ attachments = !attachments }) { Icon(Icons.Rounded.Add, "More options", tint = Ink) }
        }
    }
}
@Composable private fun Attachment(icon: ImageVector, label: String, click: () -> Unit = {}) = Column(horizontalAlignment = Alignment.CenterHorizontally) { IconButton(click) { Icon(icon, label, tint = Brand) }; Text(label) }
