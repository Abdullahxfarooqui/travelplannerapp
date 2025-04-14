package com.example.travelplannerapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        // Load user profile data
        loadProfileData()

        // Set edit button click listener
        editProfileButton.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        // Set profile image click listener
        profileImage.setOnClickListener {
            try {
                pickImage.launch(arrayOf("image/*"))
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open image picker: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Load and display saved profile image URI (safely)
        val sharedPrefs = requireContext().getSharedPreferences("profile_prefs", 0)
        val savedImageUriString = sharedPrefs.getString("profile_image_uri", null)

        savedImageUriString?.let { savedUri ->
            try {
                val uri = Uri.parse(savedUri)
                // First check if we have persisted permissions
                val hasPermission = requireContext().contentResolver.persistedUriPermissions.any { 
                    it.uri == uri && it.isReadPermission
                }
                
                if (hasPermission) {
                    try {
                        // Verify we can actually access the content
                        requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                            // If we can open the stream, we have proper access
                            profileImage.setImageURI(null) // Clear any existing image
                            profileImage.setImageURI(uri)  // Set new image
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
                sharedPrefs.edit().remove("profile_image_uri").apply()
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                Log.e("ProfileFragment", "Security exception: ${e.message}")
            } catch (e: Exception) {
                // Handle parsing errors or other exceptions
                sharedPrefs.edit().remove("profile_image_uri").apply()
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                Toast.makeText(context, "Error loading saved image", Toast.LENGTH_SHORT).show()
                Log.e("ProfileFragment", "Failed to load image URI: ${e.message}")
            }
        }
    }

    private fun loadProfileData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userRef = databaseReference.child("users").child(currentUser.uid)
            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("fullName").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)
                    val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                    // Load profile image from Firebase if available
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Picasso.get()
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .into(profileImage)
                    } else {
                        profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                    }

                    if (name.isNullOrEmpty() && email.isNullOrEmpty()) {
                        // If no data exists yet, use the basic user info
                        nameTextView.text = currentUser.displayName ?: "Name not set"
                        emailTextView.text = currentUser.email ?: "Email not available"
                    } else {
                        nameTextView.text = name ?: currentUser.displayName ?: "Name not set"
                        emailTextView.text = email ?: currentUser.email ?: "Email not available"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileFragment", "Error loading profile data: ${error.message}")
                    Toast.makeText(context, "Failed to load profile data", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            nameTextView.text = "Guest"
            emailTextView.text = "Not logged in"
        }
    }
}
