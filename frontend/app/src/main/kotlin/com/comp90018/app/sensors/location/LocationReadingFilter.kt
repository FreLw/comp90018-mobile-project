package com.comp90018.app.sensors.location

import com.comp90018.app.sensors.SensorValidity

object LocationReadingFilter {
    fun validity(
        reading: LocationReading?,
        nowNanos: Long,
        config: LocationConfig = LocationConfig(),
    ): SensorValidity {
        if (reading == null) return SensorValidity.UNKNOWN
        if (nowNanos < reading.timestampNanos || !reading.coordinate.isValid()) {
            return SensorValidity.UNRELIABLE
        }
        val accuracy = reading.accuracyMeters
        if (accuracy != null && (!accuracy.isFinite() || accuracy < 0.0 || accuracy > config.maxAcceptedAccuracyMeters)) {
            return SensorValidity.UNRELIABLE
        }
        return if (nowNanos - reading.timestampNanos > config.staleTimeoutNanos) {
            SensorValidity.UNRELIABLE
        } else {
            SensorValidity.VALID
        }
    }

    fun accepted(
        reading: LocationReading?,
        nowNanos: Long,
        config: LocationConfig = LocationConfig(),
    ): LocationReading? = reading.takeIf { validity(it, nowNanos, config) == SensorValidity.VALID }

    private fun GeoCoordinate.isValid(): Boolean =
        latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
}
