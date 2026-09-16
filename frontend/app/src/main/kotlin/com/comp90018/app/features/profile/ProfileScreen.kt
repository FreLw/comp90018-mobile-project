package com.comp90018.app.features.profile

import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.height
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.comp90018.app.ui.components.AppTextField
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.numericRoomCode
import com.comp90018.app.data.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** Read-only explorer card with a separate edit flow for mutable profile details. */
@Composable
fun ProfileScreen(userUid: String, userEmail: String, roomId: String?, repository: ProfileRepository, profile: UserProfile?, profileError: String?, onRetry: () -> Unit, onLogout: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    if (editing && profile != null) {
        EditProfileScreen(userUid, repository, profile, onBack = { editing = false }, onSaved = { editing = false })
    } else {
        ProfileOverview(userUid, userEmail, roomId, profile, profileError, onRetry, onEdit = { editing = true }, onLogout = onLogout)
    }
}

@Composable
private fun ProfileOverview(userUid: String, userEmail: String, roomId: String?, profile: UserProfile?, profileError: String?, onRetry: () -> Unit, onEdit: () -> Unit, onLogout: () -> Unit) {
    val email = profile?.email ?: userEmail
    val context = LocalContext.current
    val preferences = remember(userUid) { context.getSharedPreferences("profile_settings_$userUid", Context.MODE_PRIVATE) }
    var showEmail by remember(userUid) { mutableStateOf(preferences.getBoolean("show_email", true)) }
    var showId by remember(userUid) { mutableStateOf(preferences.getBoolean("show_id", true)) }
    var huntHints by remember(userUid) { mutableStateOf(preferences.getBoolean("hunt_hints", true)) }
    var friendAlerts by remember(userUid) { mutableStateOf(preferences.getBoolean("friend_alerts", true)) }
    var haptics by remember(userUid) { mutableStateOf(preferences.getBoolean("haptics", true)) }
    var about by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 28.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileAvatar(profile?.avatarUrl.orEmpty(), profile?.username?.ifBlank { email } ?: email, 104.dp)
                Text(
                    profile?.username?.ifBlank { profile.displayName }?.ifBlank { email.substringBefore("@") } ?: email.substringBefore("@"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    textAlign = TextAlign.Center,
                )
                if (showEmail) Text(email, style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center)
                if (showId) Text("ID: " + numericExplorerId(userUid), style = MaterialTheme.typography.bodySmall, color = Muted, textAlign = TextAlign.Center)
                Surface(color = BrandSoft, shape = RoundedCornerShape(14.dp)) {
                    Text(
                        roomId?.let { "Room " + numericRoomCode(it) } ?: "No room",
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = Brand,
                        fontWeight = FontWeight.Bold,
                    )
                }
                profile?.let { loaded ->
                    if (loaded.studentNumber.isNotBlank() || loaded.faculty.isNotBlank() || loaded.bio.isNotBlank()) {
                        HorizontalDivider(color = BrandSoft)
                        if (loaded.studentNumber.isNotBlank()) ProfileDetail("Student ID", loaded.studentNumber)
                        if (loaded.faculty.isNotBlank()) ProfileDetail("Faculty", loaded.faculty)
                        if (loaded.bio.isNotBlank()) ProfileDetail("About", loaded.bio)
                    }
                }
                OutlinedButton(onClick = onEdit, enabled = profile != null, modifier = Modifier.height(40.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit profile", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (profileError != null) {
            Text(profileError, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry profile") }
        } else if (profile == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Loading explorer profile…", color = Muted)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingToggle("Hunt hints", "Show distance and direction tips", huntHints) { huntHints = it; preferences.edit().putBoolean("hunt_hints", it).apply() }
                    HorizontalDivider(color = BrandSoft)
                    SettingToggle("Friend request alerts", "Notify me about new explorers", friendAlerts) { friendAlerts = it; preferences.edit().putBoolean("friend_alerts", it).apply() }
                    HorizontalDivider(color = BrandSoft)
                    SettingToggle("Haptic feedback", "Vibrate when a relic signal is found", haptics) { haptics = it; preferences.edit().putBoolean("haptics", it).apply() }
                }
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                TextButton(onClick = { about = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) { Text("About Lost Treasures", Modifier.weight(1f)); Text("›") }
            }
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.AutoMirrored.Rounded.Logout, null); Spacer(Modifier.width(10.dp)); Text("Sign out")
        }
    }
    if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text("Lost Treasures") },
        text = { Text("Explore campus, hunt for relics, and connect with other players through Friends and Rooms.") },
        confirmButton = { TextButton(onClick = { about = false }) { Text("Got it") } })
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Ink, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun EditProfileScreen(userUid: String, repository: ProfileRepository, profile: UserProfile, onBack: () -> Unit, onSaved: () -> Unit) {
    var username by remember(profile.uid) { mutableStateOf(profile.username) }
    var studentNumber by remember(profile.uid) { mutableStateOf(profile.studentNumber) }
    var faculty by remember(profile.uid) { mutableStateOf(profile.faculty) }
    var bio by remember(profile.uid) { mutableStateOf(profile.bio.take(150)) }
    var selectedAvatar by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val preferences = remember(userUid) { context.getSharedPreferences("profile_settings_$userUid", Context.MODE_PRIVATE) }
    var showEmail by remember(userUid) { mutableStateOf(preferences.getBoolean("show_email", true)) }
    var showId by remember(userUid) { mutableStateOf(preferences.getBoolean("show_id", true)) }
    var gender by remember(userUid) { mutableStateOf(preferences.getString("gender", profile.gender.takeUnless { it == "unspecified" }.orEmpty()).orEmpty()) }
    var birthday by remember(userUid) { mutableStateOf(preferences.getString("birthday", "").orEmpty()) }
    var region by remember(userUid) { mutableStateOf(preferences.getString("region", "").orEmpty()) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> selectedAvatar = uri; message = null }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 18.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        Box(contentAlignment = Alignment.BottomEnd) {
            ProfileAvatar(selectedAvatar?.toString() ?: profile.avatarUrl, profile.email, 112.dp)
            Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.size(44.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = "Choose profile photo")
            }
        }
        Text("Tap the camera to choose a new photo (max 5 MB).", color = Muted, style = MaterialTheme.typography.bodySmall)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(horizontal = 18.dp)) {
                ProfileEditRow("Username", username, { username = it }, "username")
                HorizontalDivider(color = BrandSoft)
                ProfileEditRow("Student ID", studentNumber, { studentNumber = it.filter(Char::isDigit).take(12) }, "6–12 digits", KeyboardType.Number)
                HorizontalDivider(color = BrandSoft)
                ProfileEditRow("Faculty", faculty, { faculty = it.take(80) }, "Faculty or department")
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About", color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = bio,
                    onValueChange = { bio = it.take(150) },
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                    decorationBox = { field -> Box { if (bio.isBlank()) Text("Tell other explorers about yourself", color = Muted); field() } },
                )
                Text(bio.length.toString() + "/150", modifier = Modifier.align(Alignment.End), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(horizontal = 18.dp)) {
                ProfileEditRow("Gender", gender, { gender = it.take(30) }, "Optional")
                HorizontalDivider(color = BrandSoft)
                ProfileEditRow("Birthday", birthday, { birthday = it.take(10) }, "DD/MM/YYYY", KeyboardType.Number)
                HorizontalDivider(color = BrandSoft)
                ProfileEditRow("Region", region, { region = it.take(60) }, "City or region")
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Profile visibility", color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SettingToggle("Show email", "Display email on your profile", showEmail) {
                    showEmail = it
                    preferences.edit().putBoolean("show_email", it).apply()
                }
                HorizontalDivider(color = BrandSoft)
                SettingToggle("Show explorer ID", "Display your numeric explorer ID", showId) {
                    showId = it
                    preferences.edit().putBoolean("show_id", it).apply()
                }
            }
        }
        message?.let { Text(it, color = if (it == "Profile saved") Brand else MaterialTheme.colorScheme.error) }
        Button(onClick = {
            saving = true; message = null
            preferences.edit().putString("gender", gender.trim()).putString("birthday", birthday.trim()).putString("region", region.trim()).apply()
            val finishSave: (String?) -> Unit = { error -> saving = false; message = error ?: "Profile saved"; if (error == null) onSaved() }
            val saveDetails: (String?) -> Unit = { avatarUrl -> repository.updateProfile(userUid, username, studentNumber, faculty, bio, avatarUrl, finishSave) }
            selectedAvatar?.let { uri -> repository.uploadAvatar(userUid, uri) { url, error -> if (url == null) { saving = false; message = error ?: "Unable to upload avatar" } else saveDetails(url) } }
                ?: saveDetails(profile.avatarUrl.takeIf { it.isNotBlank() })
        }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (saving) "Saving…" else "Save changes") }
    }
}

@Composable
private fun ProfileEditRow(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(104.dp), color = Ink, fontWeight = FontWeight.Medium)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            decorationBox = { field -> Box { if (value.isBlank()) Text(placeholder, color = Muted); field() } },
        )
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
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

private fun numericExplorerId(uid: String): String {
    val numericId = uid.fold(0L) { value, character ->
        (value * 131L + character.code) % 1_000_000_000L
    }
    return numericId.toString().padStart(9, '0')
}
