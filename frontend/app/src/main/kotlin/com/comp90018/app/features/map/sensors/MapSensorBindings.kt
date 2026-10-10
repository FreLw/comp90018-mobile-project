package com.comp90018.app.features.map.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.comp90018.app.features.map.detail.RelicScannerVisual
import kotlin.math.abs
import kotlin.math.atan2

/** Collects rotation-vector heading for map presentation and unregisters the listener on disposal. */
@Composable
internal fun rememberDeviceHeading(): Float {
    val context = LocalContext.current
    var heading by remember { mutableStateOf(0f) }
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotationMatrix = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        rotationSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return heading
}

internal data class DeviceMotionSample(
    val accelerationMagnitude: Float,
    val tiltDegrees: Float,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
)

/** Reads the accelerometer to drive [RelicScannerVisual]'s MOTION/LEVEL readout with real device motion. */
@Composable
internal fun rememberDeviceMotion(): DeviceMotionSample {
    val context = LocalContext.current
    var motion by remember { mutableStateOf(DeviceMotionSample(0f, 0f)) }
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val magnitude = kotlin.math.sqrt(x * x + y * y + z * z)
                val linearAcceleration = kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH)
                val tiltDegrees = if (magnitude > 0f) {
                    Math.toDegrees(kotlin.math.acos((z / magnitude).coerceIn(-1f, 1f)).toDouble()).toFloat()
                } else {
                    0f
                }
                val pitchDegrees = Math.toDegrees(
                    kotlin.math.atan2(-x.toDouble(), kotlin.math.sqrt((y * y + z * z).toDouble())),
                ).toFloat()
                val rollDegrees = Math.toDegrees(kotlin.math.atan2(y.toDouble(), z.toDouble())).toFloat()
                motion = DeviceMotionSample(linearAcceleration, tiltDegrees, pitchDegrees, rollDegrees)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    return motion
}
