package com.example.hersaferide.ride

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.databinding.ActivityChatBinding
import com.example.hersaferide.databinding.ItemChatMessageBinding

data class ChatMessage(
    val message: String,
    val isSender: Boolean,
    val time: String
)

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupClickListeners()

        // Dummy initial messages
        messages.add(ChatMessage("Hello, I am arriving in 5 minutes.", false, "10:00 AM"))
        messages.add(ChatMessage("Okay, I'll be waiting at the entrance.", true, "10:01 AM"))
        adapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSendMessage.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                messages.add(ChatMessage(text, true, "Now"))
                binding.etMessage.setText("")
                adapter.notifyItemInserted(messages.size - 1)
                binding.rvChat.scrollToPosition(messages.size - 1)
                
                // Simulate driver reply
                binding.rvChat.postDelayed({
                    messages.add(ChatMessage("Got it!", false, "Now"))
                    adapter.notifyItemInserted(messages.size - 1)
                    binding.rvChat.scrollToPosition(messages.size - 1)
                }, 1500)
            }
        }
    }

    class ChatAdapter(private val items: List<ChatMessage>) :
        RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            if (item.isSender) {
                holder.binding.llSender.visibility = View.VISIBLE
                holder.binding.llReceiver.visibility = View.GONE
                holder.binding.tvSenderMessage.text = item.message
                holder.binding.tvSenderTime.text = item.time
            } else {
                holder.binding.llSender.visibility = View.GONE
                holder.binding.llReceiver.visibility = View.VISIBLE
                holder.binding.tvReceiverMessage.text = item.message
                holder.binding.tvReceiverTime.text = item.time
            }
        }

        override fun getItemCount() = items.size
    }
}