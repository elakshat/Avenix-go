package com.example.hersaferide

import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.hersaferide.databinding.ActivityFakeCallBinding

class FakeCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFakeCallBinding
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFakeCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFullScreen()
        setupRingtone()
        setupClickListeners()
        setupBackPressHandler()
    }

    private fun setupFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun setupRingtone() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, uri)
        ringtone?.play()
    }

    private fun setupClickListeners() {
        binding.fabDecline.setOnClickListener {
            stopRingtone()
            finish()
        }

        binding.fabAccept.setOnClickListener {
            stopRingtone()
            showActiveCallUI()
        }

        binding.fabEndCall.setOnClickListener {
            finish()
        }
    }

    private fun showActiveCallUI() {
        binding.llActiveCallUI.isVisible = true
        binding.chronometer.base = SystemClock.elapsedRealtime()
        binding.chronometer.start()
    }

    private fun stopRingtone() {
        ringtone?.let {
            if (it.isPlaying) {
                it.stop()
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If it's an active call, we let them "end" it properly or exit if they really want
                // But generally fake call activities should be easy to exit.
                stopRingtone()
                finish()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtone()
    }
}
