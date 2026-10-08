package com.comp90018.app

import android.net.Uri
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.profile.AppSettings
import com.comp90018.app.features.profile.ProfileExtras
import com.comp90018.app.features.profile.UserProfile
import org.junit.Assert.*
import org.junit.Test

class AppShellViewModelTest {
    @Test
    fun newAccountStartsProfileInitializationWithoutWaitingForASnapshot() {
        val repository = SessionProfileRepository()

        AppShellViewModel(repository, "new-user", "explorer@example.com")

        assertEquals(listOf("new-user" to "explorer@example.com"), repository.initializations)
        assertEquals(1, repository.subscriptions)
    }

    @Test
    fun profileSnapshotsDoNotStartDuplicateInitializationRequests() {
        val repository = SessionProfileRepository()
        AppShellViewModel(repository, "user", "explorer@example.com")

        repository.emit(null, null)
        repository.emit(null, null)
        repository.completeInitialization(null)
        repository.emit(null, null)

        assertEquals(1, repository.initializations.size)
    }

    @Test
    fun successfulInitializationDoesNotHideAFailedProfileListener() {
        val repository = SessionProfileRepository()
        val viewModel = AppShellViewModel(repository, "user", "explorer@example.com")

        repository.emit(null, "Permission denied")
        repository.completeInitialization(null)

        assertEquals("Permission denied", viewModel.uiState.value.profileError)
    }

    @Test
    fun cachedSnapshotDoesNotHideProfileCreationFailure() {
        val repository = SessionProfileRepository()
        val viewModel = AppShellViewModel(repository, "user", "explorer@example.com")

        repository.completeInitialization("Unable to create profile")
        repository.emit(null, null)

        assertEquals("Unable to create profile", viewModel.uiState.value.profileError)
    }

    @Test
    fun retryReplacesAFailedProfileListenerAndReinitializesTheProfile() {
        val repository = SessionProfileRepository()
        val viewModel = AppShellViewModel(repository, "user", "explorer@example.com")
        repository.emit(null, "Permission denied")
        repository.completeInitialization("Unable to create profile")

        viewModel.retry()
        repository.emit(null, null)
        repository.completeInitialization(null)

        assertEquals(1, repository.cancellations)
        assertEquals(2, repository.subscriptions)
        assertEquals(2, repository.initializations.size)
        assertNull(viewModel.uiState.value.profileError)
    }
}

private class SessionProfileRepository : ProfileRepository {
    val initializations = mutableListOf<Pair<String, String>>()
    var subscriptions = 0
    var cancellations = 0
    private var listener: ((UserProfile?, String?) -> Unit)? = null
    private var initializationComplete: ((String?) -> Unit)? = null

    override fun observeProfile(uid: String, onChange: (UserProfile?, String?) -> Unit): Subscription {
        subscriptions++
        listener = onChange
        return Subscription {
            cancellations++
            listener = null
        }
    }

    override fun ensureProfile(uid: String, email: String, onComplete: (String?) -> Unit) {
        initializations += uid to email
        initializationComplete = onComplete
    }

    fun emit(profile: UserProfile?, error: String?) {
        listener?.invoke(profile, error)
    }

    fun completeInitialization(error: String?) {
        val callback = initializationComplete
        initializationComplete = null
        callback?.invoke(error)
    }

    override fun updateProfile(
        uid: String,
        username: String,
        gender: String,
        bio: String,
        extras: ProfileExtras,
        avatarUrl: String?,
        onComplete: (String?) -> Unit,
    ) = error("Not used")

    override fun updateSettings(uid: String, settings: AppSettings, onComplete: (String?) -> Unit) = error("Not used")
    override fun uploadAvatar(uid: String, avatarUri: Uri, onComplete: (String?, String?) -> Unit) = error("Not used")
}
