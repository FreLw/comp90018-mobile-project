package com.comp90018.app

import android.net.Uri
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseAuthService {
	fun login(
		auth: FirebaseAuth,
		email: String,
		password: String,
		onComplete: (String?) -> Unit,
	) {
		auth.signInWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
			onComplete(if (task.isSuccessful) null else task.exception.toUserMessage())
		}
	}

	fun register(
		auth: FirebaseAuth,
		email: String,
		password: String,
		onComplete: (String?) -> Unit,
	) {
		auth.createUserWithEmailAndPassword(email.trim(), password).addOnCompleteListener { task ->
			onComplete(if (task.isSuccessful) null else task.exception.toUserMessage())
		}
	}

	fun ensureProfile(
		user: FirebaseUser,
		firestore: FirebaseFirestore,
		onComplete: (String?) -> Unit,
	) {
		val reference = firestore.collection("users").document(user.uid)
		reference.get().addOnCompleteListener { readTask ->
			if (!readTask.isSuccessful) {
				onComplete(readTask.exception.toUserMessage())
				return@addOnCompleteListener
			}
			if (readTask.result.exists()) {
				val updates = mutableMapOf<String, Any>()
				if (readTask.result.getString("username").isNullOrBlank()) {
					updates["username"] = defaultUsername(user)
				}
				if (readTask.result.getString("gender").isNullOrBlank()) {
					updates["gender"] = "unspecified"
				}
				if (updates.isNotEmpty()) {
					updates["updatedAt"] = FieldValue.serverTimestamp()
					reference.update(updates).addOnCompleteListener { updateTask ->
						onComplete(if (updateTask.isSuccessful) null else updateTask.exception.toUserMessage())
					}
				} else {
					onComplete(null)
				}
				return@addOnCompleteListener
			}

			val profile = mapOf(
				"uid" to user.uid,
				"email" to user.email.orEmpty(),
				"username" to defaultUsername(user),
				"displayName" to "",
				"gender" to "unspecified",
				"bio" to "",
				"avatarUrl" to "",
				"createdAt" to FieldValue.serverTimestamp(),
				"updatedAt" to FieldValue.serverTimestamp(),
			)
			reference.set(profile).addOnCompleteListener { writeTask ->
				onComplete(if (writeTask.isSuccessful) null else writeTask.exception.toUserMessage())
			}
		}
	}

	fun updateProfile(
		firestore: FirebaseFirestore,
		uid: String,
		username: String,
		gender: String,
		bio: String,
		avatarUrl: String? = null,
		onComplete: (String?) -> Unit,
	) {
		val cleanUsername = username.trim().lowercase()
		val cleanBio = bio.trim()
		if (!Regex("^[a-z0-9_]{3,30}$").matches(cleanUsername)) {
			onComplete("Username must be 3–30 lowercase letters, numbers, or underscores")
			return
		}
		if (gender !in setOf("unspecified", "male", "female", "prefer_not_to_say")) {
			onComplete("Select a valid gender option")
			return
		}
		if (cleanBio.length > 500) {
			onComplete("Bio cannot exceed 500 characters")
			return
		}

		val updates = mutableMapOf<String, Any>(
				"username" to cleanUsername,
				"gender" to gender,
				"bio" to cleanBio,
				"updatedAt" to FieldValue.serverTimestamp(),
		)
		avatarUrl?.let { updates["avatarUrl"] = it }
		firestore.collection("users").document(uid).update(updates).addOnCompleteListener { task ->
			onComplete(if (task.isSuccessful) null else task.exception.toUserMessage())
		}
	}

	fun uploadAvatar(
		storage: FirebaseStorage,
		uid: String,
		avatarUri: Uri,
		onComplete: (String?, String?) -> Unit,
	) {
		val avatarRef = storage.reference.child("avatars/$uid/profile.jpg")
		avatarRef.putFile(avatarUri)
			.continueWithTask { upload ->
				if (!upload.isSuccessful) throw (upload.exception ?: IllegalStateException("Avatar upload failed"))
				avatarRef.downloadUrl
			}
			.addOnCompleteListener { task ->
				onComplete(if (task.isSuccessful) task.result.toString() else null, task.exception.toUserMessage())
			}
	}

	private fun defaultUsername(user: FirebaseUser): String {
		val base = user.email
			.orEmpty()
			.substringBefore('@')
			.lowercase()
			.replace(Regex("[^a-z0-9_]"), "")
			.take(18)
			.ifBlank { "user" }
		return "${base}_${user.uid.take(6).lowercase()}"
	}

	private fun Exception?.toUserMessage(): String = when (this) {
		is FirebaseAuthWeakPasswordException -> "Use a password with at least 6 characters"
		is FirebaseAuthUserCollisionException -> "This email address is already registered"
		is FirebaseAuthInvalidUserException,
		is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
		is FirebaseTooManyRequestsException -> "Too many attempts. Please try again later"
		is FirebaseNetworkException -> "Network connection failed. Please try again"
		else -> this?.localizedMessage ?: "Firebase request failed. Please try again"
	}
}
