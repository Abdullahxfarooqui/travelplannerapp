package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.Travelplannerfyp.travelplannerapp.R

class ProfileActivity : AppCompatActivity() {
    private lateinit var profilePic: ShapeableImageView
    private lateinit var changePhotoButton: MaterialButton
    private lateinit var fullNameInput: TextInputEditText
    private lateinit var bioInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var locationInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var changePasswordButton: MaterialButton
    private lateinit var logoutButton: MaterialButton
    private lateinit var deleteAccountButton: MaterialButton
    
    // TextInputLayouts for validation
    private lateinit var fullNameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var phoneLayout: TextInputLayout
    private lateinit var locationLayout: TextInputLayout
    private lateinit var bioLayout: TextInputLayout
    
    private var selectedImageUri: Uri? = null
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        changePhotoButton.isEnabled = true

        if (uri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        try {
            Log.d("ProfileActivity", "Image selected: $uri")

            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            selectedImageUri = uri
            Toast.makeText(this, "Loading image...", Toast.LENGTH_SHORT).show()

            contentResolver.openInputStream(uri)?.use { stream ->
                profilePic.setImageURI(null)
                profilePic.setImageURI(uri)

                getSharedPreferences("profile_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("profile_image_uri", uri.toString())
                    .apply()

                Toast.makeText(this, "Image loaded successfully", Toast.LENGTH_SHORT).show()
            } ?: run {
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

        initializeViews()
        setupValidation()
        loadUserData()
        setupClickListeners()
    }

    private fun initializeViews() {
        profilePic = findViewById(R.id.profilePic)
        changePhotoButton = findViewById(R.id.changePhotoButton)
        fullNameInput = findViewById(R.id.fullNameInput)
        bioInput = findViewById(R.id.bioInput)
        emailInput = findViewById(R.id.emailInput)
        phoneInput = findViewById(R.id.phoneInput)
        locationInput = findViewById(R.id.locationInput)
        saveButton = findViewById(R.id.saveButton)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        logoutButton = findViewById(R.id.logoutButton)
        deleteAccountButton = findViewById(R.id.deleteAccountButton)
        
        // Initialize TextInputLayouts
        fullNameLayout = findViewById(R.id.fullNameLayout)
        emailLayout = findViewById(R.id.emailLayout)
        phoneLayout = findViewById(R.id.phoneLayout)
        locationLayout = findViewById(R.id.locationLayout)
        bioLayout = findViewById(R.id.bioLayout)

        profilePic.setBackgroundResource(android.R.drawable.list_selector_background)
    }

    private fun setupValidation() {
        // Add text change listeners to all required fields
        val requiredFields = listOf(
            fullNameInput to fullNameLayout,
            emailInput to emailLayout,
            phoneInput to phoneLayout,
            locationInput to locationLayout,
            bioInput to bioLayout
        )

        requiredFields.forEach { (editText, layout) ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateField(editText, layout)
                    updateSaveButtonState()
                }
            })
        }
    }

    private fun validateField(editText: TextInputEditText, layout: TextInputLayout) {
        val text = editText.text.toString().trim()
        if (text.isEmpty()) {
            layout.error = "This field is required"
            layout.isErrorEnabled = true
        } else {
            layout.error = null
            layout.isErrorEnabled = false
        }
    }

    private fun updateSaveButtonState() {
        val name = fullNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val bio = bioInput.text.toString().trim()

        val isValid = name.isNotEmpty() && email.isNotEmpty() && phone.isNotEmpty() && 
                     location.isNotEmpty() && bio.isNotEmpty()

        saveButton.isEnabled = isValid
        saveButton.alpha = if (isValid) 1.0f else 0.5f
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view profile data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
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
            // --- FIX: Load profile image from Firebase using ProfileImageManager (Glide/Picasso) ---
            if (!profileImageUrl.isNullOrEmpty()) {
                Log.d("ProfileActivity", "Loading profile image from URL: $profileImageUrl")
                try {
                    com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.loadProfileImage(profilePic, profileImageUrl)
                } catch (e: Exception) {
                    Log.e("ProfileActivity", "Error loading profile image: ", e)
                    profilePic.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                Log.d("ProfileActivity", "No profile image URL found, using placeholder")
                profilePic.setImageResource(R.drawable.ic_profile_placeholder)
            }
            updateSaveButtonState()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load profile data: ${e.message}", Toast.LENGTH_LONG).show()
            profilePic.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun setupClickListeners() {
        changePhotoButton.setOnClickListener {
            changePhotoButton.isEnabled = false
            pickImage.launch(arrayOf("image/*"))
        }
        
        // Add debug click listener for profile image
        profilePic.setOnClickListener {
            Log.d("ProfileActivity", "Profile image clicked")
            // Test loading the current profile image URL
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val userRef = database.child("users").child(currentUser.uid)
                userRef.child("profileImageUrl").get().addOnSuccessListener { snapshot ->
                    val imageUrl = snapshot.getValue(String::class.java)
                    Log.d("ProfileActivity", "Current profile image URL: $imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Log.d("ProfileActivity", "Reloading profile image...")
                        com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.loadProfileImage(profilePic, imageUrl)
                    }
                }
            }
        }

        saveButton.setOnClickListener {
            if (validateAllFields()) {
                saveProfile()
            } else {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        changePasswordButton.setOnClickListener {
            showChangePasswordDialog()
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmation()
        }

        deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmation()
        }
    }

    private fun validateAllFields(): Boolean {
        val name = fullNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val bio = bioInput.text.toString().trim()

        var isValid = true

        if (name.isEmpty()) {
            fullNameLayout.error = "Name is required"
            fullNameLayout.isErrorEnabled = true
            isValid = false
        }

        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            emailLayout.isErrorEnabled = true
            isValid = false
        }

        if (phone.isEmpty()) {
            phoneLayout.error = "Phone is required"
            phoneLayout.isErrorEnabled = true
            isValid = false
        }

        if (location.isEmpty()) {
            locationLayout.error = "Location is required"
            locationLayout.isErrorEnabled = true
            isValid = false
        }

        if (bio.isEmpty()) {
            bioLayout.error = "Bio is required"
            bioLayout.isErrorEnabled = true
            isValid = false
        }

        return isValid
    }

    private fun saveProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to update profile", Toast.LENGTH_SHORT).show()
            return
        }
        val name = fullNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val bio = bioInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val userData = mapOf(
            "name" to name,
            "email" to email,
            "bio" to bio,
            "phone" to phone,
            "location" to location
        )
        saveButton.isEnabled = false
        saveButton.text = "Saving..."
        if (selectedImageUri != null) {
            Log.d("ProfileActivity", "Starting profile image upload for user: ${currentUser.uid}")
            Toast.makeText(this, "Uploading profile image...", Toast.LENGTH_SHORT).show()
            com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.saveProfileImageToDatabase(
                this,
                selectedImageUri!!,
                currentUser.uid,
                onSuccess = { dbUrl ->
                    Log.d("ProfileActivity", "Profile image upload successful, dbUrl: $dbUrl")
                    val updatedData = userData + ("profileImageUrl" to dbUrl)
                    updateUserProfile(currentUser.uid, updatedData)
                    // Refresh preview using ImageDatabaseLoader for consistency
                    com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.loadProfileImage(profilePic, dbUrl)
                    Log.d("ProfileActivity", "Profile image loaded into ImageView")
                },
                onFailure = { e ->
                    Log.e("ProfileActivity", "Profile image upload failed: ${e.message}", e)
                    Toast.makeText(this, "Failed to upload profile image: ${e.message}", Toast.LENGTH_LONG).show()
                    updateUserProfile(currentUser.uid, userData)
                }
            )
        } else {
            updateUserProfile(currentUser.uid, userData)
        }
    }

    private fun updateUserProfile(userId: String, userData: Map<String, String>) {
        database.child("users").child(userId).updateChildren(userData)
            .addOnSuccessListener {
                saveButton.text = "Save Profile"
                saveButton.isEnabled = true
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                
                val prefs = getSharedPreferences("user_profile_data", MODE_PRIVATE)
                prefs.edit().apply {
                    userData.forEach { (k, v) -> putString(k, v) }
                    apply()
                }
            }
            .addOnFailureListener { e ->
                saveButton.text = "Save Profile"
                saveButton.isEnabled = true
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showChangePasswordDialog() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to change password", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ChangePasswordActivity::class.java)
        startActivity(intent)
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                
                // Navigate to login screen
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showDeleteAccountConfirmation() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to delete account", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteUserAccount(currentUser)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUserAccount(user: com.google.firebase.auth.FirebaseUser) {
        // Delete user data from Firebase Database
        database.child("users").child(user.uid).removeValue()
            .addOnSuccessListener {
                // Delete the user account
                user.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                        
                        // Navigate to login screen
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to delete account: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete user data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
