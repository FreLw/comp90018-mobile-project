package com.comp90018.app.sensors.location

import kotlinx.coroutines.flow.StateFlow

interface LocationSensor {
    val output: StateFlow<LocationOutput>

    fun setTargetLocation(targetLocation: GeoCoordinate?)

    fun start()

    fun stop()
}
