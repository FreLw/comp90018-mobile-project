package com.comp90018.app.features.profile

import android.content.Context

data class ProfileExtras(
    val phone: String = "",
    val department: String = "",
    val major: String = "",
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
        preferences.edit().remove("${uid}_experience").apply()
        return ProfileExtras(
            phone = preferences.getString("${uid}_phone", "").orEmpty(),
            department = preferences.getString("${uid}_department", "").orEmpty(),
            major = preferences.getString("${uid}_major", "").orEmpty(),
        )
    }

    fun saveExtras(context: Context, uid: String, extras: ProfileExtras) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit()
            .putString("${uid}_phone", extras.phone.trim())
            .putString("${uid}_department", extras.department.trim())
            .putString("${uid}_major", extras.major.trim())
            .remove("${uid}_experience")
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
