package com.example.hersaferide.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivitySavedPlacesBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SavedPlacesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedPlacesBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
            .getReference("users").child(uid).child("saved_places")

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadSavedPlaces()
        setupClickListeners()
    }

    private fun loadSavedPlaces() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val home = snapshot.child("home").getValue(String::class.java)
                val work = snapshot.child("work").getValue(String::class.java)

                binding.tvHomeAddress.text = home ?: "Add Home"
                binding.tvWorkAddress.text = work ?: "Add Work"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupClickListeners() {
        binding.llHome.setOnClickListener { showEditPlaceDialog("home") }
        binding.llWork.setOnClickListener { showEditPlaceDialog("work") }
        binding.btnAddPlace.setOnClickListener {
            Toast.makeText(this, "Custom places coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditPlaceDialog(key: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update ${key.capitalize()}")

        val input = TextInputEditText(this)
        input.hint = "Enter address"
        val padding = (16 * resources.displayMetrics.density).toInt()
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(padding, padding / 2, padding, padding / 2)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Save") { _, _ ->
            val address = input.text.toString().trim()
            if (address.isNotEmpty()) {
                database.child(key).setValue(address)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}