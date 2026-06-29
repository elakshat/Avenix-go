package com.example.hersaferide.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.MainActivity
import com.example.hersaferide.databinding.ActivityOnboardingContactsBinding
import com.example.hersaferide.emergency.EmergencyContact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class OnboardingContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingContactsBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSave.setOnClickListener {
            saveContactsAndFinish()
        }

        binding.btnSkip.setOnClickListener {
            navigateToMain()
        }
    }

    private fun saveContactsAndFinish() {
        val uid = auth.currentUser?.uid ?: return
        
        val contacts = mutableMapOf<String, EmergencyContact>()
        
        // Primary Contact
        val name1 = binding.etName1.text.toString().trim()
        val phone1 = binding.etPhone1.text.toString().trim()
        if (name1.isNotEmpty() && phone1.isNotEmpty()) {
            val id1 = database.getReference("users").child(uid).child("emergency_contacts").push().key ?: "1"
            contacts[id1] = EmergencyContact(name1, phone1, id1)
        }

        // Secondary Contact
        val name2 = binding.etName2.text.toString().trim()
        val phone2 = binding.etPhone2.text.toString().trim()
        if (name2.isNotEmpty() && phone2.isNotEmpty()) {
            val id2 = database.getReference("users").child(uid).child("emergency_contacts").push().key ?: "2"
            contacts[id2] = EmergencyContact(name2, phone2, id2)
        }

        // Additional Contact
        val name3 = binding.etName3.text.toString().trim()
        val phone3 = binding.etPhone3.text.toString().trim()
        if (name3.isNotEmpty() && phone3.isNotEmpty()) {
            val id3 = database.getReference("users").child(uid).child("emergency_contacts").push().key ?: "3"
            contacts[id3] = EmergencyContact(name3, phone3, id3)
        }

        if (contacts.isEmpty()) {
            Toast.makeText(this, "Please add at least one contact", Toast.LENGTH_SHORT).show()
            return
        }

        database.getReference("users").child(uid).child("emergency_contacts")
            .updateChildren(contacts as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Welcome to the Safety Circle!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save contacts: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        @Suppress("DEPRECATION")
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
