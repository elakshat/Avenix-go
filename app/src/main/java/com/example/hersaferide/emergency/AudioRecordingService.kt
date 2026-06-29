package com.example.hersaferide.emergency

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.hersaferide.R
import com.example.hersaferide.ride.RideTrackingActivity
import java.io.File

class AudioRecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentFile: File? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): AudioRecordingService = this@AudioRecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                if (filePath != null) {
                    startRecording(filePath)
                }
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(filePath: String) {
        if (isRecording) return

        currentFile = File(filePath)
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(filePath)
                prepare()
                start()
            }
            isRecording = true
            
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "audio_recording_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Safety Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, RideTrackingActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Safety Recording Active")
            .setContentText("Recording audio locally for your safety.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop & Save", stopPendingIntent)
            .setColor(ContextCompat.getColor(this, R.color.accent_yellow))
            .build()
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    override fun onDestroy() {
        if (isRecording) stopRecording()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"
        const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"
        const val NOTIFICATION_ID = 1001
    }
}
