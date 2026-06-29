package com.example.hersaferide

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class WayGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force Enable Dark Mode globally as the UI is designed with a dark aesthetic
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
