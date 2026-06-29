package com.example.hersaferide.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityUserManagementBinding
import com.google.firebase.database.*

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val gender: String = "",
    val wallet_balance: Double = 0.0,
    val safety_score: Int = 100
)

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private lateinit var database: FirebaseDatabase
    private val userList = mutableListOf<User>()
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
        
        setupRecyclerView()
        fetchUsers()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(userList)
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter
    }

    private fun fetchUsers() {
        database.getReference("users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (data in snapshot.children) {
                    val user = data.getValue(User::class.java)
                    if (user != null) {
                        userList.add(user)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    class UserAdapter(private val users: List<User>) :
        RecyclerView.Adapter<UserAdapter.ViewHolder>() {

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
            val user = users[position]
            holder.tvName.text = user.name
            holder.tvInfo.text = "Phone: ${user.phone} | Safety: ${user.safety_score}%"
        }

        override fun getItemCount() = users.size
    }
}
