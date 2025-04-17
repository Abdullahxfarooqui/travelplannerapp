package com.example.travelplannerapp

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
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
        submitButton.isEnabled = false
        submitButton.text = "Uploading..."
        val title = titleEditText.text.toString().trim()
        val description = descriptionEditText.text.toString().trim()
        val location = locationEditText.text.toString().trim()
        val price = priceEditText.text.toString().toDouble()
        val maxGuests = guestsEditText.text.toString().toInt()
        val photoUris = photoAdapter.getPhotoUris()
        val propertyId = database.child("properties").push().key ?: UUID.randomUUID().toString()
        val imageUrls = mutableListOf<String>()
        if (photoUris.isEmpty()) {
            savePropertyToDatabase(propertyId, title, description, location, price, maxGuests, imageUrls)
            return
        }
        uploadImagesSequentially(photoUris, propertyId, 0, imageUrls) { success ->
            if (success) {
                savePropertyToDatabase(propertyId, title, description, location, price, maxGuests, imageUrls)
            } else {
                submitButton.isEnabled = true
                submitButton.text = "Submit Listing"
                Toast.makeText(this, "Failed to upload all images. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uploadImagesSequentially(photoUris: List<Uri>, propertyId: String, index: Int, imageUrls: MutableList<String>, onComplete: (Boolean) -> Unit) {
        if (index >= photoUris.size) {
            onComplete(true)
            return
        }
        val uri = photoUris[index]
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        if (userId.isNullOrEmpty() || uri.scheme == null || uri.path == null) {
            Toast.makeText(this, "Invalid image or user ID at position ${index + 1}", Toast.LENGTH_SHORT).show()
            onComplete(false)
            return
        }
        try {
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Toast.makeText(this, "Cannot access image ${index + 1}. Please reselect the photo.", Toast.LENGTH_SHORT).show()
                onComplete(false)
                return
            }
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val baos = ByteArrayOutputStream()
            
            // Compress the image to reduce size (adjust quality as needed)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            val data = baos.toByteArray()
            inputStream.close()
            
            // Check if image is too large even after compression
            if (data.size > 1 * 1024 * 1024) { // 1MB limit for base64 encoded images
                // Try to compress further if still too large
                baos.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos)
                val compressedData = baos.toByteArray()
                
                if (compressedData.size > 1 * 1024 * 1024) {
                    Toast.makeText(this, "Image ${index + 1} is too large. Please select a smaller image.", Toast.LENGTH_SHORT).show()
                    onComplete(false)
                    return
                }
            }
            
            // Convert image to Base64 string
            val base64Image = Base64.encodeToString(data, Base64.DEFAULT)
            val timestamp = System.currentTimeMillis()
            val imageId = "${userId}_${timestamp}_${index}"
            
            // Update progress on UI thread
            runOnUiThread {
                val progress = ((index + 1) * 100) / photoUris.size
                submitButton.text = "Uploading ${progress}%"
            }
            
            // Store image data directly in Firebase Realtime Database
            val imageRef = database.child("property_images").child(propertyId).child(imageId)
            
            // Create a map with image data
            val imageData = HashMap<String, Any>()
            imageData["data"] = base64Image
            imageData["timestamp"] = timestamp
            imageData["userId"] = userId
            
            // Save image to Firebase Realtime Database
            imageRef.setValue(imageData)
                .addOnSuccessListener {
                    // Create a reference URL to the image in the database
                    val imageUrl = "db://property_images/$propertyId/$imageId"
                    imageUrls.add(imageUrl)
                    
                    // Continue with next image
                    uploadImagesSequentially(photoUris, propertyId, index + 1, imageUrls, onComplete)
                }
                .addOnFailureListener { e ->
                    runOnUiThread {
                        Toast.makeText(this, "Failed to upload image ${index + 1}: ${e.message}", Toast.LENGTH_SHORT).show()
                        onComplete(false)
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing image ${index + 1}: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete(false)
        }
    }

    private fun savePropertyToDatabase(propertyId: String, title: String, description: String, 
                                      location: String, price: Double, maxGuests: Int, imageUrls: List<String>) {
        // Get current user ID
        val currentUser = FirebaseAuth.getInstance().currentUser
        val ownerId = currentUser?.uid ?: return // Don't allow anonymous listings
        
        // Log the image URLs for debugging
        Log.d("PropertyListing", "Saving property with ${imageUrls.size} images: $imageUrls")
        
        // Create property object with proper visibility flags
        val property = PropertyListing(
            id = propertyId,
            title = title,
            description = description,
            location = location,
            pricePerNight = price,
            maxGuests = maxGuests,
            imageUrls = imageUrls,
            ownerId = ownerId,
            isAvailable = true,
            isOwnProperty = true,
            amenities = listOf() // Default empty amenities list
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