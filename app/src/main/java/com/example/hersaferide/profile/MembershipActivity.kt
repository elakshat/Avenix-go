package com.example.hersaferide.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityMembershipBinding

class MembershipActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMembershipBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembershipBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnJoinPlus.setOnClickListener {
            // In a real app, this would trigger a payment flow
            Toast.makeText(this, "Welcome to WayGo+! Benefits activated.", Toast.LENGTH_LONG).show()
            binding.btnJoinPlus.text = "Manage Membership"
            binding.btnJoinPlus.isEnabled = false
        }
    }
}