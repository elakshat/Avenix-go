package com.example.hersaferide.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
            .getReference("users").child(uid)

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadUserData()
        
        binding.btnUpdateProfile.setOnClickListener {
            updateProfile()
        }
    }

    private fun loadUserData() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val phone = snapshot.child("phone").getValue(String::class.java)
                val gender = snapshot.child("gender").getValue(String::class.java)

                binding.etName.setText(name)
                binding.etEmail.setText(email)
                binding.etPhone.setText(phone)

                when (gender) {
                    "Female" -> binding.toggleGender.check(binding.btnFemale.id)
                    "Male" -> binding.toggleGender.check(binding.btnMale.id)
                    "Other" -> binding.toggleGender.check(binding.btnOther.id)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateProfile() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        
        val gender = when (binding.toggleGender.checkedButtonId) {
            binding.btnFemale.id -> "Female"
            binding.btnMale.id -> "Male"
            binding.btnOther.id -> "Other"
            else -> "Other"
        }

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "name" to name,
            "phone" to phone,
            "gender" to gender
        )

        database.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
        }
    }
}