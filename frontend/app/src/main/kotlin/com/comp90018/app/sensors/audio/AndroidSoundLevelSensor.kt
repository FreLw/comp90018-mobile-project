package com.comp90018.app.sensors.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.comp90018.app.sensors.SensorValidity
import com.comp90018.app.sensors.SoundLevelCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reads raw PCM from the mic on a dedicated thread and reports smoothed dBFS loudness. */
class AndroidSoundLevelSensor(
    private val context: Context,
    private val sampleRateHz: Int = 44_100,
) : SoundLevelSensor {
    private val _output = MutableStateFlow(SoundLevelOutput())
    override val output: StateFlow<SoundLevelOutput> = _output.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    @Volatile
    private var running = false

    @SuppressLint("MissingPermission")
    override fun start() {
        if (running) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            _output.value = SoundLevelOutput(validity = SensorValidity.UNRELIABLE)
            return
        }
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) {
            _output.value = SoundLevelOutput(validity = SensorValidity.UNRELIABLE)
            return
        }
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2,
            )
        } catch (error: SecurityException) {
            null
        }
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            _output.value = SoundLevelOutput(validity = SensorValidity.UNRELIABLE)
            return
        }
        audioRecord = record
        running = true
        record.startRecording()
        recordingThread = Thread(ReadLoop(record, minBufferSize), "SoundLevelSensor").apply { start() }
    }

    override fun stop() {
        running = false
        recordingThread?.join(RECORDING_THREAD_JOIN_TIMEOUT_MILLIS)
        recordingThread = null
        audioRecord?.apply {
            stop()
            release()
        }
        audioRecord = null
    }

    private inner class ReadLoop(private val record: AudioRecord, private val bufferSize: Int) : Runnable {
        override fun run() {
            val buffer = ShortArray(bufferSize)
            while (running) {
                val read = record.read(buffer, 0, bufferSize)
                if (read <= 0) continue
                _output.value = SoundLevelOutput(
                    decibels = SoundLevelCalculator.decibelsFullScale(buffer, read),
                    validity = SensorValidity.VALID,
                    timestampNanos = SystemClock.elapsedRealtimeNanos(),
                )
            }
        }
    }

    private companion object {
        const val RECORDING_THREAD_JOIN_TIMEOUT_MILLIS = 500L
    }
}
