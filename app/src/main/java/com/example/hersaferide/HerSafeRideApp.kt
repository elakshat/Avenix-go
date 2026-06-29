package com.example.hersaferide

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class HerSafeRideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force Disable Dark Mode globally
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
