package com.example.hersaferide.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityAdminDashboardBinding
import com.example.hersaferide.databinding.ItemEmergencyAlertBinding
import com.example.hersaferide.emergency.AudioRecordingActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.*

data class EmergencyAlert(
    val id: String = "",
    val userName: String = "",
    val time: String = "",
    val location: String = "",
    val status: String = "ACTIVE"
)

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var database: FirebaseDatabase
    private val alerts = mutableListOf<EmergencyAlert>()
    private lateinit var adapter: EmergencyAlertAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")

        setupStats()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupStats() {
        // Real-time count of total users
        database.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.tvTotalUsers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Real-time count of total rides
        database.getReference("ride_history").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.tvTotalRides.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Real-time count of active drivers
        database.getReference("drivers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activeCount = snapshot.children.count { 
                    it.child("status").getValue(String::class.java) == "ONLINE" 
                }
                binding.tvActiveDrivers.text = activeCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Monitor active emergencies
        database.getReference("active_rides").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                alerts.clear()
                for (data in snapshot.children) {
                    val status = data.child("status").getValue(String::class.java)
                    if (status == "DANGER") {
                        count++
                        alerts.add(EmergencyAlert(
                            id = data.key ?: "",
                            userName = "User: ${data.key?.takeLast(4)}",
                            time = "Just now",
                            location = "Active SOS"
                        ))
                    }
                }
                binding.tvActiveEmergencies.text = count.toString()
                adapter.notifyDataSetChanged()
                
                // Show empty state if needed (could be implemented in XML too)
                binding.rvEmergencyAlerts.visibility = if (alerts.isEmpty()) View.GONE else View.VISIBLE
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = EmergencyAlertAdapter(alerts) { alert ->
            // Handle respond click
            Toast.makeText(this, "Responding to alert: ${alert.id}", Toast.LENGTH_SHORT).show()
        }
        binding.rvEmergencyAlerts.layoutManager = LinearLayoutManager(this)
        binding.rvEmergencyAlerts.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnManageUsers.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }

        binding.btnManageDrivers.setOnClickListener {
            startActivity(Intent(this, DriverManagementActivity::class.java))
        }

        binding.btnViewSafetyRecordings.setOnClickListener {
            startActivity(Intent(this, AudioRecordingActivity::class.java))
        }

        binding.btnBroadcastMessage.setOnClickListener {
            showBroadcastDialog()
        }

        binding.btnSystemSettings.setOnClickListener {
            Toast.makeText(this, "System Status: All services operational", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogoutAdmin.setOnClickListener {
            finish()
        }
    }

    private fun showBroadcastDialog() {
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margin = (20 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, margin / 2, margin, 0)

        val inputLayout = TextInputLayout(this, null, R.style.WayGoInput)
        inputLayout.hint = "Safety Alert Message"
        val editText = TextInputEditText(inputLayout.context)
        inputLayout.addView(editText)
        container.addView(inputLayout, params)

        MaterialAlertDialogBuilder(this)
            .setTitle("Broadcast Safety Alert")
            .setMessage("This message will be sent to all active users immediately.")
            .setView(container)
            .setPositiveButton("Send") { _, _ ->
                val message = editText.text.toString()
                if (message.isNotEmpty()) {
                    database.getReference("broadcasts").push().setValue(
                        mapOf(
                            "message" to message,
                            "timestamp" to ServerValue.TIMESTAMP,
                            "type" to "SAFETY_ALERT"
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(this, "Broadcast sent successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class EmergencyAlertAdapter(
        private val alerts: List<EmergencyAlert>,
        private val onRespond: (EmergencyAlert) -> Unit
    ) : RecyclerView.Adapter<EmergencyAlertAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemEmergencyAlertBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemEmergencyAlertBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val alert = alerts[position]
            holder.binding.tvUser.text = alert.userName
            holder.binding.tvInfo.text = "ACTIVE SOS • ${alert.time}"
            holder.binding.btnRespond.setOnClickListener { onRespond(alert) }
        }

        override fun getItemCount() = alerts.size
    }
}
