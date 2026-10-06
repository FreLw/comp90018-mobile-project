package com.comp90018.app.features.haptics

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/** minSdk 26 supports waveforms; unsupported hardware is a safe no-op. */
class AndroidTreasureHapticDriver(context: Context) : TreasureHapticDriver {
    private val vibrator = context.applicationContext.getSystemService(Vibrator::class.java)

    override fun play(event: TreasureHapticEvent) {
        runCatching {
            val device = vibrator ?: return
            if (!device.hasVibrator()) return
            val pattern = TreasureHapticPattern.forEvent(event, device.hasAmplitudeControl())
            device.vibrate(VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1))
        }
    }

    fun cancel() { runCatching { vibrator?.cancel() } }
}
