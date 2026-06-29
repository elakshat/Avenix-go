package com.example.hersaferide.ride

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityRideHistoryBinding

data class RideHistoryItem(
    val destination: String,
    val date: String,
    val price: String,
    val vehicleType: String
)

class RideHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRideHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRideHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupTabs()
        setupRecyclerView()
    }

    private fun setupTabs() {
        binding.tabCurrent.setOnClickListener {
            updateTabState(it as TextView)
        }
        binding.tabPast.setOnClickListener {
            updateTabState(it as TextView)
        }
        binding.tabCancel.setOnClickListener {
            updateTabState(it as TextView)
        }
    }

    private fun updateTabState(selectedTab: TextView) {
        val tabs = listOf(binding.tabCurrent, binding.tabPast, binding.tabCancel)
        tabs.forEach { tab ->
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.bg_capsule_active)
                tab.setTextColor(ContextCompat.getColor(this, R.color.black))
            } else {
                tab.setBackgroundResource(android.R.color.transparent)
                tab.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
        }
        // In a real app, filter the list here
        Toast.makeText(this, "${selectedTab.text} rides", Toast.LENGTH_SHORT).show()
    }

    private fun setupRecyclerView() {
        val rides = listOf(
            RideHistoryItem("Alfa Romeo Stelvio", "20 March | 10:00AM", "$60", "Sedan"),
            RideHistoryItem("Tesla Model 3", "18 March | 02:30PM", "$80", "Mini"),
            RideHistoryItem("BMW X5", "15 March | 09:15AM", "$120", "SUV")
        )

        binding.rvRideHistory.layoutManager = LinearLayoutManager(this)
        binding.rvRideHistory.adapter = RideHistoryAdapter(rides) {
            Toast.makeText(this, "Cancellation requested", Toast.LENGTH_SHORT).show()
        }
    }

    class RideHistoryAdapter(
        private val rides: List<RideHistoryItem>,
        private val onCancelClick: (RideHistoryItem) -> Unit
    ) : RecyclerView.Adapter<RideHistoryAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDestination: TextView = view.findViewById(R.id.tvDestination)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvPrice: TextView = view.findViewById(R.id.tvPrice)
            val btnCancel: View = view.findViewById(R.id.btnCancelRide)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ride_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val ride = rides[position]
            holder.tvDestination.text = ride.destination
            holder.tvDate.text = ride.date
            holder.tvPrice.text = ride.price
            holder.btnCancel.setOnClickListener { onCancelClick(ride) }
        }

        override fun getItemCount() = rides.size
    }
}
