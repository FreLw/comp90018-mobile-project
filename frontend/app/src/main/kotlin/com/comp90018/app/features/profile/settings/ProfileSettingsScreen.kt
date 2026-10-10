package com.comp90018.app.features.profile.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.features.profile.AppSettings

/** Renders preference switches and save status for the current profile settings. */
@Composable
internal fun SettingsScreen(
    userUid: String,
    repository: ProfileRepository,
    initialSettings: AppSettings,
    onBack: () -> Unit,
) {
    var settings by remember(userUid, initialSettings) { mutableStateOf(initialSettings) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun update(next: AppSettings) {
        val previous = settings
        settings = next
        saving = true
        error = null
        repository.updateSettings(userUid, next) { updateError ->
            saving = false
            if (updateError != null) {
                settings = previous
                error = updateError
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
        }
        error?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error) }
        SettingsToggle("Notifications", "Show friend and room unread badges", settings.notifications, !saving) { update(settings.copy(notifications = it)) }
        SettingsToggle("Discoverable profile", "Allow other explorers to find you in search", settings.searchable, !saving) { update(settings.copy(searchable = it)) }
        SettingsToggle("Sound effects", "Play sounds for navigation actions", settings.soundEffects, !saving) { update(settings.copy(soundEffects = it)) }
        SettingsToggle("Haptic feedback", "Use vibration for navigation actions", settings.haptics, !saving) { update(settings.copy(haptics = it)) }
        SettingsToggle("Precise location", "Use high-accuracy location for challenges", settings.preciseLocation, !saving) { update(settings.copy(preciseLocation = it)) }
    }
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
