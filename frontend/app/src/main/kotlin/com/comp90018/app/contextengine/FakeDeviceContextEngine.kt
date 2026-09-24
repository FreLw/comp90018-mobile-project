package com.comp90018.app.contextengine

import com.comp90018.app.sensors.location.GeoCoordinate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDeviceContextEngine : DeviceContextEngine {
    private val _output = MutableStateFlow(DeviceContextSnapshot())
    override val output: StateFlow<DeviceContextSnapshot> = _output.asStateFlow()

    var targetLocation: GeoCoordinate? = null
        private set
    var challengeTargetHeadingDegrees: Double? = null
        private set
    var isStarted: Boolean = false
        private set
    var startCount: Int = 0
        private set
    var stopCount: Int = 0
        private set

    override fun setTargetLocation(target: GeoCoordinate?) {
        targetLocation = target
    }

    override fun setChallengeTargetHeading(requiredHeadingDegrees: Double?) {
        challengeTargetHeadingDegrees = requiredHeadingDegrees
    }

    override fun start() {
        isStarted = true
        startCount++
    }

    override fun stop() {
        isStarted = false
        stopCount++
        _output.value = DeviceContextSnapshot()
    }

    fun emit(snapshot: DeviceContextSnapshot) {
        _output.value = snapshot
    }
}
