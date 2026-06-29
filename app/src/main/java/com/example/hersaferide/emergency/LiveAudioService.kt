package com.example.hersaferide.emergency

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.hersaferide.R
import com.example.hersaferide.ride.RideTrackingActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.concurrent.atomic.AtomicBoolean

class LiveAudioService : Service() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
    private val isRunning = AtomicBoolean(false)
    
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null

    private val sampleRate = 8000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_LIVE_AUDIO) {
            stopStreaming()
            return START_NOT_STICKY
        }

        startStreaming()
        return START_STICKY
    }

    private fun startStreaming() {
        if (isRunning.get()) return
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LiveAudioService", "Permission not granted")
            stopSelf()
            return
        }

        isRunning.set(true)
        
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        recordingThread = Thread {
            val uid = auth.currentUser?.uid ?: return@Thread
            val audioRef = database.getReference("live_audio").child(uid)
            
            // Clean up old session
            audioRef.removeValue()
            val chunkIds = mutableListOf<String>()

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("LiveAudioService", "AudioRecord failed to initialize")
                    return@Thread
                }

                val data = ShortArray(bufferSize)
                audioRecord?.startRecording()

                while (isRunning.get()) {
                    val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                    if (read > 0) {
                        val byteBuffer = java.nio.ByteBuffer.allocate(read * 2)
                        byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until read) {
                            byteBuffer.putShort(data[i])
                        }
                        val base64Data = Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
                        
                        val chunkId = System.currentTimeMillis().toString()
                        audioRef.child("chunks").child(chunkId).setValue(base64Data)
                        audioRef.child("last_chunk_id").setValue(chunkId)

                        chunkIds.add(chunkId)
                        // Keep only last 10 chunks to avoid database bloat
                        if (chunkIds.size > 10) {
                            val oldChunkId = chunkIds.removeAt(0)
                            audioRef.child("chunks").child(oldChunkId).removeValue()
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("LiveAudioService", "SecurityException: ${e.message}")
            } catch (e: Exception) {
                Log.e("LiveAudioService", "Error: ${e.message}")
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {}
                audioRecord = null
            }
        }.apply { 
            name = "LiveAudioStreamingThread"
            start() 
        }
    }

    private fun stopStreaming() {
        isRunning.set(false)
        val uid = auth.currentUser?.uid
        if (uid != null) {
            database.getReference("live_audio").child(uid).removeValue()
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotification(): Notification {
        val channelId = "live_audio_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Live Audio Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification for live audio monitoring during rides"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, RideTrackingActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LiveAudioService::class.java).apply {
            action = ACTION_STOP_LIVE_AUDIO
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Live Audio Active")
            .setContentText("A trusted contact can now listen to your ride.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Streaming", stopPendingIntent)
            .setColor(ContextCompat.getColor(this, R.color.accent_yellow))
            .build()
    }

    override fun onDestroy() {
        stopStreaming()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP_LIVE_AUDIO = "ACTION_STOP_LIVE_AUDIO"
        const val NOTIFICATION_ID = 1002
    }
}
