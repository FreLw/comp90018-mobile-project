package com.comp90018.app.features.profile.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.features.profile.ProfileExtras
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.ui.components.ProfileAvatar

/** Builds the avatar, identity, details, and entry actions shown on the Profile tab. */
@Composable
internal fun ProfileOverview(
    userEmail: String,
    profile: UserProfile?,
    profileError: String?,
    currentRoomId: String?,
    extras: ProfileExtras,
    onRetry: () -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val email = profile?.email ?: userEmail
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            profileError != null -> {
                Text(profileError, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Retry") }
            }
            profile == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Brand, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Loading profile…", color = Muted)
            }
            else -> {
                ProfileAvatar(profile.avatarUrl, email, 112.dp)
                Text(profile.username.ifBlank { email }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                profile.bio.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        color = Ink,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Text(email, color = Muted, style = MaterialTheme.typography.bodyMedium)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ProfileDetail("Current room ID", currentRoomId ?: "Not in a room")
                        extras.department.takeIf { it.isNotBlank() }?.let { ProfileDetail("Department", it) }
                        extras.major.takeIf { it.isNotBlank() }?.let { ProfileDetail("Major", it) }
                        extras.phone.takeIf { it.isNotBlank() }?.let { ProfileDetail("Phone", it) }
                    }
                }
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit profile")
                }
                OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Settings")
                }
            }
        }
        TextButton(onClick = onLogout) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
        }
        HorizontalDivider(color = BrandSoft)
        Text("About", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text("Lost Treasures · Version 1.0", color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
