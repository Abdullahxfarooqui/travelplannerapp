package com.Travelplannerfyp.travelplannerapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
    private lateinit var tripPriceEditText: EditText
    private lateinit var seatsAvailableEditText: EditText
    private lateinit var createTripButton: Button

    // Dynamic days components
    private lateinit var daysContainer: LinearLayout
    private lateinit var addDayButton: ImageButton
    private var dayCounter = 1
    private val dayViews = mutableListOf<View>()

    // Activity checkboxes
    private lateinit var checkboxHiking: CheckBox
    private lateinit var checkboxSwimming: CheckBox
    private lateinit var checkboxCamping: CheckBox
    private lateinit var checkboxSightseeing: CheckBox

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
        setupDynamicDays()
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

        // Dynamic days
        daysContainer = findViewById(R.id.days_container)
        addDayButton = findViewById(R.id.add_day_button)

        // Activity checkboxes
        checkboxHiking = findViewById(R.id.checkbox_hiking)
        checkboxSwimming = findViewById(R.id.checkbox_swimming)
        checkboxCamping = findViewById(R.id.checkbox_camping)
        checkboxSightseeing = findViewById(R.id.checkbox_sightseeing)
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

    private fun setupDynamicDays() {
        addDayButton.setOnClickListener { addNewDay() }

        // Add the first day by default
        addNewDay()
    }

    private fun addNewDay() {
        val inflater = LayoutInflater.from(this)
        val dayView = inflater.inflate(R.layout.activity_item, daysContainer, false)

        // Get references to the views
        val dayLabel = dayView.findViewById<TextView>(R.id.dayLabel)
        val activityDescription = dayView.findViewById<EditText>(R.id.activityDescription)
        val removeDayButton = dayView.findViewById<ImageButton>(R.id.remove_day_button)
        val expandDayButton = dayView.findViewById<ImageButton>(R.id.expand_day_button)
        val timeSlotsContainer = dayView.findViewById<LinearLayout>(R.id.time_slots_container)
        val addTimeSlotButton = dayView.findViewById<ImageButton>(R.id.add_time_slot_button)
        val timeSlotsList = dayView.findViewById<LinearLayout>(R.id.time_slots_list)

        // Set the day label
        dayLabel.text = "Day $dayCounter:"

        // Setup expand/collapse functionality
        var isExpanded = false
        expandDayButton.setOnClickListener {
            isExpanded = !isExpanded
            if (isExpanded) {
                timeSlotsContainer.visibility = View.VISIBLE
                expandDayButton.setImageResource(R.drawable.ic_expand_less)
            } else {
                timeSlotsContainer.visibility = View.GONE
                expandDayButton.setImageResource(R.drawable.ic_expand_more)
            }
        }

        // Setup add time slot functionality
        addTimeSlotButton.setOnClickListener {
            addTimeSlot(timeSlotsList)
        }

        // Add default time slot
        addTimeSlot(timeSlotsList)

        // Setup remove button
        if (dayCounter > 1) {
            removeDayButton.visibility = View.VISIBLE
            removeDayButton.setOnClickListener { removeDay(dayView) }
        }

        // Add the view to container and list
        daysContainer.addView(dayView)
        dayViews.add(dayView)

        // Increment counter
        dayCounter++

        // Show success message
        Toast.makeText(this, "Day ${dayCounter - 1} added!", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingInflatedId")
    private fun addTimeSlot(timeSlotsList: LinearLayout) {
        val inflater = LayoutInflater.from(this)
        val timeSlotView = inflater.inflate(R.layout.time_slot_item, timeSlotsList, false)

        val startTimeEditText = timeSlotView.findViewById<EditText>(R.id.start_time)
        val endTimeEditText = timeSlotView.findViewById<EditText>(R.id.end_time)
        val activityEditText = timeSlotView.findViewById<EditText>(R.id.time_slot_activity)
        val removeTimeSlotButton = timeSlotView.findViewById<ImageButton>(R.id.remove_time_slot_button)

        // Setup time pickers
        startTimeEditText.setOnClickListener { showTimePickerDialog(startTimeEditText) }
        endTimeEditText.setOnClickListener { showTimePickerDialog(endTimeEditText) }

        // Setup remove button (show only if there's more than one time slot)
        if (timeSlotsList.childCount > 0) {
            removeTimeSlotButton.visibility = View.VISIBLE
        }
        removeTimeSlotButton.setOnClickListener {
            timeSlotsList.removeView(timeSlotView)
            updateTimeSlotRemoveButtons(timeSlotsList)
        }

        timeSlotsList.addView(timeSlotView)
        updateTimeSlotRemoveButtons(timeSlotsList)
    }

    private fun updateTimeSlotRemoveButtons(timeSlotsList: LinearLayout) {
        for (i in 0 until timeSlotsList.childCount) {
            val timeSlotView = timeSlotsList.getChildAt(i)
            val removeButton = timeSlotView.findViewById<ImageButton>(R.id.remove_time_slot_button)
            removeButton.visibility = if (timeSlotsList.childCount > 1) View.VISIBLE else View.GONE
        }
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                editText.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
            },
            hour,
            minute,
            false
        ).show()
    }

    private fun removeDay(dayView: View) {
        // Remove from container and list
        daysContainer.removeView(dayView)
        dayViews.remove(dayView)

        // Recalculate day numbers
        updateDayLabels()

        Toast.makeText(this, "Day removed!", Toast.LENGTH_SHORT).show()
    }

    private fun updateDayLabels() {
        dayCounter = 1
        for (dayView in dayViews) {
            val dayLabel = dayView.findViewById<TextView>(R.id.dayLabel)
            dayLabel.text = "Day $dayCounter:"

            // Update remove button visibility
            val removeDayButton = dayView.findViewById<ImageButton>(R.id.remove_day_button)
            if (dayCounter == 1 && dayViews.size == 1) {
                removeDayButton.visibility = View.GONE
            } else {
                removeDayButton.visibility = View.VISIBLE
            }

            dayCounter++
        }
    }

    private fun getDayActivities(): List<Map<String, Any>> {
        val activities = mutableListOf<Map<String, Any>>()

        dayViews.forEachIndexed { index, dayView ->
            val activityDescription = dayView.findViewById<EditText>(R.id.activityDescription)
            val timeSlotsList = dayView.findViewById<LinearLayout>(R.id.time_slots_list)
            val activity = activityDescription.text.toString().trim()

            // Get time slots for this day
            val timeSlots = mutableListOf<Map<String, String>>()
            for (i in 0 until timeSlotsList.childCount) {
                val timeSlotView = timeSlotsList.getChildAt(i)
                val startTime = timeSlotView.findViewById<EditText>(R.id.start_time).text.toString().trim()
                val endTime = timeSlotView.findViewById<EditText>(R.id.end_time).text.toString().trim()
                val slotActivity = timeSlotView.findViewById<EditText>(R.id.time_slot_activity).text.toString().trim()

                if (startTime.isNotEmpty() && endTime.isNotEmpty() && slotActivity.isNotEmpty()) {
                    timeSlots.add(mapOf(
                        "startTime" to startTime,
                        "endTime" to endTime,
                        "activity" to slotActivity
                    ))
                }
            }

            if (activity.isNotEmpty() || timeSlots.isNotEmpty()) {
                activities.add(mapOf(
                    "day" to "Day ${index + 1}",
                    "dayNumber" to (index + 1),
                    "activity" to activity,
                    "timeSlots" to timeSlots
                ))
            }
        }

        return activities
    }

    private fun getSelectedActivities(): List<String> {
        val selectedActivities = mutableListOf<String>()

        if (checkboxHiking.isChecked) selectedActivities.add("Hiking")
        if (checkboxSwimming.isChecked) selectedActivities.add("Swimming")
        if (checkboxCamping.isChecked) selectedActivities.add("Camping")
        if (checkboxSightseeing.isChecked) selectedActivities.add("Sightseeing")

        return selectedActivities
    }

    // Add this function to build the itinerary map in the correct format
    private fun buildItineraryMap(): Map<String, List<String>> {
        val itinerary = mutableMapOf<String, List<String>>()
        dayViews.forEachIndexed { index, dayView ->
            val timeSlotsList = dayView.findViewById<LinearLayout>(R.id.time_slots_list)
            val activities = mutableListOf<String>()
            for (i in 0 until timeSlotsList.childCount) {
                val timeSlotView = timeSlotsList.getChildAt(i)
                val startTime = timeSlotView.findViewById<EditText>(R.id.start_time).text.toString().trim()
                val endTime = timeSlotView.findViewById<EditText>(R.id.end_time).text.toString().trim()
                val slotActivity = timeSlotView.findViewById<EditText>(R.id.time_slot_activity).text.toString().trim()
                Log.d("PlanTripDebug", "Day ${index+1} - TimeSlot $i: $startTime - $endTime, $slotActivity")
                if (startTime.isNotEmpty() && endTime.isNotEmpty() && slotActivity.isNotEmpty()) {
                    val obj = org.json.JSONObject()
                    obj.put("time", "$startTime - $endTime")
                    obj.put("title", slotActivity)
                    obj.put("description", "")
                    activities.add(obj.toString())
                }
            }
            val activityDescription = dayView.findViewById<EditText>(R.id.activityDescription).text.toString().trim()
            if (activityDescription.isNotEmpty()) {
                val obj = org.json.JSONObject()
                obj.put("time", "")
                obj.put("title", activityDescription)
                obj.put("description", "")
                activities.add(obj.toString())
            }
            if (activities.isNotEmpty()) {
                itinerary["day${index + 1}"] = activities
            }
        }
        Log.d("PlanTripDebug", "Final itinerary map: $itinerary")
        return itinerary
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

            // Validate required fields
            if (listOf(tripDescription, organizerName, organizerPhone, startDate, endDate, tripPrice, seatsAvailable).any { it.isEmpty() }) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get day activities
            val dayActivities = getDayActivities()
            if (dayActivities.isEmpty()) {
                Toast.makeText(this, "Please add at least one day activity!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Build itinerary map
            val itineraryMap = buildItineraryMap()
            Log.d("PlanTripDebug", "itineraryMap: $itineraryMap")

            // Validate phone number
            if (organizerPhone.length < 10) {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val organizerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val tripData = buildTripData(
                tripDescription,
                organizerName,
                organizerPhone,
                startDate,
                endDate,
                tripPrice,
                seatsAvailable,
                organizerId,
                dayActivities,
                itineraryMap // Pass itinerary map
            )

            saveTripToFirebase(tripData)
        }
    }

    // Update buildTripData to accept itineraryMap
    private fun buildTripData(
        description: String,
        organizer: String,
        phone: String,
        start: String,
        end: String,
        price: String,
        seats: String,
        userId: String,
        dayActivities: List<Map<String, Any>>,
        itineraryMap: Map<String, List<String>>
    ): Map<String, Any> {
        return mapOf(
            "placeName" to selectedPlaceName,
            "placeDescription" to selectedPlaceDescription,
            "placeImageUrl" to selectedPlaceImage,
            "tripDescription" to description,
            "organizerName" to organizer,
            "organizerPhone" to phone,
            "startDate" to start,
            "endDate" to end,
            "tripPrice" to price,
            "seatsAvailable" to seats,
            "organizerId" to userId,
            "selectedHotels" to selectedHotels.map {
                mapOf(
                    "name" to it.name,
                    "price" to it.price,
                    "rating" to it.rating,
                    "imageUrl" to it.imageUrl
                )
            },
            "selectedActivities" to getSelectedActivities(),
            "dailyActivities" to dayActivities,
            "itinerary" to itineraryMap, // Add itinerary to trip data
            "totalDays" to dayActivities.size,
            "createdAt" to System.currentTimeMillis(),
            "status" to "active"
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