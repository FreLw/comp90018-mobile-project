package com.comp90018.app.contextengine

import com.comp90018.app.sensors.location.GeoCoordinate
import kotlinx.coroutines.flow.StateFlow

/** Aggregates location, orientation and motion sensors into one observable device-context snapshot. */
interface DeviceContextEngine {
    val output: StateFlow<DeviceContextSnapshot>

    fun setTargetLocation(target: GeoCoordinate?)

    /** Sets the fixed treasure viewing heading; this is not the location navigation bearing. */
    fun setChallengeTargetHeading(requiredHeadingDegrees: Double?)

    fun start()

    fun stop()
}
