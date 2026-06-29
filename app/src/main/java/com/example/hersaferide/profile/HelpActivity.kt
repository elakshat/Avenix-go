package com.example.hersaferide.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityHelpBinding
import com.example.hersaferide.ride.RideHistoryActivity

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnTripIssues.setOnClickListener {
            val intent = Intent(this, RideHistoryActivity::class.java)
            startActivity(intent)
        }

        binding.btnAccountHelp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waygo.com/faq/account"))
            startActivity(intent)
        }

        binding.btnSafetyHelp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waygo.com/safety-guidelines"))
            startActivity(intent)
        }

        binding.btnCovidHelp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waygo.com/travel-guidelines"))
            startActivity(intent)
        }

        binding.btnContactSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@waygo.com")
                putExtra(Intent.EXTRA_SUBJECT, "Support Request - WayGo")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
