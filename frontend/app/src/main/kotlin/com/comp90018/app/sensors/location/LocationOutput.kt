package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity

data class LocationOutput(
    val currentLocation: GeoCoordinate? = null,
    val targetLocation: GeoCoordinate? = null,
    val distanceToTargetMeters: Double? = null,
    val targetBearingDegrees: Double? = null,
    val proximity: ProximityState = ProximityState.UNKNOWN,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
    val permission: LocationPermissionState = LocationPermissionState.UNKNOWN,
    val availability: LocationAvailabilityState = LocationAvailabilityState.UNKNOWN,
    val accuracyMeters: Double? = null,
    val timestampNanos: Long? = null,
)
