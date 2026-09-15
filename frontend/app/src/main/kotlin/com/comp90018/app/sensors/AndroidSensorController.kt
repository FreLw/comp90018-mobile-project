package com.comp90018.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Magnetic-north heading of the current screen's top edge projected horizontally.
 * No declination/location conversion. The owner calls start/stop with its visible lifecycle.
 * [displayRotation] must return the current screen's Surface rotation, be fast and safe to
 * call on the sensor worker thread (for example, read an owner-maintained volatile value).
 * Calls and callbacks are serialized; no Activity or ViewModel is created or owned here.
 */
class AndroidSensorController(
    context: Context,
    private val displayRotation: () -> Int,
    private val config: SensorConfig = SensorConfig(),
) {
    private val manager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val lock = Any()
    private val mutableState = MutableStateFlow(SensorState())
    val state: StateFlow<SensorState> = mutableState.asStateFlow()
    private var session: Session? = null
    private var target: Double? = null
    private var tolerance = SensorConfig.DEFAULT_ALIGNMENT_TOLERANCE_DEGREES

    fun start() = synchronized(lock) {
        if (session != null) return@synchronized
        val next = Session()
        session = next
        try {
            next.start()
        } catch (failure: RuntimeException) {
            session = null
            next.stop()
            mutableState.value = SensorState()
            throw failure
        }
    }

    fun stop() = synchronized(lock) {
        val old = session
        session = null // Discard queued callbacks from this session, including after a restart.
        old?.stop()
        mutableState.value = SensorState()
    }

    /** Null clears the target. Bearings must use magnetic north, matching the heading. */
    fun setTargetBearing(
        degrees: Double?,
        toleranceDegrees: Double = SensorConfig.DEFAULT_ALIGNMENT_TOLERANCE_DEGREES,
    ) = synchronized(lock) {
        require(degrees == null || degrees.isFinite())
        require(toleranceDegrees.isFinite() && toleranceDegrees in 0.0..180.0)
        target = degrees?.let(DirectionProcessor::normalize)
        tolerance = toleranceDegrees
        session?.checkFreshness()
        val old = mutableState.value.direction
        if (old.headingValidity == SensorValidity.VALID) {
            mutableState.value = mutableState.value.copy(direction = DirectionProcessor.evaluate(
                old.headingDegrees, target, tolerance,
            ).copy(timestampNanos = old.timestampNanos))
        }
    }

    private inner class Session : SensorEventListener {
        private val thread = HandlerThread("SensorController")
        private lateinit var handler: Handler
        private val motionProcessor = MotionStabilityProcessor(config)
        private val directionProcessor = DirectionProcessor(config)
        private val registered = mutableMapOf<Int, Sensor>()
        private val accuracy = mutableMapOf<Int, Int>()
        private val times = mutableMapOf<Int, Long>()
        private val gravity = FloatArray(3)
        private val magnetic = FloatArray(3)
        private val matrix = FloatArray(9)
        private val screenMatrix = FloatArray(9)
        private var verticalType: Int? = null
        private var rotationVector = false
        private var directionAvailable = false
        private var rotation: Int? = null
        private var lastDirectionTime: Long? = null
        private val staleCheck = object : Runnable {
            override fun run() = synchronized(lock) {
                if (session !== this@Session) return@synchronized
                checkFreshness()
                handler.postDelayed(this, config.staleCheckIntervalMillis)
                Unit
            }
        }

        fun start() {
            thread.start()
            handler = Handler(thread.looper)
            val accelerometer = register(Sensor.TYPE_ACCELEROMETER)
            rotationVector = register(Sensor.TYPE_ROTATION_VECTOR)
            if (rotationVector) directionAvailable = true else {
                val magnetometer = register(Sensor.TYPE_MAGNETIC_FIELD)
                if (magnetometer) {
                    verticalType = if (register(Sensor.TYPE_GRAVITY)) Sensor.TYPE_GRAVITY
                        else if (accelerometer) Sensor.TYPE_ACCELEROMETER else null
                    directionAvailable = verticalType != null
                    if (!directionAvailable) registered.remove(Sensor.TYPE_MAGNETIC_FIELD)?.let {
                        manager?.unregisterListener(this, it)
                    }
                }
            }
            val motionStatus = if (accelerometer) SensorValidity.WARMING_UP else SensorValidity.UNAVAILABLE
            mutableState.value = SensorState(
                MotionOutput(validity = motionStatus), StabilityOutput(validity = motionStatus),
                directionStatus(if (directionAvailable) SensorValidity.WARMING_UP else SensorValidity.UNAVAILABLE),
            )
            handler.postDelayed(staleCheck, config.staleCheckIntervalMillis)
        }

        private fun register(type: Int): Boolean {
            val sensor = manager?.getDefaultSensor(type) ?: return false
            val success = try {
                manager.registerListener(this, sensor, config.samplingPeriodMicros, handler)
            } catch (_: SecurityException) { false }
            if (success) registered[type] = sensor
            return success
        }

        fun stop() {
            manager?.unregisterListener(this)
            if (::handler.isInitialized) handler.removeCallbacksAndMessages(null)
            thread.quitSafely()
            motionProcessor.reset()
            directionProcessor.reset()
        }

        override fun onAccuracyChanged(sensor: Sensor, value: Int) = synchronized(lock) {
            if (session !== this || sensor.type !in registered) return@synchronized
            val previous = accuracy.put(sensor.type, value)
            if (value <= SensorManager.SENSOR_STATUS_UNRELIABLE ||
                (previous != null && previous <= SensorManager.SENSOR_STATUS_UNRELIABLE)
            ) {
                times.remove(sensor.type)
                if (sensor.type == Sensor.TYPE_ACCELEROMETER) invalidateMotion(
                    if (value <= SensorManager.SENSOR_STATUS_UNRELIABLE) SensorValidity.UNRELIABLE else SensorValidity.WARMING_UP,
                )
                if (isDirectionSensor(sensor.type)) invalidateDirection(
                    if (value <= SensorManager.SENSOR_STATUS_UNRELIABLE) SensorValidity.UNRELIABLE else SensorValidity.WARMING_UP,
                )
            }
        }

        override fun onSensorChanged(event: SensorEvent) = synchronized(lock) {
            if (session !== this || event.sensor.type !in registered) return@synchronized
            val type = event.sensor.type
            val now = SystemClock.elapsedRealtimeNanos()
            val previous = times[type]
            if (event.timestamp < 0 || event.timestamp > now ||
                (previous != null && event.timestamp <= previous)
            ) return@synchronized
            if (now - event.timestamp > config.sampleGapTimeoutNanos) {
                checkFreshness()
                return@synchronized
            }
            onAccuracyChanged(event.sensor, event.accuracy)
            if (event.accuracy <= SensorManager.SENSOR_STATUS_UNRELIABLE) return@synchronized
            val requiredValues = if (type == Sensor.TYPE_ROTATION_VECTOR && event.values.size >= 4) 4 else 3
            if (event.values.size < requiredValues || (0 until requiredValues).any { !event.values[it].isFinite() }) {
                times.remove(type)
                if (type == Sensor.TYPE_ACCELEROMETER) invalidateMotion(SensorValidity.UNRELIABLE)
                if (isDirectionSensor(type)) invalidateDirection(SensorValidity.UNRELIABLE)
                return@synchronized
            }
            times[type] = event.timestamp
            if (type == Sensor.TYPE_ACCELEROMETER) {
                val output = motionProcessor.process(event.timestamp,
                    event.values[0].toDouble(), event.values[1].toDouble(), event.values[2].toDouble())
                mutableState.value = mutableState.value.copy(motion = output.motion, stability = output.stability)
            }
            if (!isDirectionSensor(type)) return@synchronized
            if (rotationVector) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
            } else {
                event.values.copyInto(if (type == Sensor.TYPE_MAGNETIC_FIELD) magnetic else gravity, endIndex = 3)
                if (!directionInputsFresh(now)) {
                    checkFreshness()
                    return@synchronized
                }
                if (!SensorManager.getRotationMatrix(matrix, null, gravity, magnetic)) {
                    invalidateDirection(SensorValidity.UNRELIABLE)
                    return@synchronized
                }
            }
            val last = lastDirectionTime
            if (last != null && event.timestamp <= last) return@synchronized
            updateRotation()
            val axisX: Int
            val axisY: Int
            when (rotation) {
                Surface.ROTATION_0 -> { axisX = SensorManager.AXIS_X; axisY = SensorManager.AXIS_Y }
                Surface.ROTATION_90 -> { axisX = SensorManager.AXIS_Y; axisY = SensorManager.AXIS_MINUS_X }
                Surface.ROTATION_180 -> { axisX = SensorManager.AXIS_MINUS_X; axisY = SensorManager.AXIS_MINUS_Y }
                Surface.ROTATION_270 -> { axisX = SensorManager.AXIS_MINUS_Y; axisY = SensorManager.AXIS_X }
                else -> { invalidateDirection(SensorValidity.UNRELIABLE); return@synchronized }
            }
            if (!SensorManager.remapCoordinateSystem(matrix, axisX, axisY, screenMatrix) ||
                screenMatrix.any { !it.isFinite() } ||
                hypot(screenMatrix[1].toDouble(), screenMatrix[4].toDouble()) < config.minimumHeadingProjection
            ) {
                invalidateDirection(SensorValidity.UNRELIABLE)
                return@synchronized
            }
            // Column Y is the screen top edge in world coordinates: east, north, up.
            val heading = DirectionProcessor.normalize(Math.toDegrees(atan2(
                screenMatrix[1].toDouble(), screenMatrix[4].toDouble(),
            )))
            val output = directionProcessor.process(event.timestamp, heading, target, tolerance)
            lastDirectionTime = event.timestamp
            mutableState.value = mutableState.value.copy(direction = output)
        }

        private fun isDirectionSensor(type: Int) = directionAvailable &&
            if (rotationVector) type == Sensor.TYPE_ROTATION_VECTOR
            else type == verticalType || type == Sensor.TYPE_MAGNETIC_FIELD

        private fun fresh(type: Int, now: Long): Boolean = times[type]?.let {
            now - it <= config.sampleGapTimeoutNanos &&
                (accuracy[type] ?: SensorManager.SENSOR_STATUS_UNRELIABLE) > SensorManager.SENSOR_STATUS_UNRELIABLE
        } ?: false

        private fun directionInputsFresh(now: Long) = if (rotationVector) fresh(Sensor.TYPE_ROTATION_VECTOR, now)
            else verticalType?.let { fresh(it, now) && fresh(Sensor.TYPE_MAGNETIC_FIELD, now) } ?: false

        private fun updateRotation() {
            val current = displayRotation()
            if (rotation != null && rotation != current) invalidateDirection(SensorValidity.WARMING_UP)
            rotation = current
        }

        fun checkFreshness() {
            val now = SystemClock.elapsedRealtimeNanos()
            if (directionAvailable) updateRotation()
            val current = mutableState.value
            if (current.motion.timestampNanos != null && !fresh(Sensor.TYPE_ACCELEROMETER, now)) {
                invalidateMotion(SensorValidity.STALE)
            }
            if (current.direction.timestampNanos != null && !directionInputsFresh(now)) {
                invalidateDirection(SensorValidity.STALE)
            }
        }

        private fun invalidateMotion(status: SensorValidity) {
            motionProcessor.reset()
            mutableState.value = mutableState.value.copy(
                motion = MotionOutput(validity = status), stability = StabilityOutput(validity = status),
            )
        }

        private fun invalidateDirection(status: SensorValidity) {
            directionProcessor.reset()
            lastDirectionTime = null
            mutableState.value = mutableState.value.copy(direction = directionStatus(status))
        }

        private fun directionStatus(status: SensorValidity) = DirectionOutput(
            headingValidity = status, comparisonValidity = status,
        )
    }
}
