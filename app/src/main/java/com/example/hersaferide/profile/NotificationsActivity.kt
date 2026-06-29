package com.example.hersaferide.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.databinding.ActivityNotificationsBinding
import com.example.hersaferide.databinding.ItemNotificationBinding

data class Notification(
    val title: String,
    val message: String,
    val time: String,
    val type: String = "PROMO"
)

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val notifications = listOf(
            Notification("Welcome to WayGo!", "Enjoy your first safe and comfortable ride with us.", "Just now"),
            Notification("50% Off your next ride!", "Use code SAFETY50 to get a discount on your next trip.", "2 hours ago"),
            Notification("New Safety Feature", "You can now record audio during your rides for extra safety.", "Yesterday"),
            Notification("Invite Friends, Get ₹50", "Share your referral code with friends and earn rewards.", "2 days ago")
        )

        if (notifications.isEmpty()) {
            binding.rvNotifications.visibility = View.GONE
            binding.llEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvNotifications.visibility = View.VISIBLE
            binding.llEmptyState.visibility = View.GONE
            binding.rvNotifications.layoutManager = LinearLayoutManager(this)
            binding.rvNotifications.adapter = NotificationAdapter(notifications)
        }
    }

    class NotificationAdapter(private val items: List<Notification>) :
        RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvNotificationTitle.text = item.title
            holder.binding.tvNotificationMessage.text = item.message
            holder.binding.tvNotificationTime.text = item.time
        }

        override fun getItemCount() = items.size
    }
}