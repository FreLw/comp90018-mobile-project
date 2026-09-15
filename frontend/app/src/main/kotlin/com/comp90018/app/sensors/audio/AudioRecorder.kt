package com.comp90018.app.sensors.audio

import android.net.Uri

/**
 * Data boundary for reading microphone input: the recorded speech is saved to a file for
 * upload, while [onAmplitude] reports the live loudness so the UI can show a level meter.
 */
interface AudioRecorder {
    fun startRecording(onAmplitude: (Int) -> Unit, onError: (String) -> Unit)
    fun stopRecording(onResult: (Uri?, String?) -> Unit)
    val isRecording: Boolean
}
