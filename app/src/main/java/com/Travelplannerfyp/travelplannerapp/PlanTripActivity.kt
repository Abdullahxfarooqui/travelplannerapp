package com.Travelplannerfyp.travelplannerapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.HotelAdapter
import com.Travelplannerfyp.travelplannerapp.models.Hotel
import com.Travelplannerfyp.travelplannerapp.utils.ImageDatabaseLoader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.Travelplannerfyp.travelplannerapp.R
import java.util.Calendar

class PlanTripActivity : AppCompatActivity() {
    private lateinit var placeNameTextView: TextView
    private lateinit var placeDescriptionTextView: TextView
    private lateinit var placeImageView: ImageView
    private lateinit var hotelRecyclerView: RecyclerView
    private lateinit var tripDescriptionEditText: EditText
    private lateinit var organizerNameEditText: EditText
    private lateinit var organizerPhoneEditText: EditText
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var seatsAvailableEditText: EditText
    private lateinit var createTripButton: Button

    // Data
    private var selectedHotels: List<Hotel> = emptyList()
    private lateinit var selectedPlaceName: String
    private lateinit var selectedPlaceDescription: String
    private lateinit var selectedPlaceImage: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_trip)

        initializeViews()
        extractIntentData()
        setupUI()
        setupDatePickers()
        setupRecyclerView()
        handleCreateTrip()
    }

    private fun initializeViews() {
        placeNameTextView = findViewById(R.id.place_name)
        placeDescriptionTextView = findViewById(R.id.place_description)
        placeImageView = findViewById(R.id.place_image)
        hotelRecyclerView = findViewById(R.id.recycler_view_hotels)
        tripDescriptionEditText = findViewById(R.id.trip_description)
        organizerNameEditText = findViewById(R.id.organizer_name)
        organizerPhoneEditText = findViewById(R.id.organizer_phone)
        startDateEditText = findViewById(R.id.start_date)
        endDateEditText = findViewById(R.id.end_date)
        seatsAvailableEditText = findViewById(R.id.seats_available)
        createTripButton = findViewById(R.id.createTripButton)
    }

    private fun extractIntentData() {
        selectedPlaceName = intent.getStringExtra("PLACE_NAME") ?: "Unknown Place"
        selectedPlaceDescription = intent.getStringExtra("PLACE_DESCRIPTION") ?: "No description available"
        selectedPlaceImage = intent.getStringExtra("PLACE_IMAGE_URL") ?: ""

        intent.getStringExtra("SELECTED_HOTELS")?.let { json ->
            val type = object : TypeToken<List<Hotel>>() {}.type
            selectedHotels = Gson().fromJson(json, type)
        }
    }

    private fun setupUI() {
        placeNameTextView.text = selectedPlaceName
        placeDescriptionTextView.text = selectedPlaceDescription

        if (selectedPlaceImage.isNotEmpty()) {
            ImageDatabaseLoader.loadImage(placeImageView, selectedPlaceImage)
        } else {
            placeImageView.setImageResource(R.drawable.ic_placeholder)
        }
    }

    private fun setupDatePickers() {
        startDateEditText.setOnClickListener { showDatePickerDialog(startDateEditText) }
        endDateEditText.setOnClickListener { showDatePickerDialog(endDateEditText) }
    }

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                editText.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupRecyclerView() {
        hotelRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PlanTripActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = HotelAdapter(
                selectedHotels,
                onItemClick = { /* future implementation */ },
                onAddToCartClick = { /* future implementation */ }
            )
        }
    }

    private fun handleCreateTrip() {
        createTripButton.setOnClickListener {
            val tripDescription = tripDescriptionEditText.text.toString().trim()
            val organizerName = organizerNameEditText.text.toString().trim()
            val organizerPhone = organizerPhoneEditText.text.toString().trim()
            val startDate = startDateEditText.text.toString().trim()
            val endDate = endDateEditText.text.toString().trim()
            val seatsAvailable = seatsAvailableEditText.text.toString().trim()

            if (listOf(tripDescription, organizerName, organizerPhone, startDate, endDate, seatsAvailable).any { it.isEmpty() }) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val organizerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val tripData = buildTripData(
                tripDescription,
                organizerName,
                organizerPhone,
                startDate,
                endDate,
                seatsAvailable,
                organizerId
            )

            saveTripToFirebase(tripData)
        }
    }

    private fun buildTripData(
        description: String,
        organizer: String,
        phone: String,
        start: String,
        end: String,
        seats: String,
        userId: String
    ): Map<String, Any> {
        return mapOf(
            "placeName" to selectedPlaceName,
            "placeDescription" to selectedPlaceDescription,
            "tripDescription" to description,
            "organizerName" to organizer,
            "organizerPhone" to phone,
            "startDate" to start,
            "endDate" to end,
            "seatsAvailable" to seats,
            "selectedHotels" to selectedHotels.map {
                mapOf(
                    "name" to it.name,
                    "price" to it.price,
                    "rating" to it.rating,
                    "imageUrl" to it.imageUrl
                )
            },
            "organizerId" to userId,
            "placeImageUrl" to selectedPlaceImage
        )
    }

    private fun saveTripToFirebase(tripData: Map<String, Any>) {
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")
        val tripKey = tripsRef.push().key

        if (tripKey == null) {
            Toast.makeText(this, "Could not generate trip ID", Toast.LENGTH_SHORT).show()
            return
        }

        tripsRef.child(tripKey).setValue(tripData)
            .addOnSuccessListener {
                Toast.makeText(this, "Trip Created Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}