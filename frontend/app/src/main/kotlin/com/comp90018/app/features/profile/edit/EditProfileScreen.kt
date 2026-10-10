package com.comp90018.app.features.profile.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.comp90018.app.Brand
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.features.profile.ProfileExtras
import com.comp90018.app.features.profile.UserProfile
import com.comp90018.app.ui.components.ProfileAvatar

/** Keeps editable form values locally and submits one profile update through the supplied callback. */
@Composable
internal fun EditProfileScreen(
    userUid: String,
    repository: ProfileRepository,
    profile: UserProfile,
    extras: ProfileExtras,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
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
                val savedExtras = ProfileExtras(phone.trim(), department.trim(), major.trim())
                val finishSave: (String?) -> Unit = { error ->
                    saving = false
                    if (error == null) {
                        onSaved()
                    } else message = error
                }
                saveProfileEdits(
                    repository = repository,
                    userUid = userUid,
                    username = normalizedUsername,
                    readGender = { gender },
                    readBio = { bio },
                    extras = savedExtras,
                    selectedAvatar = selectedAvatar,
                    existingAvatarUrl = profile.avatarUrl,
                    onComplete = finishSave,
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            enabled = !saving && usernameError == null,
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
        ) { Text(if (saving) "Saving…" else "Save changes") }
    }
}
