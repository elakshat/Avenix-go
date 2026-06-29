package com.example.hersaferide.ride

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityIntercityBinding

class IntercityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntercityBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntercityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSearchIntercity.setOnClickListener {
            val from = binding.etFromCity.text.toString()
            val to = binding.etToCity.text.toString()

            if (from.isEmpty() || to.isEmpty()) {
                Toast.makeText(this, "Please enter both cities", Toast.LENGTH_SHORT).show()
            } else {
                showSearchingState(from, to)
            }
        }

        binding.btnCancelSearch.setOnClickListener {
            hideSearchingState()
        }
    }

    private fun showSearchingState(from: String, to: String) {
        binding.llMainContent.visibility = View.GONE
        binding.llSearchingState.visibility = View.VISIBLE
        
        // Simulate finding an outstation partner
        handler.postDelayed({
            if (binding.llSearchingState.visibility == View.VISIBLE) {
                Toast.makeText(this, "Intercity Ride Found! Your driver for the trip from $from to $to is confirmed.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, RideTrackingActivity::class.java)
                intent.putExtra("RIDE_TYPE", "INTERCITY")
                intent.putExtra("DESTINATION", to)
                startActivity(intent)
                finish()
            }
        }, 3000)
    }

    private fun hideSearchingState() {
        binding.llMainContent.visibility = View.VISIBLE
        binding.llSearchingState.visibility = View.GONE
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
