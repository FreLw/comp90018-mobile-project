package com.comp90018.app.sensors.location

import kotlinx.coroutines.flow.StateFlow

interface LocationSensor {
    val output: StateFlow<LocationOutput>

    fun setTargetLocation(targetLocation: GeoCoordinate?)

    fun setTargetLocation(
        targetLocation: GeoCoordinate?,
        insideRadiusMeters: Double,
        nearbyRadiusMeters: Double,
    ) {
        setTargetLocation(targetLocation)
    }

    fun start()

    fun stop()
}
