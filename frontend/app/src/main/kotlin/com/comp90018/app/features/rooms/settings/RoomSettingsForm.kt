package com.comp90018.app.features.rooms.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted

/** Collects editable room settings before the parent submits them. */
@Composable
internal fun RoomSettingsForm(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialIdOnly: Boolean = true,
    working: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSave: (String, String, Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var idOnly by remember { mutableStateOf(initialIdOnly) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel, enabled = !working) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(name, { name = it.take(80) }, label = { Text("Room name") }, singleLine = true, enabled = !working, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it.take(500) }, label = { Text("Room description") }, minLines = 3, enabled = !working, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Require a Room ID to join", color = Ink)
                Text(if (idOnly) "Hidden from the plaza. Share the ID so others can join." else "Visible in the plaza. Anyone can join directly.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = idOnly, onCheckedChange = { idOnly = it }, enabled = !working)
        }
        Button(onClick = { onSave(name.trim(), description.trim(), idOnly) }, enabled = name.isNotBlank() && !working,
            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (working) "Saving..." else if (title == "Create a room") "Create room" else "Save settings") }
    }
}
