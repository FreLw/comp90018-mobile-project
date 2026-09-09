package com.comp90018.app.features.friends

fun genderLabel(gender: String): String = when (gender) {
    "male" -> "Male"
    "female" -> "Female"
    "prefer_not_to_say" -> "Prefer not to say"
    else -> "Not specified"
}
