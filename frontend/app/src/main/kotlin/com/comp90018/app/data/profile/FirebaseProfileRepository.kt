package com.comp90018.app.data.profile

import android.net.Uri
import com.comp90018.app.FirebaseAuthService
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.profile.AppSettings
import com.comp90018.app.features.profile.ProfileExtras
import com.comp90018.app.features.profile.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/** Firebase implementation of [ProfileRepository]. */
class FirebaseProfileRepository(
    private val firestore: FirebaseFirestore,
) : ProfileRepository {
    override fun observeProfile(uid: String, onChange: (UserProfile?, String?) -> Unit): Subscription {
        val registration = firestore.collection("users").document(uid).addSnapshotListener { document, error ->
            if (error != null) {
                onChange(null, error.localizedMessage ?: "Unable to load profile")
            } else if (document?.exists() == true) {
                onChange(
                    UserProfile(
                        uid = uid,
                        email = document.getString("email").orEmpty(),
                        username = document.getString("username").orEmpty(),
                        displayName = document.getString("displayName").orEmpty(),
                        gender = document.getString("gender") ?: "unspecified",
                        bio = document.getString("bio").orEmpty(),
                        avatarUrl = document.getString("avatarUrl").orEmpty(),
                        extras = ProfileExtras(
                            phone = document.getString("phone").orEmpty(),
                            department = document.getString("department").orEmpty(),
                            major = document.getString("major").orEmpty(),
                        ),
                        settings = AppSettings(
                            notifications = document.getBoolean("notificationsEnabled") ?: true,
                            searchable = document.getBoolean("searchable") ?: true,
                            soundEffects = document.getBoolean("soundEffectsEnabled") ?: true,
                            haptics = document.getBoolean("hapticsEnabled") ?: true,
                            preciseLocation = document.getBoolean("preciseLocationEnabled") ?: true,
                        ),
                    ),
                    null,
                )
            } else {
                onChange(null, null)
            }
        }
        return Subscription { registration.remove() }
    }

    override fun ensureProfile(uid: String, email: String, onComplete: (String?) -> Unit) {
        val reference = firestore.collection("users").document(uid)
        reference.get().addOnCompleteListener { readTask ->
            if (!readTask.isSuccessful) {
                onComplete(readTask.exception?.localizedMessage ?: "Unable to load profile")
                return@addOnCompleteListener
            }
            if (readTask.result.exists()) {
                val updates = mutableMapOf<String, Any>()
                if (readTask.result.getString("username").isNullOrBlank()) updates["username"] = defaultUsername(uid, email)
                if (readTask.result.getString("gender").isNullOrBlank()) updates["gender"] = "unspecified"
                if (readTask.result.get("phone") == null) updates["phone"] = ""
                if (readTask.result.get("department") == null) updates["department"] = ""
                if (readTask.result.get("major") == null) updates["major"] = ""
                if (readTask.result.getBoolean("notificationsEnabled") == null) updates["notificationsEnabled"] = true
                if (readTask.result.getBoolean("searchable") == null) updates["searchable"] = true
                if (readTask.result.getBoolean("soundEffectsEnabled") == null) updates["soundEffectsEnabled"] = true
                if (readTask.result.getBoolean("hapticsEnabled") == null) updates["hapticsEnabled"] = true
                if (readTask.result.getBoolean("preciseLocationEnabled") == null) updates["preciseLocationEnabled"] = true
                if (updates.isEmpty()) {
                    onComplete(null)
                } else {
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                    reference.update(updates).addOnCompleteListener { updateTask ->
                        onComplete(if (updateTask.isSuccessful) null else updateTask.exception?.localizedMessage ?: "Unable to update profile")
                    }
                }
            } else {
                reference.set(
                    mapOf(
                        "uid" to uid,
                        "email" to email,
                        "username" to defaultUsername(uid, email),
                        "displayName" to "",
                        "gender" to "unspecified",
                        "bio" to "",
                        "avatarUrl" to "",
                        "phone" to "",
                        "department" to "",
                        "major" to "",
                        "notificationsEnabled" to true,
                        "searchable" to true,
                        "soundEffectsEnabled" to true,
                        "hapticsEnabled" to true,
                        "preciseLocationEnabled" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                ).addOnCompleteListener { writeTask ->
                    onComplete(if (writeTask.isSuccessful) null else writeTask.exception?.localizedMessage ?: "Unable to create profile")
                }
            }
        }
    }

    private fun defaultUsername(uid: String, email: String): String {
        val base = email.substringBefore('@').lowercase().replace(Regex("[^a-z0-9_]"), "").take(18)
        return if (base.length >= 3) base else "explorer_${uid.take(6)}"
    }

    override fun updateProfile(
        uid: String,
        username: String,
        gender: String,
        bio: String,
        extras: ProfileExtras,
        avatarUrl: String?,
        onComplete: (String?) -> Unit,
    ) = FirebaseAuthService.updateProfile(firestore, uid, username, gender, bio, extras, avatarUrl, onComplete)

    override fun updateSettings(uid: String, settings: AppSettings, onComplete: (String?) -> Unit) {
        firestore.collection("users").document(uid).update(
            mapOf(
                "notificationsEnabled" to settings.notifications,
                "searchable" to settings.searchable,
                "soundEffectsEnabled" to settings.soundEffects,
                "hapticsEnabled" to settings.haptics,
                "preciseLocationEnabled" to settings.preciseLocation,
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).addOnCompleteListener { task ->
            onComplete(if (task.isSuccessful) null else task.exception?.localizedMessage ?: "Unable to update settings")
        }
    }

    override fun uploadAvatar(uid: String, avatarUri: Uri, onComplete: (String?, String?) -> Unit) =
        FirebaseAuthService.uploadAvatar(com.google.firebase.storage.FirebaseStorage.getInstance(), uid, avatarUri, onComplete)
}
