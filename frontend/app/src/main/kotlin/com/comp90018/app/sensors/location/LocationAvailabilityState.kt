package com.comp90018.app.sensors.location

enum class LocationAvailabilityState {
    UNKNOWN,
    AVAILABLE,
    UNAVAILABLE,
    /** The last reading went stale and an active getCurrentLocation() retry is in flight. */
    RECOVERING,
    /** A reading was once available but has gone stale and could not be refreshed after retrying. */
    EXPIRED,
}
