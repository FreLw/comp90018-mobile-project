package com.comp90018.app.sensors.audio

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [MediaRecorder]-backed [AudioRecorder]. Saves clips to the app's cache directory as AAC/M4A
 * and polls [MediaRecorder.getMaxAmplitude] on a timer to report loudness while recording.
 */
class MediaRecorderAudioRecorder(private val context: Context) : AudioRecorder {
    private val amplitudePollMs = 150L
    private val mainHandler = Handler(Looper.getMainLooper())

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var amplitudePoller: Runnable? = null

    override var isRecording: Boolean = false
        private set

    override fun startRecording(onAmplitude: (Int) -> Unit, onError: (String) -> Unit) {
        if (isRecording) return
        val file = File(context.cacheDir, "SPEECH_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".m4a")
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            outputFile = file
            isRecording = true
            startAmplitudePolling(onAmplitude)
        } catch (error: Exception) {
            newRecorder.release()
            onError(error.message ?: "Unable to start recording")
        }
    }

    override fun stopRecording(onResult: (Uri?, String?) -> Unit) {
        val active = recorder ?: run { onResult(null, "Not recording"); return }
        stopAmplitudePolling()
        try {
            active.stop()
            active.release()
            recorder = null
            isRecording = false
            onResult(Uri.fromFile(outputFile), null)
        } catch (error: Exception) {
            recorder = null
            isRecording = false
            onResult(null, error.message ?: "Unable to stop recording")
        }
    }

    private fun startAmplitudePolling(onAmplitude: (Int) -> Unit) {
        val poller = object : Runnable {
            override fun run() {
                val amplitude = recorder?.maxAmplitude ?: return
                onAmplitude(amplitude)
                mainHandler.postDelayed(this, amplitudePollMs)
            }
        }
        amplitudePoller = poller
        mainHandler.postDelayed(poller, amplitudePollMs)
    }

    private fun stopAmplitudePolling() {
        amplitudePoller?.let(mainHandler::removeCallbacks)
        amplitudePoller = null
    }
}
