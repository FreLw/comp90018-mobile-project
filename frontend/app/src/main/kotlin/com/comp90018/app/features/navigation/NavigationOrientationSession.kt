package com.comp90018.app.features.navigation

import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.orientation.OrientationOutput
import com.comp90018.app.sensors.orientation.OrientationSensor

/** One owner per visible navigation session; repeated lifecycle events do not register twice. */
internal class NavigationOrientationSession(private val sensor: OrientationSensor) {
    val output = sensor.output
    private var running = false

    fun start() {
        if (running) return
        // Navigation compares true-north headings separately; challenge target sensing stays unset.
        sensor.setTargetBearing(null)
        sensor.start()
        running = true
    }

    fun stop() {
        if (!running) return
        sensor.stop()
        running = false
    }
}

internal fun navigationDeviceHeading(
    orientation: OrientationOutput,
    simulatedHeadingDegrees: Double? = null,
): Double? {
    if (simulatedHeadingDegrees != null) return simulatedHeadingDegrees.takeIf { it.isFinite() }
    return orientation.direction.headingDegrees?.takeIf {
        orientation.direction.headingValidity == SensorValidity.VALID && it.isFinite()
    }
}
