package com.comp90018.app.sensors.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.comp90018.app.sensors.MotionStabilityOutput
import com.comp90018.app.sensors.MotionStabilityProcessor
import com.comp90018.app.sensors.SensorConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidMotionSensor(
    context: Context,
    config: SensorConfig = SensorConfig(),
) : MotionSensor {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val processor = MotionStabilityProcessor(config)

    private val _output = MutableStateFlow(MotionStabilityOutput())
    override val output: StateFlow<MotionStabilityOutput> = _output.asStateFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            _output.value = processor.process(
                event.timestamp,
                event.values[0].toDouble(),
                event.values[1].toDouble(),
                event.values[2].toDouble(),
            )
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    override fun start() {
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun stop() {
        sensorManager.unregisterListener(listener)
        processor.reset()
    }
}
