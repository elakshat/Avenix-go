package com.example.hersaferide.emergency

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityLiveAudioListenerBinding
import com.google.firebase.database.*

class LiveAudioListenerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveAudioListenerBinding
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
    private var audioTrack: AudioTrack? = null
    private var audioReference: DatabaseReference? = null
    private var isListening = false
    
    private val sampleRate = 8000
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveAudioListenerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle both Deep Link and Intent Extras
        val uid = intent.data?.lastPathSegment ?: intent.getStringExtra("USER_ID")
        
        if (uid == null) {
            Toast.makeText(this, "Invalid Audio Link", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupAudioTrack()
        startListening(uid)

        binding.btnStopListening.setOnClickListener {
            stopListening()
            finish()
        }
    }

    private fun setupAudioTrack() {
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("LiveAudioListener", "Failed to setup AudioTrack: ${e.message}")
            Toast.makeText(this, "Audio setup failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startListening(uid: String) {
        isListening = true
        audioReference = database.getReference("live_audio").child(uid).child("chunks")
        
        // Listen for new chunks added to the database
        audioReference?.limitToLast(1)?.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!isListening) return
                val base64Data = snapshot.getValue(String::class.java) ?: return
                playChunk(base64Data)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveAudioListener", "Error: ${error.message}")
            }
        })
        
        binding.tvStatus.text = "Listening to Live Audio..."
        binding.audioVisualizer.visibility = View.VISIBLE
    }

    private fun playChunk(base64Data: String) {
        try {
            val audioData = Base64.decode(base64Data, Base64.NO_WRAP)
            audioTrack?.write(audioData, 0, audioData.size)
        } catch (e: Exception) {
            Log.e("LiveAudioListener", "Playback error: ${e.message}")
        }
    }

    private fun stopListening() {
        isListening = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}
