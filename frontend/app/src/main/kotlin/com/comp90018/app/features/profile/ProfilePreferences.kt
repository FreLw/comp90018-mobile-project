package com.comp90018.app.features.profile

import android.content.Context

data class ProfileExtras(
    val phone: String = "",
    val department: String = "",
    val major: String = "",
    val experience: Int = 720,
)

data class AppSettings(
    val notifications: Boolean = true,
    val searchable: Boolean = true,
    val soundEffects: Boolean = true,
    val haptics: Boolean = true,
    val preciseLocation: Boolean = true,
)

/** Local-only preferences for fields that are not part of the current Firestore schema. */
object ProfilePreferences {
    private const val FILE_NAME = "profile_ui_preferences"

    fun loadExtras(context: Context, uid: String): ProfileExtras {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return ProfileExtras(
            phone = preferences.getString("${uid}_phone", "").orEmpty(),
            department = preferences.getString("${uid}_department", "").orEmpty(),
            major = preferences.getString("${uid}_major", "").orEmpty(),
            experience = preferences.getInt("${uid}_experience", 720),
        )
    }

    fun saveExtras(context: Context, uid: String, extras: ProfileExtras) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit()
            .putString("${uid}_phone", extras.phone.trim())
            .putString("${uid}_department", extras.department.trim())
            .putString("${uid}_major", extras.major.trim())
            .putInt("${uid}_experience", extras.experience)
            .apply()
    }

    fun loadSettings(context: Context, uid: String): AppSettings {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return AppSettings(
            notifications = preferences.getBoolean("${uid}_notifications", true),
            searchable = preferences.getBoolean("${uid}_searchable", true),
            soundEffects = preferences.getBoolean("${uid}_sound_effects", true),
            haptics = preferences.getBoolean("${uid}_haptics", true),
            preciseLocation = preferences.getBoolean("${uid}_precise_location", true),
        )
    }

    fun saveSettings(context: Context, uid: String, settings: AppSettings) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("${uid}_notifications", settings.notifications)
            .putBoolean("${uid}_searchable", settings.searchable)
            .putBoolean("${uid}_sound_effects", settings.soundEffects)
            .putBoolean("${uid}_haptics", settings.haptics)
            .putBoolean("${uid}_precise_location", settings.preciseLocation)
            .apply()
    }
}

fun levelForExperience(experience: Int): Int = (experience.coerceAtLeast(0) / 500) + 1

fun experienceInCurrentLevel(experience: Int): Int = experience.coerceAtLeast(0) % 500

fun titleForLevel(level: Int): String = when (level) {
    1 -> "New Explorer"
    2 -> "Trail Seeker"
    3 -> "Relic Scout"
    4 -> "Campus Pathfinder"
    else -> "Treasure Master"
}
