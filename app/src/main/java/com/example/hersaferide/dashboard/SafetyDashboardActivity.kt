package com.example.hersaferide.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.databinding.ActivitySafetyDashboardBinding
import com.example.hersaferide.ride.RideHistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SafetyDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySafetyDashboardBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySafetyDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/").getReference("users").child(uid)
            fetchSafetyStats()
        }

        // Setup static safety score for demo if no data
        binding.tvSafetyScore.text = "98%"
        binding.progressSafety.progress = 98
        
        setupRideHistory()
    }

    private fun fetchSafetyStats() {
        database.child("safety_score").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val score = snapshot.getValue(Int::class.java) ?: 98
                binding.tvSafetyScore.text = "$score%"
                binding.progressSafety.progress = score
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupRideHistory() {
        val history = listOf(
            RideHistoryItem("World Trade Park, Jaipur", "Oct 24, 6:30 PM", "₹180.00", "Sedan"),
            RideHistoryItem("Malviya Nagar, Jaipur", "Oct 22, 11:15 AM", "₹125.50", "Mini"),
            RideHistoryItem("Jaipur Junction", "Oct 20, 08:45 AM", "₹210.00", "SUV")
        )

        binding.rvRideHistory.layoutManager = LinearLayoutManager(this)
        binding.rvRideHistory.adapter = SafetyHistoryAdapter(history)
    }

    class SafetyHistoryAdapter(private val rides: List<RideHistoryItem>) :
        RecyclerView.Adapter<SafetyHistoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDestination: TextView = view.findViewById(android.R.id.text1)
            val tvDetails: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ride = rides[position]
            holder.tvDestination.text = ride.destination
            holder.tvDetails.text = "${ride.date} • ${ride.price} • Safety Verified"
            holder.tvDestination.setTextColor(android.graphics.Color.BLACK)
        }

        override fun getItemCount() = rides.size
    }
}
