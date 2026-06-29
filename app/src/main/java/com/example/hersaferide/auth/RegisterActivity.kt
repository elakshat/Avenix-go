package com.example.hersaferide.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")

        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun registerUser() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        val gender = when (binding.toggleGender.checkedButtonId) {
            R.id.btnFemale -> "Female"
            R.id.btnMale -> "Male"
            R.id.btnOther -> "Other"
            else -> ""
        }

        if (name.isEmpty()) {
            binding.tilFullName.error = "Name is required"
            return
        } else binding.tilFullName.error = null

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        } else binding.tilEmail.error = null

        if (phone.length < 10) {
            binding.tilPhone.error = "Enter a valid phone number"
            return
        } else binding.tilPhone.error = null

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return
        } else binding.tilPassword.error = null

        if (gender.isEmpty()) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    val userMap = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "gender" to gender,
                        "userId" to userId,
                        "wallet_balance" to 0.0,
                        "safety_score" to 100,
                        "profilePicUrl" to ""
                    )
                    
                    if (userId != null) {
                        database.getReference("users").child(userId).setValue(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Welcome to WayGo!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, OTPVerificationActivity::class.java)
                                intent.putExtra("PHONE_NUMBER", phone)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener {
                                setLoading(false)
                                Toast.makeText(this, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    setLoading(false)
                    val exception = task.exception
                    Log.e("RegisterActivity", "Registration failed", exception)
                    
                    if (exception is FirebaseAuthUserCollisionException) {
                        binding.tilEmail.error = "Email already registered"
                    } else {
                        Toast.makeText(this, "Error: ${exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !isLoading
        binding.btnRegister.text = if (isLoading) "" else "Create Account"
    }
}
