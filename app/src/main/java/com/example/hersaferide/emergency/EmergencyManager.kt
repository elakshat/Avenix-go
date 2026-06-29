package com.example.hersaferide.emergency

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmergencyManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
    private val prefs: SharedPreferences = context.getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)

    companion object {
        const val ACTION_SMS_SENT = "com.example.hersaferide.SMS_SENT"
    }

    fun sendAlertToContacts(message: String, onComplete: (Int) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: run {
            Log.e("EmergencyManager", "No user logged in")
            onComplete(0)
            return
        }
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("EmergencyManager", "SMS Permission not granted")
            onComplete(0)
            return
        }

        database.getReference("users").child(uid).child("emergency_contacts")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var sentCount = 0
                    if (snapshot.exists() && snapshot.hasChildren()) {
                        for (data in snapshot.children) {
                            val contact = data.getValue(EmergencyContact::class.java)
                            if (contact != null && contact.phone.isNotEmpty()) {
                                sendSMS(contact.phone, message)
                                sentCount++
                            }
                        }
                        onComplete(sentCount)
                    } else {
                        Log.d("EmergencyManager", "No database contacts found, trying local.")
                        val localCount = sendToLocalContacts(message)
                        onComplete(localCount)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("EmergencyManager", "Database error: ${error.message}. Falling back to local.")
                    val localCount = sendToLocalContacts(message)
                    onComplete(localCount)
                }
            })
    }

    private fun sendToLocalContacts(message: String): Int {
        val contacts = getLocalContacts()
        var count = 0
        for (contact in contacts) {
            if (contact.phone.isNotEmpty()) {
                sendSMS(contact.phone, message)
                count++
            }
        }
        return count
    }

    private fun getLocalContacts(): List<EmergencyContact> {
        val json = prefs.getString("local_contacts", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EmergencyContact>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        val sentIntent = PendingIntent.getBroadcast(
            context, 
            phoneNumber.hashCode(), 
            Intent(ACTION_SMS_SENT).apply { 
                putExtra("number", phoneNumber)
                setPackage(context.packageName)
            }, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            smsManager.sendTextMessage(phoneNumber, null, message, sentIntent, null)
            Log.d("EmergencyManager", "SMS request dispatched to system for $phoneNumber")
        } catch (e: Exception) {
            Log.e("EmergencyManager", "SMS failed to $phoneNumber: ${e.message}")
        }
    }
}
