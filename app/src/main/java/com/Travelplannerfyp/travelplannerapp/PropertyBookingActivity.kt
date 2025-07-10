package com.Travelplannerfyp.travelplannerapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.Travelplannerfyp.travelplannerapp.models.Booking
import com.Travelplannerfyp.travelplannerapp.models.BookingType
import com.Travelplannerfyp.travelplannerapp.PropertyListing
import com.Travelplannerfyp.travelplannerapp.repository.BookingRepository
import com.Travelplannerfyp.travelplannerapp.utils.CurrencyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class PropertyBookingActivity : AppCompatActivity() {
    
    @Inject
    lateinit var bookingRepository: BookingRepository
    
    @Inject
    lateinit var auth: FirebaseAuth
    
    private lateinit var propertyTitleTextView: TextView
    private lateinit var propertyLocationTextView: TextView
    private lateinit var propertyImageView: ImageView
    private lateinit var checkInDateEditText: TextInputEditText
    private lateinit var checkOutDateEditText: TextInputEditText
    private lateinit var checkInTimeEditText: TextInputEditText
    private lateinit var checkOutTimeEditText: TextInputEditText
    private lateinit var guestsSpinner: Spinner
    private lateinit var specialRequestsEditText: TextInputEditText
    private lateinit var totalAmountTextView: TextView
    private lateinit var basePriceTextView: TextView
    private lateinit var cleaningFeeTextView: TextView
    private lateinit var securityDepositTextView: TextView
    private lateinit var serviceFeeTextView: TextView
    private lateinit var bookNowButton: MaterialButton
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var contentLayout: ScrollView
    
    private var property: PropertyListing? = null
    private var selectedCheckInDate: Calendar? = null
    private var selectedCheckOutDate: Calendar? = null
    private var selectedCheckInTime: String = ""
    private var selectedCheckOutTime: String = ""
    private var numberOfGuests: Int = 1
    private var numberOfNights: Int = 1
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    companion object {
        private const val TAG = "PropertyBookingActivity"
        const val EXTRA_PROPERTY_ID = "property_id"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_booking)
        
        initializeViews()
        setupToolbar()
        loadPropertyDetails()
        setupDatePickers()
        setupTimePickers()
        setupGuestsSpinner()
        setupBookButton()
    }
    
    private fun initializeViews() {
        propertyTitleTextView = findViewById(R.id.propertyTitleTextView)
        propertyLocationTextView = findViewById(R.id.propertyLocationTextView)
        propertyImageView = findViewById(R.id.propertyImageView)
        checkInDateEditText = findViewById(R.id.checkInDateEditText)
        checkOutDateEditText = findViewById(R.id.checkOutDateEditText)
        checkInTimeEditText = findViewById(R.id.checkInTimeEditText)
        checkOutTimeEditText = findViewById(R.id.checkOutTimeEditText)
        guestsSpinner = findViewById(R.id.guestsSpinner)
        specialRequestsEditText = findViewById(R.id.specialRequestsEditText)
        totalAmountTextView = findViewById(R.id.totalAmountTextView)
        basePriceTextView = findViewById(R.id.basePriceTextView)
        cleaningFeeTextView = findViewById(R.id.cleaningFeeTextView)
        securityDepositTextView = findViewById(R.id.securityDepositTextView)
        serviceFeeTextView = findViewById(R.id.serviceFeeTextView)
        bookNowButton = findViewById(R.id.bookNowButton)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        contentLayout = findViewById(R.id.contentLayout)
    }
    
    private fun setupToolbar() {
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Book Property"
        }
    }
    
    private fun loadPropertyDetails() {
        val propertyId = intent.getStringExtra(EXTRA_PROPERTY_ID)
        if (propertyId == null) {
            Toast.makeText(this, "Property ID is required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Load property details from Firebase
        val database = com.google.firebase.database.FirebaseDatabase.getInstance()
        val propertyRef = database.getReference("properties").child(propertyId)
        
        propertyRef.get().addOnSuccessListener { snapshot ->
            property = snapshot.getValue(PropertyListing::class.java)
            property?.let { updatePropertyUI(it) }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error loading property: ${e.message}")
            Toast.makeText(this, "Failed to load property details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun updatePropertyUI(property: PropertyListing) {
        this.property = property
        propertyTitleTextView.text = property.title
        propertyLocationTextView.text = property.location
        
        // Load property image if available
        if (property.imageUrls.isNotEmpty()) {
            // Use your preferred image loading library (Picasso, Glide, etc.)
            // Picasso.get().load(property.imageUrls[0]).into(propertyImageView)
        }
        
        // Set default times
        selectedCheckInTime = property.checkInTime.ifEmpty { "15:00" }
        selectedCheckOutTime = property.checkOutTime.ifEmpty { "11:00" }
        checkInTimeEditText.setText(selectedCheckInTime)
        checkOutTimeEditText.setText(selectedCheckOutTime)
        
        // Set max guests for spinner
        setupGuestsSpinner()
        
        // Calculate initial pricing
        calculatePricing()
    }
    
    private fun setupDatePickers() {
        checkInDateEditText.setOnClickListener {
            showDatePicker { date ->
                selectedCheckInDate = date
                checkInDateEditText.setText(dateFormat.format(date.time))
                validateDates()
                calculatePricing()
            }
        }
        
        checkOutDateEditText.setOnClickListener {
            showDatePicker { date ->
                selectedCheckOutDate = date
                checkOutDateEditText.setText(dateFormat.format(date.time))
                validateDates()
                calculatePricing()
            }
        }
    }
    
    private fun setupTimePickers() {
        checkInTimeEditText.setOnClickListener {
            showTimePicker { time ->
                selectedCheckInTime = time
                checkInTimeEditText.setText(time)
            }
        }
        
        checkOutTimeEditText.setOnClickListener {
            showTimePicker { time ->
                selectedCheckOutTime = time
                checkOutTimeEditText.setText(time)
            }
        }
    }
    
    private fun setupGuestsSpinner() {
        val maxGuests = property?.maxGuests ?: 6
        val guestOptions = (1..maxGuests).toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, guestOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        guestsSpinner.adapter = adapter
        
        guestsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                numberOfGuests = guestOptions[position]
                calculatePricing()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupBookButton() {
        bookNowButton.setOnClickListener {
            if (validateForm()) {
                showBookingConfirmation()
            }
        }
    }
    
    private fun showDatePicker(onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(selectedDate)
        }, year, month, day).show()
    }
    
    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val time = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(time)
        }, hour, minute, true).show()
    }
    
    private fun validateDates(): Boolean {
        if (selectedCheckInDate == null || selectedCheckOutDate == null) {
            return false
        }
        
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        // Check if check-in date is in the future
        if (selectedCheckInDate!!.before(today)) {
            checkInDateEditText.error = "Check-in date must be in the future"
            return false
        }
        
        // Check if check-out date is after check-in date
        if (selectedCheckOutDate!!.before(selectedCheckInDate) || selectedCheckOutDate!!.equals(selectedCheckInDate)) {
            checkOutDateEditText.error = "Check-out date must be after check-in date"
            return false
        }
        
        // Calculate number of nights
        val diffInMillis = selectedCheckOutDate!!.timeInMillis - selectedCheckInDate!!.timeInMillis
        numberOfNights = (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
        
        return true
    }
    
    private fun calculatePricing() {
        val property = this.property ?: return
        val basePrice = property.pricePerNight * numberOfNights
        val cleaningFee = property.cleaningFee
        val securityDeposit = property.securityDeposit
        val serviceFee = basePrice * 0.10 // 10% service fee
        val totalAmount = basePrice + cleaningFee + securityDeposit + serviceFee
        basePriceTextView.text = CurrencyUtils.formatAsPKR(basePrice)
        cleaningFeeTextView.text = CurrencyUtils.formatAsPKR(cleaningFee)
        securityDepositTextView.text = CurrencyUtils.formatAsPKR(securityDeposit)
        serviceFeeTextView.text = CurrencyUtils.formatAsPKR(serviceFee)
        totalAmountTextView.text = CurrencyUtils.formatAsPKR(totalAmount)
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        if (selectedCheckInDate == null) {
            checkInDateEditText.error = "Please select check-in date"
            isValid = false
        }
        
        if (selectedCheckOutDate == null) {
            checkOutDateEditText.error = "Please select check-out date"
            isValid = false
        }
        
        if (!validateDates()) {
            isValid = false
        }
        
        if (selectedCheckInTime.isEmpty()) {
            checkInTimeEditText.error = "Please select check-in time"
            isValid = false
        }
        
        if (selectedCheckOutTime.isEmpty()) {
            checkOutTimeEditText.error = "Please select check-out time"
            isValid = false
        }
        
        return isValid
    }
    
    private fun showBookingConfirmation() {
        val property = this.property ?: return
        val message = """
            Confirm your booking:
            
            Property: ${property.title}
            Location: ${property.location}
            Check-in: ${dateFormat.format(selectedCheckInDate!!.time)} at $selectedCheckInTime
            Check-out: ${dateFormat.format(selectedCheckOutDate!!.time)} at $selectedCheckOutTime
            Guests: $numberOfGuests
            Nights: $numberOfNights
            Total: ${CurrencyUtils.formatAsPKR(calculateTotalAmount())}
            
            Special Requests: ${specialRequestsEditText.text.toString().ifEmpty { "None" }}
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Confirm Booking")
            .setMessage(message)
            .setPositiveButton("Confirm Booking") { _, _ ->
                createBooking()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun calculateTotalAmount(): Double {
        val property = this.property ?: return 0.0
        val basePrice = property.pricePerNight * numberOfNights
        val cleaningFee = property.cleaningFee
        val securityDeposit = property.securityDeposit
        val serviceFee = basePrice * 0.10
        return basePrice + cleaningFee + securityDeposit + serviceFee
    }
    
    private fun createBooking() {
        val property = this.property ?: return
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login to book a property", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Creating property booking for user: ${currentUser.uid}")
        Log.d(TAG, "Property details - ID: ${property.id}, Title: ${property.title}")
        
        showLoading(true)
        
        val booking = Booking(
            userId = currentUser.uid, // Ensure user ID is set
            bookingType = BookingType.PROPERTY,
            itemId = property.id,
            itemName = property.title,
            itemImageUrl = property.imageUrls.firstOrNull() ?: "",
            startDate = dateFormat.format(selectedCheckInDate!!.time),
            endDate = dateFormat.format(selectedCheckOutDate!!.time),
            checkInTime = selectedCheckInTime,
            checkOutTime = selectedCheckOutTime,
            numberOfGuests = numberOfGuests,
            numberOfNights = numberOfNights,
            totalAmount = calculateTotalAmount(),
            basePrice = property.pricePerNight * numberOfNights,
            cleaningFee = property.cleaningFee,
            securityDeposit = property.securityDeposit,
            serviceFee = (property.pricePerNight * numberOfNights) * 0.10,
            specialRequests = specialRequestsEditText.text.toString(),
            hostId = property.hostId,
            hostName = property.hostName,
            hostPhone = property.hostPhoneNumber
        )
        
        Log.d(TAG, "Booking object created - UserID: ${booking.userId}, ItemID: ${booking.itemId}, Amount: ${booking.totalAmount}")
        
        lifecycleScope.launch {
            try {
                val result = bookingRepository.createBooking(booking)
                if (result.isSuccess) {
                    val bookingId = result.getOrNull()
                    showBookingSuccess(bookingId ?: "")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    showBookingError(error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating booking: ${e.message}")
                showBookingError(e.message ?: "Unknown error")
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showBookingSuccess(bookingId: String) {
        AlertDialog.Builder(this)
            .setTitle("Booking Confirmed!")
            .setMessage("Your booking has been successfully created. Booking ID: $bookingId")
            .setPositiveButton("View My Bookings") { _, _ ->
                // Navigate to bookings list
                finish()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun showBookingError(error: String) {
        AlertDialog.Builder(this)
            .setTitle("Booking Failed")
            .setMessage("Failed to create booking: $error")
            .setPositiveButton("OK", null)
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