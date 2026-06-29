package com.example.hersaferide

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.hersaferide.databinding.ActivityMainBinding
import com.example.hersaferide.emergency.EmergencyManager
import com.example.hersaferide.emergency.SOSActivity
import com.example.hersaferide.model.Driver
import com.example.hersaferide.model.DriverRequest
import com.example.hersaferide.model.Ride
import com.example.hersaferide.profile.ProfileActivity
import com.example.hersaferide.ride.PackageDeliveryActivity
import com.example.hersaferide.ride.RideTrackingActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var emergencyManager: EmergencyManager
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
    
    private var googleMap: GoogleMap? = null
    private var findingRideJob: Job? = null
    
    private var currentLatLng = LatLng(26.9124, 75.7873)
    private var pickupLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    
    private var currentMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    
    private var selectedRideType = "Mini"
    private val calendar = Calendar.getInstance()
    private var isPickupFocused = true
    private var isProgrammaticChange = false
    
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val driverMarkers = mutableMapOf<String, Marker>()
    private var driversListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topContent.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        auth = FirebaseAuth.getInstance()
        geocoder = Geocoder(this, Locale.getDefault())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        emergencyManager = EmergencyManager(this)
        
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet.bottomSheetContainer)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupClickListeners()
        loadUserProfile()
        setupBackPressHandler()
        initializeDateTime()
        
        bottomSheetBehavior.peekHeight = (320 * resources.displayMetrics.density).toInt()
        
        pickupLatLng = currentLatLng
        updatePickupLocationText()
        updateFocusVisuals()
        
        selectRide("Mini")
    }

    private fun listenForNearbyDrivers() {
        driversListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = googleMap ?: return
                
                // Clear old markers that are no longer in the snapshot
                val currentDriverIds = snapshot.children.mapNotNull { it.key }
                val iter = driverMarkers.iterator()
                while (iter.hasNext()) {
                    val entry = iter.next()
                    if (entry.key !in currentDriverIds) {
                        entry.value.remove()
                        iter.remove()
                    }
                }

                for (ds in snapshot.children) {
                    try {
                        val driver = ds.getValue(Driver::class.java) ?: continue
                        val driverId = ds.key ?: continue

                        if (driver.isOnline && driver.isAvailable) {
                            val pos = LatLng(driver.currentLat, driver.currentLng)
                            if (driverMarkers.containsKey(driverId)) {
                                driverMarkers[driverId]?.position = pos
                            } else {
                                val marker = map.addMarker(
                                    MarkerOptions()
                                        .position(pos)
                                        .title(driver.name)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                                )
                                if (marker != null) driverMarkers[driverId] = marker
                            }
                        } else {
                            driverMarkers[driverId]?.remove()
                            driverMarkers.remove(driverId)
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Invalid driver data at ${ds.key}: ${e.message}")
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.getReference("drivers").addValueEventListener(driversListener!!)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isMapToolbarEnabled = false
        
        googleMap?.setOnMapClickListener { latLng ->
            handleMapClick(latLng)
        }
        
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        updateCurrentLocationMarker(currentLatLng)
        getCurrentLocation()
        
        // Start listening for drivers once map is ready
        listenForNearbyDrivers()
    }

    private fun handleMapClick(latLng: LatLng) {
        val targetedForPickup = isPickupFocused
        
        lifecycleScope.launch {
            try {
                @Suppress("DEPRECATION")
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                }
                val addressName = if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    addr.thoroughfare ?: addr.getAddressLine(0) ?: "Selected Location"
                } else "Selected Location"
                
                isProgrammaticChange = true
                if (targetedForPickup) {
                    pickupLatLng = latLng
                    binding.bottomSheet.etPickupLocation.setText(addressName)
                    updatePickupMarker(latLng, addressName)
                    switchToDestinationFocus()
                } else {
                    destinationLatLng = latLng
                    binding.bottomSheet.etReturnLocation.setText(addressName)
                    updateDestinationOnMap(latLng, addressName)
                }
                isProgrammaticChange = false
                
                updateMarkersAndRoute()
            } catch (_: Exception) {
                isProgrammaticChange = true
                val fallback = if (targetedForPickup) "Selected Pickup" else "Selected Destination"
                if (targetedForPickup) {
                    pickupLatLng = latLng
                    binding.bottomSheet.etPickupLocation.setText(fallback)
                    updatePickupMarker(latLng, fallback)
                    switchToDestinationFocus()
                } else {
                    destinationLatLng = latLng
                    binding.bottomSheet.etReturnLocation.setText(fallback)
                    updateDestinationOnMap(latLng, fallback)
                }
                isProgrammaticChange = false
                updateMarkersAndRoute()
            }

            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun switchToDestinationFocus() {
        isPickupFocused = false
        binding.bottomSheet.etReturnLocation.requestFocus()
        updateFocusVisuals()
    }

    private fun updateFocusVisuals() {
        val pickupVisible = isPickupFocused
        binding.bottomSheet.dotPickup.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (pickupVisible) ContextCompat.getColor(this, R.color.accent_yellow) else Color.parseColor("#888888")
        )
        binding.bottomSheet.dotDest.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (!pickupVisible) ContextCompat.getColor(this, R.color.accent_yellow) else Color.parseColor("#888888")
        )
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    updateCurrentLocationMarker(currentLatLng)
                    if (pickupLatLng == null || (pickupLatLng?.latitude == 26.9124 && pickupLatLng?.longitude == 75.7873)) {
                        pickupLatLng = currentLatLng
                        updatePickupLocationText()
                        updatePickupMarker(currentLatLng, "My Location")
                    }
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                }
            }
    }

    private fun updatePickupLocationText() {
        lifecycleScope.launch {
            try {
                pickupLatLng?.let { latLng ->
                    @Suppress("DEPRECATION")
                    val addresses = withContext(Dispatchers.IO) {
                        geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    }
                    if (!addresses.isNullOrEmpty()) {
                        isProgrammaticChange = true
                        val name = addresses[0].thoroughfare ?: addresses[0].getAddressLine(0)
                        binding.bottomSheet.etPickupLocation.setText(name)
                        isProgrammaticChange = false
                    }
                }
            } catch (_: Exception) {
                isProgrammaticChange = true
                binding.bottomSheet.etPickupLocation.setText(getString(R.string.current_location))
                isProgrammaticChange = false
            }
        }
    }

    private fun updateCurrentLocationMarker(latLng: LatLng) {
        if (currentMarker == null) {
            currentMarker = googleMap?.addMarker(
                MarkerOptions().position(latLng).title("My Location")
            )
        } else {
            currentMarker?.position = latLng
        }
    }

    private fun setupClickListeners() {
        binding.bottomSheet.etPickupLocation.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isPickupFocused = true
                updateFocusVisuals()
            }
        }

        binding.bottomSheet.etReturnLocation.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isPickupFocused = false
                updateFocusVisuals()
            }
        }

        binding.bottomSheet.etPickupLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isProgrammaticChange) pickupLatLng = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.bottomSheet.etReturnLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isProgrammaticChange) destinationLatLng = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.bottomSheet.etReturnLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                performSearch(binding.bottomSheet.etReturnLocation.text.toString(), isPickup = false)
                true
            } else false
        }

        binding.bottomSheet.etPickupLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_NEXT) {
                performSearch(binding.bottomSheet.etPickupLocation.text.toString(), isPickup = true)
                true
            } else false
        }

        binding.bottomSheet.btnSwapLocations.setOnClickListener {
            val pickupText = binding.bottomSheet.etPickupLocation.text.toString()
            val returnText = binding.bottomSheet.etReturnLocation.text.toString()
            
            isProgrammaticChange = true
            binding.bottomSheet.etPickupLocation.setText(returnText)
            binding.bottomSheet.etReturnLocation.setText(pickupText)
            isProgrammaticChange = false
            
            val tempLatLng = pickupLatLng
            pickupLatLng = destinationLatLng
            destinationLatLng = tempLatLng
            
            updateMarkersAndRoute()
            Toast.makeText(this, "Locations swapped", Toast.LENGTH_SHORT).show()
        }

        binding.bottomSheet.llSelectedDate.setOnClickListener { showDatePicker() }
        binding.bottomSheet.llSelectedTime.setOnClickListener { showTimePicker() }
        binding.btnSosHeader.setOnClickListener { startActivity(Intent(this, SOSActivity::class.java)) }
        binding.btnProfile.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.bottomSheet.btnBookRide.setOnClickListener { validateAndShowRideSelection() }

        binding.llDriverTab.setOnClickListener {
            binding.llDriverTab.setBackgroundResource(R.drawable.bg_capsule_active)
            binding.tvDriverTabText.setTextColor(Color.BLACK)
            binding.ivDriverTabIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.BLACK)
            
            binding.llPackageTab.background = null
            binding.tvPackageTabText.setTextColor(Color.WHITE)
            binding.ivPackageTabIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        }

        binding.llPackageTab.setOnClickListener {
            startActivity(Intent(this, PackageDeliveryActivity::class.java))
        }

        binding.chipHome.setOnClickListener {
            binding.bottomSheet.etReturnLocation.setText("Home")
            performSearch("Home", isPickup = false)
        }

        binding.chipWork.setOnClickListener {
            binding.bottomSheet.etReturnLocation.setText("Work")
            performSearch("Work", isPickup = false)
        }

        // Ride Selection Clicks
        binding.bottomSheet.cardMini.root.setOnClickListener { selectRide("Mini") }
        binding.bottomSheet.cardSedan.root.setOnClickListener { selectRide("Sedan") }
        binding.bottomSheet.cardSUV.root.setOnClickListener { selectRide("SUV") }
        binding.bottomSheet.cardAuto.root.setOnClickListener { selectRide("Auto") }
        binding.bottomSheet.cardBike.root.setOnClickListener { selectRide("Bike") }
        
        binding.bottomSheet.btnConfirmBooking.setOnClickListener { createRideRequest() }
        
        binding.bottomSheet.btnCancelSearchInside.setOnClickListener {
            cancelRideRequest()
        }
    }

    private fun initializeDateTime() {
        val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        
        binding.bottomSheet.tvSelectedDate.text = dateFormat.format(calendar.time)
        binding.bottomSheet.tvSelectedTime.text = timeFormat.format(calendar.time)
    }

    private fun selectRide(type: String) {
        selectedRideType = type
        val cards = listOf(
            binding.bottomSheet.cardMini.root,
            binding.bottomSheet.cardSedan.root,
            binding.bottomSheet.cardSUV.root,
            binding.bottomSheet.cardAuto.root,
            binding.bottomSheet.cardBike.root
        )
        
        cards.forEach { card -> 
            card.strokeWidth = 0 
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_dark))
        }
        
        val selectedCard = when(type) {
            "Mini" -> binding.bottomSheet.cardMini.root
            "Sedan" -> binding.bottomSheet.cardSedan.root
            "SUV" -> binding.bottomSheet.cardSUV.root
            "Auto" -> binding.bottomSheet.cardAuto.root
            "Bike" -> binding.bottomSheet.cardBike.root
            else -> binding.bottomSheet.cardMini.root
        }
        
        selectedCard.strokeWidth = (2 * resources.displayMetrics.density).toInt()
        selectedCard.strokeColor = ContextCompat.getColor(this, R.color.accent_yellow)
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface_variant_dark))
    }

    private fun validateAndShowRideSelection() {
        val pickupText = binding.bottomSheet.etPickupLocation.text.toString()
        val destText = binding.bottomSheet.etReturnLocation.text.toString()

        if (pickupText.isEmpty()) {
            Toast.makeText(this, "Please enter pickup location", Toast.LENGTH_SHORT).show()
            return@validateAndShowRideSelection
        }
        if (destText.isEmpty()) {
            Toast.makeText(this, "Please enter destination", Toast.LENGTH_SHORT).show()
            return@validateAndShowRideSelection
        }

        lifecycleScope.launch {
            if (pickupLatLng == null) {
                Toast.makeText(this@MainActivity, "Resolving pickup...", Toast.LENGTH_SHORT).show()
                pickupLatLng = resolveAddress(pickupText)
            }
            if (destinationLatLng == null) {
                Toast.makeText(this@MainActivity, "Resolving destination...", Toast.LENGTH_SHORT).show()
                destinationLatLng = resolveAddress(destText)
            }

            if (pickupLatLng == null) {
                Toast.makeText(this@MainActivity, "Could not find pickup location", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (destinationLatLng == null) {
                Toast.makeText(this@MainActivity, "Could not find destination", Toast.LENGTH_SHORT).show()
                return@launch
            }

            binding.bottomSheet.llLocationSelection.visibility = View.GONE
            binding.bottomSheet.llRideSelection.visibility = View.VISIBLE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private suspend fun resolveAddress(name: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            if (name == "My Location" || name == "Current Location") return@withContext currentLatLng
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(name, 1)
            if (!addresses.isNullOrEmpty()) {
                LatLng(addresses[0].latitude, addresses[0].longitude)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun updateMarkersAndRoute() {
        pickupLatLng?.let { updatePickupMarker(it, binding.bottomSheet.etPickupLocation.text.toString()) }
        destinationLatLng?.let { updateDestinationOnMap(it, binding.bottomSheet.etReturnLocation.text.toString()) }
        
        if (pickupLatLng != null && destinationLatLng != null) {
            drawRoute(pickupLatLng!!, destinationLatLng!!)
            calculateFares()
        }
    }

    private fun calculateFares() {
        val start = pickupLatLng ?: return
        val end = destinationLatLng ?: return

        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            results
        )
        val distanceInKm = results[0] / 1000.0

        binding.bottomSheet.apply {
            cardMini.tvRideFare.text = "₹${(50 + (distanceInKm * 12)).toInt()}"
            cardSedan.tvRideFare.text = "₹${(70 + (distanceInKm * 15)).toInt()}"
            cardSUV.tvRideFare.text = "₹${(100 + (distanceInKm * 22)).toInt()}"
            cardAuto.tvRideFare.text = "₹${(30 + (distanceInKm * 10)).toInt()}"
            cardBike.tvRideFare.text = "₹${(20 + (distanceInKm * 7)).toInt()}"
            
            cardMini.tvRideType.text = "Mini"
            cardSedan.tvRideType.text = "Sedan"
            cardSUV.tvRideType.text = "SUV"
            cardAuto.tvRideType.text = "Auto"
            cardBike.tvRideType.text = "Bike"
        }
    }

    private fun updatePickupMarker(latLng: LatLng, title: String) {
        if (pickupMarker == null) {
            pickupMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        } else {
            pickupMarker?.position = latLng
            pickupMarker?.title = title
        }
    }

    private fun updateDestinationOnMap(latLng: LatLng, title: String) {
        if (destinationMarker == null) {
            destinationMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        } else {
            destinationMarker?.position = latLng
            destinationMarker?.title = title
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
            binding.bottomSheet.tvSelectedDate.text = dateFormat.format(calendar.time)
            binding.bottomSheet.tvSelectedDate.setTextColor(Color.WHITE)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(this, { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            binding.bottomSheet.tvSelectedTime.text = timeFormat.format(calendar.time)
            binding.bottomSheet.tvSelectedTime.setTextColor(Color.WHITE)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
    }

    private fun performSearch(locationName: String, isPickup: Boolean) {
        if (isPickup) hideKeyboard(binding.bottomSheet.etPickupLocation)
        else hideKeyboard(binding.bottomSheet.etReturnLocation)
        searchLocation(locationName, isPickup)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // --- REAL RIDE LIFECYCLE METHODS ---

    private var currentRideId: String? = null
    private var rideListener: ValueEventListener? = null

    private fun createRideRequest() {
        val riderId = auth.currentUser?.uid ?: return
        val rideId = database.getReference("rides").push().key ?: return
        currentRideId = rideId
        
        binding.bottomSheet.llRideSelection.visibility = View.GONE
        binding.bottomSheet.llSearchingRide.visibility = View.VISIBLE

        val estimatedFare = calculateSingleFare(selectedRideType)
        
        val startOtp = (1000..9999).random().toString()
        val ride = Ride(
            rideId = rideId,
            riderId = riderId,
            riderName = auth.currentUser?.displayName ?: "Rider",
            pickupLat = pickupLatLng?.latitude ?: 0.0,
            pickupLng = pickupLatLng?.longitude ?: 0.0,
            pickupAddress = binding.bottomSheet.etPickupLocation.text.toString(),
            destinationLat = destinationLatLng?.latitude ?: 0.0,
            destinationLng = destinationLatLng?.longitude ?: 0.0,
            destinationAddress = binding.bottomSheet.etReturnLocation.text.toString(),
            rideType = selectedRideType,
            status = "requested",
            estimatedFare = estimatedFare,
            startOtp = startOtp,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        database.getReference("rides").child(rideId).setValue(ride)
            .addOnSuccessListener {
                startDriverDiscovery(ride)
                listenToRideUpdates(rideId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create ride request", Toast.LENGTH_SHORT).show()
                binding.bottomSheet.llSearchingRide.visibility = View.GONE
                binding.bottomSheet.llRideSelection.visibility = View.VISIBLE
            }
    }

    private fun calculateSingleFare(type: String): Double {
        val start = pickupLatLng ?: return 0.0
        val end = destinationLatLng ?: return 0.0
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        val distance = results[0] / 1000.0
        
        return when(type) {
            "Mini" -> 50 + (distance * 12)
            "Sedan" -> 70 + (distance * 15)
            "SUV" -> 100 + (distance * 22)
            "Auto" -> 30 + (distance * 10)
            "Bike" -> 20 + (distance * 7)
            else -> 50 + (distance * 12)
        }
    }

    private fun listenToRideUpdates(rideId: String) {
        rideListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ride = snapshot.getValue(Ride::class.java) ?: return
                if (ride.status == "accepted") {
                    removeRideListener()
                    findingRideJob?.cancel()
                    binding.bottomSheet.llSearchingRide.visibility = View.GONE
                    val intent = Intent(this@MainActivity, RideTrackingActivity::class.java).apply {
                        putExtra("RIDE_ID", rideId)
                    }
                    startActivity(intent)
                } else if (ride.status == "cancelled") {
                    removeRideListener()
                    findingRideJob?.cancel()
                    binding.bottomSheet.llSearchingRide.visibility = View.GONE
                    binding.bottomSheet.llLocationSelection.visibility = View.VISIBLE
                    Toast.makeText(this@MainActivity, ride.cancellationReason ?: "Ride cancelled", Toast.LENGTH_LONG).show()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.getReference("rides").child(rideId).addValueEventListener(rideListener!!)
    }

    private fun removeRideListener() {
        currentRideId?.let {
            rideListener?.let { listener ->
                database.getReference("rides").child(it).removeEventListener(listener)
            }
        }
        rideListener = null
    }

    private fun startDriverDiscovery(ride: Ride) {
        findingRideJob?.cancel()
        findingRideJob = lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Starting driver discovery for ride: ${ride.rideId}")
                
                while (isActive) {
                    val snapshot = database.getReference("drivers").get().await()
                    var requestsSent = 0
                    
                    for (ds in snapshot.children) {
                        try {
                            val d = ds.getValue(Driver::class.java)
                            val actualDriverId = ds.key ?: continue
                            
                            // Filter for real drivers: Online, Available, and matching vehicle type (Ignoring Case)
                            if (d != null && d.isOnline && d.isAvailable && d.vehicleType.equals(ride.rideType, ignoreCase = true)) {
                                requestDriver(actualDriverId, ride)
                                requestsSent++
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing driver at ${ds.key}")
                        }
                    }
                    
                    if (requestsSent == 0) {
                        Log.d("MainActivity", "No real drivers found online for ${ride.rideType}. Retrying...")
                    } else {
                        Log.d("MainActivity", "Requests broadcasted to $requestsSent real drivers.")
                    }
                    
                    delay(10000)
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                Log.e("MainActivity", "Discovery error", e)
            }
        }
    }

    private suspend fun requestDriver(driverId: String, ride: Ride): Boolean {
        val request = DriverRequest(
            rideId = ride.rideId,
            riderId = ride.riderId,
            riderName = ride.riderName ?: "Rider", // Sending rider name
            pickupLat = ride.pickupLat,
            pickupLng = ride.pickupLng,
            pickupAddress = ride.pickupAddress,
            destinationLat = ride.destinationLat,
            destinationLng = ride.destinationLng,
            destinationAddress = ride.destinationAddress,
            estimatedFare = ride.estimatedFare ?: 0.0, // Sending estimated fare
            status = "pending"
        )
        return try {
            database.getReference("driver_requests").child(driverId).child(ride.rideId).setValue(request).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun updateRideStatus(rideId: String, status: String, reason: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "updatedAt" to System.currentTimeMillis()
        )
        reason?.let { updates["cancellationReason"] = it }
        database.getReference("rides").child(rideId).updateChildren(updates)
    }

    private fun cancelRideRequest() {
        findingRideJob?.cancel()
        currentRideId?.let { rideId ->
            updateRideStatus(rideId, "cancelled", "Cancelled by rider")
            removeRideListener()
        }
        binding.bottomSheet.llSearchingRide.visibility = View.GONE
        binding.bottomSheet.llRideSelection.visibility = View.VISIBLE
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        database.getReference("users").child(user.uid).get()
            .addOnSuccessListener { snapshot ->
                if (!isFinishing) {
                    val url = snapshot.child("profilePicUrl").getValue(String::class.java)
                    if (!url.isNullOrEmpty()) {
                        Glide.with(this).load(url).circleCrop().into(binding.ivProfileImg)
                    }
                }
            }
    }

    private fun searchLocation(name: String, isPickup: Boolean) {
        lifecycleScope.launch {
            try {
                @Suppress("DEPRECATION")
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(name, 1)
                }
                if (!addresses.isNullOrEmpty()) {
                    val latLng = LatLng(addresses[0].latitude, addresses[0].longitude)
                    val fullAddress = addresses[0].thoroughfare ?: addresses[0].getAddressLine(0)
                    isProgrammaticChange = true
                    if (isPickup) {
                        pickupLatLng = latLng
                        binding.bottomSheet.etPickupLocation.setText(fullAddress)
                        updatePickupMarker(latLng, fullAddress)
                        switchToDestinationFocus()
                    } else {
                        destinationLatLng = latLng
                        binding.bottomSheet.etReturnLocation.setText(fullAddress)
                        updateDestinationOnMap(latLng, fullAddress)
                    }
                    isProgrammaticChange = false
                    
                    updateMarkersAndRoute()
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                } else {
                    Toast.makeText(this@MainActivity, "Location not found", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity, "Search error", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomSheet.llSearchingRide.visibility == View.VISIBLE) {
                    cancelRideRequest()
                } else if (binding.bottomSheet.llRideSelection.visibility == View.VISIBLE) {
                    binding.bottomSheet.llRideSelection.visibility = View.GONE
                    binding.bottomSheet.llLocationSelection.visibility = View.VISIBLE
                } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        removeRideListener()
        findingRideJob?.cancel()
        driversListener?.let { database.getReference("drivers").removeEventListener(it) }
    }
}
