package com.example.hersaferide.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityDriverSimulatorBinding
import com.example.hersaferide.model.Driver
import com.example.hersaferide.model.DriverRequest
import com.example.hersaferide.model.Ride
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DriverSimulatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverSimulatorBinding
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
    private val auth = FirebaseAuth.getInstance()
    private var driverId: String? = null
    
    private var currentRequest: DriverRequest? = null
    private var requestListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverSimulatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // For simulation, use a fixed driver ID or current user ID
        driverId = auth.currentUser?.uid ?: "sim_driver_1"
        Log.d("DriverSim", "Using Driver ID: $driverId")
        
        setupListeners()
        checkOnlineStatus()
    }

    private fun setupListeners() {
        binding.btnGoOnline.setOnClickListener {
            goOnline()
        }

        binding.btnAccept.setOnClickListener {
            acceptRequest()
        }

        binding.btnReject.setOnClickListener {
            rejectRequest()
        }

        binding.btnArriving.setOnClickListener {
            updateRideStatus("arriving")
        }

        binding.btnStartRide.setOnClickListener {
            verifyOtpAndStart()
        }

        binding.btnCompleteRide.setOnClickListener {
            completeRide()
        }
    }

    private fun checkOnlineStatus() {
        driverId?.let { id ->
            database.getReference("drivers").child(id).get().addOnSuccessListener { snapshot ->
                val driver = snapshot.getValue(Driver::class.java)
                if (driver != null && driver.isOnline) {
                    binding.btnGoOnline.text = "Go Offline"
                    listenForRequests()
                } else {
                    binding.btnGoOnline.text = "Go Online"
                }
            }.addOnFailureListener {
                Log.e("DriverSim", "Failed to check online status", it)
            }
        }
    }

    private fun goOnline() {
        val id = driverId ?: return
        val currentlyOnline = binding.btnGoOnline.text == "Go Offline"
        
        val driverUpdates = mapOf(
            "driverId" to id,
            "name" to "Priya (Sim)",
            "vehicleType" to "Mini",
            "vehicleModel" to "Swift",
            "vehicleNumber" to "RJ 14 CA 1234",
            "isOnline" to !currentlyOnline,
            "isAvailable" to !currentlyOnline,
            "verified" to true,
            "currentLat" to 26.9124, // Jaipur
            "currentLng" to 75.7873
        )
        
        Log.d("DriverSim", "Updating driver $id online status to ${!currentlyOnline}")
        database.getReference("drivers").child(id).updateChildren(driverUpdates)
            .addOnSuccessListener {
                Log.d("DriverSim", "Driver status updated successfully")
                if (!currentlyOnline) {
                    binding.btnGoOnline.text = "Go Offline"
                    listenForRequests()
                } else {
                    binding.btnGoOnline.text = "Go Online"
                    stopListeningForRequests()
                }
            }
            .addOnFailureListener {
                Log.e("DriverSim", "Failed to update driver status", it)
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun listenForRequests() {
        val id = driverId ?: return
        val requestsRef = database.getReference("driver_requests").child(id)
        
        stopListeningForRequests() // Clear existing

        requestListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val request = snapshot.getValue(DriverRequest::class.java)
                if (request != null && request.status == "pending") {
                    currentRequest = request
                    showRequestUI(request)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                if (currentRequest?.rideId == snapshot.key) {
                    hideRequestUI()
                }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("DriverSim", "Requests listener cancelled", error.toException())
            }
        }
        requestsRef.addChildEventListener(requestListener!!)
        binding.tvNoRequest.text = "Waiting for requests..."
    }

    private fun stopListeningForRequests() {
        driverId?.let { id ->
            requestListener?.let { 
                database.getReference("driver_requests").child(id).removeEventListener(it)
            }
        }
        hideRequestUI()
        binding.tvNoRequest.text = "Go online to see requests"
    }

    private fun showRequestUI(request: DriverRequest) {
        binding.tvNoRequest.visibility = View.GONE
        binding.cardRequest.visibility = View.VISIBLE
        binding.tvPickup.text = "Pickup: ${request.pickupLat}, ${request.pickupLng}"
    }

    private fun hideRequestUI() {
        binding.cardRequest.visibility = View.GONE
        binding.tvNoRequest.visibility = View.VISIBLE
    }

    private fun acceptRequest() {
        val request = currentRequest ?: return
        val id = driverId ?: return
        
        val rideRef = database.getReference("rides").child(request.rideId)
        rideRef.get().addOnSuccessListener { snapshot ->
            val ride = snapshot.getValue(Ride::class.java)
            if (ride != null && ride.status == "requested") {
                val updates = mapOf(
                    "status" to "accepted",
                    "assignedDriverId" to id,
                    "assignedDriverName" to "Priya (Sim)",
                    "assignedVehicle" to "White Swift",
                    "assignedVehicleNumber" to "RJ 14 CA 1234",
                    "updatedAt" to System.currentTimeMillis()
                )
                rideRef.updateChildren(updates).addOnSuccessListener {
                    database.getReference("driver_requests").child(id).child(request.rideId).child("status").setValue("accepted")
                    database.getReference("drivers").child(id).child("isAvailable").setValue(false)
                    
                    binding.cardRequest.visibility = View.GONE
                    binding.llActiveRide.visibility = View.VISIBLE
                    binding.tvStatus.text = "Status: Accepted"
                }
            } else {
                Toast.makeText(this, "Ride no longer available", Toast.LENGTH_SHORT).show()
                database.getReference("driver_requests").child(id).child(request.rideId).removeValue()
                hideRequestUI()
            }
        }
    }

    private fun rejectRequest() {
        val request = currentRequest ?: return
        val id = driverId ?: return
        database.getReference("driver_requests").child(id).child(request.rideId).child("status").setValue("rejected")
        hideRequestUI()
    }

    private fun updateRideStatus(status: String) {
        val rideId = currentRequest?.rideId ?: return
        database.getReference("rides").child(rideId).child("status").setValue(status)
        binding.tvStatus.text = "Status: $status"
    }

    private fun verifyOtpAndStart() {
        val rideId = currentRequest?.rideId ?: return
        val enteredOtp = binding.etOtp.text.toString()
        
        database.getReference("rides").child(rideId).get().addOnSuccessListener { snapshot ->
            val ride = snapshot.getValue(Ride::class.java)
            if (ride != null && ride.startOtp == enteredOtp) {
                updateRideStatus("started")
                binding.etOtp.visibility = View.GONE
                binding.btnStartRide.visibility = View.GONE
            } else {
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun completeRide() {
        val rideId = currentRequest?.rideId ?: return
        val id = driverId ?: return
        
        database.getReference("rides").child(rideId).child("status").setValue("completed")
        database.getReference("drivers").child(id).child("isAvailable").setValue(true)
        
        binding.llActiveRide.visibility = View.GONE
        hideRequestUI()
        currentRequest = null
        Toast.makeText(this, "Ride Completed!", Toast.LENGTH_LONG).show()
    }
}
