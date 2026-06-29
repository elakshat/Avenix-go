package com.example.hersaferide.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.hersaferide.R
import com.example.hersaferide.dashboard.SafetyDashboardActivity
import com.example.hersaferide.databinding.ActivityProfileBinding
import com.example.hersaferide.emergency.EmergencyContactsActivity
import com.example.hersaferide.ride.RideHistoryActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var storage: StorageReference
    private var userListener: ValueEventListener? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfilePicture(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/").getReference("users").child(uid)
        storage = FirebaseStorage.getInstance("gs://hersaferide-6dbaf.firebasestorage.app").getReference("profile_pics").child("$uid.jpg")

        binding.toolbar.setNavigationOnClickListener { 
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            finish() 
        }

        loadUserData()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        val clickListener = { view: View ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }

        binding.llHeader.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.ivProfilePicture.setOnClickListener {
            clickListener(it)
            pickImageLauncher.launch("image/*")
        }

        binding.cardWallet.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, PaymentActivity::class.java))
        }

        binding.cardRides.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, RideHistoryActivity::class.java))
        }

        binding.cardHelp.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, HelpActivity::class.java))
        }

        binding.cardPayment.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, PaymentActivity::class.java))
        }

        binding.cardActivity.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, RideHistoryActivity::class.java))
        }

        binding.cardMembership.setOnClickListener {
            clickListener(it)
            Toast.makeText(this, "Rewards program coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnSavedPlaces.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, SavedPlacesActivity::class.java))
        }

        binding.btnManageContacts.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, EmergencyContactsActivity::class.java))
        }

        binding.btnSafetyDashboard.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, SafetyDashboardActivity::class.java))
        }

        binding.btnSettings.setOnClickListener {
            clickListener(it)
            startActivity(Intent(this, SafetySettingsActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            auth.signOut()
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            finishAffinity()
        }
    }

    private fun loadUserData() {
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)
                    val profilePicUrl = snapshot.child("profilePicUrl").getValue(String::class.java)
                    
                    val safetyScore = snapshot.child("safety_score").getValue(Int::class.java) ?: 100
                    val walletBalance = snapshot.child("wallet_balance").getValue(Double::class.java) ?: 0.0
                    val totalRides = snapshot.child("total_rides").getValue(Int::class.java) ?: 0

                    binding.tvUserName.text = name ?: "User Name"
                    binding.tvUserEmail.text = email ?: "user@example.com"
                    
                    // Display stats
                    binding.tvSafetyScore.text = getString(R.string.safety_score_format, safetyScore)
                    binding.tvWalletBalance.text = String.format(Locale.getDefault(), "₹%.2f", walletBalance)
                    binding.tvTotalRides.text = totalRides.toString()

                    if (!isFinishing && !isDestroyed && !profilePicUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(profilePicUrl)
                            .placeholder(R.color.surface_variant_dark)
                            .error(R.color.surface_variant_dark)
                            .centerCrop()
                            .into(binding.ivProfilePicture)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(userListener!!)
    }

    private fun uploadProfilePicture(uri: Uri) {
        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()
        storage.putFile(uri).addOnSuccessListener {
            storage.downloadUrl.addOnSuccessListener { downloadUri ->
                database.child("profilePicUrl").setValue(downloadUri.toString())
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.let { database.removeEventListener(it) }
    }
}
