package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.Travelplannerfyp.travelplannerapp.R

class ProfileActivity : AppCompatActivity() {
    private lateinit var profilePic: ShapeableImageView
    private lateinit var changePhotoButton: MaterialButton
    private lateinit var fullNameInput: EditText
    private lateinit var bioInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var saveButton: MaterialButton
    private var selectedImageUri: Uri? = null
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Reset button state immediately
        changePhotoButton.isEnabled = true

        // Early return if no image selected
        if (uri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        try {
            Log.d("ProfileActivity", "Image selected: $uri")

            // Take persistable URI permission with proper flags
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            selectedImageUri = uri

            // Show a loading message
            Toast.makeText(this, "Loading image...", Toast.LENGTH_SHORT).show()

            // Load the image using a safe approach
            contentResolver.openInputStream(uri)?.use { stream ->
                // If we can open the stream, we have proper access
                profilePic.setImageURI(null) // Clear any existing image
                profilePic.setImageURI(uri)  // Set new image

                // Save URI in SharedPreferences only after successful access
                getSharedPreferences("profile_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("profile_image_uri", uri.toString())
                    .apply()

                Toast.makeText(this, "Image loaded successfully", Toast.LENGTH_SHORT).show()
            } ?: run {
                // Handle case where openInputStream returns null
                throw SecurityException("Cannot access image content")
            }
        } catch (e: SecurityException) {
            Log.e("ProfileActivity", "Security exception when accessing image", e)
            Toast.makeText(this, "Failed to access image: Permission denied", Toast.LENGTH_LONG).show()
            Toast.makeText(this, "Please try selecting a different image", Toast.LENGTH_LONG).show()
            selectedImageUri = null
            profilePic.setImageResource(R.drawable.ic_profile_placeholder)
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error loading image", e)
            val errorMessage = e.message ?: "Unknown error"
            Toast.makeText(this, "Error loading image: $errorMessage", Toast.LENGTH_LONG).show()
            Toast.makeText(this, "Please try selecting a different image", Toast.LENGTH_LONG).show()
            selectedImageUri = null
            profilePic.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_activity)

        profilePic = findViewById(R.id.profilePic)
        changePhotoButton = findViewById(R.id.changePhotoButton)
        fullNameInput = findViewById(R.id.fullNameInput)
        bioInput = findViewById(R.id.bioInput)
        emailInput = findViewById(R.id.emailInput)
        phoneInput = findViewById(R.id.phoneInput)
        locationInput = findViewById(R.id.locationInput)
        saveButton = findViewById(R.id.saveButton)

        profilePic.setBackgroundResource(android.R.drawable.list_selector_background)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view profile data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load user data from Firebase
        val userRef = database.child("users").child(currentUser.uid)
        userRef.get().addOnSuccessListener { snapshot ->
            val name = snapshot.child("name").getValue(String::class.java) ?: ""
            val email = snapshot.child("email").getValue(String::class.java) ?: ""
            val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
            val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
            val location = snapshot.child("location").getValue(String::class.java) ?: ""
            val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

            fullNameInput.setText(name)
            emailInput.setText(email)
            bioInput.setText(bio)
            phoneInput.setText(phone)
            locationInput.setText(location)

            // Load profile image from Firebase if available
            if (profileImageUrl.isNotEmpty()) {
                try {
                    com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.loadProfileImage(profilePic, profileImageUrl)
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Error loading profile image from URL", e)
                    profilePic.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                // Load local image if exists
                getSharedPreferences("profile_prefs", MODE_PRIVATE).getString("profile_image_uri", null)?.let { savedUri ->
                    try {
                        val uri = Uri.parse(savedUri)
                        val hasPermission = contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
                        if (hasPermission) {
                            contentResolver.openInputStream(uri)?.use {
                                selectedImageUri = uri
                                profilePic.setImageURI(null)
                                profilePic.setImageURI(uri)
                            }
                        }
                    } catch (e: Exception) {
                        profilePic.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load profile data: ${e.message}", Toast.LENGTH_LONG).show()
        }

        changePhotoButton.setOnClickListener {
            changePhotoButton.isEnabled = false
            pickImage.launch(arrayOf("image/*"))
        }

        saveButton.setOnClickListener {
            val name = fullNameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val bio = bioInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val location = locationInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userData = mapOf(
                "name" to name,
                "email" to email,
                "bio" to bio,
                "phone" to phone,
                "location" to location
            )

            // Handle profile image upload if a new image was selected
            if (selectedImageUri != null) {
                Toast.makeText(this, "Uploading profile image...", Toast.LENGTH_SHORT).show()
                com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.saveProfileImageToDatabase(
                    this,
                    selectedImageUri!!,
                    currentUser.uid,
                    onSuccess = { dbUrl ->
                        // Update the user profile with the image URL and other data
                        val updatedData = userData + ("profileImageUrl" to dbUrl)
                        updateUserProfile(currentUser.uid, updatedData)
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Failed to upload profile image: ${e.message}", Toast.LENGTH_LONG).show()
                        updateUserProfile(currentUser.uid, userData)
                    }
                )
            } else {
                updateUserProfile(currentUser.uid, userData)
            }
        }
    }

    private fun updateUserProfile(userId: String, userData: Map<String, String>) {
        saveButton.text = "Saving profile data..."
        database.child("users").child(userId).updateChildren(userData)
            .addOnSuccessListener {
                saveButton.text = "Save Profile"
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                // Optionally, update local cache
                val prefs = getSharedPreferences("user_profile_data", MODE_PRIVATE)
                prefs.edit().apply {
                    userData.forEach { (k, v) -> putString(k, v) }
                    apply()
                }
            }
            .addOnFailureListener { e ->
                saveButton.text = "Save Profile"
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openGallery() {
        try {
            // Disable button to prevent multiple clicks
            changePhotoButton.isEnabled = false

            // Show a toast to indicate we're opening the gallery
            Toast.makeText(this, "Opening image picker...", Toast.LENGTH_SHORT).show()

            // Launch the image picker
            pickImage.launch(arrayOf("image/*"))
        } catch (e: Exception) {
            // Log the error
            Log.e("ProfileActivity", "Failed to open image picker", e)

            // Re-enable the button
            changePhotoButton.isEnabled = true

            // Show a detailed error message
            val errorMessage = e.message ?: "Unknown error"
            Toast.makeText(this, "Failed to open image picker: $errorMessage", Toast.LENGTH_LONG).show()

            // Suggest an alternative
            Toast.makeText(this, "Please try again or select a different image", Toast.LENGTH_LONG).show()
        }
    }
}
