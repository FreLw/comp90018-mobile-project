package com.comp90018.app.sensors.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import com.comp90018.app.sensors.AttitudeProcessor
import com.comp90018.app.sensors.DirectionProcessor
import com.comp90018.app.sensors.RotationProcessor
import com.comp90018.app.sensors.SensorConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Heading comes from TYPE_ROTATION_VECTOR (fused, tilt-compensated); rotation comes from the raw gyroscope. */
class AndroidOrientationSensor(
    context: Context,
    config: SensorConfig = SensorConfig(),
) : OrientationSensor {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val directionProcessor = DirectionProcessor(config)
    private val rotationProcessor = RotationProcessor(config)
    private val attitudeProcessor = AttitudeProcessor(config)

    private val _output = MutableStateFlow(OrientationOutput())
    override val output: StateFlow<OrientationOutput> = _output.asStateFlow()

    private var challengeTargetHeadingDegrees: Double? = null
    private val rawRotationMatrix = FloatArray(9)
    private val displayRotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> onRotationVector(event)
                Sensor.TYPE_GYROSCOPE -> onGyroscope(event)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    override fun setTargetBearing(targetBearingDegrees: Double?) {
        challengeTargetHeadingDegrees = targetBearingDegrees
    }

    override fun start() {
        rotationVectorSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscopeSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun stop() {
        sensorManager.unregisterListener(listener)
        directionProcessor.reset()
        rotationProcessor.reset()
        _output.value = OrientationOutput()
    }

    private fun onRotationVector(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rawRotationMatrix, event.values)
        val (axisX, axisY) = when (currentDisplayRotation()) {
            Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
            Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
            Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
            else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
        }
        if (!SensorManager.remapCoordinateSystem(rawRotationMatrix, axisX, axisY, displayRotationMatrix)) return
        SensorManager.getOrientation(displayRotationMatrix, orientationAngles)
        val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble())
        val pitchDegrees = Math.toDegrees(orientationAngles[1].toDouble())
        val rollDegrees = Math.toDegrees(orientationAngles[2].toDouble())
        val direction = directionProcessor.process(event.timestamp, azimuthDegrees, challengeTargetHeadingDegrees)
        val attitude = attitudeProcessor.process(event.timestamp, pitchDegrees, rollDegrees)
        _output.value = _output.value.copy(direction = direction, attitude = attitude)
    }

    private fun onGyroscope(event: SensorEvent) {
        val rotation = rotationProcessor.process(
            event.timestamp,
            event.values[0].toDouble(),
            event.values[1].toDouble(),
            event.values[2].toDouble(),
        )
        _output.value = _output.value.copy(rotation = rotation)
    }

    @Suppress("DEPRECATION")
    private fun currentDisplayRotation(): Int = windowManager.defaultDisplay.rotation
}
