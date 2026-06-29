package com.example.hersaferide.ride

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityRentalsBinding

class RentalsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRentalsBinding
    private var selectedPackage = "1 hr"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupPackageSelection()

        binding.btnBookRental.setOnClickListener {
            showSearchingState()
        }

        binding.btnCancelSearch.setOnClickListener {
            hideSearchingState()
        }
    }

    private fun setupPackageSelection() {
        binding.card1Hour.setOnClickListener { selectPackage("1 hr") }
        binding.card2Hours.setOnClickListener { selectPackage("2 hr") }
        binding.card4Hours.setOnClickListener { selectPackage("4 hr") }
    }

    private fun selectPackage(pkg: String) {
        selectedPackage = pkg
        
        // Reset all cards
        resetCard(binding.card1Hour)
        resetCard(binding.card2Hours)
        resetCard(binding.card4Hours)

        // Highlight selected
        when (pkg) {
            "1 hr" -> applySelectedStyle(binding.card1Hour)
            "2 hr" -> applySelectedStyle(binding.card2Hours)
            "4 hr" -> applySelectedStyle(binding.card4Hours)
        }
        
        binding.btnBookRental.text = "Confirm $selectedPackage Rental"
    }

    private fun resetCard(card: com.google.android.material.card.MaterialCardView) {
        card.setCardBackgroundColor(Color.WHITE)
        card.strokeColor = ContextCompat.getColor(this, R.color.uber_gray_200)
        card.strokeWidth = 2 // pixels
    }

    private fun applySelectedStyle(card: com.google.android.material.card.MaterialCardView) {
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.uber_gray_100))
        card.strokeColor = Color.BLACK
        card.strokeWidth = 4
    }

    private fun showSearchingState() {
        binding.llMainContent.visibility = View.GONE
        binding.llSearchingState.visibility = View.VISIBLE
        binding.tvSearchingTitle.text = "Finding your $selectedPackage rental car..."
        
        // Simulate finding a driver
        handler.postDelayed({
            if (binding.llSearchingState.visibility == View.VISIBLE) {
                Toast.makeText(this, "Rental Car Found! Your driver is arriving shortly.", Toast.LENGTH_LONG).show()
                val intent = Intent(this, RideTrackingActivity::class.java)
                intent.putExtra("RIDE_TYPE", "RENTAL")
                intent.putExtra("RENTAL_PACKAGE", selectedPackage)
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
