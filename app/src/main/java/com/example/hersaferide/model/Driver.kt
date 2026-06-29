package com.example.hersaferide.model

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class Driver(
    var driverId: String = "",
    var name: String = "",
    var phone: String = "",
    var vehicleType: String = "",
    var vehicleModel: String = "",
    var vehicleNumber: String = "",
    var rating: Double = 5.0,
    
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,
    
    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,
    
    var currentLat: Double = 0.0,
    var currentLng: Double = 0.0,
    var gender: String = "Female",
    var verified: Boolean = true,
    var status: String = "OFFLINE" // Unifying with Admin class
)
