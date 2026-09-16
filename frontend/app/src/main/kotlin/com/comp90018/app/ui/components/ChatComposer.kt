package com.comp90018.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink

private val commonEmojis = listOf("😀", "😁", "😂", "😊", "😍", "🥰", "😎", "🤔", "😭", "👍", "👏", "🎉", "❤️", "🔥", "🗺️", "💎")

@Composable fun ChatComposer(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, onPickImage: (() -> Unit)? = null) {
    var emojis by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        if (emojis) Surface(Modifier.fillMaxWidth().padding(bottom = 8.dp), RoundedCornerShape(16.dp), color = BrandSoft) { Column { commonEmojis.chunked(8).forEach { row -> Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { emoji -> IconButton({ onValueChange(value + emoji) }, Modifier.size(38.dp)) { Text(emoji) } } } } } }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value, onValueChange, Modifier.weight(1f), placeholder = { Text("Message") }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand, unfocusedBorderColor = BrandSoft, focusedTextColor = Ink))
            IconButton({ emojis = !emojis }) { Icon(Icons.Rounded.EmojiEmotions, "Choose emoji", tint = Brand) }
            IconButton(onClick = { onPickImage?.invoke() }, enabled = onPickImage != null) { Icon(Icons.Rounded.AddPhotoAlternate, "Choose image from gallery", tint = Ink) }
        }
    }
}
