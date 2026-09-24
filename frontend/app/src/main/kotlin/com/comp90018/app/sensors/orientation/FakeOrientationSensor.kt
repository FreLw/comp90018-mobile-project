package com.comp90018.app.sensors.orientation

import com.comp90018.app.sensors.AttitudeOutput
import com.comp90018.app.sensors.DirectionOutput
import com.comp90018.app.sensors.RotationOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeOrientationSensor : OrientationSensor {
    private val _output = MutableStateFlow(OrientationOutput())
    override val output: StateFlow<OrientationOutput> = _output.asStateFlow()

    override fun setTargetBearing(targetBearingDegrees: Double?) = Unit

    override fun start() = Unit

    override fun stop() {
        _output.value = OrientationOutput()
    }

    fun emitAttitude(attitude: AttitudeOutput) {
        _output.value = _output.value.copy(attitude = attitude)
    }

    fun emitDirection(direction: DirectionOutput) {
        _output.value = _output.value.copy(direction = direction)
    }

    fun emitRotation(rotation: RotationOutput) {
        _output.value = _output.value.copy(rotation = rotation)
    }
}
