package com.example.hersaferide.ride

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hersaferide.databinding.ActivityScheduleRideBinding
import java.text.SimpleDateFormat
import java.util.*

class ScheduleRideActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleRideBinding
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleRideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupPickers()

        binding.btnSetPickupTime.setOnClickListener {
            val date = binding.tvSelectedDate.text.toString()
            val time = binding.tvSelectedTime.text.toString()

            if (date == "Select Date" || time == "Select Time") {
                Toast.makeText(this, "Please select both date and time", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Ride scheduled for $date at $time", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun setupPickers() {
        binding.cardDatePicker.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    updateDateLabel()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.minDate = System.currentTimeMillis()
                show()
            }
        }

        binding.cardTimePicker.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    updateTimeLabel()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            ).show()
        }
    }

    private fun updateDateLabel() {
        val format = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        binding.tvSelectedDate.text = format.format(calendar.time)
    }

    private fun updateTimeLabel() {
        val format = SimpleDateFormat("h:mm a", Locale.getDefault())
        binding.tvSelectedTime.text = format.format(calendar.time)
    }
}
