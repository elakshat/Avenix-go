package com.example.hersaferide.emergency

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.hersaferide.databinding.ActivitySosBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SOSActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySosBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
    private var currentLocationLink = "Location not available"
    private lateinit var emergencyManager: EmergencyManager
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("emergency_prefs", MODE_PRIVATE)
        emergencyManager = EmergencyManager(this)

        // Edge-to-edge support
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            handleExit()
        }

        binding.btnCallPolice.setOnClickListener {
            checkPermissionsAndAction(Manifest.permission.CALL_PHONE, 103)
        }

        binding.btnCallHelpline.setOnClickListener {
            checkPermissionsAndAction(Manifest.permission.CALL_PHONE, 101)
        }

        binding.btnCallEmergency.setOnClickListener {
            checkPermissionsAndAction(Manifest.permission.CALL_PHONE, 102)
        }

        binding.btnCancel.setOnClickListener {
            handleExit()
            Toast.makeText(this, "Glad you are safe!", Toast.LENGTH_SHORT).show()
        }
        
        setupBackPressHandler()
        setDangerFlag()
        requestInitialPermissions()
        startPulseAnimation()
    }

    private fun startPulseAnimation() {
        val animator = ObjectAnimator.ofFloat(binding.pulseBg, "alpha", 0.1f, 0.35f)
        animator.duration = 1200
        animator.repeatCount = ValueAnimator.INFINITE
        animator.repeatMode = ValueAnimator.REVERSE
        animator.start()
    }

    private fun handleExit() {
        removeDangerFlag()
        finish()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@SOSActivity)
                    .setTitle("Cancel SOS?")
                    .setMessage("Are you sure you want to exit? Your emergency contacts have been notified.")
                    .setPositiveButton("I am Safe") { _, _ ->
                        handleExit()
                    }
                    .setNegativeButton("Stay", null)
                    .show()
            }
        })
    }

    private fun requestInitialPermissions() {
        val permissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CALL_PHONE)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        } else {
            getCurrentLocationAndPrepareAlert()
        }
    }

    private fun checkPermissionsAndAction(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        } else {
            handleAction(requestCode)
        }
    }

    private fun handleAction(requestCode: Int) {
        when (requestCode) {
            101 -> makeCall("1091")
            102 -> fetchAndCallEmergencyContact()
            103 -> makeCall("100")
        }
    }

    private fun fetchAndCallEmergencyContact() {
        val uid = auth.currentUser?.uid ?: return
        database.getReference("users").child(uid).child("emergency_contacts").get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists() && snapshot.hasChildren()) {
                    val contacts = mutableListOf<EmergencyContact>()
                    for (data in snapshot.children) {
                        data.getValue(EmergencyContact::class.java)?.let { contacts.add(it) }
                    }
                    if (contacts.isNotEmpty()) {
                        if (contacts.size == 1) {
                            makeCall(contacts[0].phone)
                        } else {
                            showContactChooser(contacts)
                        }
                    } else {
                        tryLocalCall()
                    }
                } else {
                    tryLocalCall()
                }
            }
            .addOnFailureListener {
                tryLocalCall()
            }
    }

    private fun tryLocalCall() {
        val localContacts = getLocalContacts()
        if (localContacts.isNotEmpty()) {
            if (localContacts.size == 1) {
                makeCall(localContacts[0].phone)
            } else {
                showContactChooser(localContacts)
            }
        } else {
            Toast.makeText(this, "No emergency contacts found. Please add them in Profile.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLocalContacts(): List<EmergencyContact> {
        val json = prefs.getString("local_contacts", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EmergencyContact>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun showContactChooser(contacts: List<EmergencyContact>) {
        val names = contacts.map { "${it.name} (${it.phone})" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose Contact to Call")
            .setItems(names) { _, which ->
                makeCall(contacts[which].phone)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun makeCall(number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = "tel:$number".toUri()
            startActivity(intent)
        } catch (e: SecurityException) {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = "tel:$number".toUri()
            startActivity(intent)
        }
    }

    private fun setDangerFlag() {
        val uid = auth.currentUser?.uid ?: return
        database.getReference("active_rides").child(uid).child("status").setValue("DANGER")
    }

    private fun removeDangerFlag() {
        val uid = auth.currentUser?.uid ?: return
        database.getReference("active_rides").child(uid).child("status").setValue("SAFE")
    }

    private fun getCurrentLocationAndPrepareAlert() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocationLink = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
                    }
                    sendEmergencyAlerts()
                }
                .addOnFailureListener {
                    sendEmergencyAlerts()
                }
        } else {
            sendEmergencyAlerts()
        }
    }

    private fun sendEmergencyAlerts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission denied. Cannot send alerts.", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "EMERGENCY! I need help. My current location: $currentLocationLink"
        emergencyManager.sendAlertToContacts(message) { count ->
            if (count > 0) {
                Toast.makeText(this, "Emergency alerts sent to $count contacts!", Toast.LENGTH_SHORT).show()
            } else {
                val contacts = getLocalContacts()
                if (contacts.isEmpty()) {
                    Toast.makeText(this, "No emergency contacts found. Please add them in Profile.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send alerts. Check permissions.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            when (requestCode) {
                101, 102, 103 -> {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        handleAction(requestCode)
                    } else {
                        Toast.makeText(this, "Permission denied for calling.", Toast.LENGTH_SHORT).show()
                    }
                }
                100 -> {
                    getCurrentLocationAndPrepareAlert()
                }
            }
        }
    }
}
