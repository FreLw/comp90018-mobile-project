package com.comp90018.app.data.profile

import android.net.Uri
import com.comp90018.app.FirebaseAuthService
import com.comp90018.app.data.social.Subscription
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

    override fun updateProfile(uid: String, username: String, gender: String, bio: String, avatarUrl: String?, onComplete: (String?) -> Unit) =
        FirebaseAuthService.updateProfile(firestore, uid, username, gender, bio, avatarUrl, onComplete)

    override fun uploadAvatar(uid: String, avatarUri: Uri, onComplete: (String?, String?) -> Unit) =
        FirebaseAuthService.uploadAvatar(com.google.firebase.storage.FirebaseStorage.getInstance(), uid, avatarUri, onComplete)
}
