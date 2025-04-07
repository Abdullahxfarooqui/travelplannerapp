package com.example.travelplannerapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class PropertyListingActivity : AppCompatActivity() {

    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var locationEditText: TextInputEditText
    private lateinit var priceEditText: TextInputEditText
    private lateinit var guestsEditText: TextInputEditText
    private lateinit var addPhotoButton: Button
    private lateinit var submitButton: Button
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var photoAdapter: PropertyPhotoAdapter

    private val storage = FirebaseStorage.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            photoAdapter.addPhoto(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_listing)

        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize views
        titleEditText = findViewById(R.id.titleEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        locationEditText = findViewById(R.id.locationEditText)
        priceEditText = findViewById(R.id.priceEditText)
        guestsEditText = findViewById(R.id.guestsEditText)
        addPhotoButton = findViewById(R.id.addPhotoButton)
        submitButton = findViewById(R.id.submitButton)
        photosRecyclerView = findViewById(R.id.photosRecyclerView)

        // Set up photo recycler view
        photoAdapter = PropertyPhotoAdapter { position ->
            photoAdapter.removePhoto(position)
        }
        photosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photosRecyclerView.adapter = photoAdapter

        // Set up button click listeners
        addPhotoButton.setOnClickListener {
            getContent.launch("image/*")
        }

        submitButton.setOnClickListener {
            if (validateInputs()) {
                uploadListing()
            }
        }

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun validateInputs(): Boolean {
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()
        val priceText = priceEditText.text.toString().trim()
        val guestsText = guestsEditText.text.toString().trim()

        if (title.isEmpty()) {
            titleEditText.error = "Title is required"
            return false
        }

        if (description.isEmpty()) {
            descriptionEditText.error = "Description is required"
            return false
        }

        if (location.isEmpty()) {
            locationEditText.error = "Location is required"
            return false
        }

        if (priceText.isEmpty()) {
            priceEditText.error = "Price is required"
            return false
        }

        if (guestsText.isEmpty()) {
            guestsEditText.error = "Number of guests is required"
            return false
        }

        if (photoAdapter.getPhotoUris().isEmpty()) {
            Toast.makeText(this, "Please add at least one photo", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun uploadListing() {
        // Show loading indicator
        submitButton.isEnabled = false
        submitButton.text = "Uploading..."

        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()
        val price = priceEditText.text.toString().toDouble()
        val maxGuests = guestsEditText.text.toString().toInt()
        val photoUris = photoAdapter.getPhotoUris()

        // Generate a unique ID for the property
        val propertyId = database.child("properties").push().key ?: UUID.randomUUID().toString()
        val imageUrls = mutableListOf<String>()

        // Upload each image to Firebase Storage
        var uploadedCount = 0
        for ((index, uri) in photoUris.withIndex()) {
            val imageRef = storage.reference.child("property_images/${propertyId}/${index}.jpg")
            val uploadTask = imageRef.putFile(uri)

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                imageRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result.toString()
                    imageUrls.add(downloadUrl)
                    uploadedCount++

                    // When all images are uploaded, save the property listing
                    if (uploadedCount == photoUris.size) {
                        savePropertyToDatabase(propertyId, title, description, location, price, maxGuests, imageUrls)
                    }
                } else {
                    // Handle failure
                    submitButton.isEnabled = true
                    submitButton.text = "Submit Listing"
                    Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // If no photos to upload, save property directly
        if (photoUris.isEmpty()) {
            savePropertyToDatabase(propertyId, title, description, location, price, maxGuests, imageUrls)
        }
    }

    private fun savePropertyToDatabase(propertyId: String, title: String, description: String, 
                                      location: String, price: Double, maxGuests: Int, imageUrls: List<String>) {
        // Create property object
        val property = PropertyListing(
            id = propertyId,
            title = title,
            description = description,
            location = location,
            pricePerNight = price,
            maxGuests = maxGuests,
            imageUrls = imageUrls,
            ownerId = "user_id" // In a real app, this would be the current user's ID
        )

        // Save to Firebase
        database.child("properties").child(propertyId).setValue(property)
            .addOnSuccessListener {
                Toast.makeText(this, "Property listed successfully!", Toast.LENGTH_SHORT).show()
                finish() // Return to previous screen
            }
            .addOnFailureListener { e ->
                submitButton.isEnabled = true
                submitButton.text = "Submit Listing"
                Toast.makeText(this, "Failed to save property: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}