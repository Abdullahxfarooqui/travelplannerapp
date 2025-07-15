package com.Travelplannerfyp.travelplannerapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import com.Travelplannerfyp.travelplannerapp.R
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseException
import java.text.SimpleDateFormat
import java.util.*

class PropertyListingActivity : AppCompatActivity() {

    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var locationEditText: TextInputEditText
    private lateinit var priceEditText: TextInputEditText
    private lateinit var guestsEditText: TextInputEditText
    private lateinit var bedroomsEditText: TextInputEditText
    private lateinit var bathroomsEditText: TextInputEditText
    private lateinit var bedsEditText: TextInputEditText
    private lateinit var minimumStayEditText: TextInputEditText
    private lateinit var maximumStayEditText: TextInputEditText
    private lateinit var cleaningFeeEditText: TextInputEditText
    private lateinit var securityDepositEditText: TextInputEditText
    private lateinit var hostPhoneEditText: TextInputEditText
    private lateinit var emergencyContactEditText: TextInputEditText
    private lateinit var customRulesEditText: TextInputEditText
    private lateinit var addPhotoButton: Button
    private lateinit var submitButton: Button
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var photoAdapter: PropertyPhotoAdapter
    private lateinit var spinnerPropertyType: AutoCompleteTextView
    private lateinit var spinnerCancellationPolicy: AutoCompleteTextView
    private lateinit var houseRulesChipGroup: ChipGroup
    private lateinit var availabilityStartEditText: TextInputEditText
    private lateinit var availabilityEndEditText: TextInputEditText
    private lateinit var checkInTimeEditText: TextInputEditText
    private lateinit var checkOutTimeEditText: TextInputEditText

    private val database = FirebaseDatabase.getInstance().reference
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            photoAdapter.addPhoto(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_listing)

        // Initialize views
        initializeViews()
        setupToolbar()
        setupSpinners()
        setupDateAndTimePickers()
        setupPhotoAdapter()
        setupClickListeners()
    }

    private fun initializeViews() {
        titleEditText = findViewById(R.id.titleEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        locationEditText = findViewById(R.id.locationEditText)
        priceEditText = findViewById(R.id.priceEditText)
        guestsEditText = findViewById(R.id.guestsEditText)
        bedroomsEditText = findViewById(R.id.bedroomsEditText)
        bathroomsEditText = findViewById(R.id.bathroomsEditText)
        bedsEditText = findViewById(R.id.bedsEditText)
        minimumStayEditText = findViewById(R.id.minimumStayEditText)
        maximumStayEditText = findViewById(R.id.maximumStayEditText)
        cleaningFeeEditText = findViewById(R.id.cleaningFeeEditText)
        securityDepositEditText = findViewById(R.id.securityDepositEditText)
        hostPhoneEditText = findViewById(R.id.hostPhoneEditText)
        emergencyContactEditText = findViewById(R.id.emergencyContactEditText)
        customRulesEditText = findViewById(R.id.customRulesEditText)
        addPhotoButton = findViewById(R.id.addPhotoButton)
        submitButton = findViewById(R.id.submitButton)
        photosRecyclerView = findViewById(R.id.photosRecyclerView)
        spinnerPropertyType = findViewById(R.id.spinnerPropertyType)
        spinnerCancellationPolicy = findViewById(R.id.spinnerCancellationPolicy)
        houseRulesChipGroup = findViewById(R.id.houseRulesChipGroup)
        availabilityStartEditText = findViewById(R.id.availabilityStartEditText)
        availabilityEndEditText = findViewById(R.id.availabilityEndEditText)
        checkInTimeEditText = findViewById(R.id.checkInTimeEditText)
        checkOutTimeEditText = findViewById(R.id.checkOutTimeEditText)
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupSpinners() {
        // Property Type Spinner
        val propertyTypes = arrayOf("Apartment", "House", "Villa", "Shared Room")
        val propertyTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, propertyTypes)
        spinnerPropertyType.setAdapter(propertyTypeAdapter)

        // Cancellation Policy Spinner
        val cancellationPolicies = arrayOf("Flexible", "Moderate", "Strict")
        val cancellationPolicyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cancellationPolicies)
        spinnerCancellationPolicy.setAdapter(cancellationPolicyAdapter)
    }

    private fun setupDateAndTimePickers() {
        // Availability Start Date
        availabilityStartEditText.setOnClickListener {
            showDatePicker { date ->
                availabilityStartEditText.setText(date)
            }
        }

        // Availability End Date
        availabilityEndEditText.setOnClickListener {
            showDatePicker { date ->
                availabilityEndEditText.setText(date)
            }
        }

        // Check-in Time
        checkInTimeEditText.setOnClickListener {
            showTimePicker { time ->
                checkInTimeEditText.setText(time)
            }
        }

        // Check-out Time
        checkOutTimeEditText.setOnClickListener {
            showTimePicker { time ->
                checkOutTimeEditText.setText(time)
            }
        }
    }

    private fun setupPhotoAdapter() {
        photoAdapter = PropertyPhotoAdapter { position ->
            photoAdapter.removePhoto(position)
        }
        photosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photosRecyclerView.adapter = photoAdapter
    }

    private fun setupClickListeners() {
        addPhotoButton.setOnClickListener {
            getContent.launch("image/*")
        }

        submitButton.setOnClickListener {
            if (validateForm()) {
                uploadListing()
            }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            val dateString = dateFormat.format(selectedDate.time)
            onDateSelected(dateString)
        }, year, month, day).show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val selectedTime = Calendar.getInstance()
            selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
            selectedTime.set(Calendar.MINUTE, selectedMinute)
            val timeString = timeFormat.format(selectedTime.time)
            onTimeSelected(timeString)
        }, hour, minute, true).show()
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Required fields validation
        if (titleEditText.text.toString().trim().isEmpty()) {
            titleEditText.error = "Title is required"
            isValid = false
        }

        if (descriptionEditText.text.toString().trim().isEmpty()) {
            descriptionEditText.error = "Description is required"
            isValid = false
        }

        if (locationEditText.text.toString().trim().isEmpty()) {
            locationEditText.error = "Location is required"
            isValid = false
        }

        if (priceEditText.text.toString().trim().isEmpty()) {
            priceEditText.error = "Price is required"
            isValid = false
        }

        if (guestsEditText.text.toString().trim().isEmpty()) {
            guestsEditText.error = "Number of guests is required"
            isValid = false
        }

        if (bedroomsEditText.text.toString().trim().isEmpty()) {
            bedroomsEditText.error = "Number of bedrooms is required"
            isValid = false
        }

        if (bathroomsEditText.text.toString().trim().isEmpty()) {
            bathroomsEditText.error = "Number of bathrooms is required"
            isValid = false
        }

        if (bedsEditText.text.toString().trim().isEmpty()) {
            bedsEditText.error = "Number of beds is required"
            isValid = false
        }

        if (availabilityStartEditText.text.toString().trim().isEmpty()) {
            availabilityStartEditText.error = "Availability start date is required"
            isValid = false
        }

        if (availabilityEndEditText.text.toString().trim().isEmpty()) {
            availabilityEndEditText.error = "Availability end date is required"
            isValid = false
        }

        if (checkInTimeEditText.text.toString().trim().isEmpty()) {
            checkInTimeEditText.error = "Check-in time is required"
            isValid = false
        }

        if (checkOutTimeEditText.text.toString().trim().isEmpty()) {
            checkOutTimeEditText.error = "Check-out time is required"
            isValid = false
        }

        if (hostPhoneEditText.text.toString().trim().isEmpty()) {
            hostPhoneEditText.error = "Host phone number is required"
            isValid = false
        }

        if (photoAdapter.getPhotoUris().isEmpty()) {
            Toast.makeText(this, "Please add at least one photo", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun uploadListing() {
        submitButton.isEnabled = false
        submitButton.text = "Uploading..."

        val propertyId = database.child("properties").push().key ?: return
        val photoUris = photoAdapter.getPhotoUris()

        // Get current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid ?: return

        Log.d("PropertyListing", "Starting upload process for property: $propertyId")
        Log.d("PropertyListing", "Number of photos to upload: ${photoUris.size}")

        // Create property object
        val property = PropertyListing(
            id = propertyId,
            title = titleEditText.text.toString().trim(),
            description = descriptionEditText.text.toString().trim(),
            location = locationEditText.text.toString().trim(),
            pricePerNight = priceEditText.text.toString().toDoubleOrNull() ?: 0.0,
            maxGuests = guestsEditText.text.toString().toIntOrNull() ?: 0,
            bedrooms = bedroomsEditText.text.toString().toIntOrNull() ?: 0,
            bathrooms = bathroomsEditText.text.toString().toIntOrNull() ?: 0,
            beds = bedsEditText.text.toString().toIntOrNull() ?: 0,
            amenities = getSelectedAmenities(),
            houseRules = getHouseRules(),
            customRules = customRulesEditText.text.toString().trim(),
            cleaningFee = cleaningFeeEditText.text.toString().toDoubleOrNull() ?: 0.0,
            securityDeposit = securityDepositEditText.text.toString().toDoubleOrNull() ?: 0.0,
            availabilityStart = availabilityStartEditText.text.toString(),
            availabilityEnd = availabilityEndEditText.text.toString(),
            checkInTime = checkInTimeEditText.text.toString(),
            checkOutTime = checkOutTimeEditText.text.toString(),
            imageUrls = emptyList(),
            hostId = ownerId,
            hostName = currentUser?.displayName ?: "",
            hostPhoneNumber = hostPhoneEditText.text.toString().trim(),
            emergencyContact = emergencyContactEditText.text.toString().trim(),
            identityVerificationUrl = "",
            available = true
        )

        // Upload images first
        if (photoUris.isEmpty()) {
            Log.d("PropertyListing", "No photos to upload, saving property without images")
            savePropertyToDatabase(property, emptyList())
            return
        }

        // Upload images to Firebase Realtime Database
        val imageUrls = mutableListOf<String>()
        var uploadedCount = 0
        var hasError = false

        for ((index, uri) in photoUris.withIndex()) {
            try {
                Log.d("PropertyListing", "Processing image $index: $uri")
                
                // Verify URI is valid
                if (uri.scheme == null || uri.path == null) {
                    throw IllegalArgumentException("Invalid image URI: $uri")
                }

                // Read the image file
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    throw IllegalArgumentException("Cannot open image file")
                }

                // Convert image to Base64
                val bytes = inputStream.readBytes()
                val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                inputStream.close()

                // Create a unique image ID
                val imageId = "${propertyId}_${System.currentTimeMillis()}_${index}"
                
                // Save image to Firebase Realtime Database
                val imageRef = database.child("property_images").child(propertyId).child(imageId)
                val imageData = mapOf(
                    "data" to base64Image,
                    "timestamp" to System.currentTimeMillis(),
                    "userId" to ownerId
                )

                imageRef.setValue(imageData)
                    .addOnSuccessListener {
                        Log.d("PropertyListing", "Successfully uploaded image $index")
                        // Create a reference URL to the image in the database
                        val imageUrl = "db://property_images/$propertyId/$imageId"
                        imageUrls.add(imageUrl)
                        uploadedCount++

                        if (uploadedCount == photoUris.size && !hasError) {
                            Log.d("PropertyListing", "All images uploaded successfully, saving property")
                            savePropertyToDatabase(property.copy(imageUrls = imageUrls), imageUrls)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("PropertyListing", "Failed to upload image $index", e)
                        handleUploadError(e)
                        hasError = true
                    }

            } catch (e: Exception) {
                Log.e("PropertyListing", "Error processing image $index", e)
                handleUploadError(e)
                hasError = true
            }
        }
    }

    private fun handleUploadError(e: Exception) {
        Log.e("PropertyListing", "Upload error details", e)
        runOnUiThread {
            submitButton.isEnabled = true
            submitButton.text = "Submit Listing"
            val errorMessage = when (e) {
                is DatabaseException -> "Database error: ${e.message}"
                else -> "Failed to upload image: ${e.message}"
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun savePropertyToDatabase(property: PropertyListing, imageUrls: List<String>) {
        Log.d("PropertyListing", "Saving property to database with ${imageUrls.size} images")
        val propertyWithImages = property.copy(imageUrls = imageUrls)
        database.child("properties").child(property.id).setValue(propertyWithImages)
            .addOnSuccessListener {
                Log.d("PropertyListing", "Successfully saved property to database")
                runOnUiThread {
                    Toast.makeText(this, "Property listed successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PropertyListing", "Failed to save property to database", e)
                runOnUiThread {
                    submitButton.isEnabled = true
                    submitButton.text = "Submit Listing"
                    Toast.makeText(
                        this,
                        "Failed to save property: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun getSelectedAmenities(): List<String> {
        val amenities = mutableListOf<String>()
        
        if (findViewById<CheckBox>(R.id.checkboxWifi).isChecked) amenities.add("Wi-Fi")
        if (findViewById<CheckBox>(R.id.checkboxAC).isChecked) amenities.add("AC")
        if (findViewById<CheckBox>(R.id.checkboxTV).isChecked) amenities.add("TV")
        if (findViewById<CheckBox>(R.id.checkboxKitchen).isChecked) amenities.add("Kitchen")
        if (findViewById<CheckBox>(R.id.checkboxParking).isChecked) amenities.add("Parking")
        if (findViewById<CheckBox>(R.id.checkboxWashingMachine).isChecked) amenities.add("Washing Machine")
        if (findViewById<CheckBox>(R.id.checkboxPool).isChecked) amenities.add("Pool")
        if (findViewById<CheckBox>(R.id.checkboxPetFriendly).isChecked) amenities.add("Pet-Friendly")
        
        return amenities
    }

    private fun getHouseRules(): List<String> {
        val rules = mutableListOf<String>()
        
        if (findViewById<Chip>(R.id.chipNoSmoking).isChecked) rules.add("No Smoking")
        if (findViewById<Chip>(R.id.chipNoParties).isChecked) rules.add("No Parties")
        if (findViewById<Chip>(R.id.chipPetsAllowed).isChecked) rules.add("Pets Allowed")
        
        return rules
    }
}