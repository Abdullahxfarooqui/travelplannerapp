package com.example.travelplannerapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

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

        // Load saved image URI if exists
        getSharedPreferences("profile_prefs", MODE_PRIVATE).getString("profile_image_uri", null)?.let { savedUri ->
            try {
                val uri = Uri.parse(savedUri)
                // First check if we have persisted permissions
                val hasPermission = contentResolver.persistedUriPermissions.any { 
                    it.uri == uri && it.isReadPermission
                }
                
                if (hasPermission) {
                    try {
                        // Verify we can actually access the content
                        contentResolver.openInputStream(uri)?.use { stream ->
                            selectedImageUri = uri
                            profilePic.setImageURI(null) // Clear any existing image
                            profilePic.setImageURI(uri)  // Set new image
                        } ?: throw SecurityException("Cannot access image content")
                    } catch (e: SecurityException) {
                        throw e // Propagate security exceptions
                    } catch (e: Exception) {
                        // Handle other IO errors
                        throw SecurityException("Failed to load image: ${e.message}")
                    }
                } else {
                    throw SecurityException("No permission to access image")
                }
            } catch (e: SecurityException) {
                // Clear invalid URI and set default image
                getSharedPreferences("profile_prefs", MODE_PRIVATE)
                    .edit()
                    .remove("profile_image_uri")
                    .apply()
                profilePic.setImageResource(R.drawable.ic_profile_placeholder)
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Handle parsing errors or other exceptions
                getSharedPreferences("profile_prefs", MODE_PRIVATE)
                    .edit()
                    .remove("profile_image_uri")
                    .apply()
                profilePic.setImageResource(R.drawable.ic_profile_placeholder)
                Toast.makeText(this, "Error loading saved image", Toast.LENGTH_SHORT).show()
            }
        }

        loadUserData()

        saveButton.setOnClickListener {
            saveUserData()
        }

        val clickListener = View.OnClickListener {
            openGallery()
        }

        profilePic.setOnClickListener(clickListener)
        changePhotoButton.setOnClickListener(clickListener)
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

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view profile data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Show loading state
        val loadingToast = Toast.makeText(this, "Loading profile data...", Toast.LENGTH_SHORT)
        loadingToast.show()
        
        // Disable save button while loading
        saveButton.isEnabled = false
        
        database.child("users").child(currentUser.uid).get()
            .addOnSuccessListener { snapshot ->
                loadingToast.cancel()
                saveButton.isEnabled = true
                
                // Get profile data with fallbacks to empty string to avoid null
                val fullName = snapshot.child("fullName").getValue(String::class.java) ?: ""
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                val email = snapshot.child("email").getValue(String::class.java) ?: currentUser.email ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                val location = snapshot.child("location").getValue(String::class.java) ?: ""
                val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                
                // Set the values to the input fields
                fullNameInput.setText(fullName)
                bioInput.setText(bio)
                emailInput.setText(email)
                phoneInput.setText(phone)
                locationInput.setText(location)
                
                // If we have a profile image URL from Firebase, try to load it
                if (!profileImageUrl.isNullOrEmpty()) {
                    try {
                        // Save the URL to SharedPreferences for consistency with MainActivity
                        getSharedPreferences("profile_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("profile_image_url", profileImageUrl)
                            .apply()
                            
                        // Use ProfileImageManager to load the image consistently
                        com.example.travelplannerapp.utils.ProfileImageManager.loadProfileImage(profilePic, profileImageUrl)
                        
                        Log.d("ProfileActivity", "Loaded profile image from database URL: $profileImageUrl")
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "Error loading profile image from URL", e)
                        // If loading fails, we already have a fallback from SharedPreferences
                    }
                } else {
                    // Try to load from SharedPreferences as fallback
                    getSharedPreferences("profile_prefs", MODE_PRIVATE).getString("profile_image_uri", null)?.let { savedUri ->
                        Log.d("ProfileActivity", "No database image found, using local URI: $savedUri")
                    }
                }
            }
            .addOnFailureListener { exception ->
                loadingToast.cancel()
                saveButton.isEnabled = true
                val errorMessage = exception.message ?: "Unknown error"
                Toast.makeText(this, "Failed to load profile data: $errorMessage", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to save profile data", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Disable save button and show progress
        saveButton.isEnabled = false
        saveButton.text = "Saving..."
        
        val userData = mapOf(
            "fullName" to fullNameInput.text.toString().trim(),
            "bio" to bioInput.text.toString().trim(),
            "email" to emailInput.text.toString().trim(),
            "phone" to phoneInput.text.toString().trim(),
            "location" to locationInput.text.toString().trim()
        )

        // Handle profile image upload if a new image was selected
        selectedImageUri?.let { uri ->
            Toast.makeText(this, "Uploading profile image...", Toast.LENGTH_SHORT).show()
            saveButton.text = "Processing image..."
            
            // Show a progress dialog
            val progressDialog = android.app.ProgressDialog(this).apply {
                setTitle("Processing Profile Image")
                setMessage("Please wait...")
                setCancelable(false)
                show()
            }
            
            // Use the ProfileImageManager to save the image to Firebase Realtime Database
            com.example.travelplannerapp.utils.ProfileImageManager.saveProfileImageToDatabase(
                this,
                uri,
                currentUser.uid,
                onSuccess = { dbUrl ->
                    try {
                        if (progressDialog.isShowing) progressDialog.dismiss()
                        
                        // Save to SharedPreferences immediately for faster access next time
                        getSharedPreferences("profile_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("profile_image_uri", uri.toString())
                            .putString("profile_image_url", dbUrl)
                            .putLong("last_updated", System.currentTimeMillis())
                            .apply()
                        
                        // Update the user profile with the image URL and other data
                        val updatedData = userData + ("profileImageUrl" to dbUrl)
                        updateUserProfile(currentUser.uid, updatedData)
                    } catch (e: Exception) {
                        Log.e("ProfileActivity", "Error in success handler", e)
                        Toast.makeText(this, "Error finalizing profile update", Toast.LENGTH_SHORT).show()
                        try { if (progressDialog.isShowing) progressDialog.dismiss() } catch (_: Exception) {}
                        saveButton.isEnabled = true
                        saveButton.text = "Save Profile"
                    }
                },
                onFailure = { e ->
                    try { if (progressDialog.isShowing) progressDialog.dismiss() } catch (_: Exception) {}
                    saveButton.isEnabled = true
                    saveButton.text = "Save Profile"
                    val errorMessage = e.message ?: "Unknown error"
                    Toast.makeText(this, "Failed to upload profile image: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        } ?: updateUserProfile(currentUser.uid, userData) // If no new image, just update the profile data
    }

    private fun updateUserProfile(userId: String, userData: Map<String, String>) {
        saveButton.text = "Saving profile data..."
        
        database.child("users").child(userId).updateChildren(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                
                // Update the shared preferences with the latest data
                val prefs = getSharedPreferences("user_profile_data", MODE_PRIVATE)
                prefs.edit().apply {
                    putString("fullName", userData["fullName"])
                    putString("email", userData["email"])
                    apply()
                }
                
                // Notify any listeners that profile data has changed
                val intent = Intent("com.example.travelplannerapp.PROFILE_UPDATED")
                sendBroadcast(intent)
                
                finish()
            }
            .addOnFailureListener { exception ->
                saveButton.isEnabled = true
                saveButton.text = "Save Profile"
                val errorMessage = exception.message ?: "Unknown error"
                Toast.makeText(this, "Failed to update profile: $errorMessage", Toast.LENGTH_LONG).show()
            }
    }
}
