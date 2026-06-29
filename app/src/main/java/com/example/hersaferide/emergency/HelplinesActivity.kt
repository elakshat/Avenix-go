package com.example.hersaferide.emergency

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.hersaferide.databinding.ActivityHelplinesBinding

class HelplinesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelplinesBinding
    private var pendingNumber: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelplinesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnCall1091.setOnClickListener {
            checkPermissionAndCall("1091")
        }

        binding.btnCall112.setOnClickListener {
            checkPermissionAndCall("112")
        }

        binding.btnCall100.setOnClickListener {
            checkPermissionAndCall("100")
        }
    }

    private fun checkPermissionAndCall(number: String) {
        pendingNumber = number
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 101)
        } else {
            makeCall(number)
        }
    }

    private fun makeCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pendingNumber?.let { makeCall(it) }
        }
    }
}
