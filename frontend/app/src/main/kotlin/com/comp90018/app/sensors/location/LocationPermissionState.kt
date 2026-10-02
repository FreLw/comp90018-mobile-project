package com.comp90018.app.sensors.location

enum class LocationPermissionState {
    UNKNOWN,
    PRECISE,
    APPROXIMATE,
    DENIED;

    val isGranted: Boolean
        get() = this == PRECISE || this == APPROXIMATE

    val canUnlockTreasure: Boolean
        get() = this == PRECISE
}
