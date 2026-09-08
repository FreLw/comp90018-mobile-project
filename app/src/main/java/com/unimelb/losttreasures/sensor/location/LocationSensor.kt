package com.unimelb.losttreasures.sensor.location

import kotlinx.coroutines.flow.StateFlow

interface LocationSensor {
    val state: StateFlow<LocationState>

    fun setTargetLocation(targetLocation: GeoCoordinate?)

    fun start()

    fun stop()
}
