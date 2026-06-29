package com.example.hersaferide.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Ride(
    val rideId: String = "",
    val riderId: String = "",
    val riderName: String? = null,
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val pickupAddress: String = "",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val destinationAddress: String = "",
    val rideType: String = "",
    val status: String = "requested", // requested, accepted, arriving, started, completed, cancelled
    
    // Support both naming conventions to be safe with separate apps
    val assignedDriverId: String? = null,
    val driverId: String? = null, 
    
    val assignedDriverName: String? = null,
    val assignedVehicle: String? = null,
    val assignedVehicleNumber: String? = null,
    val estimatedFare: Double? = null,
    val finalFare: Double? = null,

    val cancellationReason: String? = null,
    val cancellationFee: Int = 0,
    val cancelledBy: String? = null,
    val startOtp: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
