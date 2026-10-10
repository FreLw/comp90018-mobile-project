package com.comp90018.app.features.rooms.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.R
import com.comp90018.app.data.rooms.TEAM_ROOM_CAPACITY
import com.comp90018.app.features.rooms.RoomPlazaScreen
import com.comp90018.app.features.rooms.RoomsUiState
import com.comp90018.app.features.rooms.RoomsViewModel
import com.comp90018.app.features.rooms.settings.RoomSettingsForm
import com.comp90018.app.ui.components.AppTextField

/** Renders create/join choices and room-entry input/error state. */
@Composable
internal fun RoomEntryScreen(state: RoomsUiState, viewModel: RoomsViewModel, onJoin: () -> Unit) {
    var creating by remember { mutableStateOf(false) }
    var browsing by remember { mutableStateOf(false) }
    if (browsing) {
        RoomPlazaScreen(state, viewModel, onBack = { browsing = false })
        return
    }
    if (creating) {
        BackHandler(enabled = !state.working) { creating = false }
        RoomSettingsForm(title = "Create a room", working = state.working, error = state.actionError,
            onCancel = { creating = false },
            onSave = { name, description, idOnly -> viewModel.createRoom(name, TEAM_ROOM_CAPACITY, description, idOnly) })
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(86.dp).background(BrandSoft, CircleShape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.nav_rooms_symbol), null, modifier = Modifier.size(76.dp))
        }
        Text("You haven't joined a room yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Text("Join an existing room or create a new one to hunt for treasure together.", color = Muted, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                AppTextField("Enter room ID", state.roomIdInput, viewModel::updateRoomId)
            }
            IconButton(onClick = onJoin, enabled = state.roomIdInput.isNotBlank() && !state.working) {
                Icon(Icons.Rounded.ChevronRight, "Find room", tint = Brand, modifier = Modifier.size(30.dp))
            }
        }
        OutlinedButton(onClick = { browsing = true }, modifier = Modifier.fillMaxWidth(), enabled = !state.working) { Text("Browse rooms") }
        (state.actionError ?: state.membershipError)?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth(), enabled = !state.working, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (state.working && !state.joining) "Creating..." else "Create a room") }
    }
}
