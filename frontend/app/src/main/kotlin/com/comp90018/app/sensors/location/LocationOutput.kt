package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity

data class LocationOutput(
    val currentLocation: GeoCoordinate? = null,
    /** Last raw fix, kept even once [currentLocation] is nulled out for being stale/inaccurate, so the map can fall back to an approximate dot instead of showing nothing. */
    val lastKnownLocation: GeoCoordinate? = currentLocation,
    val targetLocation: GeoCoordinate? = null,
    val distanceToTargetMeters: Double? = null,
    val targetBearingDegrees: Double? = null,
    val proximity: ProximityState = ProximityState.UNKNOWN,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
    val permission: LocationPermissionState = LocationPermissionState.UNKNOWN,
    val availability: LocationAvailabilityState = LocationAvailabilityState.UNKNOWN,
    val accuracyMeters: Double? = null,
    val timestampNanos: Long? = null,
    /** Provider metadata for consumers that must exclude mock GPS events. */
    val isMock: Boolean = false,
)
