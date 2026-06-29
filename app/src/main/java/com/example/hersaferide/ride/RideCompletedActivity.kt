package com.example.hersaferide.ride

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.MainActivity
import com.example.hersaferide.databinding.ActivityRideCompletedBinding

class RideCompletedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRideCompletedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRideCompletedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSubmitRating.setOnClickListener {
            val rating = binding.safetyRatingBar.rating
            Toast.makeText(this, "Thank you! Safety Rating: $rating", Toast.LENGTH_SHORT).show()
            
            // Navigate back to Main screen
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}