package com.comp90018.app.features.haptics

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/** minSdk 26 supports waveforms; unsupported hardware is a safe no-op. */
class AndroidTreasureHapticDriver(context: Context) : TreasureHapticDriver {
    private val driver = SafeTreasureHapticDriver(
        context.applicationContext.getSystemService(Vibrator::class.java)?.let { vibrator ->
            object : TreasureVibrationDevice {
                override fun hasVibrator() = vibrator.hasVibrator()
                override fun hasAmplitudeControl() = vibrator.hasAmplitudeControl()
                override fun vibrate(pattern: TreasureHapticPattern) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern.timings, pattern.amplitudes, -1))
                }
                override fun cancel() = vibrator.cancel()
            }
        },
    )
    override fun play(event: TreasureHapticEvent) = driver.play(event)
    fun cancel() = driver.cancel()
}
