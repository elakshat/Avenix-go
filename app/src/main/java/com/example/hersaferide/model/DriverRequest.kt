package com.example.hersaferide.model

data class DriverRequest(
    val rideId: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val pickupAddress: String = "",
    val destinationLat: Double = 0.0,
    val destinationLng: Double = 0.0,
    val destinationAddress: String = "",
    val estimatedFare: Double = 0.0,
    val status: String = "pending", // pending, accepted, rejected
    val createdAt: Long = System.currentTimeMillis()
)
