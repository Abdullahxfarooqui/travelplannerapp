package com.example.travelplannerapp

import android.app.Activity
import android.content.Intent
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
import com.squareup.picasso.Picasso
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var profileImage: ShapeableImageView

    private var currentFragment: Fragment? = null

    private val homeFragment = HomeFragment()
    private val browseFragment = BrowseFragment()
    private val rentFragment = RentFragment()
    private val profileFragment = ProfileFragment()

    private val REQUEST_CODE_OPEN_DOCUMENT = 1001

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    loadFragment(browseFragment)
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
        val user = firebaseAuth.currentUser
        if (user != null) {
            val userId = user.uid
            val userProfileImageRef = firebaseDatabase.reference.child("users").child(userId).child("profile_image_url")
            userProfileImageRef.get().addOnSuccessListener { snapshot ->
                val imageUrl = snapshot.value as? String
                imageUrl?.let {
                    loadImageFromUrl(it)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_OPEN_DOCUMENT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    // Upload image to Firebase Storage
                    val storageReference = firebaseStorage.reference.child("profile_images")
                        .child(UUID.randomUUID().toString())

                    storageReference.putFile(uri).addOnSuccessListener {
                        // Get the download URL
                        storageReference.downloadUrl.addOnSuccessListener { downloadUri ->
                            val downloadUrl = downloadUri.toString()
                            // Save the image URL in Firebase Realtime Database
                            val user = firebaseAuth.currentUser
                            if (user != null) {
                                val userId = user.uid
                                val userProfileImageRef = firebaseDatabase.reference.child("users").child(userId).child("profile_image_url")
                                userProfileImageRef.setValue(downloadUrl).addOnSuccessListener {
                                    Log.d("MainActivity", "Profile image URL saved to Firebase: $downloadUrl")
                                    loadImageFromUrl(downloadUrl)
                                }
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e("MainActivity", "Permission error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadImageFromUrl(url: String) {
        Picasso.get().load(url).into(profileImage)
    }
}
