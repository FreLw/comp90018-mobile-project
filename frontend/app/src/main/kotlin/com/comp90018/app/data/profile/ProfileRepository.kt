package com.comp90018.app.data.profile

import android.net.Uri
import com.comp90018.app.data.social.Subscription
import com.comp90018.app.features.profile.UserProfile

/** Data boundary for reading user profiles. */
interface ProfileRepository {
    fun observeProfile(uid: String, onChange: (UserProfile?, String?) -> Unit): Subscription
    fun ensureProfile(uid: String, email: String, onComplete: (String?) -> Unit)
    fun updateProfile(uid: String, username: String, gender: String, bio: String, avatarUrl: String?, onComplete: (String?) -> Unit)
    fun uploadAvatar(uid: String, avatarUri: Uri, onComplete: (String?, String?) -> Unit)
}
