package com.comp90018.app.features.profile

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.comp90018.app.ui.components.AppTextField
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.data.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** Read-only explorer card with a separate edit flow for mutable profile details. */
@Composable
fun ProfileScreen(userUid: String, userEmail: String, repository: ProfileRepository, profile: UserProfile?, profileError: String?, onRetry: () -> Unit, onLogout: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    if (editing && profile != null) {
        EditProfileScreen(userUid, repository, profile, onBack = { editing = false }, onSaved = { editing = false })
    } else {
        ProfileOverview(userEmail, profile, profileError, onRetry, onEdit = { editing = true }, onLogout = onLogout)
    }
}

@Composable
private fun ProfileOverview(userEmail: String, profile: UserProfile?, profileError: String?, onRetry: () -> Unit, onEdit: () -> Unit, onLogout: () -> Unit) {
    val email = profile?.email ?: userEmail
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 30.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            profileError != null -> {
                Text(profileError, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Retry") }
            }
            profile == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Brand, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Loading explorer profile…", color = Muted)
            }
            else -> {
                ProfileAvatar(profile.avatarUrl, email, 112.dp)
                Text(profile.username.ifBlank { email }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                Text("Campus explorer", color = Muted)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ProfileDetail("Email", email)
                        ProfileDetail("Username", profile.username)
                        ProfileDetail("Gender", genderLabel(profile.gender))
                        profile.bio.takeIf { it.isNotBlank() }?.let { ProfileDetail("About", it) }
                    }
                }
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                    Icon(Icons.Rounded.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit profile")
                }
            }
        }
        TextButton(onClick = onLogout) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
        }
    }
}

@Composable
private fun EditProfileScreen(userUid: String, repository: ProfileRepository, profile: UserProfile, onBack: () -> Unit, onSaved: () -> Unit) {
    var username by remember(profile.uid) { mutableStateOf(profile.username) }
    var gender by remember(profile.uid) { mutableStateOf(profile.gender) }
    var selectedAvatar by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> selectedAvatar = uri; message = null }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 18.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            Spacer(Modifier.width(6.dp)); Text("Back to profile")
        }
        Text("Edit explorer profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Box(contentAlignment = Alignment.BottomEnd) {
            ProfileAvatar(selectedAvatar?.toString() ?: profile.avatarUrl, profile.email, 112.dp)
            Button(onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.size(44.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = "Choose profile photo")
            }
        }
        Text("Tap the camera to choose a new photo (max 5 MB).", color = Muted, style = MaterialTheme.typography.bodySmall)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Username", color = Muted, style = MaterialTheme.typography.labelMedium)
                AppTextField("3–30 lowercase letters, numbers, or _", username, { username = it })
                Text("Gender", color = Muted, style = MaterialTheme.typography.labelMedium)
                val genderOptions = listOf("unspecified" to "Not specified", "male" to "Male", "female" to "Female", "prefer_not_to_say" to "Prefer not to say")
                genderOptions.chunked(2).forEach { rowOptions ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { (value, label) ->
                            Button(onClick = { gender = value }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (gender == value) Brand else BrandSoft, contentColor = if (gender == value) Color.White else Brand)) { Text(label) }
                        }
                    }
                }
                message?.let { Text(it, color = if (it == "Profile saved") Brand else MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    saving = true; message = null
                    val finishSave: (String?) -> Unit = { error -> saving = false; message = error ?: "Profile saved"; if (error == null) onSaved() }
                    val saveDetails: (String?) -> Unit = { avatarUrl -> repository.updateProfile(userUid, username, gender, profile.bio, avatarUrl, finishSave) }
                    selectedAvatar?.let { uri ->
                        repository.uploadAvatar(userUid, uri) { url, error ->
                            if (url == null) { saving = false; message = error ?: "Unable to upload avatar" } else saveDetails(url)
                        }
                    } ?: saveDetails(profile.avatarUrl.takeIf { it.isNotBlank() })
                }, modifier = Modifier.fillMaxWidth(), enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text(if (saving) "Saving…" else "Save changes") }
            }
        }
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

private fun genderLabel(gender: String): String = when (gender) {
    "male" -> "Male"
    "female" -> "Female"
    "prefer_not_to_say" -> "Prefer not to say"
    else -> "Not specified"
}
