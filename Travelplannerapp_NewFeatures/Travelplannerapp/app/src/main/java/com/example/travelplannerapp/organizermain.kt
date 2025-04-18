package com.example.travelplannerapp

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplannerapp.Adapter.placeadapter
import com.example.travelplannerapp.models.places
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream



class organizermain :AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var notificationIcon: ImageView
    private lateinit var searchContainer: CardView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: placeadapter
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
        adapter = placeadapter(placeList)
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

        try {
            val json = loadJSONFromAsset()
            val placesJSON = JSONObject(json)
            val newPlaceList = mutableListOf<places>()

            // Iterate over the JSON data to extract the place details
            placesJSON.keys().forEach { key ->
                val placeObject = placesJSON.getJSONObject(key)
                val name = placeObject.getString("name")
                val description = placeObject.getString("description")
                val imageUrl = placeObject.getString("imageUrl")

                val place = places(name, description, imageUrl)
                newPlaceList.add(place)
            }

            // Update the RecyclerView adapter with the new data
            adapter.updatePlaceList(newPlaceList)
            adapter.notifyDataSetChanged()  // Notify adapter to refresh the RecyclerView

        } catch (e: Exception) {
            Log.e("JSONError", "Error loading JSON: ${e.message}")
        }

        progressDialog.dismiss()  // Dismiss the progress dialog after fetching
    }

    // Helper function to load JSON from assets folder
    private fun loadJSONFromAsset(): String {
        return try {
            val inputStream: InputStream = assets.open("places.json")  // Replace with your file name
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
        // If the query is empty, reset to the full place list
        if (query.isEmpty()) {
            adapter.updatePlaceList(placeList) // Reset to the full list
        } else {
            // Filter the list based on the search query
            val filteredList = placeList.filter {
                it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
            }
            adapter.updatePlaceList(filteredList) // Update adapter with filtered data
        }
        adapter.notifyDataSetChanged() // Refresh RecyclerView with updated data
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.nav_profile -> startActivity(Intent(this, activity_profile::class.java))
                R.id.nav_trips_planned -> startActivity(Intent(this, trips_planned::class.java))
                R.id.nav_plan_trip -> startActivity(Intent(this, plan_trip::class.java))
                R.id.nav_logout -> logoutUser()
            }
        } catch (e: Exception) {
            Log.e("NavigationError", "Error in navigation: ${e.message}")
            // Handle exception or show error message to user
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logoutUser() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Remove role from SharedPreferences
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPreferences.edit().remove("SelectedRole").apply()

        // Show Progress Dialog
        progressDialog.show()

        // Delay to simulate a logout process
        Handler().postDelayed({
            // Redirect to MainActivity after logout
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            progressDialog.dismiss()
            finish()  // Finish current activity to avoid back press returning to this screen
        }, 2000)  // Delay for 2 seconds
    }
}