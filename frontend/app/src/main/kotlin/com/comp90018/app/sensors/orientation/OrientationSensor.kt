package com.comp90018.app.sensors.orientation

import kotlinx.coroutines.flow.StateFlow

interface OrientationSensor {
    val output: StateFlow<OrientationOutput>

    fun setTargetBearing(targetBearingDegrees: Double?)

    fun start()

    fun stop()
}
