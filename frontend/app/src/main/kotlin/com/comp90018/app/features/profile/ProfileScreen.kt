package com.comp90018.app.features.profile

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.data.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private enum class ProfilePage { Overview, Edit, Settings }

@Composable
fun ProfileScreen(
    userUid: String,
    userEmail: String,
    repository: ProfileRepository,
    profile: UserProfile?,
    profileError: String?,
    currentRoomId: String?,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    var page by remember { mutableStateOf(ProfilePage.Overview) }
    var extras by remember(userUid) { mutableStateOf(ProfilePreferences.loadExtras(context, userUid)) }

    when {
        page == ProfilePage.Edit && profile != null -> EditProfileScreen(
            userUid = userUid,
            repository = repository,
            profile = profile,
            extras = extras,
            onBack = { page = ProfilePage.Overview },
            onSaved = { savedExtras ->
                extras = savedExtras
                page = ProfilePage.Overview
            },
        )
        page == ProfilePage.Settings -> SettingsScreen(userUid, onBack = { page = ProfilePage.Overview })
        else -> ProfileOverview(
            userEmail = userEmail,
            profile = profile,
            profileError = profileError,
            currentRoomId = currentRoomId,
            extras = extras,
            onRetry = onRetry,
            onEdit = { page = ProfilePage.Edit },
            onSettings = { page = ProfilePage.Settings },
            onLogout = onLogout,
        )
    }
}

@Composable
private fun ProfileOverview(
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
                Text(email, color = Muted, style = MaterialTheme.typography.bodyMedium)
                ExperienceCard(extras.experience)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ProfileDetail("Current room ID", currentRoomId ?: "Not in a room")
                        extras.department.takeIf { it.isNotBlank() }?.let { ProfileDetail("Department", it) }
                        extras.major.takeIf { it.isNotBlank() }?.let { ProfileDetail("Major", it) }
                        extras.phone.takeIf { it.isNotBlank() }?.let { ProfileDetail("Phone", it) }
                        profile.bio.takeIf { it.isNotBlank() }?.let { ProfileDetail("About", it) }
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
private fun ExperienceCard(experience: Int) {
    val level = levelForExperience(experience)
    val levelProgress = experienceInCurrentLevel(experience)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = BrandSoft)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("Level $level", color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("$experience XP", color = Brand, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { levelProgress / 500f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Brand,
                trackColor = Color.White,
            )
            Text("${500 - levelProgress} XP to Level ${level + 1}", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EditProfileScreen(
    userUid: String,
    repository: ProfileRepository,
    profile: UserProfile,
    extras: ProfileExtras,
    onBack: () -> Unit,
    onSaved: (ProfileExtras) -> Unit,
) {
    val context = LocalContext.current
    var username by remember(profile.uid) { mutableStateOf(profile.username) }
    var gender by remember(profile.uid) { mutableStateOf(profile.gender) }
    var phone by remember(profile.uid) { mutableStateOf(extras.phone) }
    var department by remember(profile.uid) { mutableStateOf(extras.department) }
    var major by remember(profile.uid) { mutableStateOf(extras.major) }
    var bio by remember(profile.uid) { mutableStateOf(profile.bio.take(150)) }
    var selectedAvatar by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val normalizedUsername = username.trim().lowercase()
    val usernameError = when {
        normalizedUsername.isBlank() -> "Username is required"
        normalizedUsername.length !in 3..30 -> "Use 3–30 characters"
        !Regex("^[a-z0-9_]+$").matches(normalizedUsername) -> "Use only letters, numbers, or underscores"
        else -> null
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        selectedAvatar = uri
        message = null
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 10.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text("Edit profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
        }
        Box(contentAlignment = Alignment.BottomEnd) {
            ProfileAvatar(selectedAvatar?.toString() ?: profile.avatarUrl, profile.email, 104.dp)
            IconButton(
                onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.size(42.dp).background(Brand, CircleShape),
            ) { Icon(Icons.Rounded.CameraAlt, "Choose profile photo", tint = Color.White) }
        }
        Text("Choose a profile photo up to 5 MB", color = Muted, style = MaterialTheme.typography.bodySmall)
        UnderlinedField("Username *", username, { username = it; message = null }, error = usernameError)
        GenderDropdown(gender = gender, onGenderChanged = { gender = it })
        UnderlinedField("Phone (optional)", phone, { phone = it }, keyboardType = KeyboardType.Phone)
        UnderlinedField("Department (optional)", department, { department = it })
        UnderlinedField("Major (optional)", major, { major = it })
        UnderlinedField(
            label = "About (optional)",
            value = bio,
            onValueChange = { if (it.length <= 150) bio = it },
            singleLine = false,
            minLines = 4,
        )
        Text("${bio.length}/150 characters", modifier = Modifier.align(Alignment.End), color = Muted, style = MaterialTheme.typography.labelSmall)
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                saving = true
                message = null
                val savedExtras = ProfileExtras(phone.trim(), department.trim(), major.trim(), extras.experience)
                val finishSave: (String?) -> Unit = { error ->
                    saving = false
                    if (error == null) {
                        ProfilePreferences.saveExtras(context, userUid, savedExtras)
                        onSaved(savedExtras)
                    } else message = error
                }
                val saveDetails: (String?) -> Unit = { avatarUrl ->
                    repository.updateProfile(userUid, normalizedUsername, gender, bio, avatarUrl, finishSave)
                }
                selectedAvatar?.let { uri ->
                    repository.uploadAvatar(userUid, uri) { url, error ->
                        if (url == null) {
                            saving = false
                            message = error ?: "Unable to upload avatar"
                        } else saveDetails(url)
                    }
                } ?: saveDetails(profile.avatarUrl.takeIf { it.isNotBlank() })
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !saving && usernameError == null,
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
        ) { Text(if (saving) "Saving…" else "Save changes") }
    }
}

@Composable
private fun UnderlinedField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    error: String? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            minLines = minLines,
            isError = error != null,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                focusedIndicatorColor = Brand,
                focusedLabelColor = Brand,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, top = 2.dp)) }
    }
}

@Composable
private fun GenderDropdown(gender: String, onGenderChanged: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "unspecified" to "Not specified",
        "female" to "Female",
        "male" to "Male",
        "prefer_not_to_say" to "Prefer not to say",
    )
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("Gender", color = Muted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(options.firstOrNull { it.first == gender }?.second ?: "Not specified", color = Ink, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowDropDown, "Choose gender", tint = Muted)
            }
            HorizontalDivider(color = Muted.copy(alpha = 0.55f))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onGenderChanged(value); expanded = false })
            }
        }
    }
}

@Composable
private fun SettingsScreen(userUid: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var settings by remember(userUid) { mutableStateOf(ProfilePreferences.loadSettings(context, userUid)) }
    fun update(next: AppSettings) {
        settings = next
        ProfilePreferences.saveSettings(context, userUid, next)
    }
    Column(Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
        }
        SettingsToggle("Notifications", "Friend, room, and treasure updates", settings.notifications) { update(settings.copy(notifications = it)) }
        SettingsToggle("Discoverable profile", "Allow other explorers to find you in search", settings.searchable) { update(settings.copy(searchable = it)) }
        SettingsToggle("Sound effects", "Play sounds for actions and discoveries", settings.soundEffects) { update(settings.copy(soundEffects = it)) }
        SettingsToggle("Haptic feedback", "Use vibration for important actions", settings.haptics) { update(settings.copy(haptics = it)) }
        SettingsToggle("Precise location", "Improve nearby treasure accuracy", settings.preciseLocation) { update(settings.copy(preciseLocation = it)) }
    }
}

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun ProfileAvatar(source: String, fallback: String, size: Dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, source) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val stream = when {
                    source.startsWith("content:") -> context.contentResolver.openInputStream(Uri.parse(source))
                    source.startsWith("https://") -> URL(source).openStream()
                    else -> null
                }
                stream?.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    Box(Modifier.size(size).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap = bitmap!!, contentDescription = "Profile photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(fallback.take(1).uppercase(), color = Brand, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    }
}
