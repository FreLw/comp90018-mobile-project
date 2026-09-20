package com.comp90018.app.sensors.audio

import com.comp90018.app.sensors.SensorValidity

data class SoundLevelOutput(
    /** Decibels relative to full scale (dBFS); see [com.comp90018.app.sensors.SoundLevelCalculator]. */
    val decibels: Double? = null,
    val validity: SensorValidity = SensorValidity.UNKNOWN,
    val timestampNanos: Long? = null,
)
