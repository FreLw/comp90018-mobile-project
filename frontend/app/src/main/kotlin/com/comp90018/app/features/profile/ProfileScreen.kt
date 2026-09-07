package com.comp90018.app.features.profile

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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comp90018.app.AppTextField
import com.comp90018.app.Brand
import com.comp90018.app.BrandSoft
import com.comp90018.app.FirebaseAuthService
import com.comp90018.app.Ink
import com.comp90018.app.Muted
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

/** Profile editor: it owns form state while the host owns the Firestore subscription. */
@Composable
fun ProfileScreen(
    user: FirebaseUser,
    firestore: FirebaseFirestore,
    profile: UserProfile?,
    profileError: String?,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    val email = profile?.email ?: user.email.orEmpty()
    // Firestore can emit snapshots while typing. Only reset form state for another user.
    var username by remember(profile?.uid) { mutableStateOf(profile?.username.orEmpty()) }
    var gender by remember(profile?.uid) { mutableStateOf(profile?.gender ?: "unspecified") }
    var bio by remember(profile?.uid) { mutableStateOf(profile?.bio.orEmpty()) }
    var saving by remember(profile?.uid) { mutableStateOf(false) }
    var saveMessage by remember(profile?.uid) { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 30.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(96.dp).clip(CircleShape).background(Brand),
            contentAlignment = Alignment.Center,
        ) {
            Text(email.take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.displaySmall)
        }
        Text(
            username.ifBlank { email },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    profileError != null -> {
                        Text(profileError, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                    profile == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Brand, strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Loading profile…", color = Muted)
                    }
                    else -> {
                        Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)

                        Text("Email", color = Muted, style = MaterialTheme.typography.labelMedium)
                        OutlinedTextField(
                            value = email,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                        )

                        Text("Username", color = Muted, style = MaterialTheme.typography.labelMedium)
                        AppTextField(
                            "Username (3–30 lowercase letters, numbers, or _)",
                            username,
                            { username = it },
                        )

                        Text("Gender", color = Muted, style = MaterialTheme.typography.labelMedium)
                        val genderOptions = listOf(
                            "unspecified" to "Not specified",
                            "male" to "Male",
                            "female" to "Female",
                            "prefer_not_to_say" to "Prefer not to say",
                        )
                        genderOptions.chunked(2).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowOptions.forEach { (value, label) ->
                                    Button(
                                        onClick = { gender = value },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (gender == value) Brand else BrandSoft,
                                            contentColor = if (gender == value) Color.White else Brand,
                                        ),
                                    ) { Text(label) }
                                }
                            }
                        }

                        Text("Bio", color = Muted, style = MaterialTheme.typography.labelMedium)
                        AppTextField(
                            "Tell us about yourself (up to 500 characters)",
                            bio,
                            { bio = it },
                            singleLine = false,
                            maxLines = 5,
                        )

                        saveMessage?.let {
                            Text(it, color = if (it == "Profile saved") Brand else MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = {
                                saving = true
                                saveMessage = null
                                FirebaseAuthService.updateProfile(
                                    firestore = firestore,
                                    uid = user.uid,
                                    username = username,
                                    gender = gender,
                                    bio = bio,
                                ) { error ->
                                    saving = false
                                    saveMessage = error ?: "Profile saved"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !saving,
                            colors = ButtonDefaults.buttonColors(containerColor = Brand),
                        ) { Text(if (saving) "Saving…" else "Save profile") }
                    }
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
