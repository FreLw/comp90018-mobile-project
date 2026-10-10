package com.comp90018.app.features.profile

import androidx.compose.runtime.*
import com.comp90018.app.data.profile.ProfileRepository
import com.comp90018.app.features.profile.edit.EditProfileScreen
import com.comp90018.app.features.profile.overview.ProfileOverview
import com.comp90018.app.features.profile.settings.SettingsScreen

private enum class ProfilePage { Overview, Edit, Settings }

/** Switches profile overview/edit/settings pages and supplies their save/logout actions. */
@Composable
fun ProfileScreen(
    userUid: String,
    userEmail: String,
    repository: ProfileRepository,
    profile: UserProfile?,
    profileError: String?,
    currentRoomId: String?,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    var page by remember { mutableStateOf(ProfilePage.Overview) }

    when {
        page == ProfilePage.Edit && profile != null -> EditProfileScreen(
            userUid = userUid,
            repository = repository,
            profile = profile,
            extras = profile.extras,
            onBack = { page = ProfilePage.Overview },
            onSaved = { page = ProfilePage.Overview },
        )
        page == ProfilePage.Settings && profile != null -> SettingsScreen(
            userUid = userUid,
            repository = repository,
            initialSettings = profile.settings,
            onBack = { page = ProfilePage.Overview },
        )
        else -> ProfileOverview(
            userEmail = userEmail,
            profile = profile,
            profileError = profileError,
            currentRoomId = currentRoomId,
            extras = profile?.extras ?: ProfileExtras(),
            onRetry = onRetry,
            onEdit = { page = ProfilePage.Edit },
            onSettings = { page = ProfilePage.Settings },
            onLogout = onLogout,
        )
    }
}
