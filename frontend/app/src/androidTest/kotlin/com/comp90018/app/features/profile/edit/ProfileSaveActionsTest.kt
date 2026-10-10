package com.comp90018.app.features.profile.edit

import android.net.Uri
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.profile.AppSettings
import com.comp90018.app.features.profile.ProfileExtras
import com.comp90018.app.features.profile.UserProfile
import org.junit.Assert.*
import org.junit.Test

/** Verifies that moving save orchestration out of the form preserves the persistence contract. */
class ProfileSaveActionsTest {
    @Test fun unchangedAvatarKeepsItsUrlAndWaitsForTheProfileWrite() {
        val repository = RecordingProfileRepository()
        var callbacks = 0
        save(repository, selected = null, existing = "https://example.test/avatar") { callbacks++ }
        assertEquals(listOf("profile"), repository.operations)
        assertEquals("https://example.test/avatar", repository.avatarUrl)
        assertEquals(0, callbacks)
        repository.profileCompletion(null)
        assertEquals(1, callbacks)
    }

    @Test fun newAvatarMustFinishUploadingBeforeAnyProfileWrite() {
        val repository = RecordingProfileRepository()
        var callbacks = 0
        save(repository, selected = Uri.parse("content://photos/new")) { callbacks++ }
        assertEquals(listOf("upload"), repository.operations)
        assertEquals(0, callbacks)
        repository.uploadCompletion("https://example.test/new-avatar", null)
        assertEquals(listOf("upload", "profile"), repository.operations)
        assertEquals("https://example.test/new-avatar", repository.avatarUrl)
        assertEquals(0, callbacks)
        repository.profileCompletion(null)
        assertEquals(1, callbacks)
    }

    @Test fun failedAvatarUploadDoesNotSaveDetailsOrDiscardItsError() {
        val repository = RecordingProfileRepository()
        var result: String? = null
        save(repository, selected = Uri.parse("content://photos/new")) { result = it }
        repository.uploadCompletion(null, "Upload failed")
        assertEquals(listOf("upload"), repository.operations)
        assertEquals("Upload failed", result)
    }

    @Test fun editsMadeDuringAvatarUploadAreReadWhenProfileDetailsAreSaved() {
        val repository = RecordingProfileRepository()
        var gender = "unspecified"
        var bio = "Before upload"
        saveProfileEdits(
            repository, "explorer", "name", { gender }, { bio }, ProfileExtras(),
            Uri.parse("content://photos/new"), "",
        ) {}
        gender = "female"
        bio = "Edited during upload"
        repository.uploadCompletion("https://example.test/new-avatar", null)
        assertEquals("female", repository.savedGender)
        assertEquals("Edited during upload", repository.savedBio)
    }

    @Test fun blankAvatarAndMissingUploadErrorKeepTheirExistingFallbacks() {
        val repository = RecordingProfileRepository()
        save(repository, selected = null, existing = " ") {}
        assertNull(repository.avatarUrl)
        var result: String? = null
        save(repository, selected = Uri.parse("content://photos/new")) { result = it }
        repository.uploadCompletion(null, null)
        assertEquals("Unable to upload avatar", result)
        assertEquals(listOf("profile", "upload"), repository.operations)
    }

    private fun save(repository: ProfileRepository, selected: Uri?, existing: String = "", complete: (String?) -> Unit) =
        saveProfileEdits(repository, "explorer", "name", { "unspecified" }, { "bio" }, ProfileExtras(), selected, existing, complete)

    private class RecordingProfileRepository : ProfileRepository {
        val operations = mutableListOf<String>()
        var avatarUrl: String? = null
        var savedGender: String? = null
        var savedBio: String? = null
        lateinit var profileCompletion: (String?) -> Unit
        lateinit var uploadCompletion: (String?, String?) -> Unit
        override fun updateProfile(uid: String, username: String, gender: String, bio: String, extras: ProfileExtras, avatarUrl: String?, onComplete: (String?) -> Unit) {
            operations += "profile"
            this.avatarUrl = avatarUrl
            savedGender = gender
            savedBio = bio
            profileCompletion = onComplete
        }
        override fun uploadAvatar(uid: String, avatarUri: Uri, onComplete: (String?, String?) -> Unit) {
            operations += "upload"
            uploadCompletion = onComplete
        }
        override fun observeProfile(uid: String, onChange: (UserProfile?, String?) -> Unit) = Subscription {}
        override fun ensureProfile(uid: String, email: String, onComplete: (String?) -> Unit) = Unit
        override fun updateSettings(uid: String, settings: AppSettings, onComplete: (String?) -> Unit) = Unit
    }
}
