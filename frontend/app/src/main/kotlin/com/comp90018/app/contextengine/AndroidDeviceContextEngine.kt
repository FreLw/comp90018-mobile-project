package com.comp90018.app.contextengine

import android.content.Context
import com.comp90018.app.sensors.address.AddressLookup
import com.comp90018.app.sensors.address.AndroidAddressLookup
import com.comp90018.app.sensors.location.AndroidLocationSensor
import com.comp90018.app.sensors.location.GeoCoordinate
import com.comp90018.app.sensors.location.LocationSensor
import com.comp90018.app.sensors.motion.AndroidMotionSensor
import com.comp90018.app.sensors.motion.MotionSensor
import com.comp90018.app.sensors.orientation.AndroidOrientationSensor
import com.comp90018.app.sensors.orientation.OrientationSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Reverse geocoding runs on `scope` whenever the fused location moves, decoupled from the
 * high-frequency location/orientation/motion combine below so a slow network lookup never
 * blocks the sensor stream.
 */
class AndroidDeviceContextEngine(
    context: Context,
    private val scope: CoroutineScope,
    private val locationSensor: LocationSensor = AndroidLocationSensor(context),
    private val orientationSensor: OrientationSensor = AndroidOrientationSensor(context),
    private val motionSensor: MotionSensor = AndroidMotionSensor(context),
    private val addressLookup: AddressLookup = AndroidAddressLookup(context),
) : DeviceContextEngine {
    private val _output = MutableStateFlow(DeviceContextSnapshot())
    override val output: StateFlow<DeviceContextSnapshot> = _output.asStateFlow()

    private var sensorJob: Job? = null
    private var geocodeJob: Job? = null

    override fun setTargetLocation(target: GeoCoordinate?) {
        locationSensor.setTargetLocation(target)
    }

    override fun setChallengeTargetHeading(requiredHeadingDegrees: Double?) {
        orientationSensor.setTargetBearing(requiredHeadingDegrees)
    }

    override fun start() {
        locationSensor.start()
        orientationSensor.start()
        motionSensor.start()

        sensorJob?.cancel()
        sensorJob = combine(
            locationSensor.output,
            orientationSensor.output,
            motionSensor.output,
        ) { location, orientation, motionStability ->
            _output.value.copy(location = location, orientation = orientation, motionStability = motionStability)
        }.onEach { _output.value = it }.launchIn(scope)

        geocodeJob?.cancel()
        geocodeJob = locationSensor.output
            .distinctUntilChangedBy { it.currentLocation }
            .onEach { locationOutput ->
                val coordinate = locationOutput.currentLocation ?: return@onEach
                _output.value = _output.value.copy(address = addressLookup.reverseGeocode(coordinate))
            }.launchIn(scope)
    }

    override fun stop() {
        sensorJob?.cancel()
        geocodeJob?.cancel()
        sensorJob = null
        geocodeJob = null
        locationSensor.stop()
        orientationSensor.stop()
        motionSensor.stop()
        _output.value = DeviceContextSnapshot()
    }
}
