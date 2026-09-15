package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity

data class LocationOutput(
    val currentLocation: GeoCoordinate? = null,
    val targetLocation: GeoCoordinate? = null,
    val distanceToTargetMeters: Double? = null,
    val targetBearingDegrees: Double? = null,
    val proximity: ProximityState = ProximityState.UNKNOWN,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
    val timestampNanos: Long? = null,
)
