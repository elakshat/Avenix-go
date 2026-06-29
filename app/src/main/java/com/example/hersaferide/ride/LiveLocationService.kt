package com.example.hersaferide.ride

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.hersaferide.R
import com.example.hersaferide.emergency.EmergencyManager
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LiveLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
    private val auth = FirebaseAuth.getInstance()
    private lateinit var prefs: SharedPreferences
    private lateinit var emergencyManager: EmergencyManager
    
    private var lastLocation: Location? = null
    private var stopStartTime: Long = 0
    private val STOP_THRESHOLD_MS = 20000 // 20 seconds for demo purposes
    private val handler = Handler(Looper.getMainLooper())
    private var isRideCheckActive = false
    private var isBatteryAlertSent = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prefs = getSharedPreferences("safety_prefs", Context.MODE_PRIVATE)
        emergencyManager = EmergencyManager(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    updateLocationInFirebase(location.latitude, location.longitude)
                    
                    if (prefs.getBoolean("ride_check", true)) {
                        checkForRideAnomalies(location)
                    }

                    checkBatteryLevel()
                }
            }
        }
    }

    private fun checkBatteryLevel() {
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        if (level != -1 && scale != -1) {
            val batteryPct = (level.toFloat() / scale.toFloat() * 100).toInt()
            if (batteryPct < 15 && !isBatteryAlertSent) {
                val message = "Emergency Alert: My phone battery is very low ($batteryPct%). I am currently on a ride. Please check on me. My last location is being shared on WayGo."
                emergencyManager.sendAlertToContacts(message)
                isBatteryAlertSent = true
            }
        }
    }

    private fun updateLocationInFirebase(lat: Double, lon: Double) {
        val uid = auth.currentUser?.uid ?: return
        val locationMap = mapOf(
            "latitude" to lat,
            "longitude" to lon,
            "timestamp" to System.currentTimeMillis()
        )
        
        database.getReference("active_rides")
            .child(uid)
            .child("current_location")
            .setValue(locationMap)
    }

    private fun checkForRideAnomalies(currentLocation: Location) {
        if (lastLocation != null) {
            val distance = currentLocation.distanceTo(lastLocation!!)
            if (distance < 5.0) { // If user moved less than 5 meters
                if (stopStartTime == 0L) {
                    stopStartTime = System.currentTimeMillis()
                } else if (!isRideCheckActive && System.currentTimeMillis() - stopStartTime > STOP_THRESHOLD_MS) {
                    sendRideCheckNotification()
                    isRideCheckActive = true
                }
            } else {
                stopStartTime = 0L
                isRideCheckActive = false
            }
        }
        lastLocation = currentLocation
    }

    private fun sendRideCheckNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, RideTrackingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SHOW_RIDE_CHECK", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val rideCheckNotification = NotificationCompat.Builder(this, "LocationChannel")
            .setContentTitle("WayGo RideCheck")
            .setContentText("We noticed you've been stopped for a while. Need help?")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(2, rideCheckNotification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "LocationChannel")
            .setContentTitle("WayGo Protection Active")
            .setContentText("Monitoring your ride for safety...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
        requestLocationUpdates()
        
        return START_STICKY
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
            
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
        } catch (unlikely: SecurityException) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "LocationChannel",
                "WayGo Safety Monitor",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacksAndMessages(null)
        
        val uid = auth.currentUser?.uid
        if (uid != null) {
            database.getReference("active_rides").child(uid).removeValue()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
