package com.example.hersaferide.emergency

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityAudioRecordingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AudioRecordingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioRecordingBinding
    private var isRecording = false
    private var timerHandler = Handler(Looper.getMainLooper())
    private var amplitudeHandler = Handler(Looper.getMainLooper())
    private var secondsElapsed = 0
    private var currentFile: File? = null
    
    private var recordingService: AudioRecordingService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AudioRecordingService.LocalBinder
            recordingService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnStartStop.setOnClickListener {
            if (isRecording) {
                stopRecordingAction()
            } else {
                if (checkPermissions()) {
                    startRecordingAction()
                } else {
                    requestPermissions()
                }
            }
        }
        
        // Bind to service
        Intent(this, AudioRecordingService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun checkPermissions(): Boolean {
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            mic && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            mic
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 200)
    }

    private fun startRecordingAction() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "REC_${timeStamp}.3gp"
        currentFile = File(externalCacheDir, fileName)
        
        val intent = Intent(this, AudioRecordingService::class.java).apply {
            action = AudioRecordingService.ACTION_START_RECORDING
            putExtra(AudioRecordingService.EXTRA_FILE_PATH, currentFile?.absolutePath)
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        isRecording = true
        updateUI(true)
        startTimer()
        startAmplitudeUpdates()
    }

    private fun stopRecordingAction() {
        val intent = Intent(this, AudioRecordingService::class.java).apply {
            action = AudioRecordingService.ACTION_STOP_RECORDING
        }
        startService(intent)
        
        isRecording = false
        updateUI(false)
        stopTimer()
        stopAmplitudeUpdates()
        
        uploadToCloud()
    }

    private fun updateUI(recording: Boolean) {
        if (recording) {
            binding.btnStartStop.text = "Stop & Sync with Cloud"
            binding.btnStartStop.setIconResource(android.R.drawable.ic_menu_save)
            binding.tvStatus.text = "Recording in progress..."
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_error))
        } else {
            binding.btnStartStop.text = "Start New Recording"
            binding.btnStartStop.setIconResource(android.R.drawable.ic_btn_speak_now)
            binding.tvStatus.text = "Ready to record"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.uber_gray_500))
        }
    }

    private fun uploadToCloud() {
        val file = currentFile ?: return
        val user = FirebaseAuth.getInstance().currentUser ?: return

        if (!file.exists()) {
            Toast.makeText(this, "Recording file not found", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnStartStop.isEnabled = false
        binding.btnStartStop.text = "Syncing with Safety Cloud..."
        binding.tvStatus.text = "Uploading to secure cloud..."
        
        val storageRef = FirebaseStorage.getInstance("gs://hersaferide-6dbaf.firebasestorage.app").getReference("safety_recordings/${user.uid}/${file.name}")
        
        storageRef.putFile(Uri.fromFile(file))
            .addOnSuccessListener {
                Toast.makeText(this, "Recording safely stored in cloud", Toast.LENGTH_LONG).show()
                binding.btnStartStop.isEnabled = true
                binding.btnStartStop.text = "Start New Recording"
                binding.tvStatus.text = "Recording synced successfully"
                file.delete()
            }
            .addOnFailureListener { e ->
                Log.e("AudioRecording", "Cloud sync failed: ${e.message}")
                Toast.makeText(this, "Cloud sync failed. Saved locally.", Toast.LENGTH_SHORT).show()
                binding.btnStartStop.isEnabled = true
                binding.btnStartStop.text = "Start New Recording"
                binding.tvStatus.text = "Sync failed, saved on device"
            }
    }

    private fun startAmplitudeUpdates() {
        amplitudeHandler.post(object : Runnable {
            override fun run() {
                if (isRecording && isBound) {
                    val amp = recordingService?.getMaxAmplitude() ?: 0
                    // Dynamic scaling of animation based on sound level
                    val scale = 1.0f + (amp / 32767.0f) * 0.5f
                    binding.animationWave.scaleX = scale
                    binding.animationWave.scaleY = scale
                    amplitudeHandler.postDelayed(this, 100)
                } else {
                    binding.animationWave.scaleX = 1.0f
                    binding.animationWave.scaleY = 1.0f
                }
            }
        })
    }

    private fun stopAmplitudeUpdates() {
        amplitudeHandler.removeCallbacksAndMessages(null)
    }

    private fun startTimer() {
        secondsElapsed = 0
        timerHandler.post(object : Runnable {
            override fun run() {
                val minutes = secondsElapsed / 60
                val secs = secondsElapsed % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
                secondsElapsed++
                timerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun stopTimer() {
        timerHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        timerHandler.removeCallbacksAndMessages(null)
        amplitudeHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
