package com.example.hersaferide.emergency

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityEmergencyContactsBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val contactList = mutableListOf<EmergencyContact>()
    private lateinit var adapter: ContactAdapter
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        val uid = auth.currentUser?.uid
        if (uid == null) {
            finish()
            return
        }
        
        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
            .getReference("users").child(uid).child("emergency_contacts")

        setupRecyclerView()
        fetchContacts()

        binding.btnAddContact.setOnClickListener {
            if (contactList.size >= 3) {
                Toast.makeText(this, "You can only add up to 3 contacts", Toast.LENGTH_SHORT).show()
            } else {
                showAddContactDialog()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter(contactList) { contact ->
            deleteContact(contact)
        }
        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
    }

    private fun fetchContacts() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contactList.clear()
                for (data in snapshot.children) {
                    val contact = data.getValue(EmergencyContact::class.java)
                    if (contact != null) {
                        contactList.add(contact.copy(id = data.key ?: ""))
                    }
                }
                saveContactsLocally(contactList)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EmergencyContactsActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveContactsLocally(contacts: List<EmergencyContact>) {
        val json = Gson().toJson(contacts)
        prefs.edit().putString("local_contacts", json).apply()
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etContactName)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.etContactPhone)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    saveContact(name, phone)
                } else {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveContact(name: String, phone: String) {
        val contactId = database.push().key ?: return
        val contact = EmergencyContact(name, phone, contactId)
        database.child(contactId).setValue(contact)
            .addOnSuccessListener {
                Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteContact(contact: EmergencyContact) {
        database.child(contact.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show()
            }
    }

    class ContactAdapter(private val contacts: List<EmergencyContact>, private val onDelete: (EmergencyContact) -> Unit) :
        RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(android.R.id.text1)
            val tvPhone: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = contacts[position]
            holder.tvName.text = contact.name
            holder.tvPhone.text = contact.phone
            holder.itemView.setOnLongClickListener {
                onDelete(contact)
                true
            }
        }

        override fun getItemCount() = contacts.size
    }
}
