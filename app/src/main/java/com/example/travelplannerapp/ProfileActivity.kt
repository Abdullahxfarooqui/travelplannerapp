package com.example.travelplannerapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
            // Take persistable URI permission with proper flags
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            selectedImageUri = uri
            
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
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Failed to access image: Permission denied", Toast.LENGTH_SHORT).show()
            selectedImageUri = null
            profilePic.setImageResource(R.drawable.ic_profile_placeholder)
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
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
            changePhotoButton.isEnabled = false
            pickImage.launch(arrayOf("image/*"))
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open image picker: ${e.message}", Toast.LENGTH_SHORT).show()
            changePhotoButton.isEnabled = true
        }
    }

    private fun loadUserData() {
        auth.currentUser?.let { user ->
            database.child("users").child(user.uid).get()
                .addOnSuccessListener { snapshot ->
                    fullNameInput.setText(snapshot.child("fullName").getValue(String::class.java))
                    bioInput.setText(snapshot.child("bio").getValue(String::class.java))
                    emailInput.setText(snapshot.child("email").getValue(String::class.java) ?: user.email)
                    phoneInput.setText(snapshot.child("phone").getValue(String::class.java))
                    locationInput.setText(snapshot.child("location").getValue(String::class.java))
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserData() {
        auth.currentUser?.let { user ->
            // Disable save button to prevent multiple submissions
            saveButton.isEnabled = false

            val userData = mapOf(
                "fullName" to fullNameInput.text.toString(),
                "bio" to bioInput.text.toString(),
                "email" to emailInput.text.toString(),
                "phone" to phoneInput.text.toString(),
                "location" to locationInput.text.toString()
            )

            // Handle profile image upload if a new image was selected
            selectedImageUri?.let { uri ->
                val imageRef = storage.child("profile_images/${user.uid}.jpg")
                imageRef.putFile(uri)
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        imageRef.downloadUrl
                    }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUrl = task.result.toString()
                            val updatedData = userData + ("profileImageUrl" to downloadUrl)
                            updateUserProfile(user.uid, updatedData)
                        } else {
                            saveButton.isEnabled = true
                            Toast.makeText(this, "Failed to upload profile image", Toast.LENGTH_SHORT).show()
                        }
                    }
            } ?: updateUserProfile(user.uid, userData) // If no new image, just update the profile data
        }
    }

    private fun updateUserProfile(userId: String, userData: Map<String, String>) {
        database.child("users").child(userId).updateChildren(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                saveButton.isEnabled = true
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}
