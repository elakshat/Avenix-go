package com.example.hersaferide.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.MainActivity
import com.example.hersaferide.databinding.ActivityOtpVerificationBinding

class OTPVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString()
            if (otp == "123456" || otp.length == 6) { // Default 123456 for easy testing
                Toast.makeText(this, "Verification Successful!", Toast.LENGTH_SHORT).show()
                // Navigate to Onboarding
                val intent = Intent(this, OnboardingContactsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                binding.etOtp.error = "Enter 6-digit OTP"
            }
        }

        binding.btnResendOtp.setOnClickListener {
            Toast.makeText(this, "OTP Resent: 123456", Toast.LENGTH_SHORT).show()
        }
    }
}
