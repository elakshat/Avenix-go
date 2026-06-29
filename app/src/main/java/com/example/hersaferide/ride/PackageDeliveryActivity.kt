package com.example.hersaferide.ride

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityPackageDeliveryBinding

class PackageDeliveryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPackageDeliveryBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPackageDeliveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnReviewDelivery.setOnClickListener {
            val pickup = binding.etPickup.text.toString()
            val dropoff = binding.etDropoff.text.toString()
            val content = binding.etPackageContent.text.toString()

            if (pickup.isEmpty() || dropoff.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please fill in all details", Toast.LENGTH_SHORT).show()
            } else {
                showSearchingState()
            }
        }

        binding.btnCancelSearch.setOnClickListener {
            hideSearchingState()
        }
    }

    private fun showSearchingState() {
        binding.llMainContent.visibility = View.GONE
        binding.llSearchingState.visibility = View.VISIBLE
        
        // Simulate finding a partner
        handler.postDelayed({
            if (binding.llSearchingState.visibility == View.VISIBLE) {
                Toast.makeText(this, "Delivery Partner Found! They are on the way to pick up your package.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, RideTrackingActivity::class.java)
                intent.putExtra("RIDE_TYPE", "PACKAGE")
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
