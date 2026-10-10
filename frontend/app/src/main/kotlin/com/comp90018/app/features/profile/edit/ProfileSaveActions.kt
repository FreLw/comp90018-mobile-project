package com.comp90018.app.features.profile.edit

import android.net.Uri
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.features.profile.ProfileExtras


/** Uploads an optional new avatar before saving profile details, preserving the existing URL otherwise. */
internal fun saveProfileEdits(
    repository: ProfileRepository,
    userUid: String,
    username: String,
    readGender: () -> String,
    readBio: () -> String,
    extras: ProfileExtras,
    selectedAvatar: Uri?,
    existingAvatarUrl: String,
    onComplete: (String?) -> Unit,
) {
    // The original form reads these fields after an avatar upload finishes, even if
    // the explorer edits them while uploading. Keep that timing instead of taking a snapshot.
    val saveDetails: (String?) -> Unit = { avatarUrl ->
        repository.updateProfile(userUid, username, readGender(), readBio(), extras, avatarUrl, onComplete)
    }
    selectedAvatar?.let { uri ->
        repository.uploadAvatar(userUid, uri) { url, error ->
            if (url == null) onComplete(error ?: "Unable to upload avatar")
            else saveDetails(url)
        }
    } ?: saveDetails(existingAvatarUrl.takeIf { it.isNotBlank() })
}
