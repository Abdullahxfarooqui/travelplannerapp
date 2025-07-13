package com.Travelplannerfyp.travelplannerapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.Travelplannerfyp.travelplannerapp.R
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.placeadapterAQ
import com.Travelplannerfyp.travelplannerapp.models.places
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener



class organizermain :AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var notificationIcon: ImageView
    private lateinit var searchContainer: CardView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: placeadapterAQ
    private lateinit var progressDialog: ProgressDialog
    private val placeList = mutableListOf<places>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_organizermain)

        // Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        notificationIcon = findViewById(R.id.notification)
        searchContainer = findViewById(R.id.search_container)
        searchEditText = findViewById(R.id.search_edit_text)
        searchIcon = findViewById(R.id.search_icon)
        recyclerView = findViewById(R.id.recommendations_recycler)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading places...")
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set up DrawerLayout and NavigationView
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        notificationIcon.setOnClickListener { showNotificationPopup(it) }

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = placeadapterAQ(placeList)
        recyclerView.adapter = adapter

        // Fetch data from local JSON file
        fetchPlacesFromJSON()

        // Set up search icon functionality
        searchIcon.setOnClickListener {
            val searchQuery = searchEditText.text.toString().trim()
            if (searchQuery.isNotEmpty()) {
                searchPlaces(searchQuery)
            }
        }
    }

    private fun fetchPlacesFromJSON() {
        progressDialog.show()
        
        Log.d("organizermain", "Starting to fetch places from Firebase")
        
        // Reference to the places node in Firebase Realtime Database
        val databaseReference = FirebaseDatabase.getInstance().reference.child("places")
        
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val newPlaceList = mutableListOf<places>()
                    Log.d("organizermain", "Received data snapshot with ${snapshot.childrenCount} places")
                    
                    for (placeSnapshot in snapshot.children) {
                        val placeKey = placeSnapshot.key ?: continue // Skip if key is null
                        val name = placeSnapshot.child("name").getValue(String::class.java) ?: ""
                        val description = placeSnapshot.child("description").getValue(String::class.java) ?: ""
                        
                        // Improved image URL handling with clear priority order
                        var imageUrl: String? = null
                        
                        // Priority 1: Check for direct imageUrl field (highest priority)
                        val directUrl = placeSnapshot.child("imageUrl").getValue(String::class.java)
                        if (!directUrl.isNullOrEmpty()) {
                            imageUrl = directUrl
                            Log.d("organizermain", "Found direct imageUrl: $imageUrl")
                        }
                        
                        // Priority 2: Check for image field with direct Base64 data
                        else if (placeSnapshot.hasChild("image")) {
                            // Format as database reference to the image field
                            imageUrl = "db://places/$placeKey/image"
                            Log.d("organizermain", "Using database reference to image field: $imageUrl")
                        }
                        
                        // Priority 3: Check for image/data nested structure
                        else if (placeSnapshot.hasChild("image/data")) {
                            imageUrl = "db://places/$placeKey/image/data"
                            Log.d("organizermain", "Using database reference to image/data field: $imageUrl")
                        }
                        
                        // Priority 4: Use a generic database reference as fallback
                        else {
                            // Format the image URL to use the database path format
                            imageUrl = "db://places/$placeKey"
                            Log.d("organizermain", "Using generic database reference: $imageUrl")
                        }
                        
                        // Validate the URL is not empty after all attempts
                        if (imageUrl.isNullOrEmpty()) {
                            Log.w("organizermain", "No image URL found for place: $name, using placeholder")
                            imageUrl = "" // Empty string will trigger fallback in adapter
                        }
                        
                        newPlaceList.add(places(name, description, imageUrl))
                        Log.d("organizermain", "Added place: $name with image URL: $imageUrl")
                    }
                    
                    // If no places found in database, fall back to local JSON
                    if (newPlaceList.isEmpty()) {
                        Log.d("organizermain", "No places found in database, falling back to local JSON")
                        loadPlacesFromLocalJSON()
                    } else {
                        Log.d("organizermain", "Updating adapter with ${newPlaceList.size} places from database")
                        placeList.clear()
                        placeList.addAll(newPlaceList)
                        adapter.updatePlaceList(newPlaceList)
                        adapter.notifyDataSetChanged()
                    }
                } catch (e: Exception) {
                    Log.e("DatabaseError", "Error loading places from database: ${e.message}")
                    e.printStackTrace()
                    // Fall back to local JSON if database fails
                    loadPlacesFromLocalJSON()
                }
                progressDialog.dismiss()
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("DatabaseError", "Database operation cancelled: ${error.message}")
                // Fall back to local JSON if database operation is cancelled
                loadPlacesFromLocalJSON()
                progressDialog.dismiss()
            }
        })
    }
    
    private fun loadPlacesFromLocalJSON() {
        try {
            val json = loadJSONFromAsset()
            val placesJSON = JSONObject(json)
            val newPlaceList = mutableListOf<places>()

            placesJSON.keys().forEach { key ->
                val placeObject = placesJSON.getJSONObject(key)
                val name = placeObject.getString("name")
                val description = placeObject.getString("description")
                
                // Get the original image URL from the JSON
                val originalImageUrl = placeObject.getString("imageUrl")
                
                // If the URL is a web URL, use it directly
                // Otherwise, format it to use the database path format
                val imageUrl = if (originalImageUrl.startsWith("http")) {
                    originalImageUrl
                } else {
                    "db://places/$key/image"
                }
                
                newPlaceList.add(places(name, description, imageUrl))
                Log.d("organizermain", "Added place from JSON: $name with image URL: $imageUrl")
            }

            adapter.updatePlaceList(newPlaceList)
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e("JSONError", "Error loading JSON: ${e.message}")
        }
    }
    
    /**
     * Normalize place name to match the format used in drawable resources
     * This helps with local image fallback when Firebase images can't be loaded
     * @param placeName The original place name
     * @return Normalized place name for resource matching
     */
    private fun normalizePlaceName(placeName: String): String {
        return placeName.toLowerCase()
            .replace("valley", "")
            .replace("tour", "")
            .replace("kaghan", "")
            .replace("meadows", "meadows")
            .replace(" ", "")
            .trim()
    }

    private fun loadJSONFromAsset(): String {
        return try {
            val inputStream: InputStream = assets.open("places.json")
            inputStream.bufferedReader().use { it.readText() }
        } catch (ex: IOException) {
            ex.printStackTrace()
            ""
        }
    }

    private fun showNotificationPopup(anchorView: View) {
        val popupView = LayoutInflater.from(this).inflate(R.layout.notification_popup, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.showAsDropDown(anchorView, -30, 10)
    }

    private fun searchPlaces(query: String) {
        if (query.isEmpty()) {
            adapter.updatePlaceList(placeList)
        } else {
            val filteredList = placeList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
            adapter.updatePlaceList(filteredList)
        }
        adapter.notifyDataSetChanged()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.nav_profile -> startActivity(Intent(this, profile::class.java))
                R.id.nav_trips_planned -> startActivity(Intent(this, PlannedTripsTabbedActivity::class.java))
                R.id.nav_plan_trip -> startActivity(Intent(this, PlanTripActivity::class.java))
                R.id.nav_logout -> logoutUser()
                R.id.nav_switch_user -> switchToUser()
            }
        } catch (e: Exception) {
            Log.e("NavigationError", "Error in navigation: ${e.message}")
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        FirebaseAuth.getInstance().signOut()
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().remove("SelectedRole").apply()
        progressDialog.show()

        Handler().postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            progressDialog.dismiss()
            finish()
        }, 2000)
    }

    private fun switchToUser() {
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().remove("SelectedRole").apply()

        // Show toast message
        Toast.makeText(this@organizermain, "Switching to user...", Toast.LENGTH_SHORT).show()

        // Show progress dialog
        progressDialog.setMessage("Switching role...")
        progressDialog.show()

        // Post the transition with delay so that toast and dialog appear first
        Handler().postDelayed({
            progressDialog.dismiss()

            val intent = Intent(this@organizermain, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 2000) // 2 seconds buffer
    }

}

