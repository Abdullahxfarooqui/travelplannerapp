package com.example.travelplannerapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplannerapp.databinding.ActivityPlanTripBinding
import java.util.Calendar

class plan_trip : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var searchBtn: ImageView
    private lateinit var organizerName: EditText
    private lateinit var contactNumber: EditText
    private lateinit var seatsAvailable: EditText
    private lateinit var tripStartDate: EditText
    private lateinit var tripEndDate: EditText
    private lateinit var departureLocation: EditText
    private lateinit var destination: EditText
    private lateinit var tripDescription: EditText
    private lateinit var pricePerPerson: EditText
    private lateinit var createTripButton: Button

    private val calendar = Calendar.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_trip)

        // Initialize Views
        searchBar = findViewById(R.id.searchBar)
        searchBtn = findViewById(R.id.searchBtn)
        organizerName = findViewById(R.id.organizerName)
        contactNumber = findViewById(R.id.contactNumber)
        seatsAvailable = findViewById(R.id.seatsAvailable)
        tripStartDate = findViewById(R.id.tripStartDate)
        tripEndDate = findViewById(R.id.tripEndDate)
        departureLocation = findViewById(R.id.departureLocation)
        destination = findViewById(R.id.destination)
        tripDescription = findViewById(R.id.tripDescription)
        pricePerPerson = findViewById(R.id.pricePerPerson)
        createTripButton = findViewById(R.id.createTripButton)

        // Show date picker on clicking start or end date
        tripStartDate.setOnClickListener {
            showDatePickerDialog(tripStartDate)
        }

        tripEndDate.setOnClickListener {
            showDatePickerDialog(tripEndDate)
        }

        // Optional: Toast to check data when "Create Trip" is clicked
        createTripButton.setOnClickListener {
            Toast.makeText(this, "Trip Created!", Toast.LENGTH_SHORT).show()
            // You can add validation and backend connection here
        }

        // Optional: Handle search button click
        searchBtn.setOnClickListener {
            val place = searchBar.text.toString().trim()
            Toast.makeText(this, "Searching for $place", Toast.LENGTH_SHORT).show()
            // Add search logic here
        }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val formattedDate = formatDate(year, month, day)
            editText.setText(formattedDate)
        }

        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(year: Int, month: Int, day: Int): String {
        val formattedMonth = String.format("%02d", month + 1)
        val formattedDay = String.format("%02d", day)
        return "$formattedDay/$formattedMonth/$year"
    }
}