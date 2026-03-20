package com.example.ble_speaker

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.media.*
import android.os.IBinder
import android.util.Log
import android.os.Build

class AudioStreamService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_MUTE = "ACTION_MUTE"
        
        var isStreaming = false
        var isMuted = false
        var volumeGain = 1.0f // Allowed up to 4.0x
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var recordingThread: Thread? = null

    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeIn = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
    private val bufferSizeOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startStreaming()
            ACTION_STOP -> stopStreaming()
            ACTION_MUTE -> {
                isMuted = !isMuted
                NotificationHelper.updateNotification(this, isMuted)
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startStreaming() {
        if (isStreaming) return

        val notification = NotificationHelper.createNotification(this, isMuted)
        
        // Start Foreground Service with Microphone type (requires Android 14+ compat, handled in manifest)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NotificationHelper.NOTIFICATION_ID, 
                    notification, 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NotificationHelper.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e("AudioStreamService", "Failed to start foreground: ${e.message}")
        }

        isStreaming = true
        startAudioCaptureAndPlayback()
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCaptureAndPlayback() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfigIn, audioFormat, bufferSizeIn
        )

        // Using hidden API constant for USAGE_HEARING_AID if possible, fallback to MEDIA
        val usageType = try {
            val usageHearingAid = AudioAttributes::class.java.getField("USAGE_HEARING_AID").getInt(null)
            usageHearingAid
        } catch (e: Exception) {
            AudioAttributes.USAGE_MEDIA
        }

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usageType)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfigOut)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeOut)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioRecord?.startRecording()
        audioTrack?.play()

        recordingThread = Thread {
            val buffer = ShortArray(bufferSizeIn)
            while (isStreaming) {
                val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readResult > 0) {
                    if (isMuted) {
                        for (i in 0 until readResult) {
                            buffer[i] = 0
                        }
                    } else if (volumeGain != 1.0f) {
                        for (i in 0 until readResult) {
                            var sample = (buffer[i] * volumeGain).toInt()
                            sample = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            buffer[i] = sample.toShort()
                        }
                    }
                    audioTrack?.write(buffer, 0, readResult)
                }
            }
        }
        recordingThread?.priority = Thread.MAX_PRIORITY
        recordingThread?.start()
    }

    private fun stopStreaming() {
        if (!isStreaming) return
        isStreaming = false

        recordingThread?.join(1000)
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopStreaming()
    }
}
