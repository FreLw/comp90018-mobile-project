package com.comp90018.app.features.friends

/*
 * Formats names and profile labels used by friend-related screens.
 * Also identifies legacy starter contacts so they can be excluded from live friend and chat lists.
 */

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
