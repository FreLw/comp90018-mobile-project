package com.unimelb.losttreasures.sensor.location

data class LocationState(
    val currentLocation: GeoCoordinate? = null,
    val targetLocation: GeoCoordinate? = null,
    val distanceToTargetMeters: Float? = null,
    val targetBearingDegrees: Float? = null,
    val proximityState: ProximityState = ProximityState.Unknown,
    val isLocationAvailable: Boolean = false
)
