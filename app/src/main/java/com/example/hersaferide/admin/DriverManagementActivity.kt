package com.example.hersaferide.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.databinding.ActivityDriverManagementBinding
import com.google.firebase.database.*

data class Driver(
    val driverId: String = "",
    val name: String = "",
    val phone: String = "",
    val vehicleType: String = "",
    val vehicleNumber: String = "",
    val status: String = "OFFLINE",
    val rating: Double = 5.0
)

class DriverManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriverManagementBinding
    private lateinit var database: FirebaseDatabase
    private val driverList = mutableListOf<Driver>()
    private lateinit var adapter: DriverAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDriverManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
        
        setupRecyclerView()
        fetchDrivers()
    }

    private fun setupRecyclerView() {
        adapter = DriverAdapter(driverList)
        binding.rvDrivers.layoutManager = LinearLayoutManager(this)
        binding.rvDrivers.adapter = adapter
    }

    private fun fetchDrivers() {
        database.getReference("drivers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                driverList.clear()
                if (!snapshot.exists()) {
                    // Add some mock data if node is empty for demo
                    driverList.add(Driver("1", "Priya Sharma", "9876543210", "Pink Swift", "RJ 14 CA 5678", "ONLINE", 4.9))
                    driverList.add(Driver("2", "Anita Singh", "9876543211", "White Dzire", "RJ 14 AB 1234", "OFFLINE", 4.8))
                } else {
                    for (data in snapshot.children) {
                        val driver = data.getValue(Driver::class.java)
                        if (driver != null) {
                            driverList.add(driver)
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    class DriverAdapter(private val drivers: List<Driver>) :
        RecyclerView.Adapter<DriverAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(android.R.id.text1)
            val tvInfo: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val driver = drivers[position]
            holder.tvName.text = "${driver.name} (${driver.status})"
            holder.tvInfo.text = "Vehicle: ${driver.vehicleNumber} | Rating: ${driver.rating}⭐"
            
            if (driver.status == "ONLINE") {
                holder.tvName.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                holder.tvName.setTextColor(android.graphics.Color.BLACK)
            }
        }

        override fun getItemCount() = drivers.size
    }
}
