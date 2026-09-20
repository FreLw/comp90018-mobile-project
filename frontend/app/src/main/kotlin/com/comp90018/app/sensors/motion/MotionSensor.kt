package com.comp90018.app.sensors.motion

import com.comp90018.app.sensors.MotionStabilityOutput
import kotlinx.coroutines.flow.StateFlow

interface MotionSensor {
    val output: StateFlow<MotionStabilityOutput>

    fun start()

    fun stop()
}
