package com.example.travelplannerapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var tripAdapter: TripAdapter
    private lateinit var availableAdapter: AvailableAdapter

    private lateinit var tripRecyclerView: RecyclerView
    private lateinit var availableRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var profileButton: ImageView
    private lateinit var notificationIcon: ImageView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var bottomNavigationView: BottomNavigationView

    private val tripList = mutableListOf<Trip>()
    private val availableList = mutableListOf<Trip>()
    private val notifications = mutableListOf<String>() // For storing notifications

    private val originalTripList = mutableListOf<Trip>()
    private val originalAvailableList = mutableListOf<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_bottom_nav)

        searchView = findViewById(R.id.searchView)
        tripRecyclerView = findViewById(R.id.recommendedrecyclerView)
        availableRecyclerView = findViewById(R.id.availableRecyclerView)
        profileButton = findViewById(R.id.profileImage)
        notificationIcon = findViewById(R.id.notificationIcon)
        toolbar = findViewById(R.id.toolbar)
        bottomNavigationView = findViewById(R.id.bottomNavigation)

        // Setup Toolbar
        setSupportActionBar(toolbar)

        // Setup Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home screen
                    true
                }
                R.id.nav_browse -> {
                    startActivity(Intent(this, PropertyBrowseActivity::class.java))
                    true
                }
                R.id.nav_rent -> {
                    startActivity(Intent(this, PropertyListingActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
        
        // Add a button to browse rental properties
        val exploreView = findViewById<ImageView>(R.id.explore)
        exploreView.setOnClickListener {
            startActivity(Intent(this, PropertyBrowseActivity::class.java))
        }

        // Profile Button Click Listener
        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Notification Icon Click Listener
        notificationIcon.setOnClickListener {
            showNotifications()
        }

        tripAdapter = TripAdapter(this, tripList)
        availableAdapter = AvailableAdapter(this, availableList)

        tripRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        availableRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        tripRecyclerView.adapter = tripAdapter
        availableRecyclerView.adapter = availableAdapter

        fetchTrips()
        fetchAvailableTrips()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterTrips(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterTrips(newText)
                return true
            }
        })
    }

    private fun fetchTrips() {
        val database = FirebaseDatabase.getInstance().getReference("trips")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tripList.clear()
                originalTripList.clear()

                for (tripSnapshot in snapshot.children) {
                    val trip = tripSnapshot.getValue(Trip::class.java)
                    trip?.let {
                        tripList.add(it)
                        originalTripList.add(it)
                    }
                }
                tripAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load trips", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchAvailableTrips() {
        val database = FirebaseDatabase.getInstance().getReference("available_trips")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                availableList.clear()
                originalAvailableList.clear()

                for (tripSnapshot in snapshot.children) {
                    val trip = tripSnapshot.getValue(Trip::class.java)
                    trip?.let {
                        availableList.add(it)
                        originalAvailableList.add(it)
                    }
                }
                availableAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load available trips", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterTrips(query: String?) {
        val filteredTrips = if (query.isNullOrBlank()) originalTripList else originalTripList.filter {
            it.name.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true)
        }
        tripAdapter.updateTrips(filteredTrips)

        val filteredAvailable = if (query.isNullOrBlank()) originalAvailableList else originalAvailableList.filter {
            it.name.contains(query, ignoreCase = true) || it.location.contains(query, ignoreCase = true)
        }
        availableAdapter.updateTrips(filteredAvailable)
    }

    private fun showNotifications() {
        if (notifications.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Notifications")
                .setMessage("No notifications")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        } else {
            val notificationList = notifications.joinToString("\n")
            AlertDialog.Builder(this)
                .setTitle("Notifications")
                .setMessage(notificationList)
                .setPositiveButton("Clear All") { dialog, _ ->
                    notifications.clear()
                    dialog.dismiss()
                }
                .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }



    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
    }

    fun updateTrip(tripId: String, newTitle: String, newLocation: String, newDescription: String) {
        val database = FirebaseDatabase.getInstance().reference.child("trips").child(tripId)

        val updatedTrip = mapOf(
            "name" to newTitle,
            "location" to newLocation,
            "description" to newDescription
        )

        database.updateChildren(updatedTrip).addOnSuccessListener {
            Toast.makeText(this, "Trip updated successfully!", Toast.LENGTH_SHORT).show()
            fetchTrips()
            fetchAvailableTrips()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update trip!", Toast.LENGTH_SHORT).show()
        }
    }
}
