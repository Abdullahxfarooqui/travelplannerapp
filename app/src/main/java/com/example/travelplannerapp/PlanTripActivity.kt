package com.example.travelplannerapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class PlanTripActivity : AppCompatActivity() {

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

        // Handle create trip button click
        createTripButton.setOnClickListener {
            // Validate inputs
            if (validateInputs()) {
                // Create a Trip object from the input data
                val newTrip = Trip(
                    id = System.currentTimeMillis().toString(), // Generate a unique ID
                    name = "Trip to ${destination.text}",
                    location = destination.text.toString(),
                    description = tripDescription.text.toString(),
                    // Additional fields from the form
                    organizerName = organizerName.text.toString(),
                    contactNumber = contactNumber.text.toString(),
                    seatsAvailable = seatsAvailable.text.toString().toIntOrNull() ?: 0,
                    startDate = tripStartDate.text.toString(),
                    endDate = tripEndDate.text.toString(),
                    departureLocation = departureLocation.text.toString(),
                    pricePerPerson = pricePerPerson.text.toString().toDoubleOrNull() ?: 0.0
                    // Note: Image URL would be set after uploading an image to storage
                )
                
                // In a real app, this would save to Firebase or local database
                // For now, just show success message
                Toast.makeText(this, "Trip Created Successfully!", Toast.LENGTH_SHORT).show()
                finish() // Return to previous screen
            }
        }

        // Handle search button click
        searchBtn.setOnClickListener {
            val place = searchBar.text.toString().trim()
            if (place.isNotEmpty()) {
                destination.setText(place)
                Toast.makeText(this, "Destination set to $place", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a destination to search", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (organizerName.text.toString().trim().isEmpty()) {
            organizerName.error = "Organizer name is required"
            isValid = false
        }

        if (contactNumber.text.toString().trim().isEmpty()) {
            contactNumber.error = "Contact number is required"
            isValid = false
        }

        if (seatsAvailable.text.toString().trim().isEmpty()) {
            seatsAvailable.error = "Number of seats is required"
            isValid = false
        }

        if (tripStartDate.text.toString().trim().isEmpty()) {
            tripStartDate.error = "Start date is required"
            isValid = false
        }

        if (tripEndDate.text.toString().trim().isEmpty()) {
            tripEndDate.error = "End date is required"
            isValid = false
        }

        if (departureLocation.text.toString().trim().isEmpty()) {
            departureLocation.error = "Departure location is required"
            isValid = false
        }

        if (destination.text.toString().trim().isEmpty()) {
            destination.error = "Destination is required"
            isValid = false
        }

        if (pricePerPerson.text.toString().trim().isEmpty()) {
            pricePerPerson.error = "Price is required"
            isValid = false
        }

        return isValid
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