package com.comp90018.app.features.friends

fun genderLabel(gender: String): String = when (gender) {
    "male" -> "Male"
    "female" -> "Female"
    "prefer_not_to_say" -> "Prefer not to say"
    else -> "Not specified"
}

internal val com.comp90018.app.data.social.FriendSummary.displayLabel: String
    get() = displayName.ifBlank { username }

/** Hides obsolete demo IDs that may remain in locally cached chat summaries. */
internal val com.comp90018.app.data.social.FriendSummary.isStarterContact: Boolean
    get() = uid.startsWith("starter_contact_")
