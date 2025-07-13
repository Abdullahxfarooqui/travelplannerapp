package com.Travelplannerfyp.travelplannerapp

import android.app.Activity
import android.content.Intent
import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import com.Travelplannerfyp.travelplannerapp.R

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var profileImage: ShapeableImageView

    private var currentFragment: Fragment? = null

    private val homeFragment = HomeFragment()
    private val plannedTripsFragment = UserPlannedTripsFragment()
    private val rentFragment = RentFragment()
    private val profileFragment = ProfileFragment()

    private val REQUEST_CODE_OPEN_DOCUMENT = 1001

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is not logged in
        if (firebaseAuth.currentUser == null) {
            // Redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        
        setContentView(R.layout.activity_main_bottom_nav)

        toolbar = findViewById(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        profileImage = findViewById(R.id.profileImage)

        // Set up Toolbar
        setSupportActionBar(toolbar)

        // Profile Image click to pick an image
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_OPEN_DOCUMENT)
        }

        // Set up Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(homeFragment)
                    true
                }
                R.id.nav_browse -> {
                    loadFragment(plannedTripsFragment)
                    true
                }
                R.id.nav_rent -> {
                    loadFragment(rentFragment)
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(profileFragment)
                    true
                }
                else -> false
            }
        }

        handleIntent(intent)

        if (savedInstanceState == null) {
            loadFragment(homeFragment)
            bottomNavigationView.selectedItemId = R.id.nav_home
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.getStringExtra("navigation_destination")) {
            "browse" -> navigateToTab(R.id.nav_browse)
            "rent" -> navigateToTab(R.id.nav_rent)
            "profile" -> navigateToTab(R.id.nav_profile)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        if (currentFragment != null && currentFragment!!::class.java == fragment::class.java) {
            return
        }

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        fragmentTransaction.replace(R.id.fragment_container, fragment)
        fragmentTransaction.commit()

        currentFragment = fragment
    }

    fun navigateToTab(tabId: Int) {
        bottomNavigationView.selectedItemId = tabId
    }

    override fun onBackPressed() {
        if (currentFragment !is HomeFragment) {
            navigateToTab(R.id.nav_home)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfileImage()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear any ViewPager2 adapters to prevent FragmentStateAdapter crashes
        currentFragment?.let { fragment ->
            if (fragment is UserPlannedTripsFragment) {
                // The fragment will handle its own adapter clearing in onDestroyView
            }
        }
    }


    private fun loadProfileImage() {
        val user = firebaseAuth.currentUser
        if (user != null) {
            val userId = user.uid
            val userRef = firebaseDatabase.reference.child("users").child(userId)
            
            // First check if we have a cached image URL in SharedPreferences
            // This provides immediate feedback while we wait for Firebase
            val sharedPrefs = getSharedPreferences("profile_prefs", 0)
            val cachedImageUrl = sharedPrefs.getString("profile_image_url", null)
            
            if (!cachedImageUrl.isNullOrEmpty()) {
                // Load from cache immediately for better UX
                com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.loadProfileImage(profileImage, cachedImageUrl)
                Log.d("MainActivity", "Loaded profile image from cache while waiting for Firebase")
            } else {
                // No cached image, set default placeholder
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }
            
            // Then check Firebase for the most up-to-date image URL
            userRef.get().addOnSuccessListener { snapshot ->
                // Use a consistent field name - profileImageUrl (matching ProfileActivity/Fragment)
                var imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                
                // If not found, try the alternate field name for backward compatibility
                if (imageUrl.isNullOrEmpty()) {
                    imageUrl = snapshot.child("profile_image_url").getValue(String::class.java)
                }
                
                // Load the image if URL is available and different from cached URL
                if (!imageUrl.isNullOrEmpty() && imageUrl != cachedImageUrl) {
                    com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.loadProfileImage(profileImage, imageUrl)
                    Log.d("MainActivity", "Updated profile image from Firebase: $imageUrl")
                    
                    // Update the cache with the latest URL
                    sharedPrefs.edit()
                        .putString("profile_image_url", imageUrl)
                        .putLong("last_updated", System.currentTimeMillis())
                        .apply()
                } else if (imageUrl.isNullOrEmpty() && cachedImageUrl.isNullOrEmpty()) {
                    // No image found anywhere, use default
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }.addOnFailureListener { e ->
                Log.e("MainActivity", "Failed to load profile data from Firebase", e)
                
                // We already tried to load from cache at the beginning,
                // so no need to do it again here
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                Log.d("MainActivity", "Selected image URI: $uri")
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                        try {
                            contentResolver.takePersistableUriPermission(uri, takeFlags)
                        } catch (_: SecurityException) {}
                    }
                    if (!isUriValid(uri)) {
                        Toast.makeText(this, "Failed to upload profile image. The selected file is invalid or inaccessible.", Toast.LENGTH_LONG).show()
                        return@let
                    }
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId == null) {
                        Toast.makeText(this, "You must be logged in to update your profile image", Toast.LENGTH_SHORT).show()
                        return@let
                    }
                    
                    // Show a progress dialog
                    val progressDialog = android.app.ProgressDialog(this).apply {
                        setTitle("Processing Profile Image")
                        setMessage("Please wait...")
                        setCancelable(false)
                        show()
                    }
                    
                    // Use the ProfileImageManager to save the image to Firebase Realtime Database
                    com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.saveProfileImageToDatabase(
                        this,
                        uri,
                        userId,
                        onSuccess = { dbUrl ->
                            try {
                                if (progressDialog.isShowing) progressDialog.dismiss()
                                
                                // Save to SharedPreferences immediately for faster access next time
                                val sharedPrefs = getSharedPreferences("profile_prefs", 0)
                                sharedPrefs.edit()
                                    .putString("profile_image_uri", uri.toString())
                                    .putString("profile_image_url", dbUrl)
                                    .putLong("last_updated", System.currentTimeMillis())
                                    .apply()
                                
                                // Load the image
                                com.Travelplannerfyp.travelplannerapp.utils.ProfileImageManager.loadProfileImage(profileImage, dbUrl)
                                
                                Toast.makeText(this, "Profile image updated successfully", Toast.LENGTH_SHORT).show()
                                Log.d("MainActivity", "Profile image updated and saved to database")
                                
                                // Force refresh the ProfileFragment if it's currently visible
                                if (currentFragment is ProfileFragment) {
                                    loadFragment(profileFragment)
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error in success handler", e)
                                Toast.makeText(this, "Error finalizing profile update", Toast.LENGTH_SHORT).show()
                                try { if (progressDialog.isShowing) progressDialog.dismiss() } catch (_: Exception) {}
                            }
                        },
                        onFailure = { e ->
                            try { if (progressDialog.isShowing) progressDialog.dismiss() } catch (_: Exception) {}
                            Log.e("MainActivity", "Failed to save profile image", e)
                            Toast.makeText(this, "Failed to save profile image: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in onActivityResult: ${e.message}", e)
                    Toast.makeText(this, "Failed to process selected image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isUriValid(uri: Uri): Boolean {
        return try {
            // First check if the URI scheme is supported
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE -> {
                    // For content and file URIs, try to open the input stream
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        // Check if we can read from the stream and it has content
                        if (inputStream.available() <= 0) {
                            Log.w("MainActivity", "URI exists but stream has no available bytes")
                            return false
                        }
                        
                        // Check MIME type to ensure it's an image
                        val mimeType = contentResolver.getType(uri)
                        if (mimeType == null || !mimeType.startsWith("image/")) {
                            Log.w("MainActivity", "URI does not point to an image: $mimeType")
                            return false
                        }
                        
                        true
                    } ?: run {
                        // If openInputStream returns null, the URI is invalid
                        Log.e("MainActivity", "Failed to open input stream for URI: $uri")
                        false
                    }
                }
                else -> {
                    // Unsupported URI scheme
                    Log.e("MainActivity", "Unsupported URI scheme: ${uri.scheme}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Invalid URI: ${e.message}", e)
            false
        }
    }

    // This method is no longer needed as we're using ProfileImageManager
    // to handle loading images from various sources
}
