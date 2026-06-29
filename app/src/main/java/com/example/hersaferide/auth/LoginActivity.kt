package com.example.hersaferide.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.MainActivity
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "LoginActivity_Auth"
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let { navigateToMain() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupAnimations()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                setLoading(true)
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        setLoading(false)
                        if (task.isSuccessful) {
                            navigateToMain()
                        } else {
                            showError("Login Failed", task.exception?.message ?: "Invalid email or password")
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoogleSignIn.setOnClickListener {
            setLoading(true)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupAnimations() {
        // Initial state
        binding.logoCard.alpha = 0f
        binding.logoCard.translationY = -50f
        binding.tvWelcome.alpha = 0f
        binding.tvWelcome.translationX = -100f
        binding.tvSubtitle.alpha = 0f
        binding.loginFormContainer.alpha = 0f
        binding.loginFormContainer.translationY = 100f

        // Animate elements
        binding.logoCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.tvWelcome.animate()
            .alpha(1f)
            .translationX(0f)
            .setStartDelay(200)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.tvSubtitle.animate()
            .alpha(1f)
            .setStartDelay(400)
            .setDuration(800)
            .start()

        binding.loginFormContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(600)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun setupGoogleSignIn() {
        val webClientId = getString(R.string.default_web_client_id)
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            setLoading(false)
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    Log.e(TAG, "Google Sign-In API Error: ${e.statusCode}")
                    handleSignInError(e.statusCode)
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        setLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    showError("Firebase Link Failed", task.exception?.message ?: "Check your database settings.")
                }
            }
    }

    private fun handleSignInError(statusCode: Int) {
        val message = when (statusCode) {
            10 -> "Developer Error (10): Your SHA-1 or Web Client ID is wrong in Firebase Console."
            12500 -> "Config Error (12500): Check 'OAuth consent screen' in Google Cloud Console."
            else -> "Google Error Code: $statusCode"
        }
        showError("Sign-In Problem", message)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnGoogleSignIn.isEnabled = !isLoading
    }

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
