package com.example.hersaferide.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityReferEarnBinding

class ReferEarnActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReferEarnBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferEarnBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnCopyCode.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Referral Code", binding.tvReferralCode.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.btnInviteFriends.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            val shareMessage = "Hey! Use my referral code ${binding.tvReferralCode.text} to get ₹50 off on your first ride with WayGo. Download now: https://waygo.com/download"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, "Invite friends via"))
        }

        binding.tvTerms.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://waygo.com/terms"))
            startActivity(intent)
        }
    }
}
