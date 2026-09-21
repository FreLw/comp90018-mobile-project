package com.comp90018.app.sensors.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSoundLevelSensor : SoundLevelSensor {
    private val _output = MutableStateFlow(SoundLevelOutput())
    override val output: StateFlow<SoundLevelOutput> = _output.asStateFlow()

    override fun start() = Unit

    override fun stop() = Unit

    fun emit(output: SoundLevelOutput) {
        _output.value = output
    }
}
