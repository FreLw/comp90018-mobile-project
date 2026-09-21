package com.comp90018.app.contextengine

import com.comp90018.app.sensors.location.GeoCoordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDeviceContextEngine : DeviceContextEngine {
    private val _output = MutableStateFlow(DeviceContextSnapshot())
    override val output: StateFlow<DeviceContextSnapshot> = _output.asStateFlow()

    override fun setTargetLocation(target: GeoCoordinate?) = Unit

    override fun start() = Unit

    override fun stop() = Unit

    fun emit(snapshot: DeviceContextSnapshot) {
        _output.value = snapshot
    }
}
