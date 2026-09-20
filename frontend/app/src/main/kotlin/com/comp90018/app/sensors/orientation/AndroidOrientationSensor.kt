package com.comp90018.app.sensors.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val directionProcessor = DirectionProcessor(config)
    private val rotationProcessor = RotationProcessor(config)

    private val _output = MutableStateFlow(OrientationOutput())
    override val output: StateFlow<OrientationOutput> = _output.asStateFlow()

    private var targetBearingDegrees: Double? = null
    private val rotationMatrix = FloatArray(9)
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
        this.targetBearingDegrees = targetBearingDegrees
    }

    override fun start() {
        rotationVectorSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroscopeSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun stop() {
        sensorManager.unregisterListener(listener)
        directionProcessor.reset()
        rotationProcessor.reset()
    }

    private fun onRotationVector(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val azimuthDegrees = Math.toDegrees(orientationAngles[0].toDouble())
        val direction = directionProcessor.process(event.timestamp, azimuthDegrees, targetBearingDegrees)
        _output.value = _output.value.copy(direction = direction)
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
}
