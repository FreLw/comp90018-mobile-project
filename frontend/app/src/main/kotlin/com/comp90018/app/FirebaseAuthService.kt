package com.comp90018.app

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
				if (readTask.result.getString("username").isNullOrBlank()) {
					reference.update(
						mapOf(
							"username" to defaultUsername(user),
							"updatedAt" to FieldValue.serverTimestamp(),
						),
					).addOnCompleteListener { updateTask ->
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
				"bio" to "",
				"createdAt" to FieldValue.serverTimestamp(),
				"updatedAt" to FieldValue.serverTimestamp(),
			)
			reference.set(profile).addOnCompleteListener { writeTask ->
				onComplete(if (writeTask.isSuccessful) null else writeTask.exception.toUserMessage())
			}
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
		is FirebaseAuthWeakPasswordException -> "密码强度不足，请使用至少 6 位密码"
		is FirebaseAuthUserCollisionException -> "这个邮箱已经注册"
		is FirebaseAuthInvalidUserException,
		is FirebaseAuthInvalidCredentialsException -> "邮箱或密码不正确"
		is FirebaseTooManyRequestsException -> "尝试次数过多，请稍后再试"
		is FirebaseNetworkException -> "网络连接失败，请检查网络后重试"
		else -> this?.localizedMessage ?: "Firebase 请求失败，请稍后重试"
	}
}
