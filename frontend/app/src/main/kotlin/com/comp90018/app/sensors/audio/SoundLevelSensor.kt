package com.comp90018.app.sensors.audio

import kotlinx.coroutines.flow.StateFlow

interface SoundLevelSensor {
    val output: StateFlow<SoundLevelOutput>

    fun start()

    fun stop()
}
