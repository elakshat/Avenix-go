package com.example.hersaferide.profile

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivitySafetySettingsBinding
import com.example.hersaferide.emergency.AudioRecordingActivity
import com.example.hersaferide.emergency.EmergencyContactsActivity

class SafetySettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySafetySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySafetySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("safety_prefs", Context.MODE_PRIVATE)

        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        binding.switchPinVerify.isChecked = prefs.getBoolean("pin_verify", false)
        binding.switchRideCheck.isChecked = prefs.getBoolean("ride_check", true)
        binding.switchAlwaysShare.isChecked = prefs.getBoolean("always_share", false)
    }

    private fun setupListeners() {
        binding.llAudioRecording.setOnClickListener {
            startActivity(Intent(this, AudioRecordingActivity::class.java))
        }

        binding.llManageContacts.setOnClickListener {
            startActivity(Intent(this, EmergencyContactsActivity::class.java))
        }

        binding.switchPinVerify.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("pin_verify", isChecked).apply()
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(this, "PIN Verification $status", Toast.LENGTH_SHORT).show()
        }

        binding.switchRideCheck.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("ride_check", isChecked).apply()
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(this, "RideCheck $status", Toast.LENGTH_SHORT).show()
        }

        binding.switchAlwaysShare.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("always_share", isChecked).apply()
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(this, "Location Sharing $status", Toast.LENGTH_SHORT).show()
        }
    }
}
