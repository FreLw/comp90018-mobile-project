package com.comp90018.app.features.friends

import com.comp90018.app.data.social.FriendSummary

internal val FriendSummary.displayLabel: String
    get() = displayName.ifBlank { username }

/** Excludes legacy demo contacts from persisted friends and chat previews. */
internal fun isLegacyDemoContact(uid: String): Boolean = uid.startsWith("starter_contact_")

fun genderLabel(gender: String): String = when (gender) {
    "male" -> "Male"
    "female" -> "Female"
    "prefer_not_to_say" -> "Prefer not to say"
    else -> "Not specified"
}
