package com.Travelplannerfyp.travelplannerapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
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
import com.Travelplannerfyp.travelplannerapp.models.ItineraryItem

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
    private lateinit var tripPriceEditText: EditText
    private lateinit var seatsAvailableEditText: EditText
    private lateinit var createTripButton: Button
    private lateinit var visibilityRadioGroup: RadioGroup

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
        tripPriceEditText = findViewById(R.id.trip_price)
        seatsAvailableEditText = findViewById(R.id.seats_available)
        createTripButton = findViewById(R.id.createTripButton)
        visibilityRadioGroup = findViewById(R.id.visibilityRadioGroup)
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
            val tripPrice = tripPriceEditText.text.toString().trim()
            val seatsAvailable = seatsAvailableEditText.text.toString().trim()

            if (listOf(tripDescription, organizerName, organizerPhone, startDate, endDate, tripPrice, seatsAvailable).any { it.isEmpty() }) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val organizerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val visibility = if (visibilityRadioGroup.checkedRadioButtonId == R.id.publicRadioButton) {
                "PUBLIC"
            } else {
                "PRIVATE"
            }
            // When creating a trip, pass a sample itinerary for now
            // In the future, collect itinerary from user input
            val sampleItinerary = listOf(
                ItineraryItem("09:00 AM", "Breakfast at hotel", "Enjoy a buffet breakfast at the main restaurant."),
                ItineraryItem("11:00 AM", "Guided City Tour", "Explore the city's top attractions with a local guide."),
                ItineraryItem("02:00 PM", "Lunch at Cafe", "Lunch at a popular local cafe."),
                ItineraryItem("04:00 PM", "Hiking", "Scenic hike to the viewpoint.")
            )
            val tripData = try {
                buildTripData(
                    tripDescription,
                    organizerName,
                    organizerPhone,
                    startDate,
                    endDate,
                    tripPrice,
                    seatsAvailable,
                    organizerId,
                    visibility,
                    sampleItinerary // Pass the itinerary here
                )
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveTripToFirebase(tripData)
        }
    }

    private fun buildTripData(
        description: String,
        organizer: String,
        phone: String,
        start: String,
        end: String,
        price: String,
        seats: String,
        userId: String,
        visibility: String,
        itinerary: List<ItineraryItem> // NEW
    ): Map<String, Any> {
        val hotelMap = selectedHotels.firstOrNull()?.let {
            val priceDouble = it.pricePerNight.toDoubleOrNull()
            if (priceDouble == null) {
                throw IllegalArgumentException("Hotel price per night must be a valid number")
            }
            mapOf(
                "name" to it.name,
                "pricePerNight" to priceDouble,
                "rating" to it.rating,
                "imageUrl" to it.imageUrl,
                "description" to it.description,
                "imageName" to it.imageName,
                "amenities" to it.amenities // Save amenities as a list
            )
        }
        val tripMap = mutableMapOf<String, Any>(
            "placeName" to selectedPlaceName,
            "placeDescription" to selectedPlaceDescription,
            "tripDescription" to description,
            "organizerName" to organizer,
            "organizerPhone" to phone,
            "startDate" to start,
            "endDate" to end,
            "seatsAvailable" to seats,
            "pricePerPerson" to price, // Use actual price from user input
            "organizerId" to userId,
            "placeImageUrl" to selectedPlaceImage,
            "placeImageName" to selectedPlaceImage, // Add this line for drawable loading
            "visibility" to visibility,
            "itinerary" to itinerary.map {
                mapOf(
                    "time" to it.time,
                    "title" to it.title,
                    "description" to it.description
                )
            }
        )
        if (hotelMap != null) {
            tripMap["hotel"] = hotelMap
        }
        return tripMap
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