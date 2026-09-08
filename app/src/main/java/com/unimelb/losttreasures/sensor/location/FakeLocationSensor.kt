package com.unimelb.losttreasures.sensor.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLocationSensor(
    private val insideRadiusMeters: Float = 20f,
    private val nearbyRadiusMeters: Float = 120f
) : LocationSensor {
    private val _state = MutableStateFlow(LocationState())
    override val state: StateFlow<LocationState> = _state.asStateFlow()

    private var currentLocation: GeoCoordinate? = null
    private var targetLocation: GeoCoordinate? = null
    private var isStarted = false

    override fun setTargetLocation(targetLocation: GeoCoordinate?) {
        this.targetLocation = targetLocation
        refreshState()
    }

    override fun start() {
        isStarted = true
        refreshState()
    }

    override fun stop() {
        isStarted = false
        refreshState()
    }

    fun updateCurrentLocation(currentLocation: GeoCoordinate?) {
        this.currentLocation = currentLocation
        refreshState()
    }

    private fun refreshState() {
        _state.value = if (isStarted) {
            LocationCalculator.buildLocationState(
                currentLocation = currentLocation,
                targetLocation = targetLocation,
                insideRadiusMeters = insideRadiusMeters,
                nearbyRadiusMeters = nearbyRadiusMeters
            )
        } else {
            LocationState(
                currentLocation = currentLocation,
                targetLocation = targetLocation,
                isLocationAvailable = false
            )
        }
    }
}
