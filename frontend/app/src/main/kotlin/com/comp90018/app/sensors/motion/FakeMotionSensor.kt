package com.comp90018.app.sensors.motion

import com.comp90018.app.sensors.MotionStabilityOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMotionSensor : MotionSensor {
    private val _output = MutableStateFlow(MotionStabilityOutput())
    override val output: StateFlow<MotionStabilityOutput> = _output.asStateFlow()

    override fun start() = Unit

    override fun stop() {
        _output.value = MotionStabilityOutput()
    }

    fun emit(output: MotionStabilityOutput) {
        _output.value = output
    }
}
