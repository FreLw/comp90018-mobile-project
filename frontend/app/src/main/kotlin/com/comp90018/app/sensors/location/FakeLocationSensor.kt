package com.comp90018.app.sensors.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLocationSensor(
    config: LocationConfig = LocationConfig(),
) : LocationSensor {
    private val _output = MutableStateFlow(LocationOutput())
    override val output: StateFlow<LocationOutput> = _output.asStateFlow()

    private var currentLocation: GeoCoordinate? = null
    private var activeConfig = config
    private var targetLocation: GeoCoordinate? = null
    private var timestampNanos: Long? = null
    private var accuracyMeters: Double? = null
    private var started = false

    override fun setTargetLocation(targetLocation: GeoCoordinate?) {
        this.targetLocation = targetLocation
        refreshOutput()
    }

    override fun setTargetLocation(
        targetLocation: GeoCoordinate?,
        insideRadiusMeters: Double,
        nearbyRadiusMeters: Double,
    ) {
        activeConfig = activeConfig.copy(
            insideRadiusMeters = insideRadiusMeters,
            nearbyRadiusMeters = nearbyRadiusMeters,
        )
        setTargetLocation(targetLocation)
    }

    override fun start() {
        started = true
        refreshOutput()
    }

    override fun stop() {
        started = false
        refreshOutput()
    }

    fun updateCurrentLocation(
        currentLocation: GeoCoordinate?,
        timestampNanos: Long? = null,
        accuracyMeters: Double? = null,
    ) {
        this.currentLocation = currentLocation
        this.timestampNanos = timestampNanos
        this.accuracyMeters = accuracyMeters
        refreshOutput()
    }

    private fun refreshOutput() {
        _output.value = if (started) {
            LocationCalculator.buildOutput(
                currentLocation = currentLocation,
                targetLocation = targetLocation,
                timestampNanos = timestampNanos,
                config = activeConfig,
                permission = LocationPermissionState.GRANTED,
                availability = LocationAvailabilityState.AVAILABLE,
                accuracyMeters = accuracyMeters,
            )
        } else {
            LocationOutput(
                currentLocation = currentLocation,
                targetLocation = targetLocation,
                permission = LocationPermissionState.GRANTED,
                availability = LocationAvailabilityState.UNKNOWN,
                accuracyMeters = accuracyMeters,
                timestampNanos = timestampNanos,
            )
        }
    }
}
