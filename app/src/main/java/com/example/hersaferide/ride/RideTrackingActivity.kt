package com.example.hersaferide.ride

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityRideTrackingBinding
import com.example.hersaferide.emergency.EmergencyManager
import com.example.hersaferide.emergency.LiveAudioService
import com.example.hersaferide.emergency.SOSActivity
import com.example.hersaferide.model.Ride
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RideTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityRideTrackingBinding
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
    
    private var googleMap: GoogleMap? = null
    private var isLiveAudioActive = false
    
    private var rideId: String? = null
    private var rideListener: ValueEventListener? = null
    private var driverLocationListener: ValueEventListener? = null
    private var driverMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var currentRide: Ride? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            startLiveAudioStream()
        } else {
            Toast.makeText(this, "Microphone permission is required for live audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityRideTrackingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        rideId = intent.getStringExtra("RIDE_ID")
        if (rideId == null) {
            Toast.makeText(this, "Error: Ride ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupListeners()
        startLocationService()
        requestNotificationPermission()
        
        listenToRideUpdates()
    }

    private fun listenToRideUpdates() {
        rideListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ride = snapshot.getValue(Ride::class.java) ?: return
                val prevRide = currentRide
                currentRide = ride
                updateUI(ride)
                
                if (ride.status == "completed") {
                    navigateToCompletion()
                } else if (ride.status == "cancelled") {
                    Toast.makeText(this@RideTrackingActivity, "Ride cancelled: ${ride.cancellationReason}", Toast.LENGTH_LONG).show()
                    stopServicesAndFinish()
                }
                
                if (ride.assignedDriverId != null && driverLocationListener == null) {
                    trackDriverLocation(ride.assignedDriverId)
                }

                if (prevRide?.destinationLat != ride.destinationLat || prevRide?.destinationLng != ride.destinationLng) {
                    updateMarkersAndRoute()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.getReference("rides").child(rideId!!).addValueEventListener(rideListener!!)
    }

    private fun trackDriverLocation(driverId: String) {
        driverLocationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("currentLat").getValue(Double::class.java) ?: return
                val lng = snapshot.child("currentLng").getValue(Double::class.java) ?: return
                val driverLatLng = LatLng(lat, lng)
                
                updateDriverMarker(driverLatLng)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.getReference("drivers").child(driverId).addValueEventListener(driverLocationListener!!)
    }

    private fun updateDriverMarker(latLng: LatLng) {
        if (driverMarker == null) {
            driverMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Driver")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )
        } else {
            driverMarker?.position = latLng
        }
    }

    private fun updateUI(ride: Ride) {
        binding.tvRideStatus.text = when (ride.status) {
            "accepted" -> "Driver accepted"
            "arriving" -> "Driver is arriving"
            "started" -> "Trip in progress"
            else -> ride.status.replaceFirstChar { it.uppercase() }
        }
        
        binding.tvEstimatedTime.text = if (ride.status == "started") {
            "Fare: ₹${ride.estimatedFare?.toInt() ?: "..."}"
        } else {
            "Pickup at: ${ride.pickupAddress}"
        }
        
        binding.tvDriverName.text = ride.assignedDriverName ?: "Finding driver..."
        binding.tvDriverInfo.text = "${ride.assignedVehicle ?: ""} • ${ride.assignedVehicleNumber ?: ""}"
        binding.tvRidePin.text = ride.startOtp
        binding.tvCurrentAddress.text = if (ride.status == "started") ride.destinationAddress else ride.pickupAddress
        
        // Show edit button only when trip has started
        binding.btnEditDestination.visibility = if (ride.status == "started") View.VISIBLE else View.GONE
    }

    private fun updateMarkersAndRoute() {
        val ride = currentRide ?: return
        val pickupLatLng = LatLng(ride.pickupLat, ride.pickupLng)
        val destLatLng = LatLng(ride.destinationLat, ride.destinationLng)

        if (pickupMarker == null) {
            pickupMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(pickupLatLng)
                    .title("Pickup")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        } else {
            pickupMarker?.position = pickupLatLng
        }

        if (destinationMarker == null) {
            destinationMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(destLatLng)
                    .title("Destination")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        } else {
            destinationMarker?.position = destLatLng
            destinationMarker?.title = "Destination: ${ride.destinationAddress}"
        }

        drawRoute(pickupLatLng, destLatLng)
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        routePolyline?.remove()
        routePolyline = googleMap?.addPolyline(
            PolylineOptions()
                .add(start, end)
                .color(Color.parseColor("#D8FF43"))
                .width(12f)
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isMapToolbarEnabled = false
        
        currentRide?.let { ride ->
            val pickupLatLng = LatLng(ride.pickupLat, ride.pickupLng)
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 15f))
            updateMarkersAndRoute()
        }
    }

    private fun setupListeners() {
        binding.btnSosTracking.setOnClickListener {
            startActivity(Intent(this, SOSActivity::class.java))
        }
        
        binding.btnShareTrip.setOnClickListener {
            shareTrip()
        }
        
        binding.fabSafetyToolkit.setOnClickListener {
            if (isLiveAudioActive) stopLiveAudioStream() else checkAudioPermissionAndStart()
        }
        
        binding.btnCallDriver.setOnClickListener {
            Toast.makeText(this, "Calling Driver...", Toast.LENGTH_SHORT).show()
        }

        binding.btnCancelRide.setOnClickListener {
            showCancellationDialog()
        }

        binding.btnEndRide.setOnClickListener {
            startActivity(Intent(this, SOSActivity::class.java))
        }

        binding.btnEditDestination.setOnClickListener {
            showEditDestinationDialog()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showEditDestinationDialog() {
        val ride = currentRide ?: return
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val margin = (24 * resources.displayMetrics.density).toInt()
            lp.setMargins(margin, margin/2, margin, 0)
            
            val editText = EditText(this@RideTrackingActivity).apply {
                setText(ride.destinationAddress)
                hint = "Enter new destination"
                layoutParams = lp
            }
            addView(editText)
            tag = editText
        }

        val et = container.tag as EditText

        AlertDialog.Builder(this)
            .setTitle("Change Destination")
            .setView(container)
            .setPositiveButton("Update") { _, _ ->
                val newAddress = et.text.toString()
                if (newAddress.isNotEmpty() && newAddress != ride.destinationAddress) {
                    resolveAndUpdateDestination(newAddress)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resolveAndUpdateDestination(address: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@RideTrackingActivity, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(address, 1)
                if (!addresses.isNullOrEmpty()) {
                    val latLng = LatLng(addresses[0].latitude, addresses[0].longitude)
                    val fullAddress = addresses[0].thoroughfare ?: addresses[0].getAddressLine(0) ?: address
                    
                    withContext(Dispatchers.Main) {
                        updateDestinationInDatabase(latLng, fullAddress)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RideTrackingActivity, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RideTrackingActivity, "Error resolving location", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDestinationInDatabase(latLng: LatLng, address: String) {
        val rideId = rideId ?: return
        val ride = currentRide ?: return

        // Recalculate fare based on new destination
        val start = LatLng(ride.pickupLat, ride.pickupLng)
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, latLng.latitude, latLng.longitude, results)
        val distance = results[0] / 1000.0
        
        val newFare = when(ride.rideType) {
            "Mini" -> 50 + (distance * 12)
            "Sedan" -> 70 + (distance * 15)
            "SUV" -> 100 + (distance * 22)
            "Auto" -> 30 + (distance * 10)
            "Bike" -> 20 + (distance * 7)
            else -> 50 + (distance * 12)
        }

        val updates = mapOf(
            "destinationLat" to latLng.latitude,
            "destinationLng" to latLng.longitude,
            "destinationAddress" to address,
            "estimatedFare" to newFare,
            "updatedAt" to System.currentTimeMillis()
        )
        
        database.getReference("rides").child(rideId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Destination and Fare updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update destination", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCancellationDialog() {
        val ride = currentRide ?: return
        if (ride.status == "started") {
            Toast.makeText(this, "Trip already started. Cannot cancel.", Toast.LENGTH_SHORT).show()
            return
        }

        val fee = when (ride.status) {
            "requested" -> 0
            "accepted" -> 20
            "arriving" -> 30
            else -> 0
        }

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cancel_ride, null)
        val rgReasons = dialogView.findViewById<RadioGroup>(R.id.rgCancellationReasons)
        
        AlertDialog.Builder(this)
            .setTitle("Cancel Ride?")
            .setMessage("Cancellation fee: ₹$fee")
            .setView(dialogView)
            .setPositiveButton("Cancel Ride") { _, _ ->
                val selectedId = rgReasons.checkedRadioButtonId
                val reason = if (selectedId != -1) {
                    dialogView.findViewById<RadioButton>(selectedId).text.toString()
                } else "Other"
                
                performCancellation(reason, fee)
            }
            .setNegativeButton("Keep Ride", null)
            .show()
    }

    private fun performCancellation(reason: String, fee: Int) {
        val rideId = rideId ?: return
        val driverId = currentRide?.assignedDriverId
        
        val updates = mutableMapOf<String, Any>(
            "status" to "cancelled",
            "cancellationReason" to reason,
            "cancellationFee" to fee,
            "cancelledBy" to "rider",
            "updatedAt" to System.currentTimeMillis()
        )
        
        database.getReference("rides").child(rideId).updateChildren(updates)
            .addOnSuccessListener {
                if (driverId != null) {
                    database.getReference("drivers").child(driverId).child("isAvailable").setValue(true)
                }
                Toast.makeText(this, "Ride cancelled", Toast.LENGTH_SHORT).show()
                stopServicesAndFinish()
            }
    }

    private fun checkAudioPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startLiveAudioStream()
        } else {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun startLiveAudioStream() {
        isLiveAudioActive = true
        val serviceIntent = Intent(this, LiveAudioService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        
        val uid = auth.currentUser?.uid ?: ""
        val monitoringUrl = "https://waygo.com/live-audio/$uid"
        EmergencyManager(this).sendAlertToContacts(
            "WayGo: I've enabled live audio monitoring for my ride. You can listen here: $monitoringUrl"
        )
        
        Toast.makeText(this, "Live audio monitoring active. Contacts notified.", Toast.LENGTH_LONG).show()
        binding.fabSafetyToolkit.iconTint = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.accent_red)
        )
    }

    private fun stopLiveAudioStream() {
        isLiveAudioActive = false
        val serviceIntent = Intent(this, LiveAudioService::class.java)
        serviceIntent.action = LiveAudioService.ACTION_STOP_LIVE_AUDIO
        startService(serviceIntent)
        Toast.makeText(this, "Live audio monitoring stopped.", Toast.LENGTH_SHORT).show()
        binding.fabSafetyToolkit.iconTint = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.white)
        )
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LiveLocationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LiveLocationService::class.java)
        stopService(serviceIntent)
    }

    private fun shareTrip() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, "I'm on a trip! Follow my ride here: https://waygo.com/track/${auth.currentUser?.uid ?: "TRIP123"}")
        startActivity(Intent.createChooser(shareIntent, "Share Trip via"))
    }

    private fun navigateToCompletion() {
        stopServices()
        startActivity(Intent(this, RideCompletedActivity::class.java))
        finish()
    }

    private fun stopServices() {
        if (isLiveAudioActive) stopLiveAudioStream()
        stopLocationService()
    }

    private fun stopServicesAndFinish() {
        stopServices()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        rideId?.let {
            rideListener?.let { l -> database.getReference("rides").child(it).removeEventListener(l) }
        }
        currentRide?.assignedDriverId?.let {
            driverLocationListener?.let { l -> database.getReference("drivers").child(it).removeEventListener(l) }
        }
    }
}
