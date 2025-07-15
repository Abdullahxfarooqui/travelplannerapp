package com.Travelplannerfyp.travelplannerapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.Travelplannerfyp.travelplannerapp.R
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private lateinit var profileImage: CircleImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var myBookingsButton: Button
    private lateinit var switchRoleButton: Button
    private lateinit var logoutButton: Button
    private lateinit var databaseReference: DatabaseReference
    private val auth = FirebaseAuth.getInstance()


    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        try {
            // Take persistable URI permission
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            // Save URI in SharedPreferences
            requireContext().getSharedPreferences("profile_prefs", 0)
                .edit()
                .putString("profile_image_uri", uri.toString())
                .apply()
            
            // Set the image
            profileImage.setImageURI(null) // Clear existing image
            profileImage.setImageURI(uri)  // Set new image
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("ProfileFragment", "Failed to save image: ${e.message}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().reference

        // Initialize views
        profileImage = view.findViewById(R.id.profileImage)
        nameTextView = view.findViewById(R.id.userNameText)
        emailTextView = view.findViewById(R.id.emailText)
        editProfileButton = view.findViewById(R.id.editProfileButton)
        myBookingsButton = view.findViewById(R.id.myBookingsButton)
        switchRoleButton = view.findViewById(R.id.switchRoleButton)
        logoutButton = view.findViewById(R.id.logoutButton)
        
        // Set up profile image click listener
        profileImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }
        
        // Set up edit profile button
        editProfileButton.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
            } else {
                // Redirect to login if not authenticated
                Toast.makeText(context, "Please log in to edit your profile", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            }
        }
        
        // Set up my bookings button
        myBookingsButton.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val intent = Intent(requireContext(), MyBookingsActivity::class.java)
                startActivity(intent)
            } else {
                // Redirect to login if not authenticated
                Toast.makeText(context, "Please log in to view your bookings", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                startActivity(intent)
            }
        }
        
        // Set up role switch button
        switchRoleButton.setOnClickListener {
            // Get current role from SharedPreferences
            val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", 0)
            val currentRole = sharedPreferences.getString("SelectedRole", "User")
            
            // Switch role
            val newRole = if (currentRole == "User") "Organizer" else "User"
            
            // Update SharedPreferences
            sharedPreferences.edit().apply {
                putString("SelectedRole", newRole)
                apply()
            }
            
            // Show confirmation message
            Toast.makeText(context, "Switched to $newRole role", Toast.LENGTH_SHORT).show()
            
            // Redirect to appropriate activity
            val intent = if (newRole == "Organizer") {
                Intent(requireContext(), organizermain::class.java)
            } else {
                Intent(requireContext(), MainActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        
        // Set up logout button
        logoutButton.setOnClickListener {
            // Sign out from Firebase Auth
            auth.signOut()
            
            // Clear any user-specific data from SharedPreferences
            val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", 0)
            sharedPreferences.edit().clear().apply()
            
            // Show confirmation message
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
            
            // Redirect to login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
        
        // Update switch role button text based on current role
        val sharedPreferences = requireContext().getSharedPreferences("AppPreferences", 0)
        val currentRole = sharedPreferences.getString("SelectedRole", "User")
        switchRoleButton.text = if (currentRole == "User") "Switch to Organizer" else "Switch to User"
        
        // Load profile data
        loadProfileData()
    }

    private fun loadProfileData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            nameTextView.text = "Loading..."
            emailTextView.text = "Please wait"
            val userRef = databaseReference.child("users").child(currentUser.uid)
            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val name = snapshot.child("name").getValue(String::class.java)
                        val email = snapshot.child("email").getValue(String::class.java)
                        val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                        nameTextView.text = name ?: currentUser.displayName ?: "Name not set"
                        emailTextView.text = email ?: currentUser.email ?: "Email not available"
                        // Load profile image from Firebase if available
                        if (!profileImageUrl.isNullOrEmpty()) {
                            try {
                                com.squareup.picasso.Picasso.get()
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .error(R.drawable.ic_profile_placeholder)
                                    .fit()
                                    .centerCrop()
                                    .into(profileImage)
                            } catch (e: Exception) {
                                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    } catch (e: Exception) {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    nameTextView.text = currentUser.displayName ?: "Name not set"
                    emailTextView.text = currentUser.email ?: "Email not available"
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            })
        } else {
            nameTextView.text = "Guest"
            emailTextView.text = "Not logged in"
            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            editProfileButton.isEnabled = false
            editProfileButton.text = "Login to Edit Profile"
        }
    }
    
    private fun loadLocalProfileImage() {
        try {
            // Try to load image from SharedPreferences
            val sharedPrefs = context?.getSharedPreferences("profile_prefs", 0)
            val imageUriString = sharedPrefs?.getString("profile_image_uri", null)
            
            if (!imageUriString.isNullOrEmpty()) {
                try {
                    val imageUri = Uri.parse(imageUriString)
                    
                    // Check if we have permission to access this URI
                    val hasPermission = context?.contentResolver?.persistedUriPermissions?.any { 
                        it.uri == imageUri && it.isReadPermission 
                    } ?: false
                    
                    if (hasPermission) {
                        // We have permission, try to load the image
                        context?.contentResolver?.openInputStream(imageUri)?.use { _ ->
                            // If we can open the stream, we have proper access
                            profileImage.setImageURI(null) // Clear any existing image
                            profileImage.setImageURI(imageUri) // Set new image
                            Log.d("ProfileFragment", "Loaded profile image from local storage")
                        } ?: throw SecurityException("Cannot access image content")
                    } else {
                        throw SecurityException("No permission to access image")
                    }
                } catch (e: SecurityException) {
                    Log.e("ProfileFragment", "Security exception when accessing local image", e)
                    // Clear invalid URI and set default image
                    sharedPrefs.edit().remove("profile_image_uri").apply()
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error loading local profile image", e)
                    // Clear invalid URI and set default image
                    sharedPrefs.edit().remove("profile_image_uri").apply()
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                // No local image found, use placeholder
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error in loadLocalProfileImage", e)
            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }
    
    private fun loadCachedProfileData(currentUser: com.google.firebase.auth.FirebaseUser) {
        try {
            // Try to load profile data from SharedPreferences
            val sharedPrefs = context?.getSharedPreferences("user_profile_data", 0)
            if (sharedPrefs != null) {
                val name = sharedPrefs.getString("fullName", null)
                val email = sharedPrefs.getString("email", null)
                
                // Update UI with cached data if available
                if (!name.isNullOrEmpty()) {
                    nameTextView.text = name
                } else {
                    nameTextView.text = currentUser.displayName ?: "Name not set"
                }
                
                if (!email.isNullOrEmpty()) {
                    emailTextView.text = email
                } else {
                    emailTextView.text = currentUser.email ?: "Email not available"
                }
                
                Log.d("ProfileFragment", "Loaded profile data from cache")
            }
            
            // Try to load local profile image
            loadLocalProfileImage()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error loading cached profile data", e)
            // Fall back to basic info from FirebaseUser
            nameTextView.text = currentUser.displayName ?: "Name not set"
            emailTextView.text = currentUser.email ?: "Email not available"
        }
    }
}
