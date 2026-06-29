package com.example.hersaferide.profile

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.R
import com.example.hersaferide.databinding.ActivityPaymentBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var currentBalance: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            finish()
            return
        }

        database = FirebaseDatabase.getInstance("https://hersaferide-6dbaf-default-rtdb.firebaseio.com/")
            .getReference("users").child(uid)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        loadBalance()
        setupClickListeners()
    }

    private fun loadBalance() {
        database.child("wallet_balance").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentBalance = snapshot.getValue(Double::class.java) ?: 0.0
                binding.tvUberCashBalance.text = String.format("₹%.2f", currentBalance)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@PaymentActivity, "Failed to load balance", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnAddFunds.setOnClickListener {
            showAddFundsDialog()
        }

        binding.btnAddPaymentMethod.setOnClickListener {
            Toast.makeText(this, "Card integration coming in next update!", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddPromoCode.setOnClickListener {
            showPromoCodeDialog()
        }
    }

    private fun showAddFundsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Funds to Wallet")
        
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        params.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        
        val input = TextInputEditText(this)
        input.hint = "Enter amount in ₹"
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Add") { _, _ ->
            val amountStr = input.text.toString()
            if (amountStr.isNotEmpty()) {
                val amount = amountStr.toDouble()
                updateBalance(amount)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showPromoCodeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Apply Promo Code")
        
        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        params.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)

        val input = TextInputEditText(this)
        input.hint = "Enter code (e.g. WAYGO50)"
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)

        builder.setPositiveButton("Apply") { _, _ ->
            val code = input.text.toString().uppercase()
            if (code == "WAYGO50") {
                updateBalance(50.0)
                Toast.makeText(this, "₹50.00 added to your wallet!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Invalid or expired promo code", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun updateBalance(amount: Double) {
        val newBalance = currentBalance + amount
        database.child("wallet_balance").setValue(newBalance)
            .addOnSuccessListener {
                Toast.makeText(this, "Wallet updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Transaction failed", Toast.LENGTH_SHORT).show()
            }
    }
}
