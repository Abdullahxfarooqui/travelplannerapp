package com.Travelplannerfyp.travelplannerapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.Travelplannerfyp.travelplannerapp.models.Booking
import com.Travelplannerfyp.travelplannerapp.models.BookingType
import com.Travelplannerfyp.travelplannerapp.Trip
import com.Travelplannerfyp.travelplannerapp.repository.BookingRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils

@AndroidEntryPoint
class TripBookingActivity : AppCompatActivity() {
    
    @Inject
    lateinit var bookingRepository: BookingRepository
    
    @Inject
    lateinit var auth: FirebaseAuth
    
    private lateinit var tripNameTextView: TextView
    private lateinit var tripLocationTextView: TextView
    private lateinit var tripImageView: ImageView
    private lateinit var tripDescriptionTextView: TextView
    private lateinit var organizerNameTextView: TextView
    private lateinit var organizerPhoneTextView: TextView
    private lateinit var startDateTextView: TextView
    private lateinit var endDateTextView: TextView
    private lateinit var availableSeatsTextView: TextView
    private lateinit var seatsSpinner: Spinner
    private lateinit var specialRequestsEditText: TextInputEditText
    private lateinit var totalAmountTextView: TextView
    private lateinit var pricePerPersonTextView: TextView
    private lateinit var serviceFeeTextView: TextView
    private lateinit var bookNowButton: MaterialButton
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var contentLayout: ScrollView
    
    private var trip: Trip? = null
    private var numberOfSeats: Int = 1
    private var availableSeats: Int = 0
    private var tripKey: String? = null
    private var tripValueListener: ValueEventListener? = null
    
    companion object {
        private const val TAG = "TripBookingActivity"
        const val EXTRA_TRIP_ID = "trip_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "TripBookingActivity onCreate started")
        setContentView(R.layout.activity_trip_booking)
        
        initializeViews()
        setupToolbar()
        setupBookButton() // Setup button first, but it will be enabled/disabled based on data
        loadTripDetails() // Load trip details last, this will update UI
        Log.d(TAG, "TripBookingActivity onCreate completed")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Remove Firebase listener to prevent memory leaks
        tripValueListener?.let { listener ->
            tripKey?.let { key ->
                FirebaseDatabase.getInstance().getReference("trips").child(key).removeEventListener(listener)
            }
        }
    }
    
    private fun initializeViews() {
        Log.d(TAG, "Initializing views")
        tripNameTextView = findViewById(R.id.tripNameTextView)
        tripLocationTextView = findViewById(R.id.tripLocationTextView)
        tripImageView = findViewById(R.id.tripImageView)
        tripDescriptionTextView = findViewById(R.id.tripDescriptionTextView)
        organizerNameTextView = findViewById(R.id.organizerNameTextView)
        organizerPhoneTextView = findViewById(R.id.organizerPhoneTextView)
        startDateTextView = findViewById(R.id.startDateTextView)
        endDateTextView = findViewById(R.id.endDateTextView)
        availableSeatsTextView = findViewById(R.id.availableSeatsTextView)
        seatsSpinner = findViewById(R.id.seatsSpinner)
        specialRequestsEditText = findViewById(R.id.specialRequestsEditText)
        totalAmountTextView = findViewById(R.id.totalAmountTextView)
        pricePerPersonTextView = findViewById(R.id.pricePerPersonTextView)
        serviceFeeTextView = findViewById(R.id.serviceFeeTextView)
        bookNowButton = findViewById(R.id.bookNowButton)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        contentLayout = findViewById(R.id.contentLayout)
        
        if (bookNowButton == null) {
            Log.e(TAG, "Book button is null!")
        } else {
            Log.d(TAG, "Book button found successfully")
            // Initially disable booking until data loads
            bookNowButton.isEnabled = false
            bookNowButton.text = "Loading..."
        }
        Log.d(TAG, "Views initialization completed")
    }
    
    private fun setupToolbar() {
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Book Trip"
        }
    }
    
    private fun loadTripDetails() {
        val tripId = intent.getStringExtra(EXTRA_TRIP_ID)
        if (tripId == null) {
            Toast.makeText(this, "Trip ID is required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        Log.d(TAG, "Loading trip details for ID: $tripId")
        
        // First, try to find the trip in Firebase by searching through all trips
        val database = FirebaseDatabase.getInstance()
        val tripsRef = database.getReference("trips")
        
        // Search for the trip by matching the tripId or by placeName and organizer
        tripsRef.orderByChild("placeName").get().addOnSuccessListener { snapshot ->
            var tripFound = false
            val typeIndicator = object : GenericTypeIndicator<Map<String, Any>>() {}
            for (tripSnapshot in snapshot.children) {
                val tripData = tripSnapshot.getValue(typeIndicator)
                if (tripData != null) {
                    val placeName = tripData["placeName"] as? String ?: ""
                    val organizerName = tripData["organizerName"] as? String ?: ""
                    val startDate = tripData["startDate"] as? String ?: ""
                    // Check if this trip matches our tripId
                    val currentTripId = "${placeName}_${organizerName}_${startDate}".replace(" ", "_")
                    if (currentTripId == tripId || tripSnapshot.key == tripId) {
                        Log.d(TAG, "Trip found in Firebase with key: ${tripSnapshot.key}")
                        tripKey = tripSnapshot.key
                        trip = createTripFromData(tripData, tripSnapshot.key ?: tripId)
                        trip?.let { updateTripUI(it) }
                        
                        // Set up real-time listener for seat updates
                        setupRealTimeSeatListener(tripSnapshot.key ?: tripId)
                        
                        tripFound = true
                        break
                    }
                }
            }
            if (!tripFound) {
                Log.d(TAG, "Trip not found in Firebase, creating from tripId")
                createTripFromTripId(tripId)
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error loading trip from Firebase: ${e.message}")
            Log.d(TAG, "Creating trip from tripId as fallback")
            createTripFromTripId(tripId)
        }
    }

    private fun createTripFromTripId(tripId: String) {
        try {
            Log.d(TAG, "Parsing tripId: $tripId")
            // Parse tripId which contains trip details in format: placeName_organizerName_startDate
            val parts = tripId.split("_")
            Log.d(TAG, "TripId parts: ${parts.joinToString(", ")}")
            if (parts.size >= 4) {
                // Format: placeName_organizerName_startDate
                val placeName = parts[0]
                val organizerName = parts[1]
                val startDate = parts[3] // The date is at index 3, not 2
                Log.d(TAG, "Parsed - Place: $placeName, Organizer: $organizerName, Date: $startDate")
                // Calculate end date (add 1 day to start date for multi-day trips)
                val endDate = calculateEndDate(startDate)
                Log.d(TAG, "Calculated end date: $endDate")
                // Create a trip with proper start and end dates
                val mockTrip = Trip(
                    id = tripId,
                    name = placeName,
                    location = "Pakistan", // Default location
                    description = "Experience the beauty of $placeName with our guided tour.",
                    organizerName = organizerName,
                    contactNumber = "+92-300-1234567", // Default contact
                    seatsAvailable = 5, // Default seats (matching TripDetailActivity)
                    startDate = startDate,
                    endDate = endDate, // Multi-day trip
                    pricePerPerson = 150.0 // Default price
                )
                Log.d(TAG, "Created multi-day trip - Start: $startDate, End: $endDate")
                trip = mockTrip
                updateTripUI(mockTrip)
                Log.d(TAG, "Created mock trip: ${mockTrip.name}")
            } else {
                Log.e(TAG, "Invalid tripId format: $tripId (expected at least 4 parts, got ${parts.size})")
                Toast.makeText(this, "Invalid trip information", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating trip from tripId: ${e.message}")
            Toast.makeText(this, "Error loading trip details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupRealTimeSeatListener(tripKey: String) {
        val database = FirebaseDatabase.getInstance()
        val tripRef = database.getReference("trips").child(tripKey)
        tripValueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val tripData = snapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
                    if (tripData != null) {
                        val newAvailableSeats = (tripData["seatsAvailable"] as? String)?.toIntOrNull() ?: 0
                        if (newAvailableSeats != availableSeats) {
                            availableSeats = newAvailableSeats
                            updateSeatAvailabilityUI()
                            if (availableSeats < 3 && availableSeats > 0) {
                                Toast.makeText(
                                    this@TripBookingActivity,
                                    "Only $availableSeats seats remaining!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing real-time seat update: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Real-time seat listener cancelled: ${error.message}")
            }
        }
        tripRef.addValueEventListener(tripValueListener!!)
    }
    
    private fun updateSeatAvailabilityUI() {
        Log.d(TAG, "Updating seat availability UI - Available: $availableSeats, Selected: $numberOfSeats")
        
        // Update the available seats text
        availableSeatsTextView.text = "Available Seats: $availableSeats"
        
        // Update the book button with remaining seats info
        if (availableSeats > 0) {
            bookNowButton.text = "Book Now ($availableSeats seats remaining)"
            bookNowButton.isEnabled = true
            
            // Update spinner if needed
            if (numberOfSeats > availableSeats) {
                numberOfSeats = availableSeats
                setupSeatsSpinner() // This will update the spinner with new max seats
            }
        } else {
            bookNowButton.text = "No Seats Available"
            bookNowButton.isEnabled = false
        }
        
        // Recalculate pricing
        calculatePricing()
    }
    
    private fun calculateEndDate(startDate: String): String {
        return try {
            // Parse the start date (assuming DD/MM/YYYY format)
            val parts = startDate.split("/")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt() - 1 // Calendar months are 0-based
                val year = parts[2].toInt()
                
                val calendar = java.util.Calendar.getInstance()
                calendar.set(year, month, day)
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1) // Add 1 day
                
                val endDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                val endMonth = calendar.get(java.util.Calendar.MONTH) + 1 // Convert back to 1-based
                val endYear = calendar.get(java.util.Calendar.YEAR)
                
                String.format("%02d/%02d/%04d", endDay, endMonth, endYear)
            } else {
                startDate // Return same date if parsing fails
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating end date: ${e.message}")
            startDate // Return same date if calculation fails
        }
    }
    
    private fun createTripFromData(data: Map<*, *>, tripKey: String): Trip {
        val seatsAvailable = (data["seatsAvailable"] as? String)?.toIntOrNull() ?: 0
        val pricePerPerson = (data["tripPrice"] as? String)?.toDoubleOrNull() ?: 150.0
        
        Log.d(TAG, "Creating trip from Firebase data - Seats: $seatsAvailable, Price: $pricePerPerson")
        
        return Trip(
            id = tripKey,
            name = data["placeName"] as? String ?: "",
            location = data["placeDescription"] as? String ?: "",
            description = data["tripDescription"] as? String ?: "",
            organizerName = data["organizerName"] as? String ?: "",
            contactNumber = data["organizerPhone"] as? String ?: "",
            seatsAvailable = seatsAvailable,
            startDate = data["startDate"] as? String ?: "",
            endDate = data["endDate"] as? String ?: "",
            pricePerPerson = pricePerPerson,
            imageUrl = data["placeImageUrl"] as? String ?: ""
        )
    }
    
    private fun updateTripUI(trip: Trip) {
        this.trip = trip
        tripNameTextView.text = trip.name
        tripLocationTextView.text = trip.location
        tripDescriptionTextView.text = trip.description
        organizerNameTextView.text = "Organizer: ${trip.organizerName}"
        organizerPhoneTextView.text = "Contact: ${trip.contactNumber}"
        
        // Format and display dates properly
        val formattedStartDate = formatDateForDisplay(trip.startDate)
        val formattedEndDate = formatDateForDisplay(trip.endDate)
        startDateTextView.text = "Start Date: $formattedStartDate"
        endDateTextView.text = "End Date: $formattedEndDate"
        
        // Ensure seat availability is valid
        availableSeats = if (trip.seatsAvailable > 0) trip.seatsAvailable else 10
        availableSeatsTextView.text = "Available Seats: $availableSeats"
        
        Log.d(TAG, "Trip UI updated - Name: ${trip.name}, Seats: $availableSeats, Start: $formattedStartDate, End: $formattedEndDate")
        
        // Load trip image if available
        if (!trip.imageUrl.isNullOrEmpty()) {
            // Use your preferred image loading library (Picasso, Glide, etc.)
            // Picasso.get().load(trip.imageUrl).into(tripImageView)
        }
        
        // Setup seats spinner and update UI
        setupSeatsSpinner()
        updateSeatAvailabilityUI()
        
        Log.d(TAG, "Trip UI update completed with $availableSeats seats available")
    }
    
    private fun formatDateForDisplay(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "TBD"
        
        return try {
            // Handle different date formats
            when {
                dateString.contains("/") -> {
                    // Format: DD/MM/YYYY
                    val parts = dateString.split("/")
                    if (parts.size == 3) {
                        val day = parts[0].toInt()
                        val month = parts[1].toInt()
                        val year = parts[2].toInt()
                        val monthNames = arrayOf(
                            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                        )
                        "$day ${monthNames[month - 1]}, $year"
                    } else {
                        dateString
                    }
                }
                dateString.contains("-") -> {
                    // Format: YYYY-MM-DD
                    val parts = dateString.split("-")
                    if (parts.size == 3) {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt()
                        val day = parts[2].toInt()
                        val monthNames = arrayOf(
                            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                        )
                        "$day ${monthNames[month - 1]}, $year"
                    } else {
                        dateString
                    }
                }
                else -> dateString
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $dateString", e)
            dateString
        }
    }
    
    private fun setupSeatsSpinner() {
        Log.d(TAG, "Setting up seats spinner with availableSeats: $availableSeats")
        
        if (availableSeats <= 0) {
            Log.w(TAG, "No seats available, hiding spinner")
            seatsSpinner.visibility = View.GONE
            return
        }
        
        // Show spinner and enable it
        seatsSpinner.visibility = View.VISIBLE
        
        val seatOptions = (1..availableSeats).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seatOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        seatsSpinner.adapter = adapter
        
        // Set default selection
        numberOfSeats = 1
        
        seatsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                numberOfSeats = seatOptions[position]
                Log.d(TAG, "Selected seats: $numberOfSeats")
                calculatePricing()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                numberOfSeats = 1
                calculatePricing()
            }
        }
        
        Log.d(TAG, "Seats spinner setup complete with $availableSeats seats available")
    }
    
    private fun setupBookButton() {
        Log.d(TAG, "Setting up book button")
        bookNowButton.setOnClickListener {
            Log.d(TAG, "Book button clicked!")
            if (validateForm()) {
                Log.d(TAG, "Form validation passed, showing confirmation")
                showBookingConfirmation()
            } else {
                Log.d(TAG, "Form validation failed")
            }
        }
        Log.d(TAG, "Book button setup complete")
    }
    
    private fun validateForm(): Boolean {
        Log.d(TAG, "Validating form - availableSeats: $availableSeats, numberOfSeats: $numberOfSeats")
        var isValid = true
        
        if (availableSeats <= 0) {
            Log.d(TAG, "Validation failed: No seats available")
            Toast.makeText(this, "No seats available for this trip", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (numberOfSeats <= 0) {
            Log.d(TAG, "Validation failed: No seats selected")
            Toast.makeText(this, "Please select number of seats", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (numberOfSeats > availableSeats) {
            Log.d(TAG, "Validation failed: Too many seats selected")
            val message = "Only $availableSeats seats are available. Please reduce the number of people or choose another trip."
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            isValid = false
        }
        
        // Additional validation for trip dates
        val trip = this.trip
        if (trip != null) {
            if (trip.startDate.isNullOrEmpty() || trip.endDate.isNullOrEmpty()) {
                Log.d(TAG, "Validation failed: Missing trip dates")
                Toast.makeText(this, "Trip dates are not properly set", Toast.LENGTH_SHORT).show()
                isValid = false
            }
        }
        
        Log.d(TAG, "Form validation result: $isValid")
        return isValid
    }
    
    private fun calculatePricing() {
        val trip = this.trip ?: return
        
        val pricePerPerson = trip.pricePerPerson
        val baseAmount = pricePerPerson * numberOfSeats
        val serviceFee = baseAmount * 0.10 // 10% service fee
        val totalAmount = baseAmount + serviceFee
        
        pricePerPersonTextView.text = CurrencyUtils.formatAsPKR(pricePerPerson) + " per person"
        serviceFeeTextView.text = CurrencyUtils.formatAsPKR(serviceFee)
        totalAmountTextView.text = CurrencyUtils.formatAsPKR(totalAmount)
    }
    
    private fun showBookingConfirmation() {
        val trip = this.trip ?: return
        
        val message = """
            Confirm your trip booking:
            
            Trip: ${trip.name}
            Location: ${trip.location}
            Organizer: ${trip.organizerName}
            Contact: ${trip.contactNumber}
            Start Date: ${trip.startDate}
            End Date: ${trip.endDate}
            Seats: $numberOfSeats
            Price per person: ${CurrencyUtils.formatAsPKR(trip.pricePerPerson)}
            Total: ${CurrencyUtils.formatAsPKR(calculateTotalAmount())}
            
            Special Requests: ${specialRequestsEditText.text.toString().ifEmpty { "None" }}
        """.trimIndent()
        
        AlertDialog.Builder(this)
            .setTitle("Confirm Trip Booking")
            .setMessage(message)
            .setPositiveButton("Confirm Booking") { _, _ ->
                createBooking()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun calculateTotalAmount(): Double {
        val trip = this.trip ?: return 0.0
        val baseAmount = trip.pricePerPerson * numberOfSeats
        val serviceFee = baseAmount * 0.10
        return baseAmount + serviceFee
    }
    
    private fun createBooking() {
        val trip = this.trip ?: return
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login to book a trip", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Creating trip booking for user: ${currentUser.uid}")
        Log.d(TAG, "Trip details - ID: ${trip.id}, Name: ${trip.name}")
        
        showLoading(true)
        
        Log.d(TAG, "Creating booking with dates - Start: '${trip.startDate}', End: '${trip.endDate}'")
        
        val booking = Booking(
            userId = currentUser.uid, // Ensure user ID is set
            bookingType = BookingType.TRIP,
            itemId = trip.id,
            itemName = trip.name ?: "",
            itemImageUrl = trip.imageUrl ?: "",
            startDate = trip.startDate ?: "",
            endDate = trip.endDate ?: "",
            numberOfGuests = numberOfSeats,
            totalAmount = calculateTotalAmount(),
            basePrice = (trip.pricePerPerson ?: 0.0) * numberOfSeats,
            serviceFee = ((trip.pricePerPerson ?: 0.0) * numberOfSeats) * 0.10,
            specialRequests = specialRequestsEditText.text.toString(),
            hostId = trip.id, // Trip organizer ID
            hostName = trip.organizerName ?: "",
            hostPhone = trip.contactNumber ?: ""
        )
        
        Log.d(TAG, "Booking object created - UserID: ${booking.userId}, ItemID: ${booking.itemId}, Amount: ${booking.totalAmount}")
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Starting Firebase write operation...")
                val result = bookingRepository.createBooking(booking)
                Log.d(TAG, "Firebase write operation completed with result: ${result.isSuccess}")
                
                if (result.isSuccess) {
                    val bookingId = result.getOrNull()
                    Log.d(TAG, "Booking created successfully with ID: $bookingId")
                    showBookingSuccess(bookingId ?: "")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "Booking creation failed: $error")
                    showBookingError(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating booking: ${e.message}")
                showBookingError(e.message ?: "Unknown error")
            } finally {
                Log.d(TAG, "Booking creation process finished")
                showLoading(false)
            }
        }
    }
    
    private fun showBookingSuccess(bookingId: String) {
        Log.d(TAG, "Showing booking success dialog for booking ID: $bookingId")
        AlertDialog.Builder(this)
            .setTitle("Booking Confirmed!")
            .setMessage("Your trip booking has been successfully created. Booking ID: $bookingId")
            .setPositiveButton("View My Bookings") { _, _ ->
                Log.d(TAG, "User chose to view bookings, finishing activity")
                // Navigate to bookings list
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showBookingError(error: String) {
        Log.e(TAG, "Showing booking error dialog: $error")
        AlertDialog.Builder(this)
            .setTitle("Booking Failed")
            .setMessage("Failed to create booking: $error")
            .setPositiveButton("OK") { _, _ ->
                Log.d(TAG, "User dismissed error dialog")
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showLoading(show: Boolean) {
        loadingProgressBar.visibility = if (show) View.VISIBLE else View.GONE
        contentLayout.visibility = if (show) View.GONE else View.VISIBLE
        bookNowButton.isEnabled = !show
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 